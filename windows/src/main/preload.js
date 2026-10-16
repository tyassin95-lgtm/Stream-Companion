'use strict';
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('sc', {
  getState: () => ipcRenderer.invoke('state:get'),
  onState: cb => ipcRenderer.on('state', (_e, s) => cb(s)),
  addFolder: () => ipcRenderer.invoke('folder:add'),
  addFolderPath: p => ipcRenderer.invoke('folder:add-path', p),
  removeFolder: id => ipcRenderer.invoke('folder:remove', id),
  rescan: () => ipcRenderer.invoke('folder:rescan'),
  openFolder: p => ipcRenderer.invoke('folder:open', p),
  toggleServer: () => ipcRenderer.invoke('server:toggle'),
  updateSettings: patch => ipcRenderer.invoke('settings:update', patch),
  revokeDevice: id => ipcRenderer.invoke('device:revoke', id),
  newPin: () => ipcRenderer.invoke('pin:new'),
  openLogs: () => ipcRenderer.invoke('logs:open'),
  copy: text => ipcRenderer.invoke('clipboard:copy', text),
  pathForFile: f => require('electron').webUtils.getPathForFile(f),
  platform: process.platform,
});
