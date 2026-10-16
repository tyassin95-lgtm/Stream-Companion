'use strict';
const dgram = require('dgram');
const log = require('./logger');
const { PROTOCOL } = require('./server');

const MAGIC = 'STREAM_COMPANION_DISCOVER';

/**
 * Makes the server findable on the LAN, using two independent mechanisms:
 *  - mDNS / DNS-SD service "_streamcompanion._tcp"
 *  - a UDP responder (broadcast probe) for networks that block multicast
 */
class Discovery {
  constructor({ store, security }) {
    this.store = store;
    this.security = security;
    this.bonjour = null;
    this.service = null;
    this.udp = null;
  }

  _payload() {
    return {
      app: 'stream-companion',
      protocol: PROTOCOL,
      id: this.store.get('serverId'),
      name: this.store.get('serverName'),
      port: this.store.get('port'),
      fingerprint: this.security.fingerprint,
    };
  }

  start() {
    const payload = this._payload();
    try {
      const { Bonjour } = require('bonjour-service');
      this.bonjour = new Bonjour();
      this.service = this.bonjour.publish({
        name: payload.name, type: 'streamcompanion', protocol: 'tcp', port: payload.port,
        txt: { id: payload.id, fp: payload.fingerprint, v: String(PROTOCOL), app: 'stream-companion' },
      });
    } catch (e) { log.warn('mDNS unavailable', e.message); }

    try {
      const sock = dgram.createSocket({ type: 'udp4', reuseAddr: true });
      sock.on('message', (msg, rinfo) => {
        if (msg.toString('utf8').trim() !== MAGIC) return;
        const reply = Buffer.from(JSON.stringify(this._payload()));
        sock.send(reply, rinfo.port, rinfo.address);
      });
      sock.on('error', e => log.warn('UDP discovery error', e.message));
      sock.bind(payload.port, '0.0.0.0');
      this.udp = sock;
    } catch (e) { log.warn('UDP discovery unavailable', e.message); }
  }

  stop() {
    try { if (this.service) this.service.stop && this.service.stop(); } catch { /* ignore */ }
    try { if (this.bonjour) this.bonjour.destroy(); } catch { /* ignore */ }
    try { if (this.udp) this.udp.close(); } catch { /* ignore */ }
    this.bonjour = this.service = this.udp = null;
  }

  restart() { this.stop(); this.start(); }
}

module.exports = { Discovery, MAGIC };
