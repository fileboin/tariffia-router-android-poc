# Third-party notices

This PoC redistributes the following components at build time. The files below are
included in the APK assets under `app/src/main/assets/licenses/`.

## Node.js 24.21.0 (`libnode.so`)

- License: MIT (with the bundled third-party components enumerated in the Node.js
  `LICENSE`, e.g. V8, ICU, nghttp2, brotli, uvwasi, ...).
- Full text: `app/src/main/assets/licenses/nodejs-LICENSE`.
- Source: built from the official `nodejs/node` source tree.

## nodejs-mobile recipe (`fogtape/nodejs-mobile`, tag `v24.21.0-0`)

- The Android `libnode.so` used here is a prebuilt artifact published by
  `fogtape/nodejs-mobile`, a build recipe derived from `digidem/nodejs-mobile`
  (which in turn derives from the official `nodejs-mobile/nodejs-mobile`).
- License: MIT-family (per the recipe's `NOTICE.md`).
- Provenance note: `app/src/main/assets/licenses/nodejs-mobile-NOTICE.md`.
- Release ZIP SHA-256: `e3cd29a1be03405f11dd5c857af8cd3ad13f84f1409ea648f5328f0bada5bd76`
- `bin/arm64-v8a/libnode.so` SHA-256: `955b308b1dfdf7662e8fe5ee4eb8c0c7d0a313f2d306387f2307529993c4bc32`

## Tariffia Router `dist`

- The bundled `app/src/main/assets/router-dist.zip` is an **unmodified** copy of the
  Tariffia Router build output (`dist/` + `registry/ollama.json`).
- License: `app/src/main/assets/licenses/tariffia-router-LICENSE`.
- The Tariffia Router source repository is **not** modified by this PoC.

## libc++_shared.so

- Taken from Android NDK `27.3.13750724` (r27d) — the same NDK used to build the
  `libnode.so` above. Bundled into the APK via the `c++_shared` STL.
