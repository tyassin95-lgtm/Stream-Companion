'use strict';
const { spawn } = require('child_process');
const path = require('path');

function run(bin, args, { timeout = 30000, maxBuffer = 16 * 1024 * 1024 } = {}) {
  return new Promise((resolve, reject) => {
    const p = spawn(bin, args, { windowsHide: true });
    const out = [];
    let size = 0;
    let err = '';
    const timer = setTimeout(() => { p.kill('SIGKILL'); reject(new Error('timeout')); }, timeout);
    p.stdout.on('data', d => { size += d.length; if (size <= maxBuffer) out.push(d); });
    p.stderr.on('data', d => { if (err.length < 4000) err += d; });
    p.on('error', e => { clearTimeout(timer); reject(e); });
    p.on('close', code => {
      clearTimeout(timer);
      if (code === 0) resolve(Buffer.concat(out));
      else reject(new Error(`${path.basename(bin)} exited ${code}: ${err.trim().slice(-300)}`));
    });
  });
}

const TEXT_SUB_CODECS = new Set(['subrip', 'srt', 'ass', 'ssa', 'webvtt', 'mov_text', 'text', 'microdvd', 'subviewer', 'sami']);

const undToEmpty = l => (l && l !== 'und' ? l : '');

function containerOf(formatName = '', file = '') {
  const ext = path.extname(file).slice(1).toLowerCase();
  const names = formatName.split(',');
  if (names.includes('matroska')) return ext === 'webm' ? 'webm' : 'mkv';
  if (names.includes('mov') || names.includes('mp4')) return ext === 'mov' ? 'mov' : 'mp4';
  if (names.includes('mpegts')) return 'ts';
  if (names.includes('avi')) return 'avi';
  return names[0] || ext;
}

function runCapture(bin, args, timeout = 30000) {
  return new Promise((resolve, reject) => {
    const p = spawn(bin, args, { windowsHide: true });
    let err = '';
    const timer = setTimeout(() => { p.kill('SIGKILL'); reject(new Error('timeout')); }, timeout);
    p.stderr.on('data', d => { if (err.length < 400000) err += d; });
    p.stdout.resume();
    p.on('error', e => { clearTimeout(timer); reject(e); });
    p.on('close', () => { clearTimeout(timer); resolve(err); });
  });
}

const CHANNELS = { mono: 1, stereo: 2, '2.1': 3, '3.0': 3, '4.0': 4, quad: 4, '5.0': 5, '5.1': 6, '6.1': 7, '7.1': 8 };

/**
 * Parses the banner printed by `ffmpeg -i file`. Used when no ffprobe executable is bundled
 * (keeps the Windows installer ~25 MB smaller). Produces the same shape as probe().
 */
function parseBanner(text, file) {
  const lines = text.split(/\r?\n/);
  const info = { duration: 0, bitrate: 0, container: '', video: null, audio: [], subs: [] };
  const fm = /^Input #0, ([^,]+(?:,[^,]+)*?), from /m.exec(text);
  info.container = containerOf(fm ? fm[1] : '', file);
  const d = /Duration: (\d+):(\d+):(\d+(?:\.\d+)?)/.exec(text);
  if (d) info.duration = Number(d[1]) * 3600 + Number(d[2]) * 60 + Number(d[3]);
  const br = /bitrate: (\d+) kb\/s/.exec(text);
  if (br) info.bitrate = Number(br[1]) * 1000;

  let cur = null;
  for (const line of lines) {
    const sm = /^\s+Stream #0:(\d+)(?:\[0x[0-9a-f]+\])?(?:\(([^)]*)\))?: (Video|Audio|Subtitle): (.*)$/.exec(line);
    if (sm) {
      const [, idx, lang, kind, rest] = sm;
      const codec = (/^([\w.-]+)/.exec(rest) || [])[1] || '';
      const flags = { default: /\(default\)/.test(rest), forced: /\(forced\)/.test(rest) };
      if (kind === 'Video') {
        if (/\(attached pic\)/.test(rest) || info.video) { cur = null; continue; }
        const dims = /[ ,](\d{2,5})x(\d{2,5})(?=[ ,\[]|$)/.exec(rest) || [];
        const fps = /(\d+(?:\.\d+)?) fps/.exec(rest);
        const prof = /^[\w.-]+ \(([^)]*)\)/.exec(rest);
        const pix = /, (yuv\w+|gbr\w+|rgb\w*|nv\d+\w*|p0\d+\w*)(?:\(([^)]*)\))?/.exec(rest) || [];
        info.video = {
          codec, profile: prof && !/^0x/.test(prof[1]) && !/ \/ 0x/.test(prof[1]) ? prof[1] : '',
          width: Number(dims[1]) || 0, height: Number(dims[2]) || 0, fps: fps ? Number(fps[1]) : 0,
          bitDepth: /(10|12|16)(le|be)/.test(pix[1] || '') ? Number(/(10|12|16)(le|be)/.exec(pix[1])[1]) : 8,
          hdr: /smpte2084|arib-std-b67/.test(pix[2] || ''), rotation: 0,
        };
        cur = { kind, obj: info.video };
      } else if (kind === 'Audio') {
        const chm = /Hz, ([^,]+)/.exec(rest);
        let channels = 0;
        if (chm) { const c = chm[1].replace(/\(.*\)/, '').trim(); channels = CHANNELS[c] || Number((/(\d+) channels/.exec(c) || [])[1]) || 0; }
        const a = { n: info.audio.length, index: Number(idx), codec, profile: '', channels, lang: lang && lang !== 'und' ? lang : '', title: '', default: flags.default };
        info.audio.push(a);
        cur = { kind, obj: a };
      } else {
        const sub = { n: info.subs.length, index: Number(idx), codec: codec === 'subrip' || codec === 'srt' ? 'subrip' : codec, text: TEXT_SUB_CODECS.has(codec), lang: lang && lang !== 'und' ? lang : '', title: '', forced: flags.forced, default: flags.default };
        info.subs.push(sub);
        cur = { kind, obj: sub };
      }
      continue;
    }
    if (/^\s+Stream #/.test(line) || /^\S/.test(line)) { cur = null; continue; }
    if (!cur) continue;
    const tm = /^\s{4,}title\s*:\s*(.*)$/i.exec(line);
    if (tm && cur.kind !== 'Video') cur.obj.title = tm[1].trim();
    const rot = /displaymatrix: rotation of (-?[\d.]+) degrees/.exec(line);
    if (rot && cur.kind === 'Video') cur.obj.rotation = Number(rot[1]);
  }
  if (!info.video && !info.audio.length && !info.duration) throw new Error('unrecognised media file');
  return info;
}

/** Describes a media file with ffprobe when bundled, otherwise by parsing `ffmpeg -i`. */
async function probe(bins, file) {
  if (bins.ffprobe) return probeWithFfprobe(bins.ffprobe, file);
  return parseBanner(await runCapture(bins.ffmpeg, ['-hide_banner', '-i', file]), file);
}

/** Runs ffprobe and returns a compact description of the media file. */
async function probeWithFfprobe(ffprobe, file) {
  const raw = await run(ffprobe, ['-v', 'error', '-print_format', 'json', '-show_format', '-show_streams', file]);
  const j = JSON.parse(raw.toString('utf8'));
  const streams = j.streams || [];
  const fmt = j.format || {};
  const v = streams.find(s => s.codec_type === 'video' && !(s.disposition && s.disposition.attached_pic)) || null;

  const audio = streams.filter(s => s.codec_type === 'audio').map((s, n) => ({
    n,
    index: s.index,
    codec: s.codec_name,
    profile: s.profile || '',
    channels: s.channels || 0,
    lang: undToEmpty(s.tags && (s.tags.language || s.tags.LANGUAGE)),
    title: (s.tags && (s.tags.title || s.tags.TITLE)) || '',
    default: !!(s.disposition && s.disposition.default),
  }));

  const subs = streams.filter(s => s.codec_type === 'subtitle').map((s, n) => ({
    n,
    index: s.index,
    codec: s.codec_name,
    text: TEXT_SUB_CODECS.has(s.codec_name),
    lang: undToEmpty(s.tags && (s.tags.language || s.tags.LANGUAGE)),
    title: (s.tags && (s.tags.title || s.tags.TITLE)) || '',
    forced: !!(s.disposition && s.disposition.forced),
    default: !!(s.disposition && s.disposition.default),
  }));

  const duration = parseFloat(fmt.duration) || (v && parseFloat(v.duration)) || 0;
  const transfer = v && v.color_transfer;
  const bits = v ? (parseInt(v.bits_per_raw_sample, 10) || (/(10|12)le|p10|p12/.test(v.pix_fmt || '') ? 10 : 8)) : 8;

  let fps = 0;
  if (v && v.avg_frame_rate && v.avg_frame_rate !== '0/0') {
    const [a, b] = v.avg_frame_rate.split('/').map(Number);
    fps = b ? a / b : 0;
  }

  return {
    duration,
    bitrate: parseInt(fmt.bit_rate, 10) || 0,
    container: containerOf(fmt.format_name, file),
    video: v ? {
      codec: v.codec_name,
      profile: v.profile || '',
      width: v.width || 0,
      height: v.height || 0,
      fps: Math.round(fps * 100) / 100,
      bitDepth: bits,
      hdr: transfer === 'smpte2084' || transfer === 'arib-std-b67',
      rotation: Number((v.side_data_list || []).find(d => d.rotation !== undefined)?.rotation || (v.tags && v.tags.rotate) || 0),
    } : null,
    audio,
    subs,
  };
}

module.exports = { probe, probeWithFfprobe, parseBanner, runCapture, run, TEXT_SUB_CODECS };
