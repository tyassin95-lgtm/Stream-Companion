'use strict';
// Trims Electron runtime files that this app never uses (software Vulkan, etc.) to shrink the installer.
const fs = require('fs');
const path = require('path');

exports.default = async function afterPack(context) {
  const dir = context.appOutDir;
  for (const f of ['vk_swiftshader.dll', 'vk_swiftshader_icd.json', 'vulkan-1.dll']) {
    try { fs.unlinkSync(path.join(dir, f)); } catch { /* not present */ }
  }
};
