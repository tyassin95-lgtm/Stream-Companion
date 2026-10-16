'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');
const log = require('./logger');
const { run } = require('./probe');
const { LADDER, AUDIO_KBPS, SEGMENT_SECONDS, variantsFor } = require('./playback');

const SEG = SEGMENT_SECONDS;
const MAX_AHEAD = 10;        // segments the encoder may run ahead of the player
const IDLE_MS = 3 * 60 * 1000;

/**
 * On-demand HLS transcoding.
 *
 * The playlist is a fixed-length VOD playlist built from the duration alone, so the player can
 * seek anywhere immediately. Segments are produced by an ffmpeg job that is (re)started at the
 * requested position whenever the player jumps; the job pauses (is stopped) once it is far
 * enough ahead of the player and resumes on demand.
 */
class TranscodeManager {
  constructor({ ffmpeg, dir, store }) {
    this.ffmpeg = ffmpeg;
    this.dir = dir || path.join(os.tmpdir(), 'stream-companion-hls');
    this.store = store;
    this.sessions = new Map();
    this.encoder = { name: 'libx264', hw: false };
    this.hasZscale = false;
    fs.rmSync(this.dir, { recursive: true, force: true });
    fs.mkdirSync(this.dir, { recursive: true });
    this.sweeper = setInterval(() => this._sweep(), 30000);
    this.sweeper.unref();
  }

  /** Detects hardware H.264 encoders and tone-mapping filters (runs once, in the background). */
  async detect() {
    try {
      const filters = (await run(this.ffmpeg, ['-hide_banner', '-filters'], { timeout: 15000 })).toString();
      this.hasZscale = /\bzscale\b/.test(filters) && /\btonemap\b/.test(filters);
    } catch { /* keep defaults */ }
    if (this.store.get('hwAccel') === 'off') return this.encoder;
    for (const name of ['h264_nvenc', 'h264_qsv', 'h264_amf']) {
      try {
        await run(this.ffmpeg, ['-hide_banner', '-loglevel', 'error', '-f', 'lavfi', '-i', 'color=c=black:s=1280x720:d=0.5:r=25',
          '-frames:v', '5', '-c:v', name, '-f', 'null', '-'], { timeout: 15000 });
        this.encoder = { name, hw: true };
        log.info(`Hardware encoder available: ${name}`);
        break;
      } catch { /* not available on this machine */ }
    }
    return this.encoder;
  }

  // ------------------------------------------------------------ playlists
  masterPlaylist(video, { maxHeight = 0, audio = 0 } = {}) {
    const vh = video.info.video ? video.info.video.height : 720;
    const rungs = variantsFor(vh, maxHeight);
    const lines = ['#EXTM3U', '#EXT-X-VERSION:3', '#EXT-X-INDEPENDENT-SEGMENTS'];
    const vw = video.info.video ? video.info.video.width : 1280;
    for (const r of rungs) {
      const w = Math.max(2, Math.round(((vw / Math.max(vh, 1)) * r.height) / 2) * 2);
      const bw = (r.videoKbps + AUDIO_KBPS) * 1000;
      lines.push(`#EXT-X-STREAM-INF:BANDWIDTH=${bw},AVERAGE-BANDWIDTH=${Math.round(bw * 0.85)},RESOLUTION=${w}x${r.height},CODECS="avc1.640028,mp4a.40.2",NAME="${r.label}"`);
      lines.push(`v${r.height}/index.m3u8?audio=${audio}`);
    }
    return lines.join('\n') + '\n';
  }

  mediaPlaylist(video, audio = 0) {
    const dur = video.info.duration;
    const n = Math.max(1, Math.ceil(dur / SEG));
    const lines = ['#EXTM3U', '#EXT-X-VERSION:3', `#EXT-X-TARGETDURATION:${SEG}`, '#EXT-X-MEDIA-SEQUENCE:0', '#EXT-X-PLAYLIST-TYPE:VOD', '#EXT-X-INDEPENDENT-SEGMENTS'];
    for (let i = 0; i < n; i++) {
      const d = i === n - 1 ? dur - SEG * (n - 1) : SEG;
      lines.push(`#EXTINF:${Math.max(d, 0.01).toFixed(3)},`);
      lines.push(`seg_${i}.ts?audio=${audio}`);
    }
    lines.push('#EXT-X-ENDLIST');
    return lines.join('\n') + '\n';
  }

  // ------------------------------------------------------------ segments
  _session(video, height, audio) {
    const key = `${video.id}|${height}|${audio}`;
    let s = this.sessions.get(key);
    if (!s) {
      const dir = path.join(this.dir, `${video.id}_${height}_${audio}`);
      fs.mkdirSync(dir, { recursive: true });
      s = { key, video, height, audio, dir, job: null, lastReq: 0, lastIdx: 0, lastUse: Date.now() };
      this.sessions.set(key, s);
    }
    s.lastUse = Date.now();
    return s;
  }

  segCount(video) { return Math.max(1, Math.ceil(video.info.duration / SEG)); }

  /** Returns the path of segment `idx`, waiting for the encoder if necessary. */
  async segment(video, height, audio, idx) {
    if (idx < 0 || idx >= this.segCount(video)) throw Object.assign(new Error('no such segment'), { status: 404 });
    const s = this._session(video, height, audio);
    s.lastIdx = idx;
    const file = path.join(s.dir, `seg_${idx}.ts`);
    if (fs.existsSync(file)) { this._tick(s); return file; }

    const j = s.job;
    const needsStart = !j || j.done || idx < j.start || idx > j.nextExpected() + 2;
    if (needsStart) this._start(s, idx);

    const deadline = Date.now() + 90000;
    while (Date.now() < deadline) {
      if (fs.existsSync(file)) { this._tick(s); return file; }
      if (s.job && s.job.done && !fs.existsSync(file)) {
        if (s.job.failed) throw Object.assign(new Error('transcoding failed'), { status: 500 });
        // job ended before producing this segment (e.g. stopped for throttling): restart
        if (!s.job.restarted) { s.job.restarted = true; this._start(s, idx); } else break;
      }
      await new Promise(r => setTimeout(r, 80));
    }
    throw Object.assign(new Error('transcode timeout'), { status: 504 });
  }

  _tick(s) {
    // Stop the encoder once it is comfortably ahead, and trim segments far behind the player.
    const j = s.job;
    if (j && !j.done && j.nextExpected() - s.lastIdx > MAX_AHEAD) this._stop(s);
    try {
      for (const f of fs.readdirSync(s.dir)) {
        const m = /^seg_(\d+)\.ts$/.exec(f);
        if (m && Number(m[1]) < s.lastIdx - 40) fs.unlink(path.join(s.dir, f), () => {});
      }
    } catch { /* directory may be gone */ }
  }

  _stop(s) {
    if (s.job && !s.job.done) {
      s.job.stopping = true;
      s.job.proc.kill('SIGKILL');
    }
  }

  buildArgs(video, height, audio, startIdx, outDir) {
    const info = video.info;
    const rung = LADDER.find(r => r.height === height) || { height, videoKbps: 1400 };
    const t = startIdx * SEG;
    const srcH = info.video.height || height;
    const targetH = Math.min(height, srcH);
    // Never upscale; keep even dimensions.
    let vf = `scale=-2:${targetH % 2 ? targetH - 1 : targetH}`;
    if (info.video.hdr && this.hasZscale) {
      vf = 'zscale=t=linear:npl=100,format=gbrpf32le,zscale=p=bt709,tonemap=tonemap=hable:desat=0,zscale=t=bt709:m=bt709:r=tv,' + vf;
    }
    vf += ',format=yuv420p';
    const kbps = Math.round(rung.videoKbps * Math.min(1, (targetH / rung.height) ** 1.0));
    const enc = this.encoder;
    const vArgs = ['-c:v', enc.name];
    if (enc.name === 'libx264') vArgs.push('-preset', 'veryfast', '-profile:v', 'high', '-level', '4.1', '-crf', '23', '-sc_threshold', '0');
    else if (enc.name === 'h264_nvenc') vArgs.push('-preset', 'p3', '-profile:v', 'high', '-rc', 'vbr', '-cq', '25', '-forced-idr', '1');
    else if (enc.name === 'h264_qsv') vArgs.push('-preset', 'veryfast', '-profile:v', 'high', '-look_ahead', '0', '-forced_idr', '1');
    else if (enc.name === 'h264_amf') vArgs.push('-quality', 'speed', '-profile:v', 'high', '-rc', 'vbr_peak');
    vArgs.push('-b:v', `${kbps}k`, '-maxrate', `${Math.round(kbps * 1.3)}k`, '-bufsize', `${kbps * 2}k`,
      '-force_key_frames', `expr:gte(t,n_forced*${SEG})`);

    const hasAudio = info.audio.length > 0;
    const aIdx = Math.min(audio, Math.max(info.audio.length - 1, 0));
    const aArgs = hasAudio
      ? ['-map', `0:a:${aIdx}`, '-c:a', 'aac', '-b:a', `${AUDIO_KBPS}k`, '-ac', '2', '-af', 'aresample=async=1:first_pts=0']
      : ['-an'];

    return [
      '-hide_banner', '-loglevel', 'error', '-nostdin',
      ...(t > 0 ? ['-ss', String(t)] : []),
      '-i', video.path,
      '-map', '0:v:0', ...vArgs, '-vf', vf,
      ...aArgs,
      '-sn', '-dn', '-map_metadata', '-1',
      ...(t > 0 ? ['-output_ts_offset', String(t)] : []),
      '-f', 'hls', '-hls_time', String(SEG), '-hls_list_size', '0', '-hls_segment_type', 'mpegts',
      '-start_number', String(startIdx), '-hls_flags', 'temp_file+independent_segments',
      '-hls_segment_filename', path.join(outDir, 'seg_%d.ts'),
      path.join(outDir, `ffout_${startIdx}.m3u8`),
    ];
  }

  _start(s, idx) {
    this._stop(s);
    const args = this.buildArgs(s.video, s.height, s.audio, idx, s.dir);
    log.info(`Transcode ${path.basename(s.video.path)} @${s.height}p from segment ${idx} using ${this.encoder.name}`);
    const proc = spawn(this.ffmpeg, args, { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
    const job = {
      proc, start: idx, done: false, failed: false, stopping: false, restarted: false, err: '',
      nextExpected() {
        // first index whose file does not exist yet
        let i = this.start;
        while (fs.existsSync(path.join(s.dir, `seg_${i}.ts`))) i++;
        return i;
      },
    };
    proc.stderr.on('data', d => { if (job.err.length < 2000) job.err += d; });
    proc.on('error', e => { job.done = true; job.failed = true; log.error('ffmpeg spawn failed', e); });
    proc.on('close', code => {
      job.done = true;
      if (code !== 0 && !job.stopping) {
        job.failed = true;
        log.error(`ffmpeg exited ${code}: ${job.err.trim().slice(-600)}`);
        // A hardware encoder that fails mid-session is demoted to software.
        if (this.encoder.hw) { log.warn('Falling back to software encoding'); this.encoder = { name: 'libx264', hw: false }; }
      }
    });
    s.job = job;
    // Poll to throttle even when the player is not asking for new segments.
    clearInterval(s.timer);
    s.timer = setInterval(() => {
      if (job.done) return clearInterval(s.timer);
      this._tick(s);
    }, 1000);
    s.timer.unref();
  }

  _sweep() {
    const now = Date.now();
    for (const [key, s] of this.sessions) {
      if (now - s.lastUse > IDLE_MS) {
        this._stop(s);
        clearInterval(s.timer);
        fs.rm(s.dir, { recursive: true, force: true }, () => {});
        this.sessions.delete(key);
      }
    }
  }

  activeCount() { return [...this.sessions.values()].filter(s => s.job && !s.job.done).length; }

  /** Stop and forget every session for a video (e.g. when it is deleted). */
  forget(videoId) {
    for (const [key, s] of this.sessions) {
      if (s.video.id === videoId) {
        this._stop(s); clearInterval(s.timer);
        fs.rm(s.dir, { recursive: true, force: true }, () => {});
        this.sessions.delete(key);
      }
    }
  }

  shutdown() {
    clearInterval(this.sweeper);
    for (const s of this.sessions.values()) { this._stop(s); clearInterval(s.timer); }
    try { fs.rmSync(this.dir, { recursive: true, force: true }); } catch { /* best effort */ }
  }
}

module.exports = { TranscodeManager };
