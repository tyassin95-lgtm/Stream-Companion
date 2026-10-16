'use strict';
const fs = require('fs');
const path = require('path');
const log = require('./logger');
const { run } = require('./probe');

/** Generates and caches JPEG thumbnails with ffmpeg, a couple at a time. */
class Thumbnails {
  constructor({ ffmpeg, dir, library }) {
    this.ffmpeg = ffmpeg;
    this.dir = dir;
    this.inflight = new Map();
    this.queue = [];
    this.active = 0;
    fs.mkdirSync(dir, { recursive: true });
    library.on('probed', v => this.prefetch(v));
    library.on('modified', v => this._drop(v));
    library.on('removed', v => this._drop(v, true));
  }

  file(v) { return path.join(this.dir, `${v.id}.jpg`); }

  _drop(v) {
    try { fs.unlinkSync(this.file(v)); } catch { /* none cached */ }
  }

  prefetch(v) {
    if (!fs.existsSync(this.file(v))) this.get(v).catch(() => {});
  }

  /** Resolves to the thumbnail path, generating it when needed. */
  get(v) {
    const f = this.file(v);
    if (fs.existsSync(f)) return Promise.resolve(f);
    if (this.inflight.has(v.id)) return this.inflight.get(v.id);
    const p = new Promise((resolve, reject) => {
      this.queue.push({ v, resolve, reject });
      this._pump();
    }).finally(() => this.inflight.delete(v.id));
    this.inflight.set(v.id, p);
    return p;
  }

  _pump() {
    while (this.active < 2 && this.queue.length) {
      const { v, resolve, reject } = this.queue.shift();
      this.active++;
      this._make(v).then(resolve, reject).finally(() => { this.active--; this._pump(); });
    }
  }

  async _make(v) {
    const out = this.file(v);
    const dur = v.info && v.info.duration;
    const at = dur > 20 ? Math.min(dur * 0.12, 120) : dur > 2 ? dur / 2 : 0;
    const tmp = out + '.tmp.jpg';
    for (const t of [at, 0]) {
      try {
        await run(this.ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', '-ss', String(t), '-i', v.path,
          '-map', '0:v:0', '-frames:v', '1', '-vf', "scale='min(640,iw)':-2", '-q:v', '5', tmp], { timeout: 45000 });
        if (fs.existsSync(tmp) && fs.statSync(tmp).size > 0) {
          fs.renameSync(tmp, out);
          return out;
        }
      } catch (e) { log.debug('thumb attempt failed', e.message); }
    }
    try { fs.unlinkSync(tmp); } catch { /* nothing */ }
    throw new Error('thumbnail unavailable');
  }
}

module.exports = { Thumbnails };
