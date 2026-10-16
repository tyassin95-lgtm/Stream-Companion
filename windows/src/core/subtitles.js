'use strict';
const fs = require('fs');
const path = require('path');
const { run } = require('./probe');

function decodeText(buf) {
  if (buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf) return buf.slice(3).toString('utf8');
  if (buf[0] === 0xff && buf[1] === 0xfe) return buf.slice(2).toString('utf16le');
  if (buf[0] === 0xfe && buf[1] === 0xff) { const b = Buffer.from(buf.slice(2)); b.swap16(); return b.toString('utf16le'); }
  try { return new TextDecoder('utf-8', { fatal: true }).decode(buf); } catch { /* fall through */ }
  return new TextDecoder('windows-1252').decode(buf);
}

/** Converts subtitle files / embedded text tracks to WebVTT, caching the result on disk. */
class Subtitles {
  constructor({ ffmpeg, dir }) {
    this.ffmpeg = ffmpeg;
    this.dir = dir;
    this.inflight = new Map();
    fs.mkdirSync(dir, { recursive: true });
  }

  async vtt(video, key, external) {
    const m = /^(ext|emb)(\d+)$/.exec(key);
    if (!m) throw Object.assign(new Error('bad subtitle key'), { status: 404 });
    const n = Number(m[2]);
    const cacheFile = path.join(this.dir, `${video.id}_${video.mtimeMs}_${key}.vtt`);
    if (fs.existsSync(cacheFile)) return cacheFile;
    const k = cacheFile;
    if (this.inflight.has(k)) return this.inflight.get(k);
    const p = this._make(video, m[1], n, external, cacheFile).finally(() => this.inflight.delete(k));
    this.inflight.set(k, p);
    return p;
  }

  async _make(video, kind, n, external, out) {
    let text;
    if (kind === 'ext') {
      const sub = external[n];
      if (!sub) throw Object.assign(new Error('subtitle not found'), { status: 404 });
      const src = decodeText(await fs.promises.readFile(sub.file));
      if (sub.codec === 'vtt') {
        text = src.startsWith('WEBVTT') ? src : 'WEBVTT\n\n' + src;
      } else {
        const tmp = out + `.src.${sub.codec}`;
        await fs.promises.writeFile(tmp, src, 'utf8');
        try { text = (await run(this.ffmpeg, ['-hide_banner', '-loglevel', 'error', '-i', tmp, '-f', 'webvtt', '-'], { timeout: 60000 })).toString('utf8'); }
        finally { fs.promises.unlink(tmp).catch(() => {}); }
      }
    } else {
      const track = video.info.subs[n];
      if (!track || !track.text) throw Object.assign(new Error('subtitle not found'), { status: 404 });
      text = (await run(this.ffmpeg, ['-hide_banner', '-loglevel', 'error', '-i', video.path, '-map', `0:s:${n}`, '-f', 'webvtt', '-'], { timeout: 180000 })).toString('utf8');
    }
    const tmp = out + '.tmp';
    await fs.promises.writeFile(tmp, text, 'utf8');
    await fs.promises.rename(tmp, out);
    return out;
  }
}

module.exports = { Subtitles };
