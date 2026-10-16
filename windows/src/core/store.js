'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { debounce } = require('./util');

const DEFAULTS = {
  serverName: '',
  port: 47800,
  folders: [],        // { id, path, name }
  devices: [],        // { id, name, tokenHash, pairedAt, lastSeen, lastIp }
  hwAccel: 'auto',    // 'auto' | 'off'
  minimizeToTray: true,
  startWithWindows: false,
  serverEnabled: true,
};

/** Small JSON-backed settings store with atomic, debounced writes. */
class Store {
  constructor(dir, hostName = 'Windows PC') {
    this.dir = dir;
    fs.mkdirSync(dir, { recursive: true });
    this.file = path.join(dir, 'settings.json');
    let data = {};
    try { data = JSON.parse(fs.readFileSync(this.file, 'utf8')); } catch { /* first run */ }
    this.data = { ...DEFAULTS, ...data };
    if (!this.data.serverId) this.data.serverId = crypto.randomBytes(8).toString('hex');
    if (!this.data.serverName) this.data.serverName = `${hostName} · Stream Companion`;
    this._save = debounce(() => this.flush(), 300);
    this._save();
  }

  get(key) { return this.data[key]; }

  set(patch) {
    Object.assign(this.data, patch);
    this._save();
  }

  flush() {
    const tmp = this.file + '.tmp';
    try {
      fs.writeFileSync(tmp, JSON.stringify(this.data, null, 2));
      fs.renameSync(tmp, this.file);
    } catch (e) {
      require('./logger').error('Failed to save settings', e);
    }
  }
}

module.exports = { Store, DEFAULTS };
