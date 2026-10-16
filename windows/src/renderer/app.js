'use strict';
const ICONS = {
  home: '<path d="M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
  folder: '<path d="M3 6.5A1.5 1.5 0 0 1 4.5 5H10l2 2.5h7.5A1.5 1.5 0 0 1 21 9v9.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z"/>',
  phone: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
  gear: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
  play: '<path d="M7 4.5v15l12-7.5z"/>',
};
const svg = (name, extra = '') => {
  const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  s.setAttribute('viewBox', '0 0 24 24'); s.setAttribute('fill', 'none'); s.setAttribute('stroke', 'currentColor');
  s.setAttribute('stroke-width', '1.7'); s.setAttribute('stroke-linecap', 'round'); s.setAttribute('stroke-linejoin', 'round');
  s.innerHTML = ICONS[name] + extra;
  return s;
};

function h(tag, attrs, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (v === true) el.setAttribute(k, '');
    else if (v !== false && v != null) el.setAttribute(k, v);
  }
  for (const kid of kids.flat()) if (kid != null && kid !== false) el.append(kid instanceof Node ? kid : document.createTextNode(kid));
  return el;
}

const fmtBytes = n => { const u = ['B', 'KB', 'MB', 'GB', 'TB']; let i = 0; while (n >= 1024 && i < 4) { n /= 1024; i++; } return `${n.toFixed(i > 1 ? 1 : 0)} ${u[i]}`; };
const ago = t => {
  if (!t) return 'never';
  const s = Math.max(0, (Date.now() - t) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.floor(s / 60)} min ago`;
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`;
  return new Date(t).toLocaleDateString();
};

let state = null;
let page = 'overview';
const pageEl = () => document.getElementById('page');

function toast(msg, err = false) {
  const t = document.getElementById('toast');
  t.textContent = msg; t.className = 'toast' + (err ? ' err' : ''); t.hidden = false;
  clearTimeout(toast.t);
  toast.t = setTimeout(() => { t.hidden = true; }, 4500);
}

async function addFolderFlow(paths) {
  const r = paths ? await Promise.all(paths.map(p => window.sc.addFolderPath(p))) : [await window.sc.addFolder()];
  const bad = r.find(x => x && !x.ok);
  if (bad) toast(bad.error, true);
}

// ----------------------------------------------------------------------------- pages
function overview() {
  const s = state;
  const online = s.devices.filter(d => d.online).length;
  const streaming = s.devices.filter(d => d.playing);
  const url = s.addresses[0] ? `${s.addresses[0].address}:${s.port}` : `port ${s.port}`;
  return h('div', {},
    h('h1', {}, 'Overview'),
    h('p', { class: 'sub' }, 'Your personal media server for the Stream Companion Android app.'),
    s.error && h('div', { class: 'error-banner' }, s.error),
    h('div', { class: 'grid two' },
      h('div', { class: 'card hero' },
        h('h3', {}, 'Server'),
        h('div', { class: 'status-line' },
          h('span', { class: 'dot ' + (s.running ? 'ok' : 'bad') }),
          h('span', { class: 'big' }, s.running ? 'Running' : 'Stopped'),
          h('span', { style: 'flex:1' }),
          h('button', { class: 'btn ' + (s.running ? '' : 'primary'), onclick: () => window.sc.toggleServer() }, s.running ? 'Stop server' : 'Start server')),
        h('p', { class: 'hint', style: 'margin:14px 0 4px' }, s.running ? 'Phones on the same Wi-Fi find this PC automatically. If discovery is blocked on your network, enter an address manually:' : 'Start the server to let your devices connect.'),
        s.running && s.addresses.slice(0, 3).map(a => h('div', { class: 'addr' }, `${a.address}:${s.port}`, h('small', {}, a.name), h('button', { class: 'btn sm', onclick: () => { window.sc.copy(`${a.address}:${s.port}`); toast('Address copied'); } }, 'Copy'))),
        s.running && !s.addresses.length && h('p', { class: 'hint' }, 'No network connection detected.')),
      h('div', { class: 'card' },
        h('h3', {}, 'Pair a new device'),
        h('div', { class: 'pin' }, ...s.pin.split('').map(c => h('span', {}, c))),
        h('p', { class: 'hint' }, 'Open Stream Companion on your phone, pick this PC and enter this code. It changes after every successful pairing.'),
        h('div', { style: 'margin-top:12px' }, h('button', { class: 'btn sm', onclick: () => window.sc.newPin() }, 'Generate new code')))),
    h('div', { class: 'grid stats' },
      stat(s.library.videos, 'videos indexed'),
      stat(s.library.folders.length, 'shared folders'),
      stat(`${online}/${s.devices.length}`, 'devices online'),
      stat(s.activeTranscodes, 'active transcodes')),
    h('div', { class: 'card', style: 'margin-top:16px' },
      h('h3', {}, 'Now playing'),
      streaming.length
        ? h('div', { class: 'list' }, streaming.map(d => h('div', { class: 'item' },
          h('div', { class: 'ico' }, svg('play')),
          h('div', { class: 'meta' }, h('b', {}, d.playing.title), h('span', {}, `${d.name} · ${d.playing.mode === 'hls' ? 'Transcoding' + (d.playing.height ? ` ${d.playing.height}p` : '') : 'Direct stream'}`)))))
        : h('p', { class: 'hint', style: 'margin:0' }, 'Nothing is being played right now.')));
}
const stat = (n, label) => h('div', { class: 'card stat' }, h('b', {}, String(n)), h('span', {}, label));

function folders() {
  const s = state;
  const scanning = s.library.folders.some(f => f.scanning) || s.library.pendingProbe > 0;
  const drop = h('div', { class: 'empty dropzone' }, h('b', {}, 'No folders shared yet'), 'Add a folder – including everything inside its sub-folders – or drop one here.',
    h('div', { style: 'margin-top:16px' }, h('button', { class: 'btn primary', onclick: () => addFolderFlow() }, 'Add folder')));
  const el = h('div', { class: 'dropzone-wrap' },
    h('div', { class: 'head' },
      h('div', {}, h('h1', {}, 'Shared folders'), h('p', { class: 'sub' }, 'Videos are indexed with their folder structure and watched for changes automatically.')),
      h('div', { class: 'actions' },
        h('button', { class: 'btn', onclick: () => { window.sc.rescan(); toast('Rescanning…'); } }, 'Rescan'),
        h('button', { class: 'btn primary', onclick: () => addFolderFlow() }, 'Add folder'))),
    scanning && h('div', { class: 'card', style: 'margin-bottom:16px' }, `Indexing… ${s.library.pendingProbe ? s.library.pendingProbe + ' files left to analyse' : ''}`, h('div', { class: 'bar' }, h('i'))),
    s.library.folders.length
      ? h('div', { class: 'list' }, s.library.folders.map(f => h('div', { class: 'item' },
        h('div', { class: 'ico' }, svg('folder')),
        h('div', { class: 'meta' }, h('b', {}, f.name), h('span', {}, f.path)),
        f.online === false ? h('span', { class: 'chip bad' }, 'Unavailable') : f.scanning ? h('span', { class: 'chip warn' }, 'Scanning') : h('span', { class: 'chip ok' }, 'Watching'),
        h('span', { class: 'chip' }, `${f.videoCount} videos`),
        h('div', { class: 'actions' },
          h('button', { class: 'btn sm', onclick: () => window.sc.openFolder(f.path) }, 'Open'),
          h('button', { class: 'btn sm danger', onclick: () => window.sc.removeFolder(f.id) }, 'Remove')))))
      : drop,
    s.library.folders.length ? h('p', { class: 'hint', style: 'margin-top:14px' }, `${fmtBytes(s.library.bytes)} across ${s.library.videos} videos. Tip: drag folders onto this window to share them.`) : null);
  return el;
}

function devices() {
  const s = state;
  return h('div', {},
    h('h1', {}, 'Devices'),
    h('p', { class: 'sub' }, 'Only devices paired with a code can browse or stream your library.'),
    s.devices.length
      ? h('div', { class: 'list' }, [...s.devices].sort((a, b) => b.lastSeen - a.lastSeen).map(d => h('div', { class: 'item' },
        h('div', { class: 'ico' }, svg('phone')),
        h('div', { class: 'meta' }, h('b', {}, d.name), h('span', {}, d.playing ? `Watching “${d.playing.title}”` : `${d.lastIp || ''} · last seen ${ago(d.lastSeen)} · paired ${new Date(d.pairedAt).toLocaleDateString()}`)),
        d.playing ? h('span', { class: 'chip ok' }, 'Streaming') : d.online ? h('span', { class: 'chip ok' }, 'Online') : h('span', { class: 'chip' }, 'Offline'),
        h('button', { class: 'btn sm danger', onclick: () => { if (confirm(`Remove “${d.name}”? It will need the pairing code again.`)) window.sc.revokeDevice(d.id); } }, 'Remove'))))
      : h('div', { class: 'empty' }, h('b', {}, 'No devices paired'), `Install the Android app, choose “${s.serverName}” and enter the code ${s.pin}.`));
}

function settings() {
  const s = state;
  const save = patch => window.sc.updateSettings(patch).then(() => toast('Saved'));
  const toggle = (key, on) => h('label', { class: 'switch' }, h('input', { type: 'checkbox', checked: on, onchange: e => save({ [key]: e.target.checked }) }), h('i'));
  const name = h('input', { type: 'text', value: s.serverName, maxlength: 60 });
  const port = h('input', { type: 'number', value: s.port, min: 1024, max: 65535 });
  return h('div', {},
    h('h1', {}, 'Settings'),
    h('p', { class: 'sub' }, 'Sensible defaults – you normally never need to change anything here.'),
    h('div', { class: 'card' },
      h('div', { class: 'field' }, h('div', {}, h('b', {}, 'Server name'), h('p', {}, 'Shown on your phone when choosing a server.')),
        h('div', { class: 'actions' }, name, h('button', { class: 'btn', onclick: () => name.value.trim() && save({ serverName: name.value }) }, 'Save'))),
      h('div', { class: 'field' }, h('div', {}, h('b', {}, 'Port'), h('p', {}, 'Used for the secure HTTPS API and discovery. Allowed through Windows Firewall by the installer.')),
        h('div', { class: 'actions' }, port, h('button', { class: 'btn', onclick: () => save({ port: parseInt(port.value, 10) }) }, 'Save'))),
      h('div', { class: 'field' }, h('div', {}, h('b', {}, 'Start with Windows'), h('p', {}, 'Launch quietly in the tray when you sign in so your library is always available.')), toggle('startWithWindows', s.settings.startWithWindows)),
      h('div', { class: 'field' }, h('div', {}, h('b', {}, 'Keep running in the tray'), h('p', {}, 'Closing the window keeps the server running in the background.')), toggle('minimizeToTray', s.settings.minimizeToTray)),
      h('div', { class: 'field' }, h('div', {}, h('b', {}, 'Hardware video encoding'), h('p', {}, `Used when a video must be converted for your phone. Currently: ${s.encoder === 'libx264' ? 'software (x264)' : s.encoder}.`)),
        h('select', { onchange: e => save({ hwAccel: e.target.value }) }, h('option', { value: 'auto', selected: s.settings.hwAccel === 'auto' }, 'Automatic (recommended)'), h('option', { value: 'off', selected: s.settings.hwAccel === 'off' }, 'Off – software only')))),
    h('div', { class: 'card', style: 'margin-top:16px' },
      h('h3', {}, 'About'),
      h('div', { class: 'field' }, h('div', {}, h('b', {}, `Stream Companion ${s.version}`), h('p', {}, `Certificate fingerprint ${s.fingerprint.slice(0, 16).toUpperCase()}…  ·  ${fmtBytes(s.bytesSent)} streamed this session`)),
        h('button', { class: 'btn', onclick: () => window.sc.openLogs() }, 'Open logs'))));
}

// ----------------------------------------------------------------------------- shell
const NAV = [
  ['overview', 'Overview', 'home', overview],
  ['folders', 'Folders', 'folder', folders],
  ['devices', 'Devices', 'phone', devices],
  ['settings', 'Settings', 'gear', settings],
];

function renderNav() {
  const nav = document.getElementById('nav');
  nav.replaceChildren(...NAV.map(([id, label, icon]) => h('button', { class: id === page ? 'active' : '', onclick: () => { page = id; render(true); } },
    svg(icon), label,
    id === 'devices' && state.devices.length ? h('span', { class: 'badge' }, String(state.devices.length)) : null,
    id === 'folders' && state.library.folders.length ? h('span', { class: 'badge' }, String(state.library.folders.length)) : null)));
  document.getElementById('ver').textContent = `v${state.version}`;
  document.getElementById('sideStatus').replaceChildren(
    h('div', { class: 'row' }, h('span', { class: 'dot ' + (state.running ? 'ok' : 'bad') }), state.running ? 'Server online' : 'Server offline'),
    h('div', { style: 'margin-top:4px' }, state.running ? `${state.devices.filter(d => d.online).length} device(s) connected` : 'Not accepting connections'));
}

let editing = false;
function render(force) {
  if (!state) return;
  renderNav();
  // Don't clobber text inputs while the user is typing in Settings.
  if (!force && page === 'settings' && document.activeElement && /INPUT|SELECT/.test(document.activeElement.tagName)) return;
  const view = NAV.find(n => n[0] === page)[3]();
  pageEl().replaceChildren(view);
}

document.addEventListener('dragover', e => { e.preventDefault(); });
document.addEventListener('drop', e => {
  e.preventDefault();
  const paths = [...e.dataTransfer.files].map(f => window.sc.pathForFile(f)).filter(Boolean);
  if (paths.length) addFolderFlow(paths);
});

window.sc.onState(s => { state = s; render(false); });
window.sc.getState().then(s => { state = s; render(true); });
setInterval(() => { if (state && (page === 'devices' || page === 'overview')) render(false); }, 15000);
