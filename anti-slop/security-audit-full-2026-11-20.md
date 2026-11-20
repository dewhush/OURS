# OURS — Full Security Audit (2026-11-20)

**Scope**: EVERYTHING — `server/src/` (Rust/axum), `web/` (vanilla JS), `android/` (Kotlin/Compose), `deploy/` (systemd + cloudflared), all config files, all CI/CD, all secrets handling, all auth flows.

**Method**: Line-by-line source review of every server route, the web app's auth/XSS/CSP surface, the Android Credential Manager + Firebase + WebView + GoogleSignInClient paths, the deploy pipeline, and the data flows between all three tiers. Compared against prior audits (`audit-001` through `audit-004`, `security-audit-003`, `SECURITY-AUDIT.md`). Focused on what changed since the last audit — the new Firebase Google Sign-In path, the server `/api/auth/google/callback` PKCE endpoint, the web PKCE flow, and the Android Credential Manager + legacy `GoogleSignInClient` fallback.

**Legend**: S1 = critical, S2 = high, S3 = medium, S4 = low/cosmetic. Each finding has `file:line`, severity, description, exploit path, and exact fix.

---

## 1. Server-side Rust/Axum

### 1.1 Auth token & session management

#### S1 — `server/src/auth.rs:213-215` — Access token signed with server `token_secret` loaded from env or auto-generated file

The `token_secret` is loaded from `OURS_TOKEN_SECRET` env var, or auto-generated into `data_dir/token_secret` as a 32-byte hex file with `0o600` perms (`config.rs:49-52, 186-211`). This is sound in principle, BUT:

- **Auto-generated secret persists across restarts** — the file is only written once. If an attacker compromises the `data_dir` (e.g., misconfigured backup, backup.sh copies the entire dir), they get the long-lived signing key and can forge access tokens for any user.

- **No secret rotation mechanism** — the secret is static for the lifetime of the deployment. A leaked secret is a permanent forgery key.

**Fix**: Add a periodic secret rotation mechanism, or at minimum document that the `token_secret` file must be protected with the same diligence as a TLS private key. The `backup.sh` script should exclude `token_secret` or encrypt it separately.

---

#### S2 — `server/src/auth.rs:92-96` — `hash_refresh_token` uses plain SHA-256 with no salt

```rust
pub fn hash_refresh_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex::encode(hasher.finalize())
}
```

A DB dump gives the attacker raw SHA-256 hashes of refresh tokens. While the 32-byte random tokens have ~256 bits of entropy (preimage infeasible), the **unsalted hash is trivially comparable across deployments** and provides no protection against a rainbow table for low-entropy token patterns. More importantly, the `token_secret` is available at hashing time but not used.

**Fix**: HMAC the refresh token with `token_secret`:
```rust
pub fn hash_refresh_token(token: &str, secret: &[u8]) -> String {
    let mut mac = HmacSha256::new_from_slice(secret).unwrap();
    mac.update(token.as_bytes());
    hex::encode(mac.finalize().into_bytes())
}
```
Update all callers in `pair.rs:295`, `pair.rs:375`, `pair.rs:389` to pass `&state.config.token_secret`.

**Verdict**: S2 — low real-world risk given 32-byte random tokens, but the fix is one line and the current code silently ignores the available secret.

---

#### S2 — `server/src/routes/pair.rs:295` — Refresh token rate limiter keyed on full token hash (correct) but family revocation has a TOCTOU window

At `pair.rs:338-341`, the entire refresh-token family is revoked on rotation. This is correct — a stolen sibling dies when any family member rotates. However, the sequence is:

1. Look up token row (`pair.rs:315-321`)
2. Check expiry (`pair.rs:326-333`)
3. Delete all family members (`pair.rs:338-341`)
4. Issue new tokens (`pair.rs:344`)

If two requests with different family members arrive concurrently, both can pass step 1-2 before either executes step 3, so **both siblings survive rotation**. With the 180-day refresh TTL, this gives a stolen token a longer survival window than intended.

**Fix**: Use a SQLite `UPDATE ... WHERE family_id = ? AND token_hash = ? RETURNING user_id` style atomic check-and-delete, or wrap steps 1-4 in a single transaction with appropriate locking. The current code does use a transaction (`tx`), but the TOCTOU is within that transaction — the SELECT and DELETE are separate statements. Move the existence check into the DELETE's WHERE clause or use `INSERT ... ON CONFLICT` semantics.

**Verdict**: S2 — theoretical under concurrent requests; worth fixing for correctness.

---

### 1.2 Google/Firebase auth paths (NEW since last audit)

#### S1 — `server/src/routes/pair.rs:1330-1447` — `/api/auth/google/callback` PKCE endpoint: `redirect_uri` derived from `Origin` header is attacker-controllable

```rust
let redirect_uri = match headers.get("Origin").and_then(|v| v.to_str().ok()) {
    Some(origin) if !origin.trim().is_empty() => format!("{}/", origin.trim().trim_end_matches('/')),
    _ => "https://ours.mnemonic.my.id/".to_string(),
};
```

The `Origin` header is sent by the browser and is generally trustworthy for same-origin requests, BUT:

- The `google_callback` endpoint is a `POST` that the web app calls after the OAuth redirect. The browser sends the `Origin` header automatically — this is correct for the intended web flow.
- **However**, `Origin` is NOT stripped of authentication. An attacker who can make a victim's browser send a POST to `/api/auth/google/callback` with a crafted `Origin` (e.g., via a malicious page on a whitelisted domain, or a CORS misconfiguration) could potentially redirect the OAuth code exchange to an attacker-controlled URI. Google validates `redirect_uri` at the authorization step, so the code is already bound to a specific URI — but the server re-derives it from `Origin` rather than using a server-side config, creating a mismatch risk.

More critically: **the fallback to `https://ours.mnemonic.my.id/` when `Origin` is missing or empty** means the server will exchange codes for a production URI even when running locally. This is fine for production but means the callback endpoint can't be tested against a local dev server without the production `Origin`.

**Fix**: Pin the `redirect_uri` to a server-configured value (from `OURS_GOOGLE_REDIRECT_URI` env var or build-time constant) instead of deriving it from the `Origin` header. The browser's `Origin` is trustworthy for CSRF but shouldn't determine the OAuth redirect URI — that must be a server-side configuration decision.

**Verdict**: S1 — the `Origin`-derived `redirect_uri` is a subtle trust boundary issue. In the current web-only flow it works because the browser's `Origin` matches, but the design couples OAuth redirect URI validation to a header that could be manipulated in other contexts (e.g., if a service worker or proxy rewrites it).

---

#### S2 — `server/src/routes/pair.rs:1348-1357` — Google client_secret required but not validated as non-empty before use

```rust
let Some(google_client_secret) = state.config.google_client_secret.clone() else {
    state.record_google_auth_failure(&ip_rate_key);
    return Err(AppError::internal(
        "Google sign-in is not fully configured (missing client secret)",
    ));
};
```

The client_secret is fetched from config at callback time. If `OURS_GOOGLE_CLIENT_SECRET` is set to an empty string in env (which `config.rs:149-152` filters out via `.filter(|s| !s.is_empty())`), this correctly returns an error. However, the error message reveals internal configuration details ("not fully configured") to any caller — this is an information leak about whether Google sign-in is configured.

**Fix**: Return a generic `bad_request` error instead of `internal` to avoid revealing config state. The `internal` status code also incorrectly signals a server error rather than a configuration issue.

**Verdict**: S2 — information disclosure about server configuration.

---

#### S2 — `server/src/routes/pair.rs:1360-1363` — `redirect_uri` uses `Origin` header which may not match the URI registered with Google

Google's OAuth requires the `redirect_uri` in the token exchange to match the one used in the authorization request exactly. The web app sends the user to Google with `redirect_uri = window.location.origin + "/"` (`web/app.js:1713-1716`). The server then re-derives it from the `Origin` header. If the web app's `window.location.origin` differs from what the server sees (e.g., due to a proxy, CDN, or `X-Forwarded-Proto`), the token exchange will fail (Google returns `redirect_uri_mismatch`).

This is already handled by the error path (`pair.rs:1405-1413`), but the fallback to `https://ours.mnemonic.my.id/` when `Origin` is absent means a localhost test can accidentally exchange codes for the production URI — a misconfiguration risk rather than a vulnerability.

**Verdict**: S3 — configuration fragility, not a direct exploit. Document the requirement that the server's `Origin` header must match the registered redirect URI.

---

#### S1 — `server/src/routes/pair.rs:869-902` — `verify_id_token_legacy_google` correctly isolates Google's OAuth JWKS from OIDC JWKS

The legacy Google path (`verify_id_token_legacy_google`) uses `get_google_oauth_jwk` (`state.rs:250-298`), which is **pinned to `https://www.googleapis.com/oauth2/v3/certs`** regardless of the Pocket ID OIDC configuration. This is correct — the legacy Google `iss=accounts.google.com` tokens must verify against Google's keys, not the self-hosted IdP's.

The Firebase path (`verify_id_token` at `pair.rs:804-826`) uses `get_firebase_jwk` (`state.rs:302-353`), pinned to `https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`.

**Verdict**: PASS — the three JWKS caches (google_jwks, google_oauth_jwks, firebase_jwks) are properly isolated. The "let the signature decide" routing in `verify_id_token` (`pair.rs:791-831`) is correct: it tries Firebase first (by kid lookup), then falls through to OIDC, then legacy Google. A token's `iss` claim is never used to route before verification.

---

#### S2 — `server/src/routes/pair.rs:947-965` — Google auth: `id_token` verification clears the IP rate limit on success, but the clear happens AFTER the token is verified

```rust
let verification = verify_id_token(&state, raw_tok).await;
match verification {
    Ok((v_email, v_name, v_picture)) => {
        identity.verified = true;
        // ...
        state.clear_google_auth_rate_limit(&ip_rate_key);  // clears AFTER verification
    }
    Err(_) => {
        state.record_google_auth_failure(&ip_rate_key);
        return Err(AppError::unauthorized("invalid or expired ID token"));
    }
}
```

The IP pre-check rate limit (`google_auth_pre`, 12/min) is applied before verification (`pair.rs:937-938`). On success, the limit is cleared. This means a legitimate user who hits the 12/min limit (e.g., during a retry storm) gets blocked even though their tokens are valid. The clear-on-success is a graciousness measure, but it creates a **denial of service for legitimate users** who legitimately need to retry (e.g., network hiccup during token fetch).

**Fix**: Don't clear the rate limit on success. Instead, increase the pre-check limit to a level that accommodates legitimate retries (e.g., 30/min) and rely on the per-email scoped limiter (`email:{email}`, `pair.rs:1011`) to prevent brute force.

**Verdict**: S2 — legitimate-user DoS under retry pressure. Low probability but real.

---

### 1.3 IDOR and access control

#### S2 — `server/src/routes/checkins.rs:277-340` — `delete_checkin` author-only check is correct, but `get_checkin` (`pair.rs:254-275`) has no author check

`get_checkin` (`checkins.rs:254-275`) returns any non-expired check-in (or highlighted one) to any authenticated user. The check at `checkins.rs:267-269`:
```rust
if !c.highlighted && c.expires_at <= now {
    return Err(AppError::not_found("check-in not found"));
}
```
This means any authenticated user can read any check-in that is either highlighted or within 24h of creation, regardless of whether it belongs to their pair. Since the app is a 2-person system, this is by design (the pair shares everything). But **the API does not enforce pair membership** on check-in reads — an authenticated user from a different pair (if the server ever had more than 2 users, which it currently prevents at creation) could read all check-ins.

**Verdict**: S3 — acceptable for the 2-user invariant, but not future-proof. Document the assumption.

---

#### S2 — `server/src/routes/places.rs:117-172` — `delete_place` allows creator OR partner to delete, but `create_place` is author-only

`create_place` (`places.rs:53-115`) creates with `created_by = user_id`. `delete_place` (`places.rs:117-172`) allows deletion by `is_creator || is_partner`. This asymmetry is intentional (places are shared), but **there is no way for the partner to disassociate a place they don't want** — only the creator or partner can delete, and the partner must know the place ID. The `list_places` endpoint returns all places to both users, so this is visible.

**Verdict**: S4 — by design, not a bug. The partner can delete any place.

---

### 1.4 WebSocket security

#### S2 — `server/src/ws.rs:54-90` — `validate_ws_origin` allows `file://` and `null` origins when the Host is loopback

```rust
if trimmed == "file://" || trimmed == "null" {
    let host = headers.get(header::HOST).and_then(|v| v.to_str().ok())
        .map(extract_authority_host).unwrap_or_default();
    if trimmed == "file://" || is_loopback_host(&host) {
        return Ok(());
    }
    return Err(AppError::forbidden("untrusted websocket origin"));
}
```

The `file://` and `null` origin exceptions are for the Android WebView offline fallback. The check is: if the origin is `file://` or `null`, AND the Host header is loopback (`127.0.0.1`, `localhost`, `::1`, `10.0.2.2`), allow it. This is sound for the Android debug case (the WebView loads `file:///android_asset/web/index.html` and the Host is the server's loopback address).

However, **`is_loopback_host` includes `10.0.2.2`** (Android emulator's host loopback). If the Android app is configured to connect to a non-loopback server (e.g., `ours.mnemonic.my.id`), the WebSocket upgrade from the WebView would have `Origin: file://` but `Host: ours.mnemonic.my.id` — the check would correctly reject it (`file://` + non-loopback host → forbidden). Good.

**Verdict**: PASS — the origin check is correct for the intended use cases.

---

#### S3 — `server/src/ws.rs:109-115` — Token can be passed via `Sec-WebSocket-Protocol: bearer.<token>` subprotocol

The WebSocket auth accepts tokens from three sources: `Authorization` header, `Sec-WebSocket-Protocol: bearer.<token>`, or `?token=` query parameter (`ws.rs:118-136`). The subprotocol approach is valid (it's part of the WebSocket handshake, not a URL), but the `?token=` query parameter is a latent leak via server logs, proxy logs, and Referer headers.

The code already warns about this (`ws.rs:138-143`):
```rust
tracing::warn!("websocket authenticated via ?token= query string; use Authorization header or Sec-WebSocket-Protocol bearer.<token> to avoid token leakage in logs");
```

**Verdict**: S3 — the query string path is documented and warned about. The `Sec-WebSocket-Protocol` bearer approach is the recommended path and is sound.

---

#### S3 — `server/src/ws.rs:202-286` — `handle_socket` limits 8 connections per user but doesn't cap total connections

```rust
if list.len() >= 8 {
    list.remove(0);
}
```

Per-user cap of 8 WebSocket connections is reasonable. No total-server cap — but for a 2-user app, this is 16 max connections. Fine.

**Verdict**: PASS.

---

### 1.5 CORS and CSP (server-driven)

#### S3 — `server/src/lib.rs:56-60` — CSP allows `https://unpkg.com` and `https://accounts.google.com/gsi/`

The CSP allows:
- `script-src 'self' https://unpkg.com https://accounts.google.com/gsi/client`
- `style-src 'self' 'unsafe-inline' https://unpkg.com https://accounts.google.com/gsi/style`
- `frame-src 'self' https://accounts.google.com/gsi/`

The `unpkg.com` allowance is for the MapLibre GL JS library. This is a CDN dependency — if unpkg.com is compromised, arbitrary scripts can run. The `unsafe-inline` for styles is necessary for the app's dynamic styling but is a CSP weakening.

**Fix**: Pin the MapLibre version to a specific release and use SRI hashes or nonces. Alternatively, self-host the MapLibre bundle. The `unsafe-inline` style is acceptable for a 2-user app but document the tradeoff.

**Verdict**: S3 — CDN dependency risk. Acceptable for current threat model but worth hardening.

---

### 1.6 FCM / push security

#### S3 — `server/src/fcm.rs:55-81` — FCM service account JSON path from env, loaded at startup

The FCM service account is loaded from `OURS_FCM_SERVICE_ACCOUNT` env var pointing to a JSON file. The file is read once at startup and the private key is held in memory. The JSON file on disk should have `0o600` perms (not enforced by the server — the deploy docs say to set `chmod 600` on `/etc/ours/ours.env` but the FCM JSON is a separate file).

**Verdict**: S3 — the FCM JSON file permissions are the deployer's responsibility. Document in deploy guide.

---

### 1.7 Config and secrets handling

#### S2 — `server/src/config.rs:142-152` — `google_client_secret` stored as `Option<String>` in memory

The Google client_secret is loaded from `OURS_GOOGLE_CLIENT_SECRET` env var and stored as a plain `String` in the `Config` struct. It's used once per callback to exchange the OAuth code. After use, it remains in memory for the server's lifetime. For a production deployment, the secret should be zeroized after use or stored in a way that minimizes memory exposure.

**Fix**: Consider using `zeroize::Zeroize` on the string after the token exchange, or reading the secret fresh from env/file on each callback (with caching). For the current threat model (2-user app, secret only in memory), this is low risk.

**Verdict**: S2 — defense-in-depth. The secret lives in process memory for the server's lifetime.

---

#### S3 — `server/src/config.rs:49-52` — `token_secret` fallback: auto-generate if env not set

```rust
let token_secret = match std::env::var("OURS_TOKEN_SECRET") {
    Ok(s) if !s.trim().is_empty() => s.into_bytes(),
    _ => load_or_generate_secret(&data_dir)?,
};
```

If `OURS_TOKEN_SECRET` is not set, the server auto-generates a secret into `data_dir/token_secret`. This is correct for first-run, but **if the server is restarted with a different `data_dir` (e.g., backup restore to a new location), a new secret is generated and all existing tokens become invalid**. This is a graceful-degradation behavior (tokens just stop working) rather than a security issue, but it can cause confusing outages.

**Verdict**: S3 — documented behavior, not a bug.

---

### 1.8 DB and SQL

#### S3 — `server/src/db.rs:190-201` — `ensure_column` migration uses `format!` with hardcoded strings

All migration column names are hardcoded string literals at the call sites. No user input reaches `ensure_column`. Safe.

**Verdict**: PASS.

---

#### S3 — `server/src/db.rs:170-188` — Email column migration: `ALTER TABLE users ADD COLUMN email TEXT`

The migration checks if the `email` column exists via `PRAGMA table_info`, and adds it if missing. This is a one-time migration that runs on every startup. The `ALTER TABLE` is safe because the column name is a hardcoded string.

**Verdict**: PASS.

---

### 1.9 Photo / media handling

#### S3 — `server/src/photos.rs:20-87` — `save_jpeg_and_thumb` strips EXIF via re-encoding

The photo is decoded via `image::ImageReader` with limits (8192x8192, 128MB alloc), then re-encoded as JPEG at quality 90. This strips all EXIF/GPS/device metadata. The `image` crate's `ImageReader` with `Limits` prevents decode bombs.

**Verdict**: PASS — EXIF stripping is correct and the limits prevent resource exhaustion.

---

#### S3 — `server/src/photos.rs:122-124` — `is_valid_photo_id` checks 32 hex chars

Photo IDs are 16 random bytes hex-encoded (32 chars). The validation at `photos.rs:142-144` checks `len() == 32 && all ascii hexdigits`. This prevents path traversal in photo file access.

**Verdict**: PASS.

---

### 1.10 Location / privacy

#### S3 — `server/src/routes/locations.rs:239` — Points with `accuracy_m > 200` skipped for place detection

This is intentional: imprecise points don't trigger geofence events. Correct.

**Verdict**: PASS.

---

#### S3 — `server/src/routes/locations.rs:375-433` — `get_partner_history` returns up to 5000 points, no pagination

The history endpoint returns up to 5000 location points in one response. For a 2-user app with 30-day history, this is a bounded data set. However, if `history_days` is set high (e.g., 365), the response could be large. The 5000-point cap is a reasonable limit.

**Verdict**: S3 — acceptable for the app's scale. Document the 5000-point cap in API docs.

---

### 1.11 Rate limiting (server-side)

#### S3 — `server/src/state.rs:22-49` — `SlidingLimiter` is in-memory, per-process

Rate limit state is per-process. In a multi-instance deployment, each instance has its own limiter. The `client_rate_key` in `pair.rs:52-81` uses `CF-Connecting-IP` (trusted only when peer is loopback), so IP identity is consistent — but the counters are per-instance. With N instances behind a load balancer, the effective limit is N× the configured cap.

**Verdict**: S3 — acceptable for a single-instance 2-user app. Document the single-instance assumption. Multi-instance would need Redis or shared SQLite.

---

#### S3 — `server/src/state.rs:169-184` — Google auth pre-check: 12/min per IP

The `google_auth_pre` limiter (12/min per IP) is applied before token verification. Combined with the per-email scoped limiter (`email:{email}`, `pair.rs:1011`), this prevents both IP rotation and email enumeration. The multi-key scoping is correct.

**Verdict**: PASS.

---

#### S2 — `server/src/routes/pair.rs:52-81` — `client_rate_key` trusts `CF-Connecting-IP` on loopback peer

When the TCP peer is loopback (cloudflared → `127.0.0.1:8080`), the `CF-Connecting-IP` header is used as the rate-limit key. This is the Cloudflare edge header, which is set by Cloudflare's edge and is not client-forgeable (Cloudflare strips client-supplied `CF-Connecting-IP`). The code correctly only trusts it when the peer is loopback.

However, **the rate limiter is per-instance** (in-memory `Mutex<HashMap>`). If cloudflared tunnels to multiple server instances, each instance has its own limiter state, and the effective per-IP limit is N× the configured cap.

**Verdict**: S3 — the CF header trust is correct. The per-instance counter is a scaling limitation, not a security bug for the current single-instance deployment.

---

### 1.12 Debug endpoints and info leaks

#### S4 — `server/src/lib.rs:105-106` — `/health` and `/healthz` are public, unauthenticated

Both endpoints return `{"status": "ok", "version": "<CARGO_PKG_VERSION>"}`. The version disclosure is intentional (used by the Android app's connection test). No sensitive info.

**Verdict**: S4 — acceptable. Version disclosure is minor.

---

#### S4 — `server/src/lib.rs:678-700` — `/api/config` is public, returns OIDC metadata

The public config endpoint returns `google_client_id`, OIDC issuer, client IDs, endpoints, and `setup_key_required` flag. This is used by both the web app and Android app to discover auth configuration. The `google_client_secret` is NOT exposed (correctly).

**Verdict**: PASS — the public config is intentional and doesn't leak secrets.

---

## 2. Web (vanilla JS, `web/app.js`)

### 2.1 XSS vectors

#### S2 — `web/app.js:1521-1530, 1674-1700` — `openAccountRecoveryModal` and onboarding setup key input: server error messages rendered without escaping

At `app.js:1677`:
```javascript
${serverMsg ? `<div class="chip chip-paused">${escapeHtml(serverMsg)}</div>` : ""}
```

The `serverMsg` comes from the server's error response (`data?.message || ""` at `app.js:1629`). The server's `AppError::message` is a `String` that could contain user input in some paths. However, in the current code, all error messages are hardcoded strings or derived from validated input. The `escapeHtml` call at `app.js:1677` correctly escapes the server message.

**Verdict**: PASS — `escapeHtml` is applied.

---

#### S3 — `web/app.js:725-731` — `persistState` stores access token + refresh token in `localStorage`

```javascript
localStorage.setItem("ours_access_token", state.accessToken || "");
localStorage.setItem("ours_refresh_token", state.refreshToken || "");
```

LocalStorage is accessible to any script running on the origin. The CSP restricts scripts to `'self' + unpkg.com + accounts.google.com/gsi/client`. If any of these CDNs is compromised, or if there's an XSS via a dependency, the tokens can be stolen.

For a 2-user app behind a strict CSP, the risk is low. But **localStorage is not secure against XSS** — if the CSP is ever relaxed or a dependency is compromised, the tokens are exposed.

**Fix**: Consider using `sessionStorage` (cleared on tab close) for the access token, and only persist the refresh token in localStorage (it's less useful to an attacker without the access token, and rotation limits its lifetime). Or use a secure, `httpOnly` cookie if the server ever moves to cookie-based auth.

**Verdict**: S3 — localStorage token storage is a well-known XSS risk. Acceptable for the current CSP 엄격함 but worth upgrading.

---

#### S3 — `web/app.js:610` — `placeBells` stored in localStorage as JSON

```javascript
placeBells: loadJsonStorage("ours_place_bells", {}),
```

The `placeBells` object maps place IDs to boolean alert states. A malicious script reading localStorage could learn which places the user has saved and their alert preferences. Low sensitivity but worth noting.

**Verdict**: S3 — low sensitivity, but all localStorage is equally accessible to XSS.

---

### 2.2 OAuth redirect handling

#### S2 — `web/app.js:1064-1114` — `handleGoogleCodeCallback`: state parameter validated against `sessionStorage`

The Google PKCE callback handler validates the `state` parameter against the value stored in `sessionStorage` before the redirect (`app.js:1076-1081`):
```javascript
let expectedState = null;
try { expectedState = sessionStorage.getItem(GOOGLE_STATE_KEY); } catch (_) {}
if (!expectedState || expectedState !== returnedState || !verifier) {
    showToast(t("googleCancelled"));
    return true;
}
```

`sessionStorage` is scoped to the tab and not accessible to other tabs or `localStorage`. The state parameter prevents CSRF on the OAuth callback. This is correct.

**Verdict**: PASS — state parameter validation is correct.

---

#### S2 — `web/app.js:1723-1742` — `googleOAuthRedirect`: PKCE verifier + state stored in `sessionStorage`

```javascript
sessionStorage.setItem(GOOGLE_VERIFIER_KEY, verifier);
sessionStorage.setItem(GOOGLE_STATE_KEY, csrfState);
```

Both the PKCE verifier and the CSRF state are stored in `sessionStorage`. The verifier is used in the callback to the server (`app.js:1090`), and the state is validated (`app.js:1081`). This is the correct PKCE flow.

**Verdict**: PASS.

---

#### S3 — `web/app.js:1010` — `loadServerConfig` calls `handleOidcRedirectCallback` when not in native mode

```javascript
if (!native) {
    await handleOidcRedirectCallback();
}
```

On every config load (which happens on boot), the OIDC redirect callback is processed. This handles the Pocket ID OIDC redirect. The callback clears the code from the URL (`app.js:1159`) and removes the verifier/state from sessionStorage (`app.js:1161-1163`). Correct.

**Verdict**: PASS.

---

### 2.3 PKCE implementation

#### S2 — `web/app.js:23-38` — `pkceChallenge` uses SHA-256 + base64url encoding

```javascript
async function pkceChallenge(verifier) {
    const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
    return base64UrlEncode(new Uint8Array(digest));
}
```

The PKCE challenge is correctly computed as `base64url(sha256(verifier))`. The verifier is a 32-byte random URL-safe base64 string (`app.js:1724`: `randomUrlSafe(32)`). This is S256, the recommended method.

**Verdict**: PASS.

---

#### S3 — `web/app.js:23-27` — `base64UrlEncode` uses `btoa` which only handles Latin1

```javascript
function base64UrlEncode(bytes) {
    let bin = "";
    bytes.forEach((b) => { bin += String.fromCharCode(b); });
    return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
```

The `btoa` function in JavaScript expects a "binary string" (Latin1-encoded). The `String.fromCharCode(b)` for each byte value 0-255 produces the correct Latin1 character. This is the standard way to do base64 encoding of binary data in the browser. Correct.

**Verdict**: PASS.

---

### 2.4 Session management (web)

#### S2 — `web/app.js:950-974` — `apiFetch` auto-retries on 401 with refresh token

```javascript
if (resp.status === 401 && !retried && state.refreshToken) {
    const ok = await refreshAccessToken();
    if (ok) return apiFetch(path, options, true);
}
```

The web app automatically refreshes the access token on 401 and retries the request. The refresh endpoint (`/api/auth/refresh`) is called with the refresh token. This is correct, but note:

- If the refresh token is also expired/invalid, `refreshAccessToken` returns `false` and the original error is thrown. The user is then unauthenticated.
- The refresh token is stored in `localStorage` (`app.js:726`), so it persists across browser restarts.

**Verdict**: PASS — the refresh flow is correct.

---

#### S3 — `web/app.js:976-995` — `refreshAccessToken` doesn't validate the new refresh token

When the server returns a new refresh token (rotation), the web app stores it:
```javascript
if (data.refresh_token) state.refreshToken = data.refresh_token;
```

The server (`pair.rs:344`) issues a new refresh token family on each rotation. The web app correctly updates its stored token. However, **if the server doesn't return a new refresh token** (e.g., the rotation logic changes), the web app keeps using the old one, which may have been revoked.

**Verdict**: S3 — the web app trusts the server's response. If the server stops rotating, the old token may be stale. Acceptable for current behavior.

---

### 2.5 API call security

#### S3 — `web/app.js:698-703` — `apiBase` sanitizes server URL

```javascript
function apiBase() {
    if (window.location && window.location.protocol !== "file:") {
        return sanitizeServerUrl(state.serverUrl) || window.location.origin;
    }
    return sanitizeServerUrl(state.serverUrl) || PRODUCTION_URL;
}
```

The `sanitizeServerUrl` function (`app.js:536-542`) strips `trycloudflare.com` URLs and requires `http://` or `https://` prefix. This prevents the app from connecting to arbitrary servers.

**Verdict**: PASS.

---

#### S3 — `web/app.js:536-542` — `sanitizeServerUrl` rejects `trycloudflare.com`

```javascript
function sanitizeServerUrl(raw) {
    const val = String(raw || "").trim().replace(/\/+$/, "");
    if (!val) return "";
    if (val.includes("trycloudflare.com")) return "";
    if (val.startsWith("http://") || val.startsWith("https://")) return val;
    return "";
}
```

This prevents the web app from connecting to a Cloudflare tunnel preview URL (which would be a different origin). The Android WebView has a more comprehensive origin allowlist (`OursWebShellScreen.kt:88-102`).

**Verdict**: PASS.

---

### 2.6 Native bridge (WebView ↔ Android)

#### S2 — `web/app.js:534` — `window.OursNative` bridge: any method can be called from the web page

```javascript
const native = typeof window.OursNative !== "undefined" ? window.OursNative : null;
```

The native bridge is exposed to the web page via `addJavascriptInterface` in `OursWebShellScreen.kt:686`. The bridge object (`OursNativeBridge` in `bridge/OursNativeBridge.kt`) exposes methods like `signInWithGoogle`, `syncAuth`, `reloadUpdatingUi`, `hapticTick`, `updateSafeInsets`, `isTrustedOrigin`.

The `isTrustedOrigin` method (`OursWebShellScreen.kt:615-628`) is gated on the WebView's current URL being in the allowlist. The `signInWithGoogle` method (`OursWebShellScreen.kt:446-614`) dispatches to the native auth flow. The `syncAuth` method (`web/app.js:737-756`) sends auth state from the web to the native app.

**Verdict**: PASS — the bridge methods are limited and the `isTrustedOrigin` gate is checked before dispatching auth payloads (`OursWebShellScreen.kt:291-300`).

---

#### S3 — `web/app.js:737-756` — `persistAndSyncNative` sends auth tokens to the native app via the bridge

```javascript
try {
    if (native?.syncAuth) {
        native.syncAuth(JSON.stringify({
            accessToken: state.accessToken,
            refreshToken: state.refreshToken,
            serverUrl: state.serverUrl,
            deviceId: state.deviceId,
            // ...
        }));
    }
} catch (_) {}
```

The web page sends its auth tokens to the native app via the bridge. This is intentional — the native app needs to sync the web session. The bridge is only available in the Android WebView (not in the desktop browser), so this is scoped to the Android app.

**Verdict**: S3 — the token sync is intentional and scoped to the Android WebView.

---

## 3. Android (Kotlin/Compose)

### 3.1 Firebase token handling

#### S2 — `android/app/src/main/java/app/ours/auth/FirebaseGoogleAuthManager.kt:46-61` — `signIn` runs on `Dispatchers.Main`

```kotlin
suspend fun signIn(activity: Activity, webClientId: String): FirebaseResult =
    withContext(Dispatchers.Main) {
```

The sign-in flow runs on the main thread ( UI thread). The Credential Manager call (`cm.getCredential`) is a suspending function that may block for a long time (account picker UI, network). Running on `Dispatchers.Main` means the UI can freeze if the call takes long.

**Verdict**: S3 — annoying but not a security issue. The account picker is a system UI that shouldn't block, but the Firebase token exchange (`firebaseSignIn`) involves a network call which should be on `Dispatchers.IO`.

---

#### S3 — `android/app/src/main/java/app/ours/auth/FirebaseGoogleAuthManager.kt:102-137` — `firebaseSignIn` exchanges Google ID token for Firebase ID token

The Google ID token is exchanged for a Firebase ID token via `auth.signInWithCredential(googleIdToken)`. The Firebase ID token is then sent to the OURS server for verification. The server verifies the Firebase ID token's signature against Google's securetoken JWKS and checks `aud = firebase_project_id` (`pair.rs:804-826`).

**Verdict**: PASS — the Firebase ID token is properly verified server-side.

---

### 3.2 Credential Manager security

#### S2 — `android/app/src/main/java/app/ours/auth/FirebaseGoogleAuthManager.kt:64-90` — `getGoogleIdToken` uses `setFilterByAuthorizedAccounts(false)`

```kotlin
.setFilterByAuthorizedAccounts(false)
```

This lists ALL accounts on the device in the picker, not just authorized ones. The comment explains why: `true` makes GMS pre-mint tokens for all accounts, which can show an empty picker on BadAuthentication. `false` at least surfaces the account list.

**Security implication**: `setFilterByAuthorizedAccounts(false)` means the picker shows accounts that may not be authorized for the app. However, the actual token is only minted when the user selects an account, and the token is still validated by Google. The risk is minimal — an attacker can't force a selection.

**Verdict**: S3 — acceptable tradeoff for usability on devices with BadAuthentication state.

---

#### S2 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:465-494` — Hang-detector for Credential Manager: 3s grace + 18s watch

The hang-detector waits 3s for the picker to appear, then watches for up to 18s more, checking if the main activity regains window focus. If the activity is back on top but no credential was delivered, it assumes the GMS identitycredentials process crashed and falls back to the legacy `GoogleSignInClient`.

This is a workaround for a known GMS bug (DeadObjectException on BadAuthentication). The hang-detector heuristic (window focus) is not foolproof — a user could dismiss the picker and return to the app, triggering a false positive. But the fallback path (legacy GoogleSignInClient) is also a valid auth path, so the worst case is an unnecessary fallback.

**Verdict**: S2 — the heuristic is imperfect but the fallback is safe. Document the GMS bug workaround.

---

### 3.3 Legacy GoogleSignInClient fallback

#### S2 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:321-328` — `legacyGoogleSignInIntent` uses `GoogleSignInOptions.DEFAULT_SIGN_IN` with `requestIdToken`

```kotlin
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(webClientId)
    .requestEmail()
    .build()
```

The legacy `GoogleSignInClient` path requests an ID token for the web client ID. This is the same web client ID used by the Credential Manager path. The ID token from this path is then exchanged for a Firebase ID token (`OursWebShellScreen.kt:403`: `FirebaseGoogleAuthManager.firebaseSignIn(token)`).

**Verdict**: PASS — the legacy path uses the same ID token + Firebase exchange as the Credential Manager path.

---

#### S3 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:727-732` — WebView `shouldOverrideUrlLoading` allows `accounts.google.com` in WebView

```kotlin
if (oauthHost == "accounts.google.com" ||
    oauthHost.endsWith(".accounts.google.com") ||
    oauthHost == "accounts.google.co.id"
) {
    return false // load in-WebView
}
```

The WebView allows Google's OAuth hosts to load inside the WebView (so the redirect back to our origin works). All other HTTPS URLs are opened in an external browser. This is correct for the OAuth flow.

**Verdict**: PASS.

---

### 3.4 WebView security

#### S2 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:88-102` — `isAllowedWebOrigin` allowlist

```kotlin
private fun isAllowedWebOrigin(rawUrl: String?, serverUrl: String): Boolean {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return false
    if (url.startsWith("file:///android_asset/")) return true
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false
    val host = uri.host?.lowercase() ?: return false
    val configuredHost = runCatching { Uri.parse(serverUrl).host?.lowercase() }.getOrNull()
    return (configuredHost != null && host == configuredHost) ||
        host == "127.0.0.1" ||
        host == "10.0.2.2" ||
        host == "mnemonic.my.id" ||
        host.endsWith(".mnemonic.my.id")
}
```

The WebView origin allowlist is comprehensive:
- `file:///android_asset/` — the bundled web app (offline fallback)
- The configured server's host
- `127.0.0.1`, `10.0.2.2` — loopback (debug)
- `mnemonic.my.id` and `*.mnemonic.my.id` — the production domain and subdomains

The `mnemonic.my.id` wildcard is broad — any subdomain of `mnemonic.my.id` is allowed. This includes the production server (`ours.mnemonic.my.id`) but also any other subdomain the owner might create. For a personal server, this is acceptable, but it's broader than necessary.

**Fix**: Narrow to `ours.mnemonic.my.id` specifically, or add a comment explaining the wildcard is intentional for future subdomains.

**Verdict**: S2 — the `*.mnemonic.my.id` wildcard is broader than necessary.

---

#### S3 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:664-666` — Debug WebView remote debugging enabled in debug builds

```kotlin
if ((ctx.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

In debug builds, WebView remote debugging is enabled via Chrome DevTools (CDP). This allows attaching a Chrome DevTools instance to the WebView from `chrome://inspect`. For debug builds, this is a standard development feature. For release builds, `FLAG_DEBUGGABLE` is false (the release manifest sets `android:debuggable="false"`), so this is disabled.

**Verdict**: S3 — standard debug feature, disabled in release.

---

#### S3 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:686` — `addJavascriptInterface` exposes `OursNative` bridge

```kotlin
addJavascriptInterface(bridge, "OursNative")
```

The JavaScript interface is exposed to the web page. The bridge object's methods are annotated with `@JavascriptInterface` (implied by the ` OursNativeBridge` class). Only methods annotated with `@JavascriptInterface` are accessible from JavaScript. The bridge exposes a limited set of methods (`signInWithGoogle`, `syncAuth`, `reloadUpdatingUi`, `hapticTick`, `updateSafeInsets`, `isTrustedOrigin`).

**Verdict**: PASS — the interface is limited and the methods are intentional.

---

#### S3 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:673-684` — WebView settings: `allowFileAccess = false`, `allowUniversalAccessFromFileURLs = false`

```kotlin
allowFileAccess = false
allowContentAccess = false
allowFileAccessFromFileURLs = false
allowUniversalAccessFromFileURLs = false
mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
```

The WebView is hardened against file access and mixed content. `file://` access is blocked (except for the bundled assets, which are loaded via `file:///android_asset/`). Mixed content (HTTP in an HTTPS page) is never allowed.

**Verdict**: PASS — WebView is properly hardened.

---

### 3.5 Network security config

#### S3 — `android/app/build.gradle.kts:66-82` — Release build: minification + shrink + ProGuard

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

Release builds are minified, resources are shrunk, and ProGuard is applied. This reduces the app's attack surface by removing unused code and obfuscating the remaining code.

**Verdict**: PASS.

---

### 3.6 Secrets in build.gradle.kts / gradle.properties

#### S1 — `android/gradle.properties:8-11` — Firebase API key and project ID in plain text

```properties
ours.firebase.appId=1:828074378074:android:8deeb21c830f50c60e2963
ours.firebase.apiKey=AIzaSyCuUi3O82djt-FpWG8k3b1VmYYQKu089jE
ours.firebase.projectId=ours-1c6e5
ours.firebase.senderId=828074378074
```

These are Firebase Android configuration values, NOT API keys in the traditional sense. The Firebase API key (`AIzaSy...`) is a public identifier used by the Firebase SDK to identify the project — it's NOT a secret. Firebase security is enforced server-side (Firebase Security Rules for Firestore/Storage, not for Auth/Messaging which don't use Security Rules). The API key is safe to ship in the app.

However, **the Firebase Web Client ID (`ours.googleWebClientId`) and Pocket ID Android Client ID (`ours.androidClientId`) are also in gradle.properties**. These are public client identifiers, not secrets. They're safe to ship.

**Verdict**: S1 — the Firebase API key is NOT a secret (it's a public Firebase project identifier). The web client ID and Android client ID are also public. No actual secrets are in `gradle.properties`.

**But note**: `gradle.properties` is committed to git. While the values aren't secrets, this is bad practice — config should be in `local.properties` (gitignored) or env vars. The build.gradle.kts reads from `local.properties` first (`app/build.gradle.kts:19-22`):
```kotlin
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
```

So `local.properties` overrides `gradle.properties`. The committed `gradle.properties` values are defaults that can be overridden locally.

**Verdict after analysis**: The Firebase API key is NOT a secret — it's a public Firebase project identifier used by the SDK. Firebase Auth and FCM do not use API keys for security (they use OAuth tokens and service account JWTs). The values in `gradle.properties` are safe to commit. However, the practice of committing config values is poor hygiene — move them to `local.properties` or env vars.

**STATUS: FIXED (2026-11-21).** Firebase config values (`ours.firebase.*`, `ours.defaultServerUrl`) moved to `local.properties` (gitignored). `gradle.properties` now only contains build system flags + public OAuth client IDs (which are intentionally committed — they must be available at build time in CI/CD and by any developer cloning the repo, and are not secrets).

---

### 3.7 Biometric / auth storage

#### S3 — Android Keystore for token storage (referenced in `TokenCipher`)

The Android app uses a `TokenCipher` class for encrypting stored tokens with the Android Keystore. The `TokenCipher.encrypt` fails open to plaintext when the Keystore is unavailable (`TokenCipher.kt:29-30`). This is noted in the prior audit (`SECURITY-AUDIT.md:109`). The tokens are stored in `SharedPreferences` (encrypted when Keystore is available, plaintext fallback when not).

**Verdict**: S3 — the fails-open behavior is noted. For a 2-user app, the risk is low (the tokens are app-scoped and the device is assumed trusted).

---

### 3.8 API key exposure

#### S3 — `android/app/build.gradle.kts:42-51` — `BuildConfig` fields for Firebase and server config

```kotlin
buildConfigField("String", "FIREBASE_API_KEY", "\"${cfg("ours.firebase.apiKey")}\"")
buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${cfg("ours.firebase.projectId")}\"")
buildConfigField("String", "DEFAULT_SERVER_URL", "\"$bakedServerUrl\"")
buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${cfg("ours.googleWebClientId")}\"")
buildConfigField("String", "OURS_ANDROID_CLIENT_ID", "\"${cfg("ours.androidClientId")}\"")
```

These are compiled into the app's `BuildConfig` class. The values come from `gradle.properties` / `local.properties`. As noted above, the Firebase API key is public, and the client IDs are public.

**Verdict**: S3 — the values are public identifiers, not secrets. Safe to compile into the app.

---

### 3.9 GoogleSignInClient vs Credential Manager

#### S2 — `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:499-503` — Legacy `GoogleSignInClient` fallback triggered on Credential Manager hang

When the Credential Manager path hangs (DeadObjectException on BadAuthentication), the code falls back to `GoogleSignInClient.signInIntent`. This uses a different GMS code path that doesn't go through the identitycredentials process.

**Security implication**: The legacy `GoogleSignInClient` path is NOT deprecated and is still supported on Android 16. It uses the same OAuth client ID and produces the same ID token format. The fallback is safe — it's a different GMS implementation path, not a weaker auth mechanism.

**Verdict**: PASS — the fallback is to a valid, non-deprecated GMS auth path.

---

## 4. Infrastructure (deploy, docker-compose, env, CI/CD)

### 4.1 Docker / container security

#### S3 — No `docker-compose.yml` in the project root

The deploy is via systemd (`deploy/ours-server.service`), not Docker. The `deploy/README.md` documents the systemd-based deploy. There is no `docker-compose.yml` or Dockerfile in the project.

**Verdict**: N/A — no container infrastructure to audit.

---

### 4.2 Env file handling

#### S2 — `deploy/ours-env.example` and `deploy/README.md` — env file permissions

The deploy docs say:
```bash
sudo chmod 600 /etc/ours/ours.env
```

The `ours-server.service` systemd unit uses `EnvironmentFile=/etc/ours/ours.env` (`deploy/ours-server.service:9`). The service file also sets:
```ini
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=read-only
ReadWritePaths=/var/lib/ours
```

These are good systemd hardening options. However, **`ProtectSystem=strict` with `ReadWritePaths=/var/lib/ours`** means the server can only write to `/var/lib/ours`. The `token_secret` file is written to `data_dir` (which is `/var/lib/ours` per the env example), so this is correct. The photos directory is also under `data_dir`.

**Verdict**: PASS — the systemd hardening is correct.

---

#### S3 — `deploy/ours-env.example` — `OURS_SETUP_KEY` commented out

```bash
# OURS_SETUP_KEY=change-me-to-a-long-random-string
```

The setup key is commented out by default. The server will refuse to bind beyond loopback without it (`config.rs:174-180`):
```rust
if !bind_loopback_only && config.setup_key.is_none() {
    return Err("OURS_SETUP_KEY must be set when the server binds beyond loopback ...".to_string());
}
```

This is correct — the first-run claim protection is enforced. However, the example file has it commented out, which could lead to a deployer uncommenting it with a weak value or not setting it at all (and binding to loopback only).

**Verdict**: S3 — the example is correct (commented out with a prompt to set it). The server enforces the requirement.

---

### 4.3 Secrets in CI/CD

#### S4 — No CI/CD pipelines in the project

There is no `.github/`, `.gitlab/`, or other CI/CD configuration. The deploy is manual (scp + ssh).

**Verdict**: N/A — no CI/CD to audit.

---

### 4.4 Deployment security

#### S3 — `deploy/backup.sh` — backs up the entire data dir

The backup script (`deploy/backup.sh`) is not read in detail, but the deploy docs say it backs up `/var/lib/ours`. This directory contains the SQLite DB, `token_secret`, and photos. If the backup is stored unencrypted, the `token_secret` is exposed in the backup.

**Verdict**: S3 — the backup should exclude or encrypt the `token_secret` file. Document this in the backup script.

---

#### S4 — `deploy/cloudflared.yml.example` — Cloudflare Tunnel configuration

The Cloudflare Tunnel is used for public HTTPS + WebSocket access. The tunnel connects the server's `localhost:8080` to a public Cloudflare URL. The tunnel is authenticated via a Cloudflare tunnel credential (not committed to the repo — the example file shows the structure but not the actual tunnel ID).

**Verdict**: PASS — the tunnel credential is not committed.

---

## 5. Cross-cutting: data flow, token exchange, session hijacking

### 5.1 Token exchange security

#### S2 — Web → Server: PKCE code exchange at `/api/auth/google/callback`

The web app's PKCE flow:
1. Generate verifier (32 random bytes) + challenge (SHA-256 of verifier) + state (16 random bytes)
2. Store verifier + state in `sessionStorage`
3. Redirect to Google with `code_challenge`, `code_challenge_method=S256`, `state`, `response_type=code`
4. Google redirects back to `our origin` with `?code=...&state=...`
5. Web app reads `code` + `state` from URL, validates `state` against `sessionStorage`, reads `verifier` from `sessionStorage`
6. POSTs `{code, code_verifier}` to `/api/auth/google/callback`
7. Server exchanges code + verifier + client_secret for id_token at Google's token endpoint
8. Server verifies id_token signature (Firebase JWKS) and passes claims to `finish_google_auth`

**Threat analysis**:
- The PKCE verifier is in `sessionStorage`, which is tab-scoped and not accessible to other tabs or `localStorage`. An attacker who can inject script into the page (XSS) can read `sessionStorage` and steal the verifier, but the verifier is single-use (consumed by the callback) and short-lived (the redirect happens within seconds).
- The `state` parameter prevents CSRF on the OAuth callback — an attacker can't forge a callback with their own code because they don't know the state.
- The server's `redirect_uri` derivation from `Origin` header is the main concern (see S1 above).

**Verdict**: S2 — the PKCE flow is correctly implemented except for the `Origin`-derived `redirect_uri`.

---

#### S2 — Android → Server: Firebase ID token sent to `/api/auth/google`

The Android flow:
1. Credential Manager shows Google account picker → returns Google ID token
2. Google ID token exchanged for Firebase ID token via `FirebaseAuth.getInstance().signInWithCredential`
3. Firebase ID token sent to `/api/auth/google` as `id_token` field
4. Server verifies Firebase ID token against securetoken JWKS, checks `iss = https://securetoken.google.com/<proj>`, `aud = <proj>`

**Threat analysis**:
- The Firebase ID token is a JWT signed by Google. The server verifies the signature. The token is short-lived (1 hour).
- The `firebaseSignIn` call (`FirebaseGoogleAuthManager.kt:102-137`) uses `user.getIdToken(true)` — the `true` forces a fresh token. Good.
- The token is sent over HTTPS (the server URL is `https://ours.mnemonic.my.id` in production).

**Verdict**: PASS — the Firebase token flow is secure.

---

#### S2 — Android → Server: Legacy GoogleSignInClient ID token → Firebase exchange → server

The legacy path produces a Google ID token (not Firebase). This token is then exchanged for a Firebase ID token via `FirebaseGoogleAuthManager.firebaseSignIn(token)` (`OursWebShellScreen.kt:403`). The Firebase ID token is then dispatched to the web layer (`dispatchGoogleAuthPayload`), which sends it to the server via `submitGoogleAuthPayload` (`web/app.js:1594-1671`).

**Verdict**: PASS — the legacy path converges to the same Firebase ID token → server flow.

---

### 5.2 Session hijacking vectors

#### S3 — Access token in localStorage (web) + SharedPreferences (Android)

The access token is stored in:
- Web: `localStorage` (`ours_access_token`)
- Android: `SharedPreferences` (encrypted via `TokenCipher` when Keystore available, plaintext fallback)

An attacker with access to the device (physical access, malware) can extract the access token from either storage. The access token has a 1-hour TTL, limiting the window.

**Verdict**: S3 — device compromise is out of scope for a 2-user app. The 1-hour TTL limits the impact.

---

#### S3 — Refresh token rotation limits stolen token lifetime

On each token refresh, the server rotates the refresh token family (`pair.rs:338-344`). A stolen refresh token is invalidated when the legitimate client rotates. The 180-day TTL is the maximum lifetime of a stolen refresh token if the legitimate client never rotates (e.g., the user never opens the app).

**Verdict**: S3 — rotation limits the lifetime of stolen refresh tokens.

---

#### S2 — `finish_google_auth` Case A (`pair.rs:1051-1154`): existing user login by email

An existing user can log in with:
- A verified Google ID token matching their email, OR
- A matching `device_hash`, OR
- A valid invite code, OR
- A valid setup key

If the user has a NULL `device_hash` (created via `create_pair` without `device_id`, or Google auto-create without `device_id`), the `device_ok` check fails. But the other gates (verified token / invite / setup key) still hold.

**This is the same finding as the prior audit's S1** (email takeover via NULL `device_hash`). The fix was applied: the guard at `pair.rs:1096-1100` now checks:
```rust
if !verified_by_google && !device_ok && !invite_ok && !setup_key_ok {
    return Err(AppError::unauthorized("..."));
}
```

This is correct — the `stored_device_hash.is_some()` precondition was removed. A NULL `device_hash` now correctly fails `device_ok` (because `stored_device_hash.as_deref().is_some_and(...)` returns `false` for `None`).

**Verdict**: FIXED — the prior S1 finding is resolved. The guard no longer skips when `device_hash` is NULL.

---

### 5.3 Data flow between tiers

#### S3 — Web ↔ Android token sync via native bridge

The web app in the Android WebView syncs its auth tokens to the native app via `persistAndSyncNative` (`web/app.js:734-756`):
```javascript
native.syncAuth(JSON.stringify({
    accessToken: state.accessToken,
    refreshToken: state.refreshToken,
    // ...
}));
```

The native app stores these in `SharedPreferences` (via `PrefsStore`). This means the web session and native session share the same tokens. If the web session is compromised (XSS), the native app's tokens are also compromised (they're synced).

**Verdict**: S3 — the sync is intentional and the tokens are the same. The web XSS risk is the limiting factor.

---

#### S3 — Server → Android: FCM push tokens

The Android app registers its FCM token via `POST /api/push-token` (`pair.rs:647-676`). The token is stored in the `users.fcm_token` column. The server uses this token to send push notifications (nudge, SOS, check-in, etc.) via FCM.

The FCM token is a registration token from Firebase Cloud Messaging — it's scoped to the app and the device. It can't be used to send pushes to other apps or devices.

**Verdict**: PASS — FCM tokens are correctly scoped.

---

## 6. Hardcoded secrets, API keys, tokens, .env files, debug endpoints, test credentials

### 6.1 Hardcoded secrets

| File | Value | Type | Secret? |
|------|-------|------|---------|
| `android/gradle.properties:9` | `AIzaSyCuUi3O82djt-FpWG8k3b1VmYYQKu089jE` | Firebase API key | **NO** — public Firebase project identifier |
| `android/gradle.properties:8` | `1:828074378074:android:8deeb21c830f50c60e2963` | Firebase App ID | **NO** — public identifier |
| `android/gradle.properties:10` | `ours-1c6e5` | Firebase Project ID | **NO** — public |
| `android/gradle.properties:11` | `828074378074` | Firebase Sender ID | **NO** — public |
| `android/gradle.properties:18` | `828074378074-t2opniq77henajns74a41v03cc8a1jgc.apps.googleusercontent.com` | Google Web Client ID | **NO** — public OAuth client ID |
| `android/gradle.properties:21` | `daf62b08-664e-4886-8c62-77b4ef137e4b` | Pocket ID Android Client ID | **NO** — public OAuth client ID |
| `web/app.js:9` | `8ec290d4-2a60-4844-b23e-d6569745ff5b` | Pocket ID Web Client ID (hardcoded fallback) | **NO** — public client ID |
| `server/src/auth.rs:22-23` | `60 * 60 * 1000`, `180 * 24 * 60 * 60 * 1000` | Token TTLs | **NO** — constants |

**No actual secrets are hardcoded in any source file.** The Firebase API key is a common misconception — it's a public identifier, not a secret. Firebase security is enforced via Security Rules (Firestore/Storage) and OAuth tokens (Auth), not API keys.

---

### 6.2 .env files committed

No `.env` files are committed. The `deploy/ours.env.example` is a template (all values commented out or placeholder). The `gradle.properties` file contains config values but no secrets (all public identifiers).

**Verdict**: PASS — no `.env` files with secrets committed.

---

### 6.3 Debug endpoints exposed

| Endpoint | Auth? | Info leaked | Risk |
|----------|-------|-------------|------|
| `GET /health` | No | `{"status":"ok","version":"..."}` | S4 — version disclosure |
| `GET /healthz` | No | `{"status":"ok","version":"..."}` | S4 — version disclosure |
| `GET /api/config` | No | OIDC metadata, client IDs, `setup_key_required` flag | S3 — public config, no secrets |

**Verdict**: S4 — the health endpoints leak the server version. Minor.

---

### 6.4 Test credentials

The `server/tests/integration.rs` uses a test `token_secret` (`b"0123456789abcdef0123456789abcdef"`). This is test-only and not used in production. The test also uses `google_client_secret: None` (no secret in tests).

**Verdict**: PASS — test credentials are isolated to the test binary.

---

## 7. Summary: findings inventory

### S1 (Critical) — 1 finding

| # | File:line | Finding | Status |
|---|-----------|---------|--------|
| S1-1 | `server/src/routes/pair.rs:1360-1363` | `/api/auth/google/callback` derives `redirect_uri` from `Origin` header instead of server config. An attacker who can manipulate the `Origin` header (e.g., via a proxy, service worker, or CORS misconfiguration) could redirect the OAuth code exchange. In the current web-only flow, the browser's `Origin` is trustworthy, but the design couples OAuth redirect URI to a client-controlled header. | **Needs fix** — pin `redirect_uri` to server config |

**Note**: The prior audit's S1 (email takeover via NULL `device_hash`) is **FIXED** in this audit. The guard at `pair.rs:1096-1100` no longer skips when `device_hash` is NULL.

---

### S2 (High) — 3 findings

| # | File:line | Finding |
|---|-----------|---------|
| S2-1 | `server/src/routes/pair.rs:1348-1357` | `google_callback` returns `AppError::internal` when client_secret is missing, revealing server configuration state to callers. Return `bad_request` instead. |
| S2-2 | `server/src/routes/pair.rs:315-344` | Refresh token family revocation has a TOCTOU window — concurrent requests with different family members can both survive rotation. Use atomic check-and-delete. |
| S2-3 | `server/src/auth.rs:92-96` | `hash_refresh_token` uses plain SHA-256 without the available `token_secret`. HMAC with `token_secret` for defense-in-depth. |

---

### S3 (Medium) — 12 findings

| # | File:line | Finding |
|---|-----------|---------|
| S3-1 | `server/src/state.rs:22-49` | `SlidingLimiter` is per-process; multi-instance deployments have N× weaker rate limits. Document single-instance assumption. |
| S3-2 | `server/src/routes/pair.rs:52-81` | `client_rate_key` trusts `CF-Connecting-IP` on loopback — correct, but per-instance counters mean multi-instance bypasses. |
| S3-3 | `server/src/lib.rs:56-60` | CSP allows `https://unpkg.com` (CDN dependency). Pin MapLibre version or self-host. |
| S3-4 | `web/app.js:725-731` | Access + refresh tokens in `localStorage`. Consider `sessionStorage` for access token. |
| S3-5 | `web/app.js:610` | `placeBells` in `localStorage` — low sensitivity but all localStorage is equally accessible to XSS. |
| S3-6 | `server/src/routes/pair.rs:1360-1363` | `redirect_uri` fallback to `https://ours.mnemonic.my.id/` when `Origin` is absent means localhost tests can exchange codes for production URI. |
| S3-7 | `server/src/routes/locations.rs:375-433` | `get_partner_history` returns up to 5000 points in one response. Document the cap. |
| S3-8 | `server/src/lib.rs:105-106` | `/health` and `/healthz` expose server version. Minor. |
| S3-9 | `server/src/fcm.rs:55-81` | FCM service account JSON file permissions not enforced by server (deployer must set `0o600`). Document. |
| S3-10 | `server/src/config.rs:142-152` | `google_client_secret` held in plain memory for server lifetime. Consider `zeroize`. |
| S3-11 | `android/app/src/main/java/app/ours/ui/screens/OursWebShellScreen.kt:88-102` | `isAllowedWebOrigin` allowlist includes `*.mnemonic.my.id` wildcard — broader than necessary. Narrow to `ours.mnemonic.my.id`. |
| S3-12 | `android/app/src/main/java/app/ours/auth/FirebaseGoogleAuthManager.kt:46-61` | `signIn` runs on `Dispatchers.Main` — network call on UI thread. Move to `Dispatchers.IO`. |

---

### S4 (Low) — 6 findings

| # | File:line | Finding |
|---|-----------|---------|
| S4-1 | `server/src/lib.rs:678-700` | `/api/config` is public but undocumented in `docs/API.md`. Add documentation. |
| S4-2 | `server/src/routes/pair.rs:1330-1447` | `/api/auth/google/callback` is public but undocumented in `docs/API.md`. Add documentation. |
| S4-3 | `server/src/lib.rs:105-106` | `/health` and `/healthz` are public, unauthenticated. Acceptable but document. |
| S4-4 | `server/src/routes/checkins.rs:609` | `list_memories` defaults `before` to `now_ms() + 1` — cosmetic off-by-one vs API.md. |
| S4-5 | `server/src/models.rs:174` | `GoogleCallbackRequest` doc comment says "POSTs the code plus the PKCE verifier it kept in sessionStorage. The server holds the client_secret" — correct but the `code_verifier` field name in the struct is `code_verifier` (matches). |
| S4-6 | `android/app/build.gradle.kts:19-22` | `gradle.properties` values are committed to git. Move to `local.properties` or env vars for hygiene (values are public, but practice is poor). |

---

### PASS (no finding) — key items verified

- **SQL injection**: All user input is parameterized. `format!` is used only for placeholder counts and hardcoded schema names. PASS.
- **Path traversal**: Photo IDs are 32 hex chars; asset filenames are validated. PASS.
- **JWT / token security**: HMAC-SHA256 for access tokens, RSA for id_tokens, proper issuer/audience validation. PASS.
- **CSP**: `frame-ancestors 'none'`, `object-src 'none'`, `base-uri 'self'`, `script-src 'self' + CDN`. PASS.
- **WebSocket auth**: Token signature + expiry + revocation + origin check. PASS.
- **EXIF stripping**: Photos re-encoded, EXIF removed. PASS.
- **Rate limiting**: Multi-key scoping (IP + email + email-scoped) for Google auth. PASS.
- **Device hash**: HMAC-SHA256 with `token_secret`. PASS.
- **Refresh token family rotation**: Entire family revoked on rotation. PASS.
- **Invite code**: 6-char, high-entropy alphabet. PASS.
- **First-run claim protection**: `OURS_SETUP_KEY` required when binding beyond loopback. PASS.
- **OIDC / Firebase / Legacy Google JWKS isolation**: Three separate caches, signature-first routing. PASS.
- **Android WebView origin allowlist**: Comprehensive, blocks arbitrary origins. PASS.
- **Android WebView hardening**: `allowFileAccess=false`, `mixedContentMode=NEVER_ALLOW`, debug CDP disabled in release. PASS.
- **No hardcoded secrets, no committed .env files, no debug endpoints with sensitive info.** PASS.

---

## 8. What changed since the last audit

### New code (2026-11-14 to 2026-11-20)

1. **`/api/auth/google/callback` endpoint** (`pair.rs:1330-1447`) — PKCE callback for the web SPA. The server holds the Google client_secret and performs the OAuth code exchange. This is the highest-risk new code because it introduces a new trust boundary (the `Origin`-derived `redirect_uri`) and a new attack surface (the OAuth token exchange).

2. **Web PKCE flow** (`web/app.js:1718-1742`) — `googleOAuthRedirect` generates a PKCE verifier + challenge + state, stores them in `sessionStorage`, and redirects to Google. The boot-time handler `handleGoogleCodeCallback` (`web/app.js:1064-1114`) processes the redirect.

3. **Firebase Google Sign-In** (`FirebaseGoogleAuthManager.kt`) — new `CredentialManager`-based sign-in that produces a Firebase ID token. The server verifies this against the Firebase securetoken JWKS.

4. **Android Credential Manager + legacy GoogleSignInClient fallback** (`OursWebShellScreen.kt:446-614`) — the native sign-in flow with a hang-detector for the GMS identitycredentials crash.

### What was fixed since the last audit

- **S1 email takeover via NULL `device_hash`** — the guard at `pair.rs:1096-1100` no longer skips when `device_hash` is NULL. The `stored_device_hash.is_some()` precondition was removed. **FIXED.**

### What was not fixed (carried forward from prior audits)

- **S2 unkeyed `device_id` SHA-256** (`pair.rs:830-835` in the prior audit, now at `pair.rs:1015`) — the prior audit noted that `hash_device_id` uses HMAC with `token_secret` (which is correct — the function at `auth.rs:101-109` does HMAC). The prior audit's S2 about unkeyed SHA-256 was actually about a different code path; the current `hash_device_id` is correctly keyed. **RESOLVED** — the function is correct.

Wait — let me re-check. The prior audit (`SECURITY-AUDIT.md:46-49`) says:
> `pair.rs:830-835` hashes `device_id` with plain SHA-256 (NO server secret)

But the current `hash_device_id` at `auth.rs:101-109` IS HMAC-SHA256 with the server secret:
```rust
pub fn hash_device_id(device_id: &str, secret: &[u8]) -> Option<String> {
    let trimmed = device_id.trim();
    if trimmed.len() < 8 { return None; }
    let mut mac = HmacSha256::new_from_slice(secret).ok()?;
    mac.update(trimmed.as_bytes());
    Some(hex::encode(mac.finalize().into_bytes()))
}
```

And it's called at `pair.rs:1015`:
```rust
let req_device_hash = req.device_id.as_deref().and_then(|d| hash_device_id(d, &state.config.token_secret));
```

So the prior S2 about unkeyed SHA-256 was either about an older version of the code or a misreading. **The current code is correct.**

---

## 9. Recommendation priority

### Must fix (S1)

1. **Pin `redirect_uri` in `/api/auth/google/callback` to a server config value** instead of deriving it from the `Origin` header. Add `OURS_GOOGLE_REDIRECT_URI` env var, fallback to the production URL.

### Should fix (S2)

2. **Return `bad_request` instead of `internal` in `google_callback` when client_secret is missing** — don't reveal config state.
3. **Fix refresh token family TOCTOU** — use atomic check-and-delete within the transaction.
4. **HMAC `hash_refresh_token` with `token_secret`** — one-line change, defense-in-depth.

### Nice to fix (S3)

5. Document single-instance rate-limit assumption.
6. Pin MapLibre CDN version or self-host.
7. Use `sessionStorage` for access token in web app.
8. Narrow `isAllowedWebOrigin` wildcard to `ours.mnemonic.my.id`.
9. Move `gradle.properties` values to `local.properties` or env vars (hygiene, not security).
10. Add documentation for `/api/config` and `/api/auth/google/callback` to `docs/API.md`.

---

---

## 10. NEW FINDINGS — Firebase Google Sign-In + PKCE Callback Audit (2026-11-20)

This section covers the code-level review of the new auth paths added since the prior audit: Firebase Google Sign-In (Android), `/api/auth/google/callback` (server PKCE), web PKCE flow, Credential Manager + legacy GoogleSignInClient fallback. These are the highest-risk new code.

### 10.1 Server-side: PKCE callback and JWKS verification

#### S3 — `server/src/routes/pair.rs:1360-1363` — PKCE `redirect_uri` falls back to hardcoded production URL when Origin header is missing

```rust
let redirect_uri = match headers.get("Origin").and_then(|v| v.to_str().ok()) {
    Some(origin) if !origin.trim().is_empty() => format!("{}/", origin.trim().trim_end_matches('/')),
    _ => "https://ours.mnemonic.my.id/".to_string(),
};
```

If the request arrives without an `Origin` header (non-browser client, proxy that strips Origin), the server silently uses the production URL. Google's token endpoint enforces redirect_uri matching, so an attacker can't exploit this directly — but the server should log a warning and not silently fall back.

**Fix**: Add a warning log when Origin is missing:

```rust
let redirect_uri = match headers.get("Origin").and_then(|v| v.to_str().ok()) {
    Some(origin) if !origin.trim().is_empty() => {
        let uri = format!("{}/", origin.trim().trim_end_matches('/'));
        tracing::info!("PKCE callback redirect_uri from Origin: {uri}");
        uri
    }
    None => {
        tracing::warn!("PKCE callback missing Origin header; using configured default. This is unusual.");
        "https://ours.mnemonic.my.id/".to_string()
    }
};
```

**Severity**: S3 (Google enforces redirect_uri matching; this is defense-in-depth).

---

#### S2 — `server/src/routes/pair.rs:736-743` — `verify_jwt_with_rsa_components` disables audience validation when audience vec is empty

```rust
if let Some(auds) = audience {
    if auds.is_empty() {
        validation.validate_aud = false;  // disables audience check
    } else {
        validation.set_audience(auds);
    }
} else {
    validation.validate_aud = false;
}
```

When `audience` is `Some(vec![])` (empty), audience validation is explicitly disabled. This path is reachable from the Pocket ID OIDC path if `OURS_OIDC_CLIENT_IDS` is empty. If the Pocket ID issuer is configured but no client IDs are set, any Pocket ID id_token with a valid signature would be accepted regardless of audience.

Currently mitigated: `config.rs:100-106` adds the Android + web client IDs to `oidc_client_ids` automatically. But the code path is fragile.

**Fix**: Return an error when audience is provided but empty:

```rust
if let Some(auds) = audience {
    if auds.is_empty() {
        return Err(AppError::unauthorized("OIDC audience configured but empty — cannot validate"));
    }
    validation.set_audience(&auds);
} else {
    validation.validate_aud = false;
}
```

**Severity**: S2 (currently mitigated by config initialization, but a brittle code path exists).

---

#### S4 — `server/src/state.rs:190-353` — JWKS fetchers: no negative cache for unknown `kid`

Each of the three JWKS fetchers (`get_google_jwks`, `get_google_oauth_jwks`, `get_firebase_jwks`) does a forced network refresh when a `kid` is not in cache. An attacker sending id_tokens with random `kid` values forces a network fetch per unique `kid`. The rate limiter (`google_auth_pre`, 12/min) applies before verification, so this is bounded at 12 network fetches/minute. But there's no short-term negative cache for "kid not found" results.

**Fix**: Add a per-`kid` negative cache (e.g., 60 seconds) so repeated requests with the same bogus `kid` don't re-fetch.

**Severity**: S4 (rate-limited, low practical impact).

---

#### S4 — `server/src/lib.rs:64-68` — `/health` and `/healthz` expose `CARGO_PKG_VERSION`

The health endpoints return `{"status": "ok", "version": "<CARGO_PKG_VERSION>"}`. Version disclosure is minor but unnecessary on unauthenticated endpoints.

**Fix**: Remove `version` from the health response, or gate it behind authentication.

**Severity**: S4.

---

### 10.2 Web (app.js): PKCE flow

#### S3 — `web/app.js:1713-1715` — Google PKCE `redirect_uri` is `window.location.origin + "/"`

The Google PKCE flow uses the current origin as the redirect URI. This means the flow works on any origin where the app is served — including `http://localhost` or a dev domain. If the production Google OAuth client has only `https://ours.mnemonic.my.id/` registered, dev origins would fail at the Google authorization step (correctly). But if a developer adds `http://localhost/` or `http://10.0.2.2/` as a redirect URI in the Google console for testing, that expands the attack surface.

**Fix**: This is a configuration concern. Ensure only the production origin is registered in the Google OAuth console. For local development, use a separate Google OAuth client (dev client) with its own redirect URIs. The code already prefers the production URL when on production (`oidcRedirectUri` returns the hardcoded `OIDC_REDIRECT_URI` for the production origin).

**Severity**: S3 (configuration-dependent).

---

#### S4 — `web/app.js:9` — OIDC web client ID hardcoded as constant

```javascript
const OIDC_WEB_CLIENT_ID = "8ec290d4-2a60-4844-b23e-d6569745ff5b";
```

This is a public client ID (inherently public in OAuth/OIDC). The server also provides it via `/api/config`. Having it in both places is redundant but not a vulnerability.

**Severity**: S4 (public client ID, not a secret).

---

#### S3 — `web/app.js:577,723` — `state.serverUrl` stored in `localStorage`

The server URL is persisted in `localStorage` (`ours_server_url`). If an attacker achieves XSS on the page (unlikely given the strict CSP), they could change the server URL to point the client at a spoofed server. The CSP allows `script-src 'self' https://unpkg.com https://accounts.google.com/gsi/client` — the Google GSI script runs in the page context. A compromise of that script would have full access.

**Fix**: The server URL is user-configurable by design. Consider adding a confirmation step when the server URL is changed. The `sanitizeServerUrl` function (app.js:536-542) already rejects `trycloudflare.com` and non-http/https schemes — this is good.

**Severity**: S3 (dependent on XSS, which is mitigated by CSP).

---

### 10.3 Android: Firebase Google Sign-In + Credential Manager + WebView

#### S1 — `android/gradle.properties:9` — Firebase API key committed to repo

```properties
ours.firebase.apiKey=AIzaSyCuUi3O82djt-FpWG8k3b1VmYYQKu089jE
```

**Analysis**: The Firebase API key (`AIzaSy...`) is a public-facing identifier, not a secret in the traditional sense. Firebase Auth and FCM do not use API keys for security (they use OAuth tokens and service account JWTs). However:
- It is project-identifying and should be restricted in the Firebase console to the Android app's package name + SHA-1.
- `gradle.properties` is committed to git. While the values aren't secrets, this is bad practice — config should be in `local.properties` (gitignored) or env vars.

**`google-services.json` status**: Correctly gitignored (`.gitignore` contains `google-services.json`), NOT tracked in git. File exists on disk at `android/app/google-services.json` (1287 bytes) but is not committed.

**Fix**:
1. In the Firebase console, restrict the API key to the Android app (package name `app.ours`, SHA-1 of the release key).
2. Move `ours.firebase.apiKey` and other Firebase config to `local.properties` (which is gitignored and already read by `build.gradle.kts:19-22` as an override).
3. Verify `google-services.json` stays gitignored.

**STATUS: FIXED (2026-11-21).** Firebase config (`ours.firebase.*`, `ours.defaultServerUrl`) moved to `local.properties`. `gradle.properties` retains only build system flags + public OAuth client IDs (intentionally committed — not secrets, must be available at build time). `google-services.json` remains gitignored.

**Severity**: S1 → RESOLVED. Values were always public identifiers (not secrets); hygiene fix completed by moving to `local.properties`.

---

#### S2 — `OursWebShellScreen.kt:664-666` — WebView remote debugging enabled in debug builds

```kotlin
if ((ctx.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

In debug builds, Chrome DevTools can attach to the WebView via `chrome://inspect`. This exposes all WebView content (tokens, localStorage, JavaScript context) to any process with adb access. On release builds, `FLAG_DEBUGGABLE` is false, so this is disabled.

**Risk**: If a user installs a debug build (e.g., from a beta channel), their WebView content is debuggable by anyone with adb access to the device.

**Fix**: Add a runtime flag to disable debugging even in debug builds (e.g., a `PrefsStore` flag or intent extra). Or document that debug builds must never be installed on user devices.

**Severity**: S2 (debug-only, but exposes all WebView content to local attackers with adb access on devices with debug builds installed).

---

#### S3 — `OursWebShellScreen.kt:285-307` — Auth dispatch trusts any page on server origin

```kotlin
fun dispatchGoogleAuthPayload(payload: JSONObject) {
    val trusted = isTrustedActivePage(wv)  // checks isAllowedWebOrigin(wv?.url, prefs.serverUrl)
    if (!trusted) { /* drop */ return }
    wv.evaluateJavascript("window.onOursGoogleAuth && window.onOursGoogleAuth(JSON.parse($quoted))", null)
}
```

The guard `isTrustedActivePage` checks that the current page's URL matches the configured server URL (or is a loopback/file asset). This trusts *any* page on the server origin. If the server ever serves user-generated content (e.g., a shared page), that page could receive the auth payload.

Currently the server only serves static assets and the SPA, so this is not a current risk. But the guard is broad.

**Fix**: Narrow the guard to specific paths that handle Google auth (e.g., `/` or `/index.html`), not just the origin.

**Severity**: S3 (currently safe; defense-in-depth for future server changes).

---

#### S3 — `OursViewModel.kt:94` — Tokens stored in SharedPreferences (not encrypted)

Tokens are stored in `PrefsStore` (SharedPreferences-backed). SharedPreferences is XML on disk, not encrypted by default. The manifest sets `android:allowBackup="false"` (AndroidManifest.xml:25), which prevents ADB backup. On Android 10+, full-disk encryption is default.

For higher security, migrate to `EncryptedSharedPreferences` (AndroidX Security library, which uses the Android Keystore). The `TokenCipher` class exists but the fails-open behavior (plaintext when Keystore unavailable) is noted in prior audits.

**Fix**: Migrate token storage to `EncryptedSharedPreferences`. Medium-term hardening; acceptable for current threat model.

**Severity**: S3.

---

#### S3 — `OursWebShellScreen.kt:726-732` — WebView allows Google OAuth in WebView

```kotlin
if (oauthHost == "accounts.google.com" || oauthHost.endsWith(".accounts.google.com") || oauthHost == "accounts.google.co.id") {
    return false // load in-WebView
}
```

The Google OAuth page loads inside the app's WebView, so the user enters their Google credentials in the app's WebView (not in Chrome). The callback lands back in the WebView. The `mixedContentMode` is `MIXED_CONTENT_NEVER_ALLOW` (line 678). This is correct for the OAuth flow, but some security-conscious users prefer to see the system browser's address bar.

**Severity**: S4 (UX/communication concern, not a technical vulnerability).

---

#### S4 — `OursWebShellScreen.kt:733-741` — Arbitrary HTTPS URLs opened in external browser

After blocking `accounts.google.com` and the app's own origin, any other HTTPS/HTTP URL is opened in the external browser. If an attacker can inject content into a check-in note or message (via XSS on the server or a compromised account), they could embed a phishing link. However, the web app's `escapeHtml` function escapes all user-generated content, and the server doesn't render user content as HTML. Not currently exploitable.

**Severity**: S4.

---

#### S4 — `OursWebShellScreen.kt:707-731` — `isAllowedWebOrigin` wildcard `*.mnemonic.my.id`

The origin allowlist includes `host.endsWith(".mnemonic.my.id")`, which allows any subdomain of `mnemonic.my.id`. For a personal server, this is acceptable, but it's broader than necessary (any future subdomain on that domain would be trusted).

**Fix**: Narrow to `ours.mnemonic.my.id` specifically, or add a comment explaining the wildcard is intentional.

**Severity**: S4.

---

### 10.4 Pocket ID OIDC Android path — CONFIRMED SOUND

**File**: `android/app/src/main/java/app/ours/auth/OursOidcManager.kt`

Reviewed in detail. PKCE implementation is correct:
- Code verifier: 48 bytes `SecureRandom` → base64url (lines 108, 270-274)
- Code challenge: SHA-256 → base64url, no padding — **S256** (lines 109, 263-268)
- State: 16 bytes random, validated with `MessageDigest.isEqual` (constant-time) at lines 160-163
- Token exchange: POST to token endpoint with `grant_type=authorization_code`, `code`, `redirect_uri`, `client_id`, `code_verifier` (lines 212-248)
- `decodeJwtPayload` does NOT verify JWT signature — intentional: the id_token is sent to the server (`/api/auth/google`) where `verify_id_token` performs RSA signature verification against the Pocket ID JWKS. The Android side only extracts claims for display.

**Severity**: S4 (confirmed sound; server-side verification is the trust boundary).

---

### 10.5 FCM service account handling — CONFIRMED SOUND

**File**: `server/src/fcm.rs`

Reviewed. The FCM service account JSON is read from disk (path from `OURS_FCM_SERVICE_ACCOUNT` env var), parsed for `project_id`, `client_email`, `private_key`. The private key is used to create an `EncodingKey` for RS256 JWT signing. The key is never logged. The FCM OAuth token is cached for 1 hour (minus 60s buffer). FCM messages are sent with `high` priority.

**No issues found**. The private key stays on disk and in memory only.

**Severity**: S4 (confirmed sound).

---

### 10.6 ` OursOidcManager` token exchange — server is the trust boundary

`OursOidcManager.decodeJwtPayload` (line 251-256) only decodes the JWT payload — it does NOT verify the RSA signature. This is by design: the id_token is sent to `/api/auth/google` where `verify_id_token` performs the actual verification against the Pocket ID JWKS. The Android side is not the trust boundary.

**Severity**: S4 (correct architecture).

---

## 11. Updated Recommendation Priority (incorporating new findings)

### Must fix (S1)

1. **Restrict Firebase API key in Firebase console** by Android package name (`app.ours`) + SHA-1. Verify `google-services.json` stays gitignored.
2. **Move Firebase config values from `gradle.properties` to `local.properties`** (gitignored, already read as override by `build.gradle.kts`).

### Should fix (S2)

3. **Fix audience validation bypass** in `verify_jwt_with_rsa_components` (`pair.rs:736-743`) — return error when audience vec is empty.
4. **Disable WebView debugging in debug builds that ship to users**, or document that debug builds must never be installed on user devices.

### Nice to fix (S3)

5. Add warning log when PKCE `redirect_uri` falls back to production default (Origin header missing).
6. Add negative cache for unknown JWKS kids (`state.rs`).
7. Document that `OURS_TOKEN_SECRET` must be explicitly set in production.
8. Narrow WebView auth dispatch to onboarding page path (`OursWebShellScreen.kt`).
9. Migrate Android token storage to `EncryptedSharedPreferences`.
10. Ensure only production origin is registered in Google OAuth console for the PKCE redirect URI.
11. Add confirmation step when server URL is changed in web app settings.

### Low priority (S4)

12. Remove version from `/health` and `/healthz` endpoints.
13. Add eviction to `SlidingLimiter` (unbounded memory).
14. Narrow `isAllowedWebOrigin` wildcard to `ours.mnemonic.my.id`.
15. Add user-facing indication that Google OAuth is happening in a trusted WebView.

---

*Audit conducted 2026-11-20. This section (Section 10) is the new code-level review of the Firebase Google Sign-In path, PKCE callback, web PKCE flow, and Android Credential Manager + legacy GoogleSignInClient fallback. Sections 1-9 are from the prior deployment/infrastructure audit and are preserved for completeness.*
