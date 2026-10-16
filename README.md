# Stream Companion

Stream the videos stored on your Windows PC to your Android phone over Wi-Fi.

| | |
|---|---|
| **Windows app** (`windows/`) | Electron + Node.js media server. Shares folders, indexes them (keeping the folder structure), watches for changes, serves videos over an authenticated HTTPS API, and converts videos on the fly (HLS) when a phone cannot play the original. |
| **Android app** (`android/`) | Kotlin + Jetpack Compose + Media3 ExoPlayer. Finds the PC automatically, shows a folder/thumbnail library and plays with resume, subtitles, speed, audio tracks and quality selection. |

Ready-to-install files are in [`dist/`](dist):

* `StreamCompanion-Setup-1.0.0.exe` – Windows installer (self-contained: Electron runtime, Node.js and ffmpeg are bundled; no .NET/Node/ffmpeg install needed).
* `StreamCompanion-1.0.0.apk` – signed Android app (Android 8.0+).

## Using it

1. **PC:** run the installer (it asks for administrator rights once, to add a Windows Firewall rule so your phone can connect). Stream Companion opens; go to **Folders → Add folder** (or drag folders onto the window).
2. **Phone:** install the APK and open the app. Your PC appears in the list (discovered via mDNS + UDP broadcast). Tap it and enter the 6-digit code from the PC's **Overview** page. That is the only setup.
3. Browse, tap a video, watch. Double-tap the sides to skip 10 s, drag the left/right edge up/down for brightness/volume.

If discovery is blocked by your router (guest networks, AP isolation), use *Connect with an address* and type the `192.168.x.x:47800` address shown on the PC.

## How it works

```
 Android (Compose, Media3)                     Windows (Electron main process)
 ┌──────────────────────┐   mDNS _streamcompanion._tcp / UDP "STREAM_COMPANION_DISCOVER"
 │ Discovery            │◄──────────────────────────────────────────────────────────┐
 │ Pairing (PIN + HMAC) │── POST /api/pair ──►  Security  (self-signed TLS, tokens)  │
 │ Library UI           │── GET  /api/browse ─►  Library  (scan, ffprobe, chokidar)   │
 │ ExoPlayer            │── GET  /api/videos/:id/stream (HTTP Range)  ► direct play   │
 │  OkHttp, cert-pinned │── GET  /hls/:id/master.m3u8 …  ► on-demand ffmpeg → HLS      │
 └──────────────────────┘── GET  …/thumb, …/subtitles/ext0.vtt ─► ffmpeg thumbnails/WebVTT
```

* **Direct play** when the phone can decode the file (it reports its hardware decoders): the original file is served with HTTP range requests, so seeking is instant and nothing is re-encoded.
* **Adaptive transcoding** otherwise (AVI, DTS/TrueHD audio, 10-bit H.264, HEVC on phones without a hardware decoder …) or when requested: the PC produces an HLS ladder (1080/720/480/360p, H.264 + AAC) lazily per segment. Seeking anywhere restarts ffmpeg at that point; HDR is tone-mapped to SDR; NVENC/QuickSync/AMF hardware encoders are used when available. Quality can be forced from the player (e.g. 480p on slow Wi-Fi), or *Always adaptive* turned on in the app settings.
* If a phone fails to decode a "compatible" file, the app automatically falls back to transcoding.
* **Security:** HTTPS only, with a per-install self-signed certificate. Pairing needs the PIN shown on the PC; the PIN is never sent – both sides exchange HMACs bound to the certificate fingerprint, so a rogue server cannot impersonate your PC and a phone cannot be tricked into trusting one. The phone pins the fingerprint and sends a 256-bit bearer token (only its hash is stored on the PC). Wrong PINs are rate-limited, the PIN changes after every pairing, and each device can be removed in the **Devices** page.
* **Change detection:** a file watcher (plus a periodic rescan) picks up new, modified, moved and deleted videos; the phone notices library revisions within a few seconds.

## Rebuilding the installers

```bash
./build.sh            # both → dist/        (./build.sh windows | android for one)
```

| Target | Requirements | Command |
|---|---|---|
| Windows `.exe` | Node.js 18+. On Windows: nothing else. On Linux/macOS: Wine (`apt install wine64 wine32:i386 xvfb`) | `windows/scripts/build-windows.sh` or `windows\scripts\build-windows.ps1` |
| Android `.apk` | JDK 17+, Android SDK (platform 35, build-tools 35.0.0) | `android/scripts/build-android.sh` |

The scripts run the automated tests first (`windows`: `npm test`; `android`: unit tests). The Android build creates a release signing key on first use (`android/signing/`, `android/keystore.properties`). **This repository ships a personal-use key so updates always install over each other; generate your own (delete both files and rebuild) before distributing the app publicly.**

### Tests

* `windows/test/integration.test.js` – end-to-end server tests: pairing/lock-out/MITM proof, auth, folder hierarchy, HTTP ranges, HLS playlists + seeking + timestamp alignment, transcoding of AVI/MPEG-4/AC-3, thumbnails, subtitles, live change detection, UDP discovery, device revocation.
* `windows/test/probe.test.js` – media analysis parser vs. ffprobe.
* `windows/test/ui/ui-smoke.js` – drives the real Electron UI through Playwright.
* `android/app/src/test` – protocol vectors shared with the server (pairing proofs), JSON contract and formatting.

## Project layout

```
windows/src/core      server, library, security, transcoding, discovery (Electron-independent; src/cli.js runs it headless)
windows/src/main      Electron main process, tray, IPC
windows/src/renderer  dashboard UI (vanilla JS, no build step)
android/app/src/main  Compose UI (ui/), networking + pairing (data/), player (ui/player)
dist/                 built installers
```

## Limitations

* Image-based subtitles (PGS/VobSub) are not converted; text subtitles (SRT/ASS/WebVTT, embedded or next to the video) are.
* The installer is not code-signed (no certificate), so Windows SmartScreen may ask you to confirm "More info → Run anyway".
