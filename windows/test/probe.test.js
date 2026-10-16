'use strict';
// Verifies that the ffmpeg-banner parser (used in the packaged Windows app, which ships no ffprobe)
// agrees with real ffprobe output on a range of containers and codecs.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawnSync } = require('child_process');
const bins = require('../src/core/binaries').resolve();
bins.ffprobe = bins.ffprobe || require('ffprobe-static').path;
const { probeWithFfprobe, parseBanner, runCapture } = require('../src/core/probe');

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'sc-probe-'));
const gen = (name, args) => {
  const f = path.join(tmp, name);
  const r = spawnSync(bins.ffmpeg, ['-v', 'error', '-y', ...args, f], { encoding: 'utf8' });
  assert.equal(r.status, 0, r.stderr);
  return f;
};

const cases = [
  ['a.mp4', ['-f', 'lavfi', '-i', 'testsrc2=s=640x360:r=30:d=2', '-f', 'lavfi', '-i', 'sine=d=2', '-c:v', 'libx264', '-c:a', 'aac', '-shortest']],
  ['b.mkv', ['-f', 'lavfi', '-i', 'testsrc2=s=1280x720:r=24:d=2', '-f', 'lavfi', '-i', 'sine=d=2', '-c:v', 'libx264', '-pix_fmt', 'yuv420p10le', '-c:a', 'ac3', '-ac', '6', '-metadata:s:a:0', 'language=eng', '-metadata:s:a:0', 'title=Surround', '-shortest']],
  ['c.avi', ['-f', 'lavfi', '-i', 'testsrc2=s=320x240:r=25:d=2', '-f', 'lavfi', '-i', 'sine=d=2', '-c:v', 'mpeg4', '-c:a', 'mp3', '-shortest']],
  ['d.webm', ['-f', 'lavfi', '-i', 'testsrc2=s=480x270:r=15:d=2', '-c:v', 'libvpx-vp9', '-an']],
  ['e.ts', ['-f', 'lavfi', '-i', 'testsrc2=s=640x360:r=25:d=2', '-f', 'lavfi', '-i', 'sine=d=2', '-c:v', 'libx264', '-c:a', 'aac', '-shortest']],
];

for (const [name, args] of cases) {
  test(`banner parser matches ffprobe for ${name}`, async () => {
    if (!bins.ffprobe) return;
    const f = gen(name, args);
    const a = await probeWithFfprobe(bins.ffprobe, f);
    const b = parseBanner(await runCapture(bins.ffmpeg, ['-hide_banner', '-i', f]), f);
    assert.equal(b.container, a.container);
    assert.ok(Math.abs(b.duration - a.duration) < 0.1, `${b.duration} vs ${a.duration}`);
    assert.equal(!!b.video, !!a.video);
    if (a.video) {
      assert.equal(b.video.codec, a.video.codec);
      assert.equal(b.video.width, a.video.width);
      assert.equal(b.video.height, a.video.height);
      assert.equal(b.video.bitDepth, a.video.bitDepth);
    }
    assert.deepEqual(b.audio.map(x => [x.codec, x.channels, x.lang, x.title]), a.audio.map(x => [x.codec, x.channels, x.lang, x.title]));
  });
}

test('banner parser reads subtitles and rejects non-media', async () => {
  const f = gen('s.mkv', ['-f', 'lavfi', '-i', 'testsrc2=s=320x240:r=10:d=2', '-f', 'lavfi', '-i', 'color=c=black:s=16x16:d=1', '-c:v', 'libx264']);
  const srt = path.join(tmp, 's.srt');
  fs.writeFileSync(srt, '1\n00:00:00,000 --> 00:00:01,000\nHi\n');
  const withSub = path.join(tmp, 'sub.mkv');
  const r = spawnSync(bins.ffmpeg, ['-v', 'error', '-y', '-i', f, '-i', srt, '-map', '0:v', '-map', '1:0', '-c', 'copy', '-metadata:s:s:0', 'language=fra', '-disposition:s:0', 'forced', withSub], { encoding: 'utf8' });
  assert.equal(r.status, 0, r.stderr);
  const info = parseBanner(await runCapture(bins.ffmpeg, ['-hide_banner', '-i', withSub]), withSub);
  assert.equal(info.subs.length, 1);
  assert.equal(info.subs[0].lang, 'fra');
  assert.equal(info.subs[0].text, true);
  assert.equal(info.subs[0].forced, true);
  const junk = path.join(tmp, 'junk.mp4');
  fs.writeFileSync(junk, 'not a video');
  assert.throws(() => parseBanner('junk.mp4: Invalid data found when processing input\n', junk));
});
