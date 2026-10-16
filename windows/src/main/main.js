'use strict';
const path = require('path');
const fs = require('fs');
const { app, BrowserWindow, Tray, Menu, nativeImage, ipcMain, dialog, shell, clipboard, Notification } = require('electron');
const { Core } = require('../core');

const hidden = process.argv.includes('--hidden');

// Last-resort crash reporting: write to a file the user can send, instead of dying silently.
function crash(err) {
  try {
    const dir = app.getPath('userData');
    fs.mkdirSync(dir, { recursive: true });
    fs.appendFileSync(path.join(dir, 'crash.log'), `${new Date().toISOString()} ${err && err.stack || err}\n`);
  } catch { /* nothing more we can do */ }
}
process.on('uncaughtException', crash);
process.on('unhandledRejection', crash);
let win = null;
let tray = null;
let core = null;
let quitting = false;

if (process.env.SC_USER_DATA) app.setPath('userData', process.env.SC_USER_DATA);

// A tray utility with a simple dashboard: software rendering is plenty and avoids GPU-driver problems.
app.disableHardwareAcceleration();

if (!app.requestSingleInstanceLock()) { app.quit(); }
app.on('second-instance', () => showWindow());
app.setAppUserModelId('com.streamcompanion.server');

const asset = name => path.join(app.isPackaged ? process.resourcesPath : path.join(__dirname, '..', '..', 'build'), name);

function createWindow() {
  win = new BrowserWindow({
    width: 1120, height: 740, minWidth: 940, minHeight: 620,
    show: false,
    backgroundColor: '#0b0d12',
    title: 'Stream Companion',
    icon: asset('icon.png'),
    autoHideMenuBar: true,
    ...(process.env.SC_PLAIN_FRAME ? {} : { titleBarStyle: 'hidden', titleBarOverlay: { color: '#0b0d12', symbolColor: '#c7cbd6', height: 44 } }),
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false, sandbox: false },
  });
  win.setMenuBarVisibility(false);
  win.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));
  win.once('ready-to-show', () => { if (!hidden) win.show(); });
  win.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  win.webContents.on('will-navigate', e => e.preventDefault());
  win.on('close', e => {
    if (quitting || !core.store.get('minimizeToTray')) return;
    e.preventDefault();
    win.hide();
    if (!core.store.get('trayHintShown')) {
      core.store.set({ trayHintShown: true });
      if (Notification.isSupported()) new Notification({ title: 'Stream Companion is still running', body: 'The server keeps running in the tray. Right-click the tray icon to quit.' }).show();
    }
  });
}

function showWindow() {
  if (!win) return;
  if (win.isMinimized()) win.restore();
  win.show();
  win.focus();
}

function createTray() {
  const img = nativeImage.createFromPath(asset('tray.png')).resize({ width: 16, height: 16 });
  tray = new Tray(img);
  tray.setToolTip('Stream Companion');
  tray.on('click', showWindow);
  refreshTray();
}

function refreshTray() {
  if (!tray || !core) return;
  const st = core.state();
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: 'Open Stream Companion', click: showWindow },
    { type: 'separator' },
    { label: st.running ? `Server running on port ${st.port}` : 'Server stopped', enabled: false },
    { label: st.running ? 'Stop server' : 'Start server', click: () => (st.running ? core.stopServer() : core.startServer()) },
    { type: 'separator' },
    { label: 'Quit', click: () => { quitting = true; app.quit(); } },
  ]));
}

function applyLoginItem() {
  if (process.platform !== 'win32' && process.platform !== 'darwin') return;
  app.setLoginItemSettings({ openAtLogin: !!core.store.get('startWithWindows'), args: ['--hidden'] });
}

function wireIpc() {
  const folderPrompt = async p => {
    try { await core.library.addRoot(p); return { ok: true }; } catch (e) { return { ok: false, error: e.message }; }
  };
  ipcMain.handle('state:get', () => core.state());
  ipcMain.handle('folder:add', async () => {
    const r = await dialog.showOpenDialog(win, { title: 'Choose a folder to share', properties: ['openDirectory', 'multiSelections'] });
    if (r.canceled) return { ok: true };
    const errors = [];
    for (const p of r.filePaths) { const res = await folderPrompt(p); if (!res.ok) errors.push(res.error); }
    return errors.length ? { ok: false, error: errors.join(' ') } : { ok: true };
  });
  ipcMain.handle('folder:add-path', async (_e, p) => {
    if (typeof p !== 'string') return { ok: false, error: 'Invalid path' };
    return folderPrompt(p);
  });
  ipcMain.handle('folder:remove', (_e, id) => { core.library.removeRoot(id); core.emit('state', core.state()); });
  ipcMain.handle('folder:rescan', () => core.library.rescanAll());
  ipcMain.handle('folder:open', (_e, p) => {
    if (core.store.get('folders').some(f => f.path === p)) shell.openPath(p);
  });
  ipcMain.handle('server:toggle', () => (core.server.running ? core.stopServer() : core.startServer()));
  ipcMain.handle('settings:update', async (_e, patch) => { await core.updateSettings(patch || {}); applyLoginItem(); });
  ipcMain.handle('device:revoke', (_e, id) => { core.security.revoke(id); core.emit('state', core.state()); });
  ipcMain.handle('pin:new', () => { core.security.newPin(); core.emit('state', core.state()); });
  ipcMain.handle('logs:open', () => shell.openPath(path.join(core.dataDir, 'logs')));
  ipcMain.handle('clipboard:copy', (_e, t) => clipboard.writeText(String(t)));
}

app.whenReady().then(async () => {
  core = new Core(app.getPath('userData'), { tmpDir: app.getPath('temp') });
  wireIpc();
  if (!process.env.SC_NO_WINDOW) createWindow();
  if (!process.env.SC_NO_WINDOW && !process.env.SC_NO_TRAY) createTray();
  applyLoginItem();
  let timer = null;
  core.on('state', () => {
    if (timer) return;
    timer = setTimeout(() => {
      timer = null;
      refreshTray();
      if (win && !win.isDestroyed()) win.webContents.send('state', core.state());
    }, 250);
  });
  await core.start();
});

app.on('before-quit', async e => {
  if (core && !core._down) {
    e.preventDefault();
    core._down = true;
    quitting = true;
    await core.shutdown().catch(() => {});
    app.exit(0);
  }
});
app.on('window-all-closed', () => { /* keep running in tray */ });
