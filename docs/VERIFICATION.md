# OURS verification checklist

Owner: `verifier`. Source of truth: docs/PRD.md, section "OURS PRD (merged, current)" (line 339+), plus the implementation decisions in the coordinator briefs (board #1, #2). Contract: docs/API.md.

Status key: `TODO` not yet checked, `PASS`, `FAIL (owner)`, `N/A-sandbox` (needs real device / network, cannot be checked here), `MANUAL` (needs the user on real phones).

Check methods: **B** build/test command, **C** curl/websocket against a locally started server, **R** code review (file:line), **E** emulator smoke test, **D** real-device field test by the user.

## 0. Build gates

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| B1 | `cd server && cargo build --release --offline` succeeds | B | TODO |
| B2 | `cd server && cargo test --offline` passes; integration tests cover pairing, refresh, locations + piggyback, pause, places enter/exit, check-in upload/expiry/highlight, replies, photo auth, WebSocket | B + R | TODO |
| B3 | Release profile: lto, opt-level s/3, strip; binary size reported | R + B | TODO |
| B4 | `cd android && ./gradlew assembleDebug --offline` succeeds | B | TODO |
| B5 | `./gradlew assembleRelease --offline` succeeds; R8 minify + shrinkResources on; arm64-v8a ABI filter; APK size reported | B + R | TODO |
| B6 | Android unit tests pass (`./gradlew testDebugUnitTest --offline`): adaptive sampling, upload queue, API parsing | B | TODO |
| B7 | Build succeeds with empty Firebase BuildConfig values (no google-services plugin) | B + R | TODO |

## 1. Functional requirements (PRD §8)

| ID | Req (priority) | Server check | Android check | Status |
| --- | --- | --- | --- | --- |
| F1 | Pairing: one-time invite code, only two accounts (Must) | C: `/api/pair/create` without / wrong `X-Setup-Key` → 401/403; with key → user+tokens+6-char code; second create → rejected; join with code → ok; reuse code → rejected; third join (fresh code via `/api/pair/invite` after pairing) → rejected; expired code (24h) covered by test | R/E: onboarding has server URL → create (setup key) or join (code); tokens stored in private prefs | TODO |
| F2 | Live location: position, speed, last-updated, battery (Must) | C: POST `/api/locations` batch (oldest first); GET `/api/partner/location` returns latest with all fields; `last_seen` updated | R/E: Map screen shows partner marker, speed, battery, last-updated (relative time) with tabular numbers | TODO |
| F3 | Background reliability: FGS, battery exemption, Xiaomi guide, server push fallback (Must) | R: stale-phone job every minute, FCM `wake` data msg after `OURS_STALE_MINUTES`, rate-limited, skipped when paused; server runs and logs "pushes disabled" without service account | R: manifest FGS type `location`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, `RECEIVE_BOOT_COMPLETED` + boot receiver, AlarmManager watchdog, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, Xiaomi guide gated on Build.MANUFACTURER (Xiaomi/Redmi/POCO) with Autostart intent + fallbacks; FCM `wake` → fresh fix + upload | TODO |
| F4 | Check-in: photo + note + location tag, reply text/photo, 24h expiry (Must) | C: multipart POST `/api/checkins` (photo, note, lat, lon, place_name) → 2xx; thumbnail generated (~400px, verify dims); GET `/api/checkins` lists it with replies inline; POST replies (text, photo); expiry job deletes row + replies + files after 24h (test with clock override or test) | R/E: ≤3 taps (button → camera → send); on-device resize max 1600px JPEG ~80; queued upload with retry; 24h countdown shown | TODO |
| F5 | Highlights: save before expiry, live in timeline + map (Must) | C: POST/DELETE `/api/checkins/{id}/highlight`; highlighted check-in + replies survive expiry job; `/api/memories?before&limit` newest first | R/E: save-as-highlight action; Memories timeline + map pins using thumbnails | TODO |
| F6 | Notifications: check-ins, replies, place arrive/leave (Must) | C: events `checkin`, `reply`, `place` created for the partner; place enter inside radius, exit beyond radius+50 m (hysteresis: point at radius+20 m does not exit); event visible in piggyback response of `/api/locations?after_event_id=` and `/api/events?after=` and over WS | R: piggybacked events → local notifications (channels, POST_NOTIFICATIONS runtime request on 33+); FCM path optional | TODO |
| F7 | Custom map: both people, places, highlight pins (Must) | — | R/E: MapLibre with OpenFreeMap tiles and bundled OURS style JSON in assets colored from tokens; markers for me, partner, places, highlights | TODO |
| F8 | Pause sharing with clear paused state on other side (Should) | C: POST `/api/sharing {paused:true}`; subsequent locations not stored; partner GET `/api/partner/location` → `paused:true` and NO lat/lon; `sharing` event to partner; `/api/me` shows `partner_paused`; history hides paused span; no wake pushes while paused | R/E: Settings toggle; service stops uploading (or server ignores) while paused; partner sees "Paused" chip, not a stale position | TODO |
| F9 | Thinking of you: one tap haptic buzz (Could) | C: POST `/api/nudge` → `nudge` event for partner | R: nudge button; notification with vibration on receipt | TODO |
| F10 | Location history 30 days, auto delete (Could) | C/R: `/api/partner/history?since=` returns points; `OURS_HISTORY_DAYS=0` stores none; hourly retention job deletes older rows (test) | R: (optional UI) | TODO |

## 2. User stories (PRD §7)

| ID | Story | Covered by | Status |
| --- | --- | --- | --- |
| U1 | See other's live location, battery, speed on map | F2, F7, E1 | TODO |
| U2 | Post photo with short note saying where I am | F4, E2 | TODO |
| U3 | Reply with message or photo | F4 | TODO |
| U4 | Save a check-in as highlight before it expires | F5 | TODO |
| U5 | Browse highlights on map and timeline | F5, F7 | TODO |
| U6 | Notification when other arrives home / saved place | F6 | TODO |
| U7 | Pause sharing quietly | F8 | TODO |
| U8 | Send "thinking of you" buzz | F9 | TODO |

## 3. UI / UX (PRD §9 + design lists)

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| X1 | Map-first home; check-in reachable in one tap | R/E | TODO |
| X2 | Exactly four screens in bottom bar: Map, Memories, Chat, Settings (places, pause, Xiaomi guide, server URL, sign out) | R | TODO |
| X3 | Design system exists before screens: tokens color/type/spacing/radius/motion; components button, card, bottom sheet, avatar, status chip, loading, send confirmation | R | TODO |
| X4 | One action color: grep screens for hard-coded colors outside the token file | R | TODO |
| X5 | Motion: all durations 150–250 ms, exits ~20% shorter, ease-out entrances, no spring/bounce (grep `spring(`, `DampingRatio`, `tween(` values), press scale 0.97 | R | TODO |
| X6 | Tabular numbers (`fontFeatureSettings = "tnum"`) for speed/battery/time/countdown | R | TODO |
| X7 | Loading states + animated feedback on send/upload/location update | R/E | TODO |
| X8 | Large touch targets (≥48dp), few choices per screen | R | TODO |
| X9 | Nested radius rule (outer = inner + padding) in tokens/components | R | TODO |
| X10 | Onboarding order: server URL → pair → fine location → background "Allow all the time" → notifications → battery exemption → Xiaomi guide (Autostart, No restrictions, lock in recents) | R/E | TODO |

## 4. Technical requirements (PRD §10)

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| T1 | Few deps: no analytics/ads, no Room/KSP, no Navigation lib, no Play Services location | R (build.gradle) | TODO |
| T2 | Adaptive sampling ~30 s moving / ~5 min stationary, unit tested | R + B6 | TODO |
| T3 | Platform LocationManager (fused if available, else GPS+network) | R | TODO |
| T4 | SQLiteOpenHelper cache + upload queue with retry | R + B6 | TODO |
| T5 | WebSocket while foreground; transparent token refresh (401 → refresh → retry, rotation persisted) | R | TODO |
| T6 | Every Android API call matches docs/API.md field-for-field (paths, methods, JSON names, multipart part names, query params) | R | TODO |
| T7 | Server: axum 0.8, rusqlite bundled, image jpeg-only, tokens HMAC-SHA256 1h access / hashed rotating 180d refresh | R | TODO |
| T8 | Server config via env: OURS_BIND, OURS_DATA_DIR, OURS_SETUP_KEY (required, refuses to start without), OURS_TOKEN_SECRET (auto-generated), OURS_HISTORY_DAYS, OURS_STALE_MINUTES, OURS_FCM_SERVICE_ACCOUNT | C/R | TODO |
| T9 | Jobs: hourly expired check-in deletion (rows + files), history retention | R + B2 | TODO |
| T10 | Deploy: systemd unit, example env, cloudflared config, backup.sh (sqlite3 .backup + photos), deploy/README.md for 192.168.100.92 / user mnemonic | R | TODO |
| T11 | Server idle RSS small (measure with `ps` after start) | C | TODO |
| T12 | `/health` works without auth | C | TODO |

## 5. Privacy and security (PRD §11)

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| S1 | Every `/api/*` endpoint except pair/create, pair/join, auth/refresh returns 401 without/with bad/expired token (loop over all routes in API.md) | C | TODO |
| S2 | `/api/photos/{id}` requires auth; no static file serving of data dir; path traversal in photo id rejected | C | TODO |
| S3 | WebSocket rejects missing/invalid token | C | TODO |
| S4 | Setup key enforced; create impossible once a user exists; third user cannot join | C | TODO |
| S5 | Refresh token single-use (reuse of old token rejected), stored hashed in DB | C + R (sqlite3 inspect) | TODO |
| S6 | Tampered access token (modified payload/signature) rejected | C | TODO |
| S7 | Paused user's coordinates never returned (partner/location, history, WS, events) | C | TODO |
| S8 | Android: tokens in private prefs, cleartext only allowed for debug/LAN (network security config), no secrets in repo | R | TODO |
| S9 | Expired photos deleted from disk | C/B2 | TODO |

## 6. Success metrics (PRD §12)

| ID | Metric | How checked | Status |
| --- | --- | --- | --- |
| M1 | Partner location never more than a few minutes old over a week without opening the app | D (real Poco phones, a week). Proxy here: R of FGS + boot + watchdog + FCM wake; server `last_seen` + stale job | MANUAL |
| M2 | Check-in posted in <10 s from opening app | D. Proxy: E tap count ≤3, upload queued async | MANUAL |
| M3 | Battery use modest | D (Android battery stats over a day). Proxy: adaptive sampling + batching | MANUAL |

## 7. Emulator smoke test (if image available)

| ID | Check | Status |
| --- | --- | --- |
| E1 | Install debug APK, server URL `http://10.0.2.2:8080`, create pair, join second user via curl, post partner location via curl, partner marker + battery/speed visible | TODO |
| E2 | Check-in flow posts and appears via `GET /api/checkins` | TODO |

## Findings log

(Filled in as verification runs.)
