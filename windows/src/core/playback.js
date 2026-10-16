'use strict';

/** Quality ladder for transcoded (HLS) playback. */
const LADDER = [
  { height: 1080, videoKbps: 6500, label: '1080p' },
  { height: 720, videoKbps: 3200, label: '720p' },
  { height: 480, videoKbps: 1400, label: '480p' },
  { height: 360, videoKbps: 700, label: '360p' },
];
const AUDIO_KBPS = 160;
const SEGMENT_SECONDS = 6;

const DIRECT_CONTAINERS = new Set(['mp4', 'mov', 'mkv', 'webm', 'ts']);
const BASE_VIDEO = new Set(['h264', 'vp8']);
const BASE_AUDIO = new Set(['aac', 'mp3', 'opus', 'vorbis', 'flac', 'pcm_s16le', 'pcm_s24le', 'alac']);

function parseCaps(str) {
  const set = new Set();
  for (const part of String(str || '').split(/[,;\s]+/)) if (part) set.add(part.toLowerCase());
  return set;
}

/** Chooses which ladder rungs apply for a source of the given height. */
function variantsFor(sourceHeight, maxHeight) {
  const cap = maxHeight || 99999;
  let rungs = LADDER.filter(r => r.height <= cap && r.height <= Math.max(sourceHeight, 1));
  // always offer a rung at or below the source's own size
  if (!rungs.length) {
    const lowest = LADDER[LADDER.length - 1];
    rungs = [{ height: Math.min(sourceHeight || lowest.height, cap, lowest.height), videoKbps: lowest.videoKbps, label: 'source' }];
  }
  // if the source is a little taller than a rung (e.g. 1088, 800) keep the next rung up too
  const next = LADDER.slice().reverse().find(r => r.height > sourceHeight && r.height <= cap && sourceHeight > 0 && r.height / sourceHeight < 1.25);
  if (next && !rungs.find(r => r.height === next.height)) rungs.unshift(next);
  return rungs.sort((a, b) => b.height - a.height);
}

/**
 * Decides how to serve a video to a client.
 * @param {object} video   library entry (with .info)
 * @param {object} opts    { quality: 'auto'|'original'|'1080'|..., caps: Set, audio: number, adaptive: bool }
 * @returns {{mode:'direct'|'hls', reason:string, maxHeight?:number}}
 */
function decide(video, { quality = 'auto', caps = new Set(), audio = 0, adaptive = false } = {}) {
  const info = video.info;
  const canDirect = directPlayable(info, caps, audio);
  if (quality === 'original') {
    return canDirect.ok ? { mode: 'direct', reason: 'original requested' } : { mode: 'hls', reason: canDirect.reason, maxHeight: 0 };
  }
  const q = parseInt(quality, 10);
  if (q > 0) return { mode: 'hls', reason: `limited to ${q}p`, maxHeight: q };
  if (adaptive) return { mode: 'hls', reason: 'adaptive streaming', maxHeight: 0 };
  if (canDirect.ok) return { mode: 'direct', reason: 'compatible' };
  return { mode: 'hls', reason: canDirect.reason, maxHeight: 0 };
}

function directPlayable(info, caps, audioIdx) {
  if (!info || info.broken) return { ok: false, reason: 'unreadable file' };
  if (!DIRECT_CONTAINERS.has(info.container)) return { ok: false, reason: `container ${info.container} unsupported` };
  const v = info.video;
  if (!v) return { ok: false, reason: 'no video stream' };
  const vOk = BASE_VIDEO.has(v.codec) || caps.has(v.codec) || (v.codec === 'h264' && true);
  if (!vOk) return { ok: false, reason: `${v.codec} not supported by device` };
  if (v.codec === 'h264' && (v.bitDepth > 8 || /high (10|4:2:2|4:4:4)/i.test(v.profile))) return { ok: false, reason: '10-bit H.264 not supported by device' };
  if (v.rotation && Math.abs(v.rotation) % 360 !== 0 && info.container === 'ts') return { ok: false, reason: 'rotated TS' };
  const a = info.audio[audioIdx] || info.audio[0];
  if (a && !(BASE_AUDIO.has(a.codec) || caps.has(a.codec))) return { ok: false, reason: `audio ${a.codec} not supported by device` };
  if (info.audio.length > 1 && audioIdx > 0) return { ok: true, reason: 'ok' };
  return { ok: true, reason: 'ok' };
}

module.exports = { LADDER, AUDIO_KBPS, SEGMENT_SECONDS, parseCaps, variantsFor, decide, directPlayable };
