# Full Security & Apple Liquid Glass Redesign Audit 003 (2026-11-15)

- **Repository**: `/home/dewhush/.gemini/antigravity/scratch/ours`
- **Scope**:
  1. **Visual & UX Redesign**: Apple Liquid Glass (`backdrop-filter: blur(28px) saturate(190%)` + specular top-edge rim highlights) with **Broken White (`#FAF9F5` / `#FFFFFC` / `#F0EFE9`) + Electric Blue (`#0055FF` Light / `#4D8BFF` Dark)** palette and **zero orange**.
  2. **Full-Stack Security Audit & Hardening**: Backend (`server/`), Web App (`web/`), Android Hybrid Shell (`android/`), and VPS Deployment (`deploy/`).

---

## Security Findings & Remediations (`#S1` - `#S8`)

### S1. [CRITICAL] Unauthenticated Account Takeover via Email Enumeration in `POST /api/auth/google`
- **Location**: `server/src/routes/pair.rs:606-705`, `server/src/db.rs:28-38`
- **Vulnerability**: `google_auth` Case A looks up `SELECT id FROM users WHERE lower(email) = lower(?1)` and immediately issues fresh `access_token` and `refresh_token` without verifying a cryptographic device secret, partner invite code, or rate limit. An attacker who knows a user's email can take over their account with a single POST request.
- **Fix**:
  1. Add `device_hash TEXT` column to `users` in `server/src/db.rs`.
  2. Apply per-client/per-email rate limiting (`check_join_rate_limit` / `record_join_failure`) on `google_auth`.
  3. Bind `users.device_hash = sha256(device_id)` when a user creates or joins a pair (or on first device registration).
  4. On subsequent Case A logins for an existing user, require a matching `device_id` (`constant_time_eq`) OR a valid `invite_code` from the partner. Reject unverified requests with `401 Unauthorized` and record a rate-limit failure.

### S2. [HIGH] Missing HTTP Security Headers (`CSP`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`)
- **Location**: `server/src/lib.rs:29-96`, `server/src/routes/web.rs:15-39`
- **Vulnerability**: Responses lack clickjacking, MIME-sniffing, and Content-Security-Policy headers.
- **Fix**: Add a global security-headers middleware in `server/src/lib.rs` setting:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: geolocation=(self), camera=(self), microphone=()`
  - `Content-Security-Policy: default-src 'self'; script-src 'self' https://unpkg.com; style-src 'self' 'unsafe-inline' https://unpkg.com; img-src 'self' blob: data: https://*.basemaps.cartocdn.com https://*.tile.openstreetmap.org; connect-src 'self' ws: wss: https://*.basemaps.cartocdn.com https://unpkg.com; worker-src 'self' blob:; frame-ancestors 'none'; base-uri 'self'; object-src 'none'`

### S3. [HIGH] Inline Event Handler in `web/index.html` & Strict URL Sanitization in `web/app.js`
- **Location**: `web/index.html:10`, `web/app.js`
- **Vulnerability**: `onload="this.media='all'"` in `web/index.html` requires `'unsafe-inline'` in `script-src`, weakening CSP against XSS.
- **Fix**: Remove inline event handlers from `web/index.html` so `script-src 'self' https://unpkg.com` (without `'unsafe-inline'`) is enforced strictly, and validate all dynamic `href`/`src` URLs in `web/app.js`.

### S4. [HIGH] WebView Origin Isolation & `@JavascriptInterface` Hardening in Android Shell
- **Location**: `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt`, `android/app/src/main/java/app/ours/bridge/OursNativeBridge.kt`
- **Vulnerability**: `window.OursNative` is attached to the `WebView` without restricting main-frame navigation to trusted origins or validating `setServerUrl` schemes.
- **Fix**:
  1. Override `shouldOverrideUrlLoading` in `OursWebShellScreen.kt` to allow main-frame navigation only to `file:///android_asset/` and the configured trusted server origin; open external links (`https://wa.me/...`, etc.) via system `Intent(Intent.ACTION_VIEW)`.
  2. Disable `allowFileAccessFromFileURLs` and `allowUniversalAccessFromFileURLs`.
  3. Validate `setServerUrl` in `OursNativeBridge.kt` to allow only `https://` or local/LAN/Tailscale `http://` origins.

### S5. [HIGH] WebSocket Connection Cap, Frame Size Limit, and Header/Subprotocol Auth
- **Location**: `server/src/ws.rs:31-94`
- **Vulnerability**: `ws_handler` allows unlimited concurrent connections per user and default 64MB frame sizes, enabling memory exhaustion DoS.
- **Fix**: Cap concurrent WebSocket connections to `8` per user (closing the oldest when exceeded), set `max_message_size(64 * 1024)` and `max_frame_size(64 * 1024)`, and support `Authorization: Bearer` header in addition to query token.

### S6. [MEDIUM] JPEG Decompression Bomb ("Pixel Flood" DoS) Protection
- **Location**: `server/src/photos.rs:19-29`
- **Vulnerability**: `image::load_from_memory_with_format` decodes JPEG headers of arbitrary pixel dimensions before checking width/height, allowing a small compressed JPEG with huge dimensions (`30000x30000`) to allocate gigabytes of RAM.
- **Fix**: Configure `image::ImageReader` with `Limits` (`max_image_width = Some(8192)`, `max_image_height = Some(8192)`, `max_alloc = Some(128 * 1024 * 1024)`) before decoding.

### S7. [MEDIUM] Restrictive File & Directory Permissions (`0700` / `0600`) for Private Photos and Data Directory
- **Location**: `server/src/config.rs:29-33`, `server/src/photos.rs:57-68`
- **Vulnerability**: `data_dir`, `data_dir/photos`, and saved JPEG files (`*_full.jpg`, `*_thumb.jpg`) use default world-readable permissions (`0755` / `0644`).
- **Fix**: Set `0o700` on `data_dir` and `data_dir/photos`, and `0o600` on every written JPEG file on Unix.

### S8. [MEDIUM] Refresh Token Session Cap & Expired Invite Cleanup
- **Location**: `server/src/auth.rs:77-98`, `server/src/jobs.rs`
- **Vulnerability**: `refresh_tokens` and `invites` tables accumulate old rows if a client repeatedly re-authenticates without logging out.
- **Fix**: In `issue_tokens`, delete expired `refresh_tokens` and keep at most the 10 newest sessions per `user_id`; clean up expired `invites` in background jobs.

---

## Verification & Delivery Gate (2026-11-15)

All 8 security remediations (`#S1`–`#S8`) and the Apple Liquid Glass redesign (Broken White `#FAF9F5` / `#FFFFFC` / `#F0EFE9` + Electric Blue `#0055FF` Light / `#4D8BFF` Dark, zero orange) were verified across commits `f830384`, `c38830c`, `77dda29`, and `9ffdfba`:

1. **WCAG AA Contrast (`Tokens.kt`)**: Ran `/home/dewhush/.gemini/config/skills/antislop-human/contrast-check.py` on all 44 Light & Dark token pairs: **44/44 PASS (`>= 4.5:1`)**.
2. **Rust Clippy & Integration Suite (`server/`)**:
   - `cargo clippy --offline --all-targets -- -D warnings`: **0 warnings, exit 0**.
   - `cargo test --offline`: **19 passed; 0 failed** (including `security_headers_present_on_root_and_healthz`, `google_auth_rejects_different_device_for_existing_user`, and `google_auth_creates_and_restores_session`).
3. **Android Build, Unit Tests & Web Asset Sync (`android/`)**:
   - `./gradlew testDebugUnitTest assembleDebug --offline`: **BUILD SUCCESSFUL** (`:app:syncWebAssets`, `:app:testDebugUnitTest`, `:app:assembleDebug`).
4. **Zero Orange & Zero Em-Dash Gate**:
   - Legacy orange hexes (`#D4623F`, `#E8714D`, `#D95D39`) across `web/`, `android/app/src/`, `server/src/`, `DESIGN.md`, and `docs/`: **0 matches**.
   - Em dashes (`\u2014`) across `web/`, `server/src/`, `android/app/src/`, `DESIGN.md`, `README.md`, and `docs/*.md`: **0 matches**.
5. **Design (`docs/OURS-Design.figcraft`)**: Updated to Broken White + Electric Blue Apple Liquid Glass (`f830384`, `1221 nodes`, `7` sections, zero warnings).

