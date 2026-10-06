// PoC launcher. Runs inside the embedded Node 24 runtime.
// It reports the runtime facts the UI needs, sanity-checks ICU/Intl, then
// imports the UNMODIFIED Tariffia Router CLI and starts `serve`.
import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const info = {
  version: process.version,
  arch: process.arch,
  platform: process.platform,
  mobile: process.versions && process.versions.mobile ? process.versions.mobile : null,
};

try {
  if (process.env.POC_INFO_FILE) {
    writeFileSync(process.env.POC_INFO_FILE, JSON.stringify(info));
  }
} catch (err) {
  console.error('[poc] could not write info file:', err && err.message);
}

console.log('[poc] node', process.version, process.arch, 'mobile=', info.mobile);

try {
  const order = 'a'.localeCompare('b');
  console.log('[poc] localeCompare ok ->', order);
} catch (err) {
  console.error('[poc] localeCompare FAILED ->', err && err.message);
}

const entry = process.env.POC_ROUTER_ENTRY;
console.log('[poc] importing Router CLI:', entry);
const mod = await import(pathToFileURL(entry).href);
await mod.main(['serve']);
