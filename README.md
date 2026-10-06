# tariffia-router-android-poc

**Temporary, standalone proof-of-concept.** It proves one narrow thing only:

```
Android app  ->  fogtape/nodejs-mobile (Node 24.21.0, arm64-v8a, 16 KB, full ICU)
             ->  existing Tariffia Router `dist` (unmodified copy)
             ->  127.0.0.1:8910
             ->  GET /healthz, GET /v1/models, POST /v1/messages
```

This repository is **not** a product. It contains **no** production integration, **no**
provider-key sync, **no** foreground service, **no** Claude/OpenCode/agent code, and **no**
VPS/SSH/Termux/PRoot.

## Hard rules honored here

- The Tariffia **Router** and Tariffia **Panel** repositories are **not** modified.
- `libnode.so` is **never committed**. GitHub Actions downloads the pinned
  `fogtape/nodejs-mobile` release and verifies its SHA-256 before use.
- No real provider/API key is used anywhere. The PoC runs with a temporary local test token.
- The Router is bound to `127.0.0.1:8910` only.

## Runtime (verified statically in CI, **not** on a physical phone)

- Runtime: `fogtape/nodejs-mobile` release `v24.21.0-0` (a `nodejs-mobile` recipe build).
  - Android release ZIP: `nodejs-mobile-android-24.21.0-0.zip`
  - SHA-256 (ZIP): `e3cd29a1be03405f11dd5c857af8cd3ad13f84f1409ea648f5328f0bada5bd76`
  - `bin/arm64-v8a/libnode.so` SHA-256: `955b308b1dfdf7662e8fe5ee4eb8c0c7d0a313f2d306387f2307529993c4bc32`
  - Node 24.21.0, `NODE_MODULE_VERSION 137`, full ICU (`icu_78`), ELF `p_align = 0x4000` (16 KB)
  - Exported entry point: `node::Start(int, char**)` (`_ZN4node5StartEiPPc`)
  - `DT_NEEDED`: `libm.so libdl.so liblog.so libc++_shared.so libc.so`
- `libc++_shared.so` is taken from the **same NDK** used to build libnode (NDK `27.3.13750724`, r27d).

## Build

CI only. Locally there is no Android SDK/JDK requirement:

- JDK 17 (Temurin)
- Gradle 8.7
- Android Gradle Plugin 8.6.0
- Kotlin 1.9.24
- compileSdk/targetSdk 35, minSdk 26
- NDK 27.3.13750724
- ABI: `arm64-v8a` only
- 16 KB link flags on the JNI bridge; APK alignment checked after build

The workflow downloads the pinned libnode, verifies both SHA-256 values, extracts the arm64
library and headers, obtains `libc++_shared.so` from the NDK, builds a debug APK, runs ELF/APK
alignment checks, and uploads the APK as a GitHub Actions artifact.

## What this PoC does **not** claim

Nothing about runtime behavior is claimed until the APK is installed on a real Android 15
device. See `THIRD_PARTY_NOTICES.md` for licensing/attribution.
