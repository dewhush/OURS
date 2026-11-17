# OURS — Security audit findings

Authorized read-only source review of the OURS Rust/Axum backend (`server/`) + Kotlin client,
mapped to the URL/API surface in `docs/API.md`. Findings were live-confirmed against a throwaway
loopback instance (no repo files edited). Date: 2026-11-18.

> Read this together with the reporting gate in `~/ObsidianVault/persona/guidelines.md`.
> Items marked **hardening note** did NOT pass the gate as standalone findings but are kept as evidence.

---

## CRITICAL — Email-only account takeover via NULL `device_hash`

Any account created through the normal pairing flow can be taken over by anyone who knows the
victim's email. No credential, token, device, or setup key needed. Both members of a pair are
independently impersonatable, and the attacker reads the partner's precise live location.

- Guard — `server/src/routes/pair.rs:961-970` (current numbering ~973-982):
  ```rust
  if stored_device_hash.is_some()          // NULL device_hash => whole gate SKIPPED
      && !verified_by_google && !device_ok && !invite_ok && !setup_key_ok {
      return Err(AppError::unauthorized("..."));
  }
  // falls through to issue_tokens(...) at pair.rs:~1009
  ```
- Root cause: `create_pair` (`pair.rs:132`) and `join_pair` (`pair.rs:250`) INSERT users without
  `device_hash` -> NULL. `db.rs:192` creates a nullable column (no default). Google auto-create
  without a `device_id` (`pair.rs:1161`) is also NULL. **No backfill anywhere** (sole writer
  `pair.rs:~984` runs only after a login that supplied `device_id`). NULL is the steady state.
- PoC (live-confirmed by two independent slices):
  ```
  POST /api/auth/google
  {"email":"<victim>","name":"attacker"}
  -> 200 {"status":"authenticated","user":{"id":1,...},"tokens":{"access_token":"<VALID>","refresh_token":"..."}}
  GET /api/me            -> 200 as victim
  GET /api/partner/location -> 200, precise lat/lon/accuracy/battery of the partner
  ```
- Impact (gate 1+2): real person harmed (valid session as victim) + access to private live
  location. Surveillance-grade; app's core feature.
- Fix: make the gate fail-closed — drop the `stored_device_hash.is_some()` precondition so
  `!verified_by_google && !device_ok && !invite_ok && !setup_key_ok` ALWAYS rejects; treat NULL
  stored hash as "not device_ok". Optionally set `device_hash` at creation.

## HIGH — Unkeyed client-supplied `device_id` authenticates on its own

`pair.rs:830-835` hashes `device_id` with plain SHA-256 (NO server secret); `device_ok` compare at
~932-936. Once a `device_hash` is set, `{"email":"<victim>","device_id":"<victim device_id>"}`
logs in with no Google token / invite / setup key. Guard factor is purely the secrecy of an
unkeyed client registration string. Fix: HMAC the device_id with `token_secret`, or drop it as a factor.

## MEDIUM

- **`email_verified` never enforced on any OIDC path** — `pair.rs:727-733`: every `verify_id_token`
  branch passes `Some(issuer)`, so `email_verified_ok` is always `true` for issuer-set providers.
  An IdP account emitting an unverified email equal to a victim's address logs in as them.
- **`CF-Connecting-IP` trusted on loopback peer -> rate-limit bypass** — `pair.rs:59-80`
  (`client_rate_key`): when TCP peer is loopback (cloudflared -> `127.0.0.1:8080`), client-supplied
  `CF-Connecting-IP`/`X-Real-IP`/`X-Forwarded-For` become the limiter key. LIVE: rotating the
  header defeats the 20/min refresh lockout and the 10/min join/invite brute-force guard
  (25 attempts, zero 429) vs control tripping at the limit. Re-enables offline guessing of the
  6-char invite code and `OURS_SETUP_KEY` behind the tunnel. Fix: don't trust the client header
  for limiter identity (verify via CF edge / key on peer IP).
- **No refresh-family revocation on rotation** — `pair.rs:315-326` deletes only the presented
  hash then rotates; no family/thread tracking. A stolen-then-rotated token stays valid up to the
  180-day TTL unless full logout.
- **Refresh limiter keyed on 64-bit hash prefix** — `pair.rs:285` `&token_hash[..16]`; collisions
  share a bucket (can lock out another client); cycling candidate tokens spreads attempts.

## LOW

- **First-run claim race when `OURS_SETUP_KEY` unset** — `config.rs:27,31-34` + `create_pair`
  (`pair.rs:94-137`): LIVE — `POST /api/pair/create` succeeds with no `X-Setup-Key` when env
  unset. With `0.0.0.0:8080`, whoever hits the server first on first boot claims the owner slot.
- **`nudge`/`sos` have no rate limit** — `events.rs:20-70,96-186`: LIVE — 30 rapid `/api/sos`
  -> 30x 200, zero 429; each fires a real FCM push. SOS `message` unbounded (`:115-118`) vs chat
  2000 / note 500 / place name 100. Notification-fatigue / battery, bounded to the 2 paired users.
- **`join_pair` invite limiter is per-IP only** — `pair.rs:207-208,271`: no global/per-code bucket
  (asymmetric vs `google_auth`'s scoped `email:{email}` bucket). With the forwarded-header rotation
  above, the per-IP wall is removable; `31^6 ~ 887M` still makes full brute-force impractical.
- **WS token in URL query string** — `ws.rs:22-24,113`: validated identically to Bearer
  (signature+exp+revocation+origin, all sound), but query-param tokens are a latent log/Referer
  leak. Not PoC'd (no bypass).
- **No email verification on invite-join** — `join_pair` (`pair.rs:222-259`) + Google Case B create
  a partner for any email + valid invite -> identity/partner-binding confusion, not full takeover.

## HARDENING NOTES (kept, not submitted as standalone findings)

- **`delete_place` (`places.rs:120`) has no ownership guard.** `docs/API.md:280` documents
  `DELETE /api/places/{id} -> 204 (either user)` and `API.md:95` says "Places are shared by both
  users" — shared-resource by design, NOT an IDOR (contrast `delete_checkin`, author-only,
  `checkins.rs:295-297`). It does cascade-wipe both users' geofence state (`place_states ...
  ON DELETE CASCADE`, `db.rs:112-113`) — confirm that's intended, or add the author-scope
  symmetry. One-line fix if you want the guard.
- **Places are never partner-scoped** — safe only while the 2-account invariant holds.
- **Android `TokenCipher.encrypt` fails OPEN to plaintext** when AndroidKeyStore is unavailable
  (`TokenCipher.kt:29-30`), contradicting its doc comment. Device-local at-rest only, no network path.

---

## REFUTED (defending code — no finding)

- OIDC `alg`/`none`/HS256 confusion — RS256 pinned (`pair.rs:705`), `kid` required + resolved
  against JWKS, `exp` enforced by `jsonwebtoken` 9.3.1.
- OIDC `aud` disabled on empty client-IDs — guarded by `if !audience.is_empty()`
  (`pair.rs:789-805`) in the current tree.
- Access-token forgery / `alg` confusion — custom HMAC-SHA256, no `alg` header (`auth.rs:66-70`).
- Revocation bypass / `iat_ms` default-0 — `iat_ms <= revoked_before` rejects it; new tokens
  forced strictly after revocation (`auth.rs:108,212`).
- Account-cap / invite `used_at` TOCTOU — all COUNT/check/UPDATE/INSERT serialized in one
  transaction under the global DB mutex.
- Timing side channels — `constant_time_eq` byte-constant-time for all secret compares.
- SQLi — every user-input query bound-parameterized; `format!` near queries builds notification
  titles only, not SQL.
- Photo/web path traversal, unauth photo access, image-decoder bomb, EXIF leak — photo_id
  validated 32-hex before path join; web serving uses hardcoded filenames +
  `is_safe_asset_filename`; JPEG format pinned; AuthUser required; EXIF stripped on re-encode.
- WS origin/CSWSH, WebSocket hijack — origin validated, token-derived uid, per-conn caps.
- SSRF — all outbound URLs (FCM/JWKS) are static config-built; user token goes in body.
- Secret logging / token alg / cloudflared over-exposure / systemd-root.
- Missing `AuthUser` on any `/api` resource route; place_states cross-user write; checkin/reply/
  message cross-author edit-delete; events inbox cross-recipient read; cursor/int overflow; avatar
  cross-user write; `set_space_name` post-setup mutation; Android IDOR / refresh-rotation race.
