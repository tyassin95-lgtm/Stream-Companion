'use strict';
const fs = require('fs');
const path = require('path');

/**
 * Locates the bundled ffmpeg / ffprobe executables.
 * Packaged app: <resources>/bin/. Development: the npm packages. SC_FFMPEG / SC_FFPROBE override both.
 */
function resolve() {
  const exe = process.platform === 'win32' ? '.exe' : '';
  const res = process.resourcesPath;
  const candidates = {
    ffmpeg: [process.env.SC_FFMPEG, res && path.join(res, 'bin', 'ffmpeg' + exe)],
    ffprobe: [process.env.SC_FFPROBE, res && path.join(res, 'bin', 'ffprobe' + exe)],
  };
  // Development only: prefer an ffmpeg on PATH on non-Windows hosts, then the npm package.
  const onPath = process.platform !== 'win32' && require('child_process').spawnSync('ffmpeg', ['-version'], { stdio: 'ignore' }).status === 0;
  if (onPath) candidates.ffmpeg.push('ffmpeg');
  try { candidates.ffmpeg.push(require('ffmpeg-static')); } catch { /* not installed (packaged app uses resources/bin) */ }
  candidates.ffmpeg.push('ffmpeg');
  // ffprobe is optional: without it media is analysed through `ffmpeg -i` (see probe.js).
  const pick = list => list.find(p => p && (p === 'ffmpeg' || fs.existsSync(p)));
  return { ffmpeg: pick(candidates.ffmpeg), ffprobe: pick(candidates.ffprobe) };
}

module.exports = { resolve };
