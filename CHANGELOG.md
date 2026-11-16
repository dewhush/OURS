# Changelog

All notable changes to **OURS** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

Tracked in GitHub Issues for remaining 1:1 FigCraft (`docs/OURS-Design.figcraft`) implementation work:
- **Slice 3 Polish**: Prevent 2-line wrap on `"23h left"` `ExpiryPill` badge inside `Bubble_Photo24h` and finalize Slice 3 component verification.
- **Slice 4 Screens (1:1 FigCraft Parity)**: Wire `web/app.js` and `web/index.html` to compose exclusively from Slice 1 tokens, Slice 2 self-hosted fonts/SVGs/map styles, and Slice 3 `.ours-*` components across Onboarding, Map (`4.1`-`4.6` + Desktop split), Chat (`5.1`-`5.4`), Memories (`6.1`-`6.4`), and Settings (`6.5`-`6.6`) with `390x844` centered frame accuracy.
- **Slice 5 Interactions & Motion**: Bottom sheet 3-detent drag without tearing down MapLibre, 24h expiry countdown ticks, loading skeletons, and empty/error states under `prefers-reduced-motion`.
- **Slice 6 End-to-End Verification**: Full flow verification on `https://ours.mnemonic.my.id` (`390x844`) and synced Android APK builds.
- **Docs & Cleanup**: Update `README.md` to reflect the hybrid WebView + Rust + Web App architecture and gate dev `*-preview.html` routes before final production lock.

---

## [0.4.0] - 2026-11-16

### Added
- **FigCraft 1:1 Parity - Slice 0 (Inventory)**: Cataloged all 34 frames across 8 sections (`2,573` nodes, `Inter` + `Geist Mono`, `Light`/`Dark` tokens) in `docs/OURS-Design.figcraft`.
- **FigCraft 1:1 Parity - Slice 1 (Design Tokens)** (`f20faae`..`614ad37`, `e6ef683`):
  - Documented measured design tokens in `docs/tokens.md` and CSS custom properties in `web/styles.css`.
  - Served `/tokens-preview.html` and `/tokens-preview.js` from `server/src/routes/web.rs` and verified live on `https://ours.mnemonic.my.id/tokens-preview.html` (Light `Tokens_Colors_Glass` & `Tokens_Type_Radius` `520x1060` + Dark mode toggle).
- **FigCraft 1:1 Parity - Slice 2 (Fonts, SVG Icons, Map Styles & Assets)** (`b5aa776`, `8f2242a`, `663f453`):
  - Self-hosted variable WOFF2 fonts (`web/fonts/Inter-VF.woff2`, `web/fonts/GeistMono-VF.woff2`) with `font-optical-sizing: none` and calibrated FigCraft tracking (`--tracking-heading: 0`, `-0.03em` display-only).
  - Exported 19 pure-vector SVGs into `web/assets/icons/` (brand marks `ours-icon-dark.svg` `120x120` & `ours-icon-light.svg` `88x88`, 7 `UI_UX_Asset_Library` illustrations, 6 Map FABs, and 4 navigation icons) and Light/Dark MapLibre palettes (`web/assets/map-style-light.json`, `web/assets/map-style-dark.json`).
  - Documented asset inventory in `docs/assets.md` and served `/assets-preview.html` (`Brand_Icon_DynamicIsland` `510x1060` & `UI_UX_Asset_Library` `520x1060`) live on `https://ours.mnemonic.my.id/assets-preview.html`.
- **FigCraft 1:1 Parity - Slice 3 (Atomic Components Wave 3A & 3B)** (`d598a38`, `86d2bbd`, `7c83073`):
  - Built reusable atomic component styles and helpers in `web/components.css`, `web/components.js`, and `docs/components.md` across 11 component categories (`buttons`, `inputs`, `chips`, `avatars`, `badges`, `cards`, `bottom nav`, `top bar`, `sheets`, `toasts`, `list rows`) covering `default`, `pressed`, `disabled`, and `focus` states in Light and Dark themes.
  - Built `#panel-components-sheet` (`540x1060`) matching `Components_Sheet` (`#24540`) and deployed `/components-preview.html` live.
- **Life360 Spatial UX Benchmark & FigCraft Upgrade** (`3983a04`): Updated `docs/OURS-Design.figcraft` with spatial map drill-down and split desktop layouts.
- **Security Audit 004 & Jakob's Law UX Sync** (`anti-slop/audit-004-2026-11-16.md`, `3b6342e`):
  - Added Android `OursNativeBridgeTest` unit tests (`android/app/src/test/java/app/ours/bridge/OursNativeBridgeTest.kt`), Credential Manager dependencies (`androidx.credentials`, `googleid`), and transparent splash theme handling (`ic_splash_transparent.xml`, `themes.xml`).
  - Documented Jakob's Law mental models in `DESIGN.md` and synced updated `web/` assets into `android/app/src/main/assets/web/`.

### Security & Verification
- Added `OURS_GOOGLE_CLIENT_ID` configuration documentation in `deploy/ours.env.example`.
- Verified `cargo test --offline` (`24 passed; 0 failed`) and Android `:app:testDebugUnitTest --offline` (`BUILD SUCCESSFUL`).

---

## [0.3.0] - 2026-11-15

### Added
- **Hybrid WebView Architecture & Hosted PWA** (`d40f3f0`, `b272b41`, `8702174`): Converted the Android app into a hot-updating hybrid WebView shell backed by `OursNativeBridge`, paired with a server-hosted Apple HIG PWA (`web/`) and offline bundled asset fallback.
- **Production VPS + Cloudflare Tunnel Deployment** (`2cf0801`, `a0fb748`, `5e3e21a`): Added systemd unit configuration and Cloudflare domain binding scripts for `https://ours.mnemonic.my.id` with fast server URL probing and non-blocking MapLibre initialization.
- **Mandatory Google Sign-In Onboarding** (`ac7f733`, `7e074e1`, `668612b`, `26af8fb`): Added native Android Google Credential Manager picker bridge, server `/api/auth/google` endpoint, and simplified pair creation/joining onboarding flow.
- **Broken White & Electric Blue Liquid Glass Redesign** (`70c2dda`, `f830384`, `77dda29`, `c38830c`, `9ffdfba`, `be221ec`): Transitioned tokens and UI across `DESIGN.md`, `docs/OURS-Design.figcraft`, `android/`, and `web/` to `#FAF9F5` Broken White, `#0055FF`/`#4D8BFF` Electric Blue, and `#090B10` Deep Obsidian with Apple-style specular glass.
- **Life360 Spatial UX & Audit Documentation** (`ad0e2e8`, `6c11f2c`, `db61a0f`): Verified 40 audit items (`audit-002-2026-11-15.md`), security verification (`#S1`-`#S8`), and spatial UX benchmark notes.

### Fixed & Hardened
- Server security hardening (`#S1`-`#S8`, `c38830c`, `e5270c9`, `8713e15`): Strict CSP & `X-Frame-Options: DENY`, zero inline scripts/handlers, bounded WebSocket channel & connection caps, per-key/per-client auth rate limiting, backup `umask`, and `0600` SQLite file permissions.
- Android accessibility & WCAG AA contrast fixes (`0c6ada3`, `62ee509`, `908a464`): Fixed `OursDarkColors` action contrast, `inkMuted` contrast, `44dp` segmented touch targets, and Compose semantics.

---

## [0.2.0] - 2026-11-14

### Added
- **Glassmorphism v2 / Cobalt Blue Exploration & Audits** (`1fa80f6`..`fa2bc3f`):
  - Server fixes (`af10a27`): Check-in expiration and photo file cleanup on delete, auth throttling, Saved Places 50 m enter/exit hysteresis, keyset `/api/memories` pagination, and wake check updates.
  - Android fixes (`1fa80f6`, `da7366e`, `aab764a`, `82c00cb`, `34e4375`, `085a1a7`): Full-bleed MapLibre layout and `textureMode`, camera saveable state, Contacts permission flow, FCM SOS/avatar push handling, concurrent Coil 401 refresh locking, and GPS fallback.
  - UI & Antislop audit passes (`ca5a4a7`, `891c8d3`, `969f904`, `b00ac36`, `fa2bc3f`): Addressed tracked UI/accessibility issues `#9`-`#58` (WCAG AA contrast, `>= 44dp` tap targets, Settings dock clearance, dark map layers, glass shadow bleed-through, splash glow/eyebrow removal, and collapsible search).
