#!/usr/bin/env node
'use strict';
// Headless runner: `node src/cli.js --data <dir> [--folder <dir>]... [--port N]`
const path = require('path');
process.env.SC_LOG_CONSOLE = process.env.SC_LOG_CONSOLE || '1';
const { Core } = require('./core');

const args = process.argv.slice(2);
const opt = (name, all = false) => {
  const out = [];
  for (let i = 0; i < args.length; i++) if (args[i] === `--${name}`) out.push(args[i + 1]);
  return all ? out : out[0];
};

(async () => {
  const dataDir = path.resolve(opt('data') || path.join(require('os').homedir(), '.stream-companion'));
  const core = new Core(dataDir);
  if (opt('port')) core.store.set({ port: Number(opt('port')) });
  for (const f of opt('folder', true)) await core.library.addRoot(f).catch(e => console.error(e.message));
  await core.start();
  console.log(`PIN: ${core.security.pin}  fingerprint: ${core.security.fingerprint}`);
  const stop = async () => { await core.shutdown(); process.exit(0); };
  process.on('SIGINT', stop); process.on('SIGTERM', stop);
})();
