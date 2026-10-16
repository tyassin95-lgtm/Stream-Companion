'use strict';
const fs = require('fs');
const path = require('path');

let stream = null;
let echo = true;

function init(dir, { console: toConsole = true } = {}) {
  echo = toConsole;
  try {
    fs.mkdirSync(dir, { recursive: true });
    const file = path.join(dir, 'server.log');
    try { if (fs.statSync(file).size > 2 * 1024 * 1024) fs.renameSync(file, file + '.1'); } catch { /* no log yet */ }
    stream = fs.createWriteStream(file, { flags: 'a' });
  } catch { stream = null; }
}

function write(level, args) {
  const msg = args.map(a => (a instanceof Error ? a.stack || a.message : typeof a === 'string' ? a : JSON.stringify(a))).join(' ');
  const line = `${new Date().toISOString()} ${level} ${msg}`;
  if (stream) stream.write(line + '\n');
  if (echo) {
    // stdout may be an invalid handle in a packaged GUI app; logging must never throw.
    try { (level === 'ERROR' ? console.error : console.log)(line); } catch { echo = false; }
  }
}

module.exports = {
  init,
  info: (...a) => write('INFO ', a),
  warn: (...a) => write('WARN ', a),
  error: (...a) => write('ERROR', a),
  debug: (...a) => { if (process.env.SC_DEBUG) write('DEBUG', a); },
};
