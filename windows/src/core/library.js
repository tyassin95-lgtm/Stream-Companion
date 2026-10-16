'use strict';
const fs = require('fs');
const path = require('path');
const { EventEmitter } = require('events');
const chokidar = require('chokidar');
const log = require('./logger');
const { probe } = require('./probe');
const { pathKey, shortId, naturalCompare, pool, debounce } = require('./util');

const VIDEO_EXT = new Set(['.mp4', '.m4v', '.mkv', '.webm', '.mov', '.avi', '.wmv', '.flv', '.ts', '.m2ts', '.mts', '.mpg', '.mpeg', '.3gp', '.ogv', '.vob', '.divx', '.asf']);
const SKIP_DIRS = new Set(['$recycle.bin', 'system volume information', 'node_modules', '@eadir', '.git']);
const SUB_EXT = new Set(['.srt', '.vtt', '.ass', '.ssa']);

const isVideo = f => VIDEO_EXT.has(path.extname(f).toLowerCase());

function prettyTitle(file) {
  let t = path.basename(file, path.extname(file));
  if (!/\s/.test(t)) t = t.replace(/[._]+/g, ' ');
  return t.replace(/\s+/g, ' ').trim() || path.basename(file);
}

/**
 * In-memory index of every video under the shared folders, kept up to date by a file watcher.
 * Folder hierarchy is preserved: browse(folderId) returns the immediate sub-folders and videos.
 */
class Library extends EventEmitter {
  constructor({ store, bins, cacheFile }) {
    super();
    this.store = store;
    this.bins = bins;
    this.cacheFile = cacheFile;
    this.videos = new Map();      // id -> video
    this.byPath = new Map();      // pathKey -> id
    this.dirs = new Map();        // folderId -> { id, path, rootId, videos:Set, dirs:Set }
    this.watchers = new Map();    // rootId -> FSWatcher
    this.status = new Map();      // rootId -> { scanning, online, error }
    this.cache = {};
    this.revision = 1;
    this.probeQueue = [];
    this.probing = 0;
    this._countCache = new Map();
    this._countRev = 0;
    try { this.cache = JSON.parse(fs.readFileSync(cacheFile, 'utf8')); } catch { /* cold start */ }
    this._saveCache = debounce(() => this._flushCache(), 3000);
    this._notify = debounce(() => this.emit('changed'), 400);
  }

  // ---------------------------------------------------------------- roots
  get roots() { return this.store.get('folders'); }

  async start() {
    for (const r of this.roots) this._startRoot(r);
    this._rescanTimer = setInterval(() => {
      for (const r of this.roots) this.scanRoot(r).catch(e => log.warn('periodic rescan failed', e));
    }, 10 * 60 * 1000);
    this._rescanTimer.unref();
  }

  async stop() {
    clearInterval(this._rescanTimer);
    await Promise.all([...this.watchers.values()].map(w => w.close().catch(() => {})));
    this.watchers.clear();
    this._flushCache();
  }

  async addRoot(dir) {
    const full = path.resolve(dir);
    const st = await fs.promises.stat(full).catch(() => null);
    if (!st || !st.isDirectory()) throw new Error('That folder does not exist.');
    const key = pathKey(full);
    for (const r of this.roots) {
      const rk = pathKey(r.path);
      if (rk === key) throw new Error('That folder is already shared.');
      if (key.startsWith(rk + path.sep)) throw new Error(`It is already included in the shared folder "${r.name}".`);
    }
    // Sub-folders of the new root that were shared separately are now redundant.
    for (const r of [...this.roots]) {
      if (pathKey(r.path).startsWith(key + path.sep)) this.removeRoot(r.id);
    }
    const root = { id: shortId(key, 8), path: full, name: path.basename(full) || full };
    this.store.set({ folders: [...this.roots, root] });
    this._startRoot(root);
    return root;
  }

  removeRoot(id) {
    const root = this.roots.find(r => r.id === id);
    if (!root) return;
    this.store.set({ folders: this.roots.filter(r => r.id !== id) });
    const w = this.watchers.get(id);
    if (w) { w.close().catch(() => {}); this.watchers.delete(id); }
    for (const v of [...this.videos.values()]) if (v.rootId === id) this._remove(v);
    this.status.delete(id);
    this._bump();
  }

  _startRoot(root) {
    this.status.set(root.id, { scanning: true, online: true, error: null });
    this.scanRoot(root).catch(e => log.error('scan failed', e)).finally(() => this._watch(root));
  }

  rescanAll() {
    return Promise.all(this.roots.map(r => this.scanRoot(r)));
  }

  // ---------------------------------------------------------------- scanning
  async scanRoot(root) {
    const st = this.status.get(root.id) || {};
    if (st.busy) return;
    this.status.set(root.id, { ...st, busy: true, scanning: true });
    this.emit('status');
    try {
      await fs.promises.access(root.path);
      const seen = new Set();
      await this._walk(root, root.path, seen);
      for (const v of [...this.videos.values()]) {
        if (v.rootId === root.id && !seen.has(v.id)) this._remove(v);
      }
      this.status.set(root.id, { scanning: false, online: true, error: null, lastScan: Date.now() });
      this._bump();
    } catch (e) {
      log.warn(`Shared folder unavailable: ${root.path}`, e.message);
      this.status.set(root.id, { scanning: false, online: false, error: 'Folder is not reachable' });
    }
    this.emit('status');
  }

  async _walk(root, dir, seen) {
    let entries;
    try { entries = await fs.promises.readdir(dir, { withFileTypes: true }); } catch (e) {
      if (dir === root.path) throw e;
      return;
    }
    const files = [];
    for (const ent of entries) {
      const full = path.join(dir, ent.name);
      if (ent.isDirectory()) {
        if (ent.name.startsWith('.') || SKIP_DIRS.has(ent.name.toLowerCase())) continue;
        await this._walk(root, full, seen);
      } else if (ent.isFile() && isVideo(ent.name) && !ent.name.startsWith('._')) {
        files.push(full);
      }
    }
    await pool(files, 8, async f => {
      const v = await this._upsert(root, f);
      if (v) seen.add(v.id);
    });
  }

  async _upsert(root, file, { force = false } = {}) {
    let st;
    try { st = await fs.promises.stat(file); } catch { return null; }
    if (!st.isFile()) return null;
    const key = pathKey(file);
    const id = this.byPath.get(key);
    const existing = id && this.videos.get(id);
    if (existing && !force && existing.size === st.size && existing.mtimeMs === Math.floor(st.mtimeMs)) return existing;

    const cached = this.cache[key];
    const fresh = cached && cached.size === st.size && cached.mtimeMs === Math.floor(st.mtimeMs);
    const v = existing || {
      id: shortId(key, 16),
      rootId: root.id,
      path: file,
      addedAt: (cached && cached.addedAt) || Date.now(),
    };
    Object.assign(v, {
      fileName: path.basename(file),
      title: prettyTitle(file),
      size: st.size,
      mtimeMs: Math.floor(st.mtimeMs),
      info: fresh ? cached.info : null,
      thumbVersion: existing ? (existing.thumbVersion || 0) + 1 : 0,
    });
    if (!existing) {
      this.videos.set(v.id, v);
      this.byPath.set(key, v.id);
      this._link(v, root);
      this.emit('added', v);
    } else {
      this.emit('modified', v);
    }
    if (!v.info) this._enqueueProbe(v);
    this._bump();
    return v;
  }

  _remove(v) {
    this.videos.delete(v.id);
    this.byPath.delete(pathKey(v.path));
    delete this.cache[pathKey(v.path)];
    this._unlink(v);
    this.emit('removed', v);
    this._bump();
  }

  _bump() {
    this.revision++;
    this._notify();
    this._saveCache();
  }

  // ---------------------------------------------------------------- probing
  _enqueueProbe(v) {
    this.probeQueue.push(v.id);
    this._pumpProbe();
  }

  _pumpProbe() {
    while (this.probing < 3 && this.probeQueue.length) {
      const v = this.videos.get(this.probeQueue.shift());
      if (!v || v.info) continue;
      this.probing++;
      probe(this.bins, v.path)
        .then(info => { v.info = info; })
        .catch(e => { log.warn(`ffprobe failed for ${v.path}: ${e.message}`); v.info = { duration: 0, bitrate: 0, container: path.extname(v.path).slice(1), video: null, audio: [], subs: [], broken: true }; })
        .finally(() => {
          this.cache[pathKey(v.path)] = { size: v.size, mtimeMs: v.mtimeMs, addedAt: v.addedAt, info: v.info };
          this.probing--;
          this.emit('probed', v);
          this._bump();
          this._pumpProbe();
        });
    }
  }

  _flushCache() {
    try {
      const tmp = this.cacheFile + '.tmp';
      fs.writeFileSync(tmp, JSON.stringify(this.cache));
      fs.renameSync(tmp, this.cacheFile);
    } catch (e) { log.warn('cache save failed', e.message); }
  }

  // ---------------------------------------------------------------- watching
  _watch(root) {
    if (this.watchers.has(root.id) || !this.roots.find(r => r.id === root.id)) return;
    const network = root.path.startsWith('\\\\');
    const w = chokidar.watch(root.path, {
      ignoreInitial: true,
      persistent: true,
      usePolling: network,
      interval: 5000,
      depth: 99,
      ignorePermissionErrors: true,
      awaitWriteFinish: { stabilityThreshold: 2500, pollInterval: 500 },
      ignored: p => {
        const base = path.basename(p);
        return base.startsWith('.') || SKIP_DIRS.has(base.toLowerCase());
      },
    });
    const onFile = f => { if (isVideo(f)) this._upsert(root, f).catch(() => {}); };
    w.on('add', onFile)
      .on('change', f => { if (isVideo(f)) this._upsert(root, f, { force: true }).catch(() => {}); })
      .on('unlink', f => {
        const id = this.byPath.get(pathKey(f));
        if (id) this._remove(this.videos.get(id));
      })
      .on('unlinkDir', d => {
        const prefix = pathKey(d) + path.sep;
        for (const v of [...this.videos.values()]) if (pathKey(v.path).startsWith(prefix)) this._remove(v);
      })
      .on('addDir', d => { if (pathKey(d) !== pathKey(root.path)) this._walk(root, d, new Set()).catch(() => {}); })
      .on('error', e => log.warn('watcher error', e.message));
    this.watchers.set(root.id, w);
  }

  // ---------------------------------------------------------------- tree
  _dir(dirPath, root) {
    const id = shortId(pathKey(dirPath));
    let d = this.dirs.get(id);
    if (!d) {
      d = { id, path: dirPath, rootId: root.id, videos: new Set(), dirs: new Set() };
      this.dirs.set(id, d);
    }
    return d;
  }

  _link(v, root) {
    let dirPath = path.dirname(v.path);
    let child = this._dir(dirPath, root);
    child.videos.add(v.id);
    v.dirId = child.id;
    while (pathKey(dirPath) !== pathKey(root.path)) {
      dirPath = path.dirname(dirPath);
      const parent = this._dir(dirPath, root);
      parent.dirs.add(child.id);
      child = parent;
    }
  }

  _unlink(v) {
    let d = this.dirs.get(v.dirId);
    if (!d) return;
    d.videos.delete(v.id);
    const root = this.roots.find(r => r.id === d.rootId);
    while (d && !d.videos.size && !d.dirs.size && (!root || pathKey(d.path) !== pathKey(root.path))) {
      this.dirs.delete(d.id);
      const parent = this.dirs.get(shortId(pathKey(path.dirname(d.path))));
      if (parent) parent.dirs.delete(d.id);
      d = parent;
    }
  }

  _collect(dir, limit, out = []) {
    for (const id of dir.videos) { if (out.length >= limit) return out; out.push(id); }
    for (const cid of dir.dirs) { if (out.length >= limit) return out; this._collect(this.dirs.get(cid), limit, out); }
    return out;
  }

  _count(dir) {
    if (this._countRev !== this.revision) { this._countCache.clear(); this._countRev = this.revision; }
    let n = this._countCache.get(dir.id);
    if (n === undefined) {
      n = dir.videos.size;
      for (const cid of dir.dirs) n += this._count(this.dirs.get(cid));
      this._countCache.set(dir.id, n);
    }
    return n;
  }

  _folderInfo(dir, name, parentId) {
    return {
      id: dir.id,
      name,
      parentId,
      videoCount: this._count(dir),
      previewIds: this._collect(dir, 4),
    };
  }

  _rootDir(root) {
    return this.dirs.get(shortId(pathKey(root.path)));
  }

  /** Lists a folder (or the list of shared folders when folderId is falsy). */
  browse(folderId) {
    if (!folderId) {
      const folders = [];
      for (const r of this.roots) {
        const d = this._rootDir(r);
        const info = d ? this._folderInfo(d, r.name, null) : { id: shortId(pathKey(r.path)), name: r.name, parentId: null, videoCount: 0, previewIds: [] };
        folders.push(info);
      }
      folders.sort((a, b) => naturalCompare(a.name, b.name));
      return { folder: { id: null, name: 'Library', parentId: null, breadcrumbs: [] }, folders, videos: [], revision: this.revision };
    }
    const dir = this.dirs.get(folderId);
    if (!dir) return null;
    const root = this.roots.find(r => r.id === dir.rootId);
    const rootKey = pathKey(root.path);
    const crumbs = [];
    for (let p = dir.path; ; p = path.dirname(p)) {
      const d = this.dirs.get(shortId(pathKey(p)));
      crumbs.unshift({ id: shortId(pathKey(p)), name: pathKey(p) === rootKey ? root.name : path.basename(p) });
      if (pathKey(p) === rootKey || !d) break;
    }
    const parentId = crumbs.length > 1 ? crumbs[crumbs.length - 2].id : null;
    const folders = [...dir.dirs].map(cid => {
      const c = this.dirs.get(cid);
      return this._folderInfo(c, path.basename(c.path), dir.id);
    }).sort((a, b) => naturalCompare(a.name, b.name));
    const videos = [...dir.videos].map(id => this.summary(this.videos.get(id)))
      .sort((a, b) => naturalCompare(a.fileName, b.fileName));
    return { folder: { id: dir.id, name: crumbs[crumbs.length - 1].name, parentId, breadcrumbs: crumbs }, folders, videos, revision: this.revision };
  }

  summary(v) {
    const i = v.info;
    return {
      id: v.id,
      title: v.title,
      fileName: v.fileName,
      size: v.size,
      duration: i ? Math.round(i.duration * 1000) / 1000 : 0,
      width: i && i.video ? i.video.width : 0,
      height: i && i.video ? i.video.height : 0,
      videoCodec: i && i.video ? i.video.codec : '',
      audioCodec: i && i.audio[0] ? i.audio[0].codec : '',
      container: i ? i.container : '',
      addedAt: v.addedAt,
      modifiedAt: v.mtimeMs,
      thumb: `/api/videos/${v.id}/thumb?v=${v.thumbVersion || 0}`,
      ready: !!i,
    };
  }

  search(q, limit = 100) {
    const terms = q.toLowerCase().split(/\s+/).filter(Boolean);
    if (!terms.length) return [];
    const out = [];
    for (const v of this.videos.values()) {
      const hay = (v.title + ' ' + path.relative(this.roots.find(r => r.id === v.rootId)?.path || '', v.path)).toLowerCase();
      if (terms.every(t => hay.includes(t))) out.push(this.summary(v));
      if (out.length >= limit) break;
    }
    return out.sort((a, b) => naturalCompare(a.title, b.title));
  }

  recent(limit = 20) {
    return [...this.videos.values()].sort((a, b) => b.addedAt - a.addedAt).slice(0, limit).map(v => this.summary(v));
  }

  get(id) { return this.videos.get(id); }

  /** Subtitle files that sit next to the video and share its base name. */
  async externalSubtitles(v) {
    const dir = path.dirname(v.path);
    const base = path.basename(v.path, path.extname(v.path)).toLowerCase();
    let names = [];
    try { names = await fs.promises.readdir(dir); } catch { return []; }
    const matches = n => {
      const low = n.toLowerCase();
      if (!SUB_EXT.has(path.extname(low)) || !low.startsWith(base)) return false;
      const rest = low.slice(base.length);
      return rest[0] === '.';   // "Movie.srt" and "Movie.en.srt" belong to "Movie.mp4"; "Movie 2.srt" does not
    };
    return names
      .filter(matches)
      .sort(naturalCompare)
      .map((n, i) => {
        const tag = path.basename(n, path.extname(n)).slice(base.length).replace(/^[.\-_ ]+/, '');
        return { n: i, file: path.join(dir, n), codec: path.extname(n).slice(1).toLowerCase(), lang: tag.split(/[.\-_ ]/)[0] || '', title: tag };
      });
  }

  stats() {
    let size = 0;
    for (const v of this.videos.values()) size += v.size;
    return {
      videos: this.videos.size,
      bytes: size,
      pendingProbe: this.probeQueue.length + this.probing,
      folders: this.roots.map(r => ({
        ...r,
        ...(this.status.get(r.id) || {}),
        videoCount: this._rootDir(r) ? this._count(this._rootDir(r)) : 0,
      })),
    };
  }
}

module.exports = { Library, isVideo, VIDEO_EXT };
