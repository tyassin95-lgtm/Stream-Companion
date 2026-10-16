'use strict';
// Drives the real Electron UI (under Xvfb on Linux) through Playwright/CDP:
//   xvfb-run -a node test/ui/ui-smoke.js <mediaDir> <screenshotDir>
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');
const https = require('https');
const crypto = require('crypto');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

const media = path.resolve(process.argv[2]);
const shots = path.resolve(process.argv[3] || os.tmpdir());
const root = path.join(__dirname, '..', '..');
const data = fs.mkdtempSync(path.join(os.tmpdir(), 'sc-ui-'));
fs.mkdirSync(shots, { recursive: true });

const sleep = ms => new Promise(r => setTimeout(r, ms));
const assert = (c, m) => { if (!c) { console.error('FAIL:', m); cleanup(1); } else console.log('ok -', m); };
let proc;
function cleanup(code) { try { proc.kill('SIGKILL'); } catch { /* gone */ } fs.rmSync(data, { recursive: true, force: true }); process.exit(code); }

const hmac = (k, d) => crypto.createHmac('sha256', k).update(d).digest('hex');
const post = (port, p, body) => new Promise((res, rej) => {
  const r = https.request({ host: '127.0.0.1', port, path: p, method: 'POST', rejectUnauthorized: false }, x => { let b = ''; x.on('data', c => (b += c)); x.on('end', () => res(JSON.parse(b))); });
  r.on('error', rej); r.end(JSON.stringify(body));
});

(async () => {
  proc = spawn(path.join(root, 'node_modules', '.bin', 'electron'), [root, '--no-sandbox', '--disable-gpu', '--remote-debugging-port=9333'], {
    env: { ...process.env, SC_USER_DATA: data }, stdio: 'ignore',
  });
  let browser;
  for (let i = 0; i < 40 && !browser; i++) { await sleep(500); try { browser = await chromium.connectOverCDP('http://127.0.0.1:9333'); } catch { /* not up */ } }
  assert(browser, 'connected to Electron');
  const page = browser.contexts()[0].pages()[0];
  await page.setViewportSize({ width: 1120, height: 740 });
  await page.waitForSelector('.pin span');
  assert((await page.locator('.pin span').allTextContents()).join('').length === 6, 'pairing PIN shown');

  await page.click('nav button:has-text("Folders")');
  assert(await page.locator('.empty').isVisible(), 'empty folders state');
  await page.screenshot({ path: path.join(shots, 'folders-empty.png') });

  // Add a folder through the same IPC the dialog uses.
  const r = await page.evaluate(p => window.sc.addFolderPath(p), media);
  assert(r.ok, 'folder added via IPC');
  await page.waitForSelector('.item .chip:has-text("videos")');
  await page.waitForFunction(() => /[1-9]\d* videos/.test(document.querySelector('.item .chip:nth-of-type(2)')?.textContent || ''), null, { timeout: 30000 }).catch(() => {});
  await page.screenshot({ path: path.join(shots, 'folders.png') });
  const dup = await page.evaluate(p => window.sc.addFolderPath(p), media);
  assert(!dup.ok, 'duplicate folder rejected: ' + dup.error);

  // Pair a fake device and see it show up.
  const st = await page.evaluate(() => window.sc.getState());
  const nonce = crypto.randomBytes(16).toString('hex');
  const pr = await post(st.port, '/api/pair', { deviceId: 'ui-test', deviceName: 'Pixel 9 (test)', clientNonce: nonce, proof: hmac(st.pin, `sc-pair|${nonce}|ui-test|${st.fingerprint}`) });
  assert(pr.token, 'device paired');
  await page.click('nav button:has-text("Devices")');
  await page.waitForSelector('.item:has-text("Pixel 9 (test)")');
  await page.screenshot({ path: path.join(shots, 'devices.png') });
  await page.click('nav button:has-text("Settings")');
  await page.screenshot({ path: path.join(shots, 'settings.png') });
  await page.click('nav button:has-text("Overview")');
  await page.screenshot({ path: path.join(shots, 'overview.png') });
  assert(await page.locator('.stat b').first().textContent() !== '0', 'overview shows indexed videos');
  cleanup(0);
})().catch(e => { console.error(e); cleanup(1); });
