'use strict';
const os = require('os');

const VIRTUAL = /vethernet|vmware|virtualbox|vbox|hyper-v|docker|wsl|loopback|bluetooth|tailscale|zerotier|vpn|tap-|tun/i;

/** Private IPv4 addresses of this machine, best candidates first. */
function addresses() {
  const out = [];
  for (const [name, list] of Object.entries(os.networkInterfaces())) {
    for (const a of list || []) {
      if (a.family !== 'IPv4' || a.internal) continue;
      const priv = /^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/.test(a.address);
      out.push({ name, address: a.address, score: (priv ? 2 : 0) + (VIRTUAL.test(name) ? -3 : 0) + (a.address.startsWith('192.168.') ? 1 : 0) });
    }
  }
  return out.sort((a, b) => b.score - a.score).map(({ name, address }) => ({ name, address }));
}

module.exports = { addresses };
