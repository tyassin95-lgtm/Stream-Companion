'use strict';
const crypto = require('crypto');
const path = require('path');

const IS_WIN = process.platform === 'win32';

/** Normalised, case-folded key for a filesystem path (Windows paths are case-insensitive). */
function pathKey(p) {
  const n = path.normalize(p).replace(/[\\/]+$/, '');
  return IS_WIN ? n.toLowerCase() : n;
}

function shortId(str, len = 12) {
  return crypto.createHash('sha1').update(str).digest('hex').slice(0, len);
}

const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });
const naturalCompare = (a, b) => collator.compare(a, b);

/** Runs `worker` over items with bounded concurrency. */
async function pool(items, limit, worker) {
  let i = 0;
  const run = async () => {
    while (i < items.length) {
      const idx = i++;
      try { await worker(items[idx], idx); } catch { /* individual failures are handled by the worker */ }
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, run));
}

function debounce(fn, ms) {
  let t = null;
  const d = (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
  d.flush = (...a) => { clearTimeout(t); fn(...a); };
  return d;
}

function safeEqual(a, b) {
  const ba = Buffer.from(String(a));
  const bb = Buffer.from(String(b));
  if (ba.length !== bb.length) return false;
  return crypto.timingSafeEqual(ba, bb);
}

module.exports = { IS_WIN, pathKey, shortId, naturalCompare, pool, debounce, safeEqual };
