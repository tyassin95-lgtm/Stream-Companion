'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const selfsigned = require('selfsigned');
const log = require('./logger');
const { safeEqual } = require('./util');

const sha256 = s => crypto.createHash('sha256').update(s).digest('hex');
const hmac = (key, data) => crypto.createHmac('sha256', key).update(data).digest('hex');

/**
 * TLS identity + device pairing.
 *
 *  - The server owns a long-lived self-signed certificate. Clients pin its SHA-256 fingerprint.
 *  - Pairing needs the 6-digit PIN shown in the Windows app. The PIN never travels over the wire:
 *    both sides exchange HMACs keyed by the PIN and bound to the certificate fingerprint, so a
 *    look-alike server cannot complete pairing and a client cannot be tricked into trusting it.
 *  - A successful pairing yields a random 256-bit bearer token (only its hash is stored here).
 */
class Security {
  constructor(store, dir) {
    this.store = store;
    this.dir = dir;
    this.failures = [];
    this.lockedUntil = 0;
    this._loadIdentity();
    this.newPin();
  }

  _loadIdentity() {
    const certFile = path.join(this.dir, 'server-cert.pem');
    const keyFile = path.join(this.dir, 'server-key.pem');
    let cert; let key;
    try {
      cert = fs.readFileSync(certFile, 'utf8');
      key = fs.readFileSync(keyFile, 'utf8');
      if (new Date(new crypto.X509Certificate(cert).validTo) < new Date(Date.now() + 30 * 864e5)) throw new Error('expiring');
    } catch {
      log.info('Generating TLS identity');
      // Native key generation is ~100x faster than selfsigned's pure-JS one (which would freeze the UI).
      const keyPair = crypto.generateKeyPairSync('rsa', {
        modulusLength: 2048,
        publicKeyEncoding: { type: 'spki', format: 'pem' },
        privateKeyEncoding: { type: 'pkcs1', format: 'pem' },
      });
      const pems = selfsigned.generate([{ name: 'commonName', value: 'Stream Companion' }], {
        days: 3650, keyPair, algorithm: 'sha256',
        extensions: [{ name: 'subjectAltName', altNames: [{ type: 2, value: 'stream-companion.local' }] }],
      });
      cert = pems.cert; key = pems.private;
      fs.writeFileSync(certFile, cert);
      fs.writeFileSync(keyFile, key, { mode: 0o600 });
    }
    this.cert = cert;
    this.key = key;
    this.fingerprint = new crypto.X509Certificate(cert).fingerprint256.replace(/:/g, '').toLowerCase();
  }

  newPin() {
    this.pin = String(crypto.randomInt(0, 1000000)).padStart(6, '0');
    this.pinIssuedAt = Date.now();
    return this.pin;
  }

  /** Handles a pairing request. Returns { ok, status, body }. */
  pair({ deviceId, deviceName, clientNonce, proof }, ip) {
    const now = Date.now();
    if (now < this.lockedUntil) return { status: 429, body: { error: 'too_many_attempts', retryAfter: Math.ceil((this.lockedUntil - now) / 1000) } };
    if (typeof deviceId !== 'string' || typeof clientNonce !== 'string' || typeof proof !== 'string' || !deviceId || clientNonce.length < 16 || clientNonce.length > 128) {
      return { status: 400, body: { error: 'bad_request' } };
    }
    const expected = hmac(this.pin, `sc-pair|${clientNonce}|${deviceId}|${this.fingerprint}`);
    if (!safeEqual(expected, proof)) {
      this.failures = this.failures.filter(t => now - t < 10 * 60 * 1000);
      this.failures.push(now);
      if (this.failures.length % 5 === 0) this.lockedUntil = now + 60 * 1000;
      if (this.failures.length >= 10) { this.newPin(); this.failures = []; }
      log.warn(`Pairing attempt with wrong PIN from ${ip}`);
      return { status: 403, body: { error: 'invalid_pin' } };
    }
    const token = crypto.randomBytes(32).toString('base64url');
    const devices = this.store.get('devices').filter(d => d.id !== deviceId);
    const device = {
      id: deviceId,
      name: String(deviceName || 'Android device').slice(0, 60),
      tokenHash: sha256(token),
      pairedAt: now,
      lastSeen: now,
      lastIp: ip,
    };
    this.store.set({ devices: [...devices, device] });
    const serverProof = hmac(this.pin, `sc-srv|${clientNonce}|${this.fingerprint}`);
    this.failures = [];
    this.newPin();
    log.info(`Paired device "${device.name}" (${ip})`);
    return { status: 200, body: { token, serverProof, deviceId } };
  }

  /** Returns the device record for a request's bearer token, or null. */
  authenticate(req) {
    const h = req.headers.authorization || '';
    const m = /^Bearer ([\w-]{20,})$/.exec(h);
    if (!m) return null;
    const hash = sha256(m[1]);
    const dev = this.store.get('devices').find(d => safeEqual(d.tokenHash, hash));
    return dev || null;
  }

  revoke(deviceId) {
    this.store.set({ devices: this.store.get('devices').filter(d => d.id !== deviceId) });
  }
}

module.exports = { Security, hmac, sha256 };
