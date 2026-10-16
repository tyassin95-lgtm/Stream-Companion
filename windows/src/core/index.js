'use strict';
const os = require('os');
const path = require('path');
const { EventEmitter } = require('events');
const log = require('./logger');
const binaries = require('./binaries');
const { Store } = require('./store');
const { Security } = require('./security');
const { Library } = require('./library');
const { Thumbnails } = require('./thumbs');
const { TranscodeManager } = require('./transcode');
const { Subtitles } = require('./subtitles');
const { MediaServer } = require('./server');
const { Discovery } = require('./discovery');
const pkg = require('../../package.json');

/** Wires every service together. Independent of Electron so it can run headless (see cli.js). */
class Core extends EventEmitter {
  constructor(dataDir, { tmpDir } = {}) {
    super();
    this.dataDir = dataDir;
    log.init(path.join(dataDir, 'logs'), { console: !!process.env.SC_LOG_CONSOLE });
    this.bins = binaries.resolve();
    this.store = new Store(dataDir, os.hostname());
    this.security = new Security(this.store, dataDir);
    this.library = new Library({ store: this.store, bins: this.bins, cacheFile: path.join(dataDir, 'library-cache.json') });
    this.thumbs = new Thumbnails({ ffmpeg: this.bins.ffmpeg, dir: path.join(dataDir, 'thumbs'), library: this.library });
    this.transcoder = new TranscodeManager({ ffmpeg: this.bins.ffmpeg, dir: tmpDir && path.join(tmpDir, 'hls'), store: this.store });
    this.subtitles = new Subtitles({ ffmpeg: this.bins.ffmpeg, dir: path.join(dataDir, 'subs') });
    this.server = new MediaServer({ store: this.store, security: this.security, library: this.library, thumbs: this.thumbs, transcoder: this.transcoder, subtitles: this.subtitles });
    this.discovery = new Discovery({ store: this.store, security: this.security });
    this.error = null;

    const push = () => this.emit('state', this.state());
    this.library.on('changed', push);
    this.library.on('status', push);
    this.server.on('activity', push);
    this.server.on('pin-changed', push);
    this.library.on('removed', v => this.transcoder.forget(v.id));
  }

  async start() {
    log.info(`Stream Companion ${pkg.version} starting (ffmpeg: ${this.bins.ffmpeg})`);
    this.transcoder.detect().then(() => this.emit('state', this.state())).catch(() => {});
    await this.library.start();
    if (this.store.get('serverEnabled')) await this.startServer();
  }

  async startServer() {
    try {
      await this.server.start();
      this.discovery.start();
      this.error = null;
      this.store.set({ serverEnabled: true });
    } catch (e) {
      this.error = e.code === 'EADDRINUSE' ? `Port ${this.store.get('port')} is already in use by another program.` : `Could not start the server: ${e.message}`;
      log.error('server start failed', e);
    }
    this.emit('state', this.state());
  }

  async stopServer() {
    this.discovery.stop();
    await this.server.stop();
    this.store.set({ serverEnabled: false });
    this.emit('state', this.state());
  }

  async updateSettings(patch) {
    const allowed = {};
    if (typeof patch.serverName === 'string' && patch.serverName.trim()) allowed.serverName = patch.serverName.trim().slice(0, 60);
    if (Number.isInteger(patch.port) && patch.port >= 1024 && patch.port <= 65535) allowed.port = patch.port;
    if (['auto', 'off'].includes(patch.hwAccel)) allowed.hwAccel = patch.hwAccel;
    for (const k of ['minimizeToTray', 'startWithWindows']) if (typeof patch[k] === 'boolean') allowed[k] = patch[k];
    const restart = (allowed.port && allowed.port !== this.store.get('port')) || (allowed.serverName && allowed.serverName !== this.store.get('serverName'));
    this.store.set(allowed);
    if (allowed.hwAccel) this.transcoder.detect().then(() => this.emit('state', this.state()));
    if (restart && this.server.running) { await this.stopServer(); await this.startServer(); }
    this.emit('state', this.state());
  }

  state() {
    const s = this.store;
    const playing = this.server.nowPlaying();
    return {
      version: pkg.version,
      serverId: s.get('serverId'),
      serverName: s.get('serverName'),
      port: s.get('port'),
      running: this.server.running,
      error: this.error,
      addresses: require('./network').addresses(),
      pin: this.security.pin,
      fingerprint: this.security.fingerprint,
      settings: {
        hwAccel: s.get('hwAccel'),
        minimizeToTray: s.get('minimizeToTray'),
        startWithWindows: s.get('startWithWindows'),
      },
      encoder: this.transcoder.encoder.name,
      activeTranscodes: this.transcoder.activeCount(),
      library: this.library.stats(),
      devices: s.get('devices').map(d => ({
        id: d.id, name: d.name, pairedAt: d.pairedAt, lastSeen: d.lastSeen, lastIp: d.lastIp,
        online: this.server.isOnline(d.id), playing: playing[d.id] || null,
      })),
      bytesSent: this.server.bytesSent,
    };
  }

  async shutdown() {
    try { this.discovery.stop(); } catch { /* ignore */ }
    await this.server.stop().catch(() => {});
    await this.library.stop().catch(() => {});
    this.transcoder.shutdown();
    this.store.flush();
  }
}

module.exports = { Core };
