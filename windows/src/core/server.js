'use strict';
const fs = require('fs');
const https = require('https');
const path = require('path');
const { EventEmitter } = require('events');
const log = require('./logger');
const { parseCaps, decide } = require('./playback');
const pkg = require('../../package.json');

const MIME = {
  '.mp4': 'video/mp4', '.m4v': 'video/mp4', '.mov': 'video/quicktime', '.mkv': 'video/x-matroska', '.webm': 'video/webm',
  '.ts': 'video/mp2t', '.m2ts': 'video/mp2t', '.mts': 'video/mp2t', '.avi': 'video/x-msvideo', '.wmv': 'video/x-ms-wmv',
  '.flv': 'video/x-flv', '.mpg': 'video/mpeg', '.mpeg': 'video/mpeg', '.3gp': 'video/3gpp', '.ogv': 'video/ogg',
};
const PROTOCOL = 1;

class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}

/** HTTPS API + streaming server. */
class MediaServer extends EventEmitter {
  constructor({ store, security, library, thumbs, transcoder, subtitles }) {
    super();
    Object.assign(this, { store, security, library, thumbs, transcoder, subtitles });
    this.server = null;
    this.playing = new Map();   // deviceId -> { videoId, title, mode, at }
    this.seen = new Map();      // deviceId -> last activity
    this.bytesSent = 0;
  }

  get running() { return !!(this.server && this.server.listening); }

  start() {
    if (this.running) return Promise.resolve();
    const server = https.createServer({ key: this.security.key, cert: this.security.cert, minVersion: 'TLSv1.2' }, (req, res) => {
      this._handle(req, res).catch(err => this._fail(res, err));
    });
    server.requestTimeout = 0;
    server.keepAliveTimeout = 65000;
    server.headersTimeout = 30000;
    server.on('tlsClientError', () => { /* clients probing without trust: ignore */ });
    this.server = server;
    const port = this.store.get('port');
    return new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(port, '0.0.0.0', () => {
        server.off('error', reject);
        server.on('error', e => log.error('server error', e));
        log.info(`Media server listening on :${port}`);
        this._ticker = setInterval(() => this.emit('activity'), 3000);
        this._ticker.unref();
        resolve();
      });
    });
  }

  stop() {
    clearInterval(this._ticker);
    this.playing.clear();
    return new Promise(resolve => {
      if (!this.server) return resolve();
      this.server.close(() => resolve());
      this.server.closeAllConnections();
      this.server = null;
    });
  }

  nowPlaying() {
    const now = Date.now();
    const out = {};
    for (const [id, p] of this.playing) if (now - p.at < 20000) out[id] = p;
    return out;
  }

  isOnline(deviceId) { return Date.now() - (this.seen.get(deviceId) || 0) < 30000; }

  // ---------------------------------------------------------------- plumbing
  json(res, status, body, headers = {}) {
    const data = Buffer.from(JSON.stringify(body));
    res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': data.length, 'Cache-Control': 'no-store', ...headers });
    res.end(data);
  }

  text(res, status, body, type, headers = {}) {
    const data = Buffer.from(body);
    res.writeHead(status, { 'Content-Type': type, 'Content-Length': data.length, 'Cache-Control': 'no-store', ...headers });
    res.end(data);
  }

  _fail(res, err) {
    if (err.status === undefined) log.error('request failed', err);
    if (res.headersSent) { res.destroy(); return; }
    this.json(res, err.status || 500, { error: err.status ? err.message : 'internal_error' });
  }

  async _readJson(req, limit = 16 * 1024) {
    const chunks = [];
    let size = 0;
    for await (const c of req) {
      size += c.length;
      if (size > limit) throw new HttpError(413, 'payload too large');
      chunks.push(c);
    }
    try { return JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}'); } catch { throw new HttpError(400, 'invalid json'); }
  }

  ips() { return require('./network').addresses(); }

  info() {
    return {
      app: 'stream-companion',
      protocol: PROTOCOL,
      version: pkg.version,
      id: this.store.get('serverId'),
      name: this.store.get('serverName'),
      fingerprint: this.security.fingerprint,
    };
  }

  // ---------------------------------------------------------------- routing
  async _handle(req, res) {
    const url = new URL(req.url, 'https://local');
    const p = url.pathname;
    const method = req.method;
    const ip = (req.socket.remoteAddress || '').replace('::ffff:', '');

    if (method === 'GET' && p === '/api/info') return this.json(res, 200, this.info());
    if (method === 'POST' && p === '/api/pair') {
      const r = this.security.pair(await this._readJson(req), ip);
      if (r.status === 200) { this.seen.set(r.body.deviceId, Date.now()); this.emit('pin-changed'); }
      return this.json(res, r.status, r.body);
    }

    const device = this.security.authenticate(req);
    if (!device) return this.json(res, 401, { error: 'unauthorized' });
    this._touch(device, ip);

    let m;
    if (method !== 'GET' && method !== 'HEAD') throw new HttpError(405, 'method not allowed');

    if (p === '/api/browse') {
      const r = this.library.browse(url.searchParams.get('folder') || null);
      if (!r) throw new HttpError(404, 'folder not found');
      return this.json(res, 200, r);
    }
    if (p === '/api/revision') return this.json(res, 200, { revision: this.library.revision });
    if (p === '/api/search') return this.json(res, 200, { videos: this.library.search(url.searchParams.get('q') || '') });
    if (p === '/api/recent') return this.json(res, 200, { videos: this.library.recent(Number(url.searchParams.get('limit')) || 20) });

    if ((m = /^\/api\/videos\/([0-9a-f]+)(?:\/([a-z]+)(?:\/([\w.]+))?)?$/.exec(p))) {
      const v = this.library.get(m[1]);
      if (!v) throw new HttpError(404, 'video not found');
      switch (m[2]) {
        case undefined: return this.json(res, 200, await this._detail(v));
        case 'playback': return this._playback(v, url, res, device);
        case 'stream': return this._direct(v, req, res, device);
        case 'thumb': return this._thumb(v, req, res);
        case 'subtitles': return this._subtitle(v, (m[3] || '').replace(/\.vtt$/, ''), res);
        default: throw new HttpError(404, 'not found');
      }
    }

    if ((m = /^\/hls\/([0-9a-f]+)\/(.+)$/.exec(p))) {
      const v = this.library.get(m[1]);
      if (!v || !v.info || !v.info.video) throw new HttpError(404, 'video not found');
      return this._hls(v, m[2], url, res, device);
    }
    throw new HttpError(404, 'not found');
  }

  _touch(device, ip) {
    const now = Date.now();
    this.seen.set(device.id, now);
    if (now - (device.lastSeen || 0) > 60000 || device.lastIp !== ip) {
      device.lastSeen = now; device.lastIp = ip;
      this.store.set({ devices: this.store.get('devices') });
      this.emit('activity');
    }
  }

  async _detail(v) {
    const base = this.library.summary(v);
    const info = v.info || { audio: [], subs: [] };
    const ext = await this.library.externalSubtitles(v);
    return {
      ...base,
      bitrate: info.bitrate || 0,
      fps: info.video ? info.video.fps : 0,
      hdr: !!(info.video && info.video.hdr),
      audioTracks: info.audio.map(a => ({ n: a.n, codec: a.codec, channels: a.channels, lang: a.lang, title: a.title, default: a.default })),
      subtitles: [
        ...ext.map(s => ({ key: `ext${s.n}`, lang: s.lang, title: s.title || s.file && path.basename(s.file), codec: s.codec, external: true, forced: false, default: false })),
        ...info.subs.filter(s => s.text).map(s => ({ key: `emb${s.n}`, lang: s.lang, title: s.title, codec: s.codec, external: false, forced: s.forced, default: s.default })),
      ],
    };
  }

  _playback(v, url, res, device) {
    if (!v.info) throw new HttpError(503, 'still analysing this file, try again in a moment');
    const q = url.searchParams;
    const audio = Math.max(0, parseInt(q.get('audio') || '0', 10) || 0);
    const d = decide(v, { quality: q.get('quality') || 'auto', caps: parseCaps(q.get('caps')), audio, adaptive: q.get('adaptive') === '1' });
    const base = `/api/videos/${v.id}`;
    this.playing.set(device.id, { videoId: v.id, title: v.title, mode: d.mode, at: Date.now() });
    const body = d.mode === 'direct'
      ? { mode: 'direct', url: `${base}/stream`, reason: d.reason, audio }
      : { mode: 'hls', url: `/hls/${v.id}/master.m3u8?max=${d.maxHeight || 0}&audio=${audio}`, reason: d.reason, audio };
    log.info(`Playback "${v.title}" -> ${body.mode} (${d.reason}) for ${device.name}`);
    return this.json(res, 200, body);
  }

  // ---------------------------------------------------------------- direct streaming
  async _direct(v, req, res, device) {
    let st;
    try { st = await fs.promises.stat(v.path); } catch { throw new HttpError(404, 'file missing'); }
    const size = st.size;
    const type = MIME[path.extname(v.path).toLowerCase()] || 'application/octet-stream';
    const range = req.headers.range;
    let start = 0; let end = size - 1; let status = 200;
    const headers = { 'Content-Type': type, 'Accept-Ranges': 'bytes', 'Cache-Control': 'no-store', 'Last-Modified': st.mtime.toUTCString() };
    if (range) {
      const m = /^bytes=(\d*)-(\d*)$/.exec(range.trim());
      if (!m || (m[1] === '' && m[2] === '')) { res.writeHead(416, { 'Content-Range': `bytes */${size}` }); return res.end(); }
      if (m[1] === '') { start = Math.max(0, size - Number(m[2])); }
      else { start = Number(m[1]); if (m[2] !== '') end = Math.min(Number(m[2]), size - 1); }
      if (start > end || start >= size) { res.writeHead(416, { 'Content-Range': `bytes */${size}` }); return res.end(); }
      status = 206;
      headers['Content-Range'] = `bytes ${start}-${end}/${size}`;
    }
    headers['Content-Length'] = end - start + 1;
    res.writeHead(status, headers);
    if (req.method === 'HEAD' || size === 0) return res.end();
    const prev = this.playing.get(device.id);
    if (!prev || prev.videoId !== v.id) this.playing.set(device.id, { videoId: v.id, title: v.title, mode: 'direct', at: Date.now() });
    const stream = fs.createReadStream(v.path, { start, end, highWaterMark: 1 << 20 });
    stream.on('data', c => { this.bytesSent += c.length; });
    const cur = this.playing.get(device.id);
    if (cur) cur.at = Date.now();
    const keep = setInterval(() => { const c = this.playing.get(device.id); if (c && c.videoId === v.id) c.at = Date.now(); }, 5000);
    const done = () => { clearInterval(keep); stream.destroy(); };
    res.on('close', done);
    stream.on('error', () => { clearInterval(keep); res.destroy(); });
    stream.pipe(res);
  }

  // ---------------------------------------------------------------- thumbnails / subtitles
  async _thumb(v, req, res) {
    let file;
    try { file = await this.thumbs.get(v); } catch { throw new HttpError(404, 'no thumbnail'); }
    const st = await fs.promises.stat(file);
    const etag = `"${st.size}-${Math.floor(st.mtimeMs)}"`;
    const headers = { 'Content-Type': 'image/jpeg', 'Cache-Control': 'private, max-age=86400', ETag: etag };
    if (req.headers['if-none-match'] === etag) { res.writeHead(304, headers); return res.end(); }
    res.writeHead(200, { ...headers, 'Content-Length': st.size });
    if (req.method === 'HEAD') return res.end();
    fs.createReadStream(file).pipe(res);
  }

  async _subtitle(v, key, res) {
    const ext = await this.library.externalSubtitles(v);
    let file;
    try { file = await this.subtitles.vtt(v, key, ext); } catch (e) { throw e.status ? e : new HttpError(500, 'subtitle conversion failed'); }
    const data = await fs.promises.readFile(file);
    res.writeHead(200, { 'Content-Type': 'text/vtt; charset=utf-8', 'Content-Length': data.length, 'Cache-Control': 'private, max-age=3600' });
    res.end(data);
  }

  // ---------------------------------------------------------------- HLS
  async _hls(v, rest, url, res, device) {
    const audio = Math.max(0, parseInt(url.searchParams.get('audio') || '0', 10) || 0);
    let m;
    if (rest === 'master.m3u8') {
      const max = parseInt(url.searchParams.get('max') || '0', 10) || 0;
      return this.text(res, 200, this.transcoder.masterPlaylist(v, { maxHeight: max, audio }), 'application/vnd.apple.mpegurl');
    }
    if ((m = /^v(\d+)\/index\.m3u8$/.exec(rest))) {
      return this.text(res, 200, this.transcoder.mediaPlaylist(v, audio), 'application/vnd.apple.mpegurl');
    }
    if ((m = /^v(\d+)\/seg_(\d+)\.ts$/.exec(rest))) {
      const file = await this.transcoder.segment(v, Number(m[1]), audio, Number(m[2]));
      const st = await fs.promises.stat(file);
      res.writeHead(200, { 'Content-Type': 'video/mp2t', 'Content-Length': st.size, 'Cache-Control': 'no-store' });
      const cur = this.playing.get(device.id);
      this.playing.set(device.id, { videoId: v.id, title: v.title, mode: 'hls', at: Date.now(), height: Number(m[1]) });
      void cur;
      const rs = fs.createReadStream(file);
      rs.on('data', c => { this.bytesSent += c.length; });
      res.on('close', () => rs.destroy());
      return rs.pipe(res);
    }
    throw new HttpError(404, 'not found');
  }
}

module.exports = { MediaServer, PROTOCOL };
