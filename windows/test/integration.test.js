'use strict';
// End-to-end test of the server core: pairing, browsing, direct streaming, HLS transcoding,
// subtitles, thumbnails, change detection and LAN discovery. Requires ffmpeg (bundled via npm).
process.env.SC_LOG_CONSOLE = '';
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const os = require('os');
const path = require('path');
const https = require('https');
const dgram = require('dgram');
const crypto = require('crypto');
const { spawnSync } = require('child_process');
const { Core } = require('../src/core');
const bins = require('../src/core/binaries').resolve();
const ffprobeBin = require('ffprobe-static').path;

const PORT = 47890 + Math.floor(Math.random() * 50);
let core; let tmp; let media; let token; let fp;

const hmac = (k, d) => crypto.createHmac('sha256', k).update(d).digest('hex');
const sleep = ms => new Promise(r => setTimeout(r, ms));

function req(method, p, { body, headers = {}, auth = true, raw = false } = {}) {
  return new Promise((resolve, reject) => {
    const r = https.request({ host: '127.0.0.1', port: PORT, path: p, method, rejectUnauthorized: false, headers: { ...(auth && token ? { Authorization: `Bearer ${token}` } : {}), ...headers } }, res => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        const buf = Buffer.concat(chunks);
        let json = null;
        if (!raw && /json/.test(res.headers['content-type'] || '')) json = JSON.parse(buf.toString());
        resolve({ status: res.statusCode, headers: res.headers, buf, json, text: buf.toString() });
      });
    });
    r.on('error', reject);
    if (body) r.end(JSON.stringify(body)); else r.end();
  });
}

function ff(args) {
  const r = spawnSync(bins.ffmpeg, ['-v', 'error', '-y', ...args], { encoding: 'utf8' });
  if (r.status !== 0) throw new Error(r.stderr);
}

async function waitFor(fn, ms = 20000) {
  const end = Date.now() + ms;
  for (;;) {
    const v = await fn();
    if (v) return v;
    if (Date.now() > end) throw new Error('timeout waiting for condition');
    await sleep(150);
  }
}

before(async () => {
  tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'sc-test-'));
  media = path.join(tmp, 'media');
  fs.mkdirSync(path.join(media, 'Movies'), { recursive: true });
  fs.mkdirSync(path.join(media, 'Shows', 'Season 1'), { recursive: true });
  ff(['-f', 'lavfi', '-i', 'testsrc2=s=1280x720:r=30:d=26', '-f', 'lavfi', '-i', 'sine=f=440:d=26', '-c:v', 'libx264', '-preset', 'ultrafast', '-g', '60', '-c:a', 'aac', '-shortest', path.join(media, 'Movies', 'Sample.Movie.2020.mp4')]);
  fs.writeFileSync(path.join(media, 'Movies', 'Sample.Movie.2020.en.srt'), '1\n00:00:01,000 --> 00:00:03,000\nHello world\n\n');
  ff(['-f', 'lavfi', '-i', 'testsrc2=s=640x360:r=25:d=14', '-f', 'lavfi', '-i', 'sine=f=330:d=14', '-c:v', 'mpeg4', '-c:a', 'ac3', '-shortest', path.join(media, 'Shows', 'Season 1', 'S01E01 Old.avi')]);
  core = new Core(path.join(tmp, 'data'), { tmpDir: tmp });
  core.store.set({ port: PORT, serverName: 'Test Server' });
  await core.library.addRoot(media);
  await core.start();
  fp = core.security.fingerprint;
  await waitFor(() => core.library.stats().videos === 2 && core.library.stats().pendingProbe === 0);
});

after(async () => {
  await core.shutdown();
  fs.rmSync(tmp, { recursive: true, force: true });
});

test('info is public and exposes the certificate fingerprint', async () => {
  const r = await req('GET', '/api/info', { auth: false });
  assert.equal(r.status, 200);
  assert.equal(r.json.fingerprint, fp);
  assert.equal(r.json.name, 'Test Server');
  const cert = await new Promise(res => { const s = require('tls').connect({ host: '127.0.0.1', port: PORT, rejectUnauthorized: false }, () => { const c = s.getPeerCertificate(); s.end(); res(c); }); });
  assert.equal(cert.fingerprint256.replace(/:/g, '').toLowerCase(), fp);
});

test('API requires authentication', async () => {
  assert.equal((await req('GET', '/api/browse', { auth: false })).status, 401);
  assert.equal((await req('GET', '/api/browse', { auth: false, headers: { Authorization: 'Bearer ' + 'x'.repeat(43) } })).status, 401);
  assert.equal((await req('GET', '/hls/abc/master.m3u8', { auth: false })).status, 401);
});

test('pairing: wrong PIN rejected, right PIN yields token and server proof', async () => {
  const nonce = crypto.randomBytes(16).toString('hex');
  const bad = await req('POST', '/api/pair', { auth: false, body: { deviceId: 'dev1', deviceName: 'Pixel', clientNonce: nonce, proof: hmac('000000x', `sc-pair|${nonce}|dev1|${fp}`) } });
  assert.equal(bad.status, 403);
  const proof = hmac(core.security.pin, `sc-pair|${nonce}|dev1|${fp}`);
  const pin = core.security.pin;
  const ok = await req('POST', '/api/pair', { auth: false, body: { deviceId: 'dev1', deviceName: 'Pixel', clientNonce: nonce, proof } });
  assert.equal(ok.status, 200);
  assert.equal(ok.json.serverProof, hmac(pin, `sc-srv|${nonce}|${fp}`));
  token = ok.json.token;
  assert.notEqual(core.security.pin, pin, 'PIN rotates after pairing');
  assert.equal(core.state().devices[0].name, 'Pixel');
  // a proof bound to a different certificate must fail (MITM protection)
  const n2 = crypto.randomBytes(16).toString('hex');
  const mitm = await req('POST', '/api/pair', { auth: false, body: { deviceId: 'dev2', deviceName: 'X', clientNonce: n2, proof: hmac(core.security.pin, `sc-pair|${n2}|dev2|${'0'.repeat(64)}`) } });
  assert.equal(mitm.status, 403);
});

test('pairing locks out after repeated failures', async () => {
  for (let i = 0; i < 5; i++) {
    await req('POST', '/api/pair', { auth: false, body: { deviceId: 'atk', deviceName: 'a', clientNonce: 'n'.repeat(20), proof: 'bad' } });
  }
  const r = await req('POST', '/api/pair', { auth: false, body: { deviceId: 'atk', deviceName: 'a', clientNonce: 'n'.repeat(20), proof: 'bad' } });
  assert.equal(r.status, 429);
  core.security.lockedUntil = 0; core.security.failures = [];
});

test('browse preserves the folder hierarchy', async () => {
  const root = (await req('GET', '/api/browse')).json;
  assert.equal(root.folders.length, 1);
  assert.equal(root.folders[0].name, 'media');
  assert.equal(root.folders[0].videoCount, 2);
  const lvl1 = (await req('GET', '/api/browse?folder=' + root.folders[0].id)).json;
  assert.deepEqual(lvl1.folders.map(f => f.name), ['Movies', 'Shows']);
  const shows = (await req('GET', '/api/browse?folder=' + lvl1.folders[1].id)).json;
  assert.deepEqual(shows.folders.map(f => f.name), ['Season 1']);
  const season = (await req('GET', '/api/browse?folder=' + shows.folders[0].id)).json;
  assert.equal(season.videos[0].title, 'S01E01 Old');
  assert.equal(season.videos[0].height, 360);
  assert.equal(season.folder.breadcrumbs.length, 3);
  assert.equal(season.folder.parentId, shows.folder.id);
  assert.equal((await req('GET', '/api/browse?folder=nope')).status, 404);
});

const videoByName = async name => (await req('GET', '/api/search?q=' + encodeURIComponent(name))).json.videos[0];

test('direct play honours HTTP range requests', async () => {
  const v = await videoByName('Sample');
  const pb = (await req('GET', `/api/videos/${v.id}/playback?quality=auto&caps=h264,aac`)).json;
  assert.equal(pb.mode, 'direct');
  const file = fs.readFileSync(path.join(media, 'Movies', 'Sample.Movie.2020.mp4'));
  const full = await req('GET', pb.url, { raw: true });
  assert.equal(full.status, 200);
  assert.equal(full.headers['accept-ranges'], 'bytes');
  assert.ok(full.buf.equals(file));
  const part = await req('GET', pb.url, { raw: true, headers: { Range: 'bytes=100-199' } });
  assert.equal(part.status, 206);
  assert.equal(part.headers['content-range'], `bytes 100-199/${file.length}`);
  assert.ok(part.buf.equals(file.subarray(100, 200)));
  const tail = await req('GET', pb.url, { raw: true, headers: { Range: 'bytes=-50' } });
  assert.ok(tail.buf.equals(file.subarray(file.length - 50)));
  assert.equal((await req('GET', pb.url, { raw: true, headers: { Range: `bytes=${file.length + 5}-` } })).status, 416);
});

test('incompatible files switch to HLS; quality can be forced', async () => {
  const avi = await videoByName('Old');
  const pb = (await req('GET', `/api/videos/${avi.id}/playback?quality=auto&caps=h264,aac`)).json;
  assert.equal(pb.mode, 'hls');
  const mp4 = await videoByName('Sample');
  const lim = (await req('GET', `/api/videos/${mp4.id}/playback?quality=480&caps=h264`)).json;
  assert.equal(lim.mode, 'hls');
  assert.match(lim.url, /max=480/);
  const orig = (await req('GET', `/api/videos/${avi.id}/playback?quality=original&caps=h264`)).json;
  assert.equal(orig.mode, 'hls');
});

function probeTs(buf) {
  const f = path.join(tmp, 'p_' + crypto.randomBytes(4).toString('hex') + '.ts');
  fs.writeFileSync(f, buf);
  const r = spawnSync(ffprobeBin, ['-v', 'error', '-print_format', 'json', '-show_streams', '-show_format', f], { encoding: 'utf8' });
  assert.equal(r.status, 0, r.stderr);
  return JSON.parse(r.stdout);
}

test('HLS: master/variant playlists and seekable, aligned segments', async () => {
  const v = await videoByName('Sample');
  const master = await req('GET', `/hls/${v.id}/master.m3u8?max=0&audio=0`);
  assert.equal(master.status, 200);
  assert.match(master.text, /#EXT-X-STREAM-INF/);
  const variants = master.text.split('\n').filter(l => l.startsWith('v'));
  assert.ok(variants.length >= 2, 'multiple quality variants');
  const rung = variants[variants.length - 1];           // lowest quality: fastest to transcode
  const pl = await req('GET', `/hls/${v.id}/${rung}`);
  const segs = pl.text.split('\n').filter(l => l.startsWith('seg_'));
  assert.equal(segs.length, Math.ceil(26 / 6));
  assert.match(pl.text, /#EXT-X-ENDLIST/);
  const base = rung.split('/')[0];

  const s0 = await req('GET', `/hls/${v.id}/${base}/${segs[0]}`, { raw: true });
  assert.equal(s0.status, 200);
  const i0 = probeTs(s0.buf);
  const start0 = parseFloat(i0.format.start_time);
  assert.ok(Math.abs(parseFloat(i0.format.duration) - 6) < 0.5, 'segment ~6s: ' + i0.format.duration);
  assert.deepEqual(i0.streams.map(s => s.codec_name).sort(), ['aac', 'h264']);
  assert.ok(i0.streams.find(s => s.codec_type === 'video').height <= 360);

  // seek far ahead: encoder must restart at segment 3 and timestamps must stay aligned
  const s3 = await req('GET', `/hls/${v.id}/${base}/${segs[3]}`, { raw: true });
  assert.equal(s3.status, 200);
  const i3 = probeTs(s3.buf);
  const delta = parseFloat(i3.format.start_time) - start0;
  assert.ok(Math.abs(delta - 18) < 0.3, `segment 3 starts at +${delta}s (expected 18)`);
  const last = await req('GET', `/hls/${v.id}/${base}/${segs[4]}`, { raw: true });
  assert.ok(parseFloat(probeTs(last.buf).format.duration) < 3, 'short final segment');
  assert.equal((await req('GET', `/hls/${v.id}/${base}/seg_99.ts`)).status, 404);
});

test('HLS transcodes AVI/MPEG-4 + AC-3 to something playable', async () => {
  const v = await videoByName('Old');
  const pb = (await req('GET', `/api/videos/${v.id}/playback?caps=h264,aac`)).json;
  const master = await req('GET', pb.url);
  const rung = master.text.split('\n').find(l => l.startsWith('v'));
  const pl = await req('GET', `/hls/${v.id}/${rung}`);
  const seg = pl.text.split('\n').find(l => l.startsWith('seg_'));
  const s = await req('GET', `/hls/${v.id}/${rung.split('/')[0]}/${seg}`, { raw: true });
  assert.equal(s.status, 200);
  assert.deepEqual(probeTs(s.buf).streams.map(x => x.codec_name).sort(), ['aac', 'h264']);
});

test('thumbnails and subtitles', async () => {
  const v = await videoByName('Sample');
  const t = await req('GET', `/api/videos/${v.id}/thumb`, { raw: true });
  assert.equal(t.status, 200);
  assert.equal(t.headers['content-type'], 'image/jpeg');
  assert.equal(t.buf[0], 0xff);
  const detail = (await req('GET', `/api/videos/${v.id}`)).json;
  assert.equal(detail.subtitles.length, 1);
  const vtt = await req('GET', `/api/videos/${v.id}/subtitles/${detail.subtitles[0].key}.vtt`, { raw: true });
  assert.match(vtt.text, /^WEBVTT/);
  assert.match(vtt.text, /00:0?1\.000 --> 00:0?3\.000/);
  assert.match(vtt.text, /Hello world/);
});

test('library detects new, modified and deleted videos', async () => {
  const f = path.join(media, 'Movies', 'New Clip.mp4');
  ff(['-f', 'lavfi', '-i', 'testsrc2=s=320x180:r=10:d=3', '-c:v', 'libx264', '-preset', 'ultrafast', f]);
  await waitFor(() => core.library.search('New Clip').length === 1, 30000);
  await waitFor(() => core.library.search('New Clip')[0].ready);
  assert.equal(core.library.search('New Clip')[0].height, 180);
  fs.mkdirSync(path.join(media, 'Extra', 'Deep'), { recursive: true });
  fs.copyFileSync(f, path.join(media, 'Extra', 'Deep', 'Moved.mp4'));
  await waitFor(() => core.library.search('Moved').length === 1, 30000);
  const rootFolder = (await req('GET', '/api/browse')).json.folders[0];
  assert.ok((await req('GET', '/api/browse?folder=' + rootFolder.id)).json.folders.some(x => x.name === 'Extra'));
  fs.unlinkSync(f);
  await waitFor(() => core.library.search('New Clip').length === 0, 30000);
  fs.rmSync(path.join(media, 'Extra'), { recursive: true });
  await waitFor(() => core.library.search('Moved').length === 0, 30000);
  await waitFor(() => !(core.library.browse(rootFolder.id).folders.some(x => x.name === 'Extra')), 5000);
});

test('UDP broadcast discovery replies with server identity', async () => {
  const sock = dgram.createSocket('udp4');
  const reply = await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('no reply')), 3000);
    sock.on('message', m => { clearTimeout(timer); resolve(JSON.parse(m.toString())); });
    sock.send('STREAM_COMPANION_DISCOVER', PORT, '127.0.0.1');
  });
  sock.close();
  assert.equal(reply.fingerprint, fp);
  assert.equal(reply.port, PORT);
});

test('revoking a device cuts off access', async () => {
  const id = core.state().devices[0].id;
  core.security.revoke(id);
  assert.equal((await req('GET', '/api/browse')).status, 401);
});
