#!/usr/bin/env node
'use strict';
// Downloads the 64-bit Windows ffmpeg.exe that gets bundled into the installer (build/bin/ffmpeg.exe).
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const https = require('https');
const { pipeline } = require('stream/promises');

const TAG = process.env.FFMPEG_RELEASE || 'b6.1.1';
const URL_ = `https://github.com/eugeneware/ffmpeg-static/releases/download/${TAG}/ffmpeg-win32-x64.gz`;
const OUT = path.join(__dirname, '..', 'build', 'bin', 'ffmpeg.exe');

function get(url, redirects = 8) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'stream-companion-build' } }, res => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location && redirects > 0) {
        res.resume();
        return resolve(get(new URL(res.headers.location, url).toString(), redirects - 1));
      }
      if (res.statusCode !== 200) { res.resume(); return reject(new Error(`HTTP ${res.statusCode} for ${url}`)); }
      resolve(res);
    }).on('error', reject);
  });
}

(async () => {
  if (fs.existsSync(OUT) && fs.statSync(OUT).size > 10e6 && !process.argv.includes('--force')) {
    console.log(`ffmpeg.exe already present (${(fs.statSync(OUT).size / 1e6).toFixed(0)} MB)`);
    return;
  }
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  console.log(`Downloading ${URL_}`);
  const tmp = OUT + '.part';
  await pipeline(await get(URL_), zlib.createGunzip(), fs.createWriteStream(tmp));
  const head = Buffer.alloc(2);
  const fd = fs.openSync(tmp, 'r'); fs.readSync(fd, head, 0, 2, 0); fs.closeSync(fd);
  if (head.toString() !== 'MZ') throw new Error('Downloaded file is not a Windows executable');
  fs.renameSync(tmp, OUT);
  console.log(`Saved ${OUT} (${(fs.statSync(OUT).size / 1e6).toFixed(0)} MB)`);
})().catch(e => { console.error(e.message); process.exit(1); });
