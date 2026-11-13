# OURS verification checklist

Owner: `verifier`. Source of truth: docs/PRD.md, section "OURS PRD (merged, current)" (line 339+), plus the implementation decisions in the coordinator briefs (board #1, #2). Contract: docs/API.md.

Status key: `PASS`, `FAIL (owner)`, `N/A-sandbox` (needs real device / network, cannot be checked here), `MANUAL` (needs the user on real phones), `IN-PROGRESS`.

Check methods: **B** build/test command, **C** curl/websocket against a locally started server, **R** code review (file:line), **E** emulator smoke test, **D** real-device field test by the user.

## 0. Build gates

| ID | Check | Method | Status | Notes |
| --- | --- | --- | --- | --- |
| B1 | `cd server && cargo build --release --offline` succeeds | B | PASS | |
| B2 | `cd server && cargo test --offline` passes; integration tests cover pairing, refresh, locations + piggyback, pause, places enter/exit, check-in upload/expiry/highlight, replies, photo auth, WebSocket | B + R | PASS | 5/5 integration tests passed |
| B3 | Release profile: lto, opt-level s/3, strip; binary size reported | R + B | PASS | `opt-level="s"`, `lto=true`, `strip=true`, `4.8M` |
| B4 | `cd android && ./gradlew assembleDebug --offline` succeeds | B | PASS | `41M` (arm64-v8a + x86_64) |
| B5 | `./gradlew assembleRelease --offline` succeeds; R8 minify + shrinkResources on; arm64-v8a ABI filter; APK size reported | B + R | PASS | `15M` (`isMinifyEnabled=true`, `isShrinkResources=true`, `arm64-v8a`) |
| B6 | Android unit tests pass (`./gradlew testDebugUnitTest --offline`): adaptive sampling, upload queue, API parsing | B | PASS | 10/10 (`ApiParsingTest` 3, `UploadRetryPolicyTest` 3, `AdaptiveLocationPolicyTest` 4) |
| B7 | Build succeeds with empty Firebase BuildConfig values (no google-services plugin) | B + R | PASS | `FirebaseInitProvider` removed, manual init from `BuildConfig` |

## 1. Functional requirements (PRD §8)

| ID | Req (priority) | Server check | Android check | Status |
| --- | --- | --- | --- | --- |
| F1 | Pairing: one-time invite code, only two accounts (Must) | C: 403 without/wrong `X-Setup-Key`, 201 + 6-char code, 409 second create, 404 bad code, 201 join, 409 third user & `/api/pair/invite` | R: `OnboardingScreen` server URL -> create/join pair; tokens in `PrefsStore` | PASS |
| F2 | Live location: position, speed, last-updated, battery (Must) | C: `POST /api/locations`, `GET /api/partner/location`, `GET /api/partner/history` | R: `MapScreen` shows partner marker, speed, battery, last-updated with `tnum` | PASS |
| F3 | Background reliability: FGS, battery exemption, Xiaomi guide, server push fallback (Must) | R: `jobs.rs` minute wake check, FCM `wake` after `OURS_STALE_MINUTES`, skipped when paused, runs without service account | R: Manifest FGS `location`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, `RECEIVE_BOOT_COMPLETED`, `BootReceiver`, `WatchdogReceiver` (AlarmManager), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `XiaomiGuideHelper`, `OursMessagingService` | PASS |
| F4 | Check-in: photo + note + location tag, reply text/photo, 24h expiry (Must) | C: multipart `POST /api/checkins`, 400px thumb verified, `POST /api/checkins/{id}/replies`, expiry covered by `B2` | R: `ChatScreen` <=3-tap camera flow, 1600px JPEG 80 compression, `UploadManager` retry queue, `countdown24h` | PASS |
| F5 | Highlights: save before expiry, live in timeline + map (Must) | C: `POST`/`DELETE /api/checkins/{id}/highlight`, `GET /api/memories`; expiry survival covered by `B2` | R: `ChatScreen` Save highlight, `MemoriesScreen` timeline + map pins | PASS |
| F6 | Notifications: check-ins, replies, place arrive/leave (Must) | C: place enter/exit with 50m hysteresis (120m no exit, 200m exit, 0m enter), piggyback + inbox + WS | R: `LocationTrackingService` piggyback events -> `OursNotifier`, `POST_NOTIFICATIONS` | PASS |
| F7 | Custom map: both people, places, highlight pins (Must) | — | R: MapLibre + OpenFreeMap + `assets/ours_map_style.json` | PASS |
| F8 | Pause sharing with clear paused state on other side (Should) | C: `POST /api/sharing {paused:true}` clears coords, drops uploads, `partner_paused:true`, history `[]` | R: `SettingsScreen` toggle, `LocationTrackingService`/`WatchdogReceiver`/`BootReceiver` respect `sharingPaused`, `MapScreen` shows "Sharing paused" | PASS |
| F9 | Thinking of you: one tap haptic buzz (Could) | C: `POST /api/nudge` -> 204 + `nudge` event | R: `MapScreen` nudge action, `OursNotifier` | PASS |
| F10 | Location history 30 days, auto delete (Could) | C/R: `/api/partner/history`, `jobs.rs` hourly retention delete | — | PASS |

## 2. User stories (PRD §7)

| ID | Story | Covered by | Status |
| --- | --- | --- | --- |
| U1-U8 | All 8 user stories | F2-F9 | PASS |

## 3. UI / UX (PRD §9 + design lists)

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| X1-X10 | Map-first home, 4 tabs, design tokens (`Tokens.kt`), single action color (`#D95D39`/`#E8714D`), motion 200ms/160ms EaseOut/EaseInOut (no `spring`/bounce), `PRESS_SCALE=0.97f`, `tnum`, nested radius (`mediaCard = inner + sm`), onboarding order | R | PASS |

## 4. Technical requirements (PRD §10)

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| T1-T12 | No Room/KSP/Play Services location; adaptive sampling; `LocationManager`; `SQLiteOpenHelper`; WS + 401 refresh-once; DTOs match `docs/API.md` field-for-field; server env config (refuses without `OURS_SETUP_KEY`); `deploy/` files for `192.168.100.92` (`mnemonic`); server idle RSS `6.21 MB`; `/health` unauthed | R + C | PASS |

## 5. Privacy and security (PRD §11)

| ID | Check | Method | Status |
| --- | --- | --- | --- |
| S1-S9 | 21 protected routes + WS reject missing/invalid/tampered tokens (401); photo auth + path traversal rejected; setup key enforced; 2-user cap; refresh token single-use & hashed in SQLite; paused coords hidden | C + R | PASS |

## 6. Success metrics (PRD §12)

| ID | Metric | How checked | Status |
| --- | --- | --- | --- |
| M1-M3 | Week-long freshness, <10s check-in, modest battery | Real Poco X8 devices | MANUAL |

## 7. Emulator smoke test

| ID | Check | Status |
| --- | --- | --- |
| E1-E2 | Launch debug APK on `dyn35` emulator against local server | N/A-sandbox |
