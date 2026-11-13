# OURS: PRD and Design Notes

Notes from a TikTok on design systems, saved for the Android couples app (shared location plus photo and message replies).

**App name: OURS.** A private two-person Android app for live location and shared memories. This doc holds the design tips, animation tips, UX principles, features, and the PRD.

## The list

1. Delete filler badges
2. Drop redundant labels
3. Cut unnecessary sizes and random borders
4. Use mono font only where needed
5. Use a design system
6. Show the actual product
7. Enlarge the main feature
8. Group with spacing
9. Make the headline specific
10. Emphasize the CTA
11. Add an action color
12. Keep icons consistent
13. Remove embellishments
14. Nested radius: outer radius = inner radius + padding
15. Improve text contrast
16. Wrap headlines properly
17. Align by eye, not pixels
18. Use tabular numbers
19. Add loading states
20. Animate user feedback

## Ideas for the couples app

- Loading states and animated feedback fit moments like sending a photo or updating a location.
- Start with a design system so the Android UI stays consistent as the app grows.

## 20 ways to make a beautiful animation

A second TikTok list, this one on motion. Values are as heard, so double-check the numbers before shipping.

1. Ease-out for entrances
2. Keep dropdowns between 150 and 250 ms
3. Stagger releases by about 30 ms
4. Scale to 0.97 on press
5. Stretch moving elements by about 12 to 20%
6. Stage detail states so they settle in
7. Use arcs for object movement
8. Pulse to confirm success
9. Ease-in-out for moves
10. Start popovers at 0.95 scale
11. Enter from the trigger
12. Exit to the trigger
13. No bounce on everyday UI
14. Don't tween while held
15. Make exits about 20% shorter than entrances
16. Overlap old and new states
17. Use masks for ripples and color changes
18. Crossfade icons with a slight blur
19. Rubber-band past the bounds
20. Reuse the same layout ID for shared-element transitions

## My UX and animation principles

My own principle list: the first twelve follow Disney's animation principles, the rest are UX laws and rules of thumb. Names are corrected from the voice transcription.

1. Squash and stretch
2. Anticipation
3. Staging (focus)
4. Pose to pose
5. Follow through
6. Slow in and slow out
7. Arcs
8. Secondary action
9. Timing
10. Exaggeration (feedback)
11. Solid drawing (in 3D)
12. Appeal
13. Fitts's law
14. Bigger hit areas
15. Hick's law
16. Miller's law
17. Doherty threshold
18. Postel's law
19. Jakob's law
20. Frequency of use

## Background location: keeping updates live on Poco / Xiaomi

The problem: Life360-style apps stop updating after a few days unless opened, even with the foreground service and battery exemption already set. Xiaomi's MIUI / HyperOS (used on Poco phones) kills background apps aggressively.

- Run location as a foreground service with a visible notification.
- Ask the user to exempt the app from battery optimization.
- On Xiaomi / Poco: turn on Autostart, set the app's battery saver to "No restrictions", and lock the app in the recent apps screen.
- Detect Xiaomi devices in the app and show a short setup guide on first launch.
- Add a server fallback: if a phone hasn't reported in a while, send a high-priority push that wakes the app to send a fresh location.
- Check dontkillmyapp.com for the exact per-brand steps.

## App features

Two pillars: practical (location) and sentimental (memories).

- Live shared location, always on between the two of us.
- Photo check-ins: post a photo with "I'm here", and the other replies with a message or a photo.
- Shared storage for photos and memories.
- Idea: pin each photo to the spot where it was taken, so the shared map becomes a visual diary of places we've been.
- Idea: "heading home" status with arrival notifications.
- Idea: a "thinking of you" tap that buzzes the other phone.
- Idea: a quiet pause toggle for location sharing that either person can use, even if rarely.

## PRD: Couples app (Android, v1)

**Summary.** A private two-person Android app that combines always-on live location with a shared photo and message stream, so a couple can see where each other is and keep memories in one place.

### Problem

- Life360 stops updating location in the background unless the app is opened every few days, so it often shows a stale position.
- Location apps and photo apps are separate; there is no single private space for the two of us.

### Goals

- Location stays live without the user reopening the app, including on Poco / Xiaomi phones.
- Posting a photo check-in ("I'm here") and replying takes a few taps.
- Memories and places live together on one shared map.
- A consistent design system so the UI stays coherent as features are added.

### Users

- Two people, me and my girlfriend. Android first, I use a Poco phone.

### Core features (v1)

- Live shared location, always on between the two of us.
- Photo check-ins with a message or photo reply.
- Shared storage for photos and memories.

### Later / ideas

- Photos pinned to the place they were taken, forming a map-based memory diary.
- "Heading home" status with arrival notifications.
- "Thinking of you" tap that buzzes the other phone.
- Quiet pause toggle for location sharing.

### Technical requirements

- Foreground service with a visible notification for location.
- Battery optimization exemption, plus a first-launch guide for Xiaomi (Autostart, "No restrictions" battery saver, lock in recents).
- Server fallback: high-priority push to wake the app when a phone hasn't reported in a while.

### Design principles

- Use the 20 design tips, the 20 animation tips, and my principle list above.
- Start from a design system, then build the Android screens.

### Open questions

- Backend: decided to use my home server (free, private, always on). Still to settle: how phones reach it from anywhere (no VPN app on her phone, so a public address through something like Cloudflare Tunnel, secured with login tokens), and photo and location storage on it.
- How long to keep location history, if at all.
- Notification and privacy defaults.
- Home screen layout and navigation.

## Full PRD: Couples app (Android v1)

**Status:** draft, written without a Q&A round from everything discussed so far. This section supersedes the shorter PRD above where they differ.

### 1. Overview

A private, two-person Android app for me and my girlfriend. It shows each other's live location on a shared map and adds a photo and message stream tied to places, so the app is both a safety tool and a shared memory book. It is not a product for the public: no accounts for strangers, no social feed, no ads.

### 2. Problem statement

- Life360-style apps stop refreshing in the background unless the app is opened every few days, so the map often shows a stale position. This persisted on a Poco phone even after enabling the foreground service and battery exemption.
- Location apps and photo apps are separate. There is no single private place for two people that combines where we are with what we saw there.
- Existing apps are built for families or big friend groups, with subscription tiers, circles, driving reports and other features a couple does not need.

### 3. Competitive reference

| App | What it does well | What we take or avoid |
| --- | --- | --- |
| Life360 | Circles of people, real-time location, place alerts when someone arrives or leaves a saved place, location history, crash detection, SOS. Freemium: the free tier limits places (two) and history (two days). | Take: place alerts, history. Avoid: tiers, driving reports, circle management. |
| Zenly | Put the map at the center with an playful, social feel. Shut down by Snap on February 3, 2023. | Take: map-first home screen, expressive feel. Avoid: public friend graph. |
| Bump (from the Zenly team) | Free, precise real-time sharing positioned as the closest Zenly replacement. | Take: map-first simplicity. Battery claims are doubted by some users, so measure our own. |
| iSharing | Location sharing plus messaging, voice and alarms, tuned to reduce battery use. | Take: lightweight messaging next to the map. |

Our gap: none of them combine live location with a shared photo-and-reply memory stream for exactly two people, on a backend we control.

### 4. Goals and non-goals

**Goals**

- Location stays fresh without reopening the app, including on Poco / Xiaomi.
- Posting a check-in photo and replying takes three taps or fewer.
- Photos pin to places, so the map doubles as a memory diary.
- A reusable design system keeps the UI consistent as features grow.
- Runs on my home server at no recurring cost.

**Non-goals (v1)**

- iOS, web, or more than two users.
- Public sharing, social feed, ads, or paid tiers.
- Driving reports, crash detection, or emergency dispatch.

### 5. Users

Two users, me and my girlfriend, each on the same phone model, a Poco X8 (Xiaomi HyperOS). Both consent to always-on sharing; either can pause. Because both phones are Xiaomi, the Autostart and battery setup guide is needed on both.

### 6. User stories

- As a partner, I see the other's live location and battery level on a map.
- As a partner, I post a photo with a short note saying where I am.
- As a partner, I reply to a check-in with a message or a photo.
- As a partner, I browse our photos on a map and as a timeline.
- As a partner, I get a notification when the other arrives home or at a saved place.
- As a partner, I can pause sharing quietly when I need privacy.
- As a partner, I send a "thinking of you" tap that buzzes the other phone.

### 7. Functional requirements

| Area | Requirement | Priority |
| --- | --- | --- |
| Pairing | Link the two phones with a one-time invite code; only two accounts allowed. | Must |
| Live location | Share location continuously; map shows the other's position, last-updated time and battery level. | Must |
| Background reliability | Foreground service, battery exemption, Xiaomi setup guide, and a server push fallback when a phone goes quiet. | Must |
| Check-in | Post a photo with a note and a location tag; the other can reply with text or a photo. | Must |
| Shared storage | All photos kept on our server, browsable as a map view and a timeline. | Must |
| Notifications | Push for new check-ins and replies; place alerts (such as "arrived home") for arriving and leaving saved places. | Must |
| Pause sharing | Quiet toggle either person can use, with a clear "paused" state on the other side. | Should |
| Thinking of you | One tap sends a haptic buzz notification. | Could |
| Location history | Keep route history for 30 days (proposed default, undecided), then delete older data automatically. | Could |

### 8. Design requirements

- Android first, built on a design system with tokens for color, type, spacing, radius, and motion before screens are drawn.
- Map-first home screen with the partner's position, status and battery; check-ins reachable in one tap.
- Apply the 20 design tips (clear headlines, one action color, consistent icons, loading states) and the 20 animation tips (ease-out entrances, 150 to 250 ms durations, no bounce on everyday UI).
- Follow my principle list: Fitts's law for large hit areas, Hick's and Miller's laws to keep choices few, Doherty threshold for fast responses, Jakob's law to stay familiar.
- Every send, upload and location update has a loading state and animated confirmation.

### 9. Technical architecture

- **Client:** native Android app with a foreground location service; see the Front end section below.
- **Backend:** Rust API on my home server; see the Back end section below.
- **Connectivity:** a public address through something like Cloudflare Tunnel, secured with login tokens; no VPN app on her phone.
- **Wake-up:** the server sends a high-priority push when a phone has not reported for a set time, prompting a fresh location.
- **Open point:** reliable Android push without Google services needs a decision; Firebase Cloud Messaging is the easiest and can be used for wake-ups alone.

### 10. Privacy and security

- Only two accounts; all data stays on my server and is never sold or shared.
- Login tokens over HTTPS; photos and locations are not publicly reachable.
- Either person can pause sharing; the paused state is visible.
- Define a retention period for location history and delete older data.

### 11. Success metrics

- The other person's location is never more than a few minutes old over a full week without opening the app.
- A check-in is posted in under 10 seconds from opening the app.
- Battery use from the app stays modest during a normal day.

### 12. Risks and open questions

- Xiaomi / HyperOS may still kill the service even with all settings; needs real-device testing over several days.
- A home server depends on home internet and power; plan backups for photos.
- Choice of database and photo storage layout.
- How long to keep location history: proposing 30 days, to be confirmed.
- Home screen layout and navigation: to be designed next.

### 13. Milestones

1. Design system and home screen mockups.
2. Backend on the home server with pairing and location endpoints.
3. Android app with live location and background reliability tests on Poco.
4. Check-ins, replies and shared photo storage.
5. Notifications, place alerts and pause toggle.
6. Polish: animations, loading states, and the Xiaomi setup guide.

## PRD update: lightweight app, UI/UX, front end and Rust back end

This section adds to the full PRD above. Where it differs, this section wins.

### Lightweight requirement

- The app must be lightweight: small install size, low memory use, minimal battery drain and fast cold start.
- Few dependencies; no heavy SDKs, ad or analytics libraries.
- Location sampling adapts: frequent while moving, sparse while stationary.
- Photos are compressed and resized on the phone before upload; thumbnails are used in lists and on the map.
- The server must run comfortably on a small home machine with low RAM and idle CPU.

### UI / UX

- Map-first home screen showing the partner's position, last-updated time, battery and status, with check-in reachable in one tap.
- Few screens: Map, Memories (timeline and map of photos), Chat/replies, and Settings (places, pause, setup guide).
- Build the design system first: tokens for color, type, spacing, radius and motion, plus a small component set (buttons, cards, bottom sheet, avatar, status chip).
- One action color, consistent icons, clear headlines, loading states, and animated feedback on send and upload, following the saved design and animation lists.
- Keep motion simple and fast: ease-out entrances, 150 to 250 ms, no bounce on everyday UI.
- Large touch targets and few choices per screen (Fitts's, Hick's and Miller's laws).
- First-launch onboarding: pair phones with an invite code, grant location permissions step by step, then the Xiaomi / HyperOS setup guide (Autostart, battery saver "No restrictions", lock in recents).

### Front end (Android)

- Native Android with Kotlin and Jetpack Compose, chosen because it is lightweight, gives direct access to the foreground service, geofencing and battery APIs, and makes the Xiaomi workarounds easier than a cross-platform layer.
- Maps: a lightweight map SDK; OpenStreetMap-based options avoid extra cost and heavy dependencies. To be decided.
- Local storage: a small on-device database for cached locations, check-ins and an upload queue, so the app works through poor connections.
- Background: foreground service for location, geofences for place alerts, and a high-priority push as the wake-up fallback.
- Networking: HTTPS with token auth, a WebSocket or lightweight streaming connection for live updates, and background upload with retry.

### Back end (Rust)

- Language: Rust, preferred for its low memory use and speed on a home server. A framework such as Axum is a likely fit; final choice to be made at build time.
- Database: embedded SQLite for users, locations, places, check-ins and replies; simple, lightweight, and easy to back up.
- Photos: stored as files on local disk with metadata in SQLite; generate thumbnails on upload.
- Real-time: WebSocket endpoint pushing partner location and new messages; REST for the rest.
- Auth: two paired accounts, invite-code pairing, and short-lived tokens over HTTPS.
- Wake-up pushes: server tracks last-seen time per phone and sends a high-priority push when a phone has been quiet too long.
- Deployment: a single small binary on the home server, reached through Cloudflare Tunnel or similar with no VPN app on either phone, with automatic backups of the database and photos.
- Retention: a scheduled job deletes location history older than the retention period (30 days proposed).

### Development environment: connecting to the VPS

- Development happens on my laptop; the VPS sits on the same local network, so the laptop reaches it by its local address.
- VPS local address: 192.168.100.92, SSH user "mnemonic" (credentials are kept out of this doc; use SSH keys instead of a password).
- Local-only: this address works only inside the home network. Phones away from home need the public route through Cloudflare Tunnel or similar.
- Development flow: build and run the Rust server on the VPS, point the Android debug build at the local address, then switch to the tunnel address for real use.
- An AI agent is used alongside to generate code; keep the PRD as the source of truth for what it should build.

## PRD decisions: status, expiry and map

These decisions override earlier wording where they differ.

- **Status shown:** the partner's location, battery level and speed.
- **Check-in expiry:** check-in photos expire after 24 hours, like Instagram stories.
- **Map:** a custom-styled map made for OURS, with its own colors and look that match the design system. The map provider and tile source are still to be decided; a style-able, lightweight option fits best.
- **Highlights: either person can save a check-in photo permanently, like an Instagram highlight, before its 24 hours run out. Saved highlights stay in the shared memory timeline and on the map; unsaved photos are deleted after 24 hours.**

### Decisions from the latest round

- The partner view shows location, battery level and speed.
- Check-in photos expire after 24 hours, like Instagram stories.
- Undecided: whether expired photos disappear completely or move into the shared memories timeline.
- Undecided: map provider (OpenStreetMap is free and light; Google Maps looks more familiar).

## OURS PRD (merged, current)

**This is the single, current PRD for OURS.** It combines the earlier short PRD, the full PRD and the later updates. If it differs from an older section above, this one wins.

### 1. Overview

OURS is a private, two-person Android app for me and my girlfriend. It shows each other's live location on a shared map and adds a photo and message stream tied to places, so it is both a safety tool and a shared memory book. There are no public accounts, no social feed and no ads.

### 2. Problem

- Life360-style apps stop refreshing in the background unless the app is opened every few days, so the map often shows a stale position, even after the foreground service and battery exemption are set.
- Location apps and photo apps are separate; there is no single private space for two people that combines where we are with what we saw there.
- Existing apps are built for families or large groups, with subscription tiers and features a couple does not need.

### 3. Competitive reference

| App | What it does well | What OURS takes or avoids |
| --- | --- | --- |
| Life360 | Circles, real-time location, place alerts on arriving and leaving, location history, crash detection, SOS. Free tier limits places and history. | Take: place alerts. Avoid: tiers, driving reports, circle management. |
| Zenly | Map-first, playful social feel. Shut down by Snap on February 3, 2023. | Take: map-first home screen and expressive feel. Avoid: public friend graph. |
| Bump | Free, precise real-time sharing from the original Zenly team. | Take: map-first simplicity. Measure our own battery use. |
| iSharing | Location plus messaging, voice and alarms, tuned for low battery use. | Take: light messaging beside the map. |

Our gap: none of them combine live location with a shared photo-and-reply memory stream for exactly two people, on a backend we control.

### 4. Goals and non-goals

**Goals**

- Location stays fresh without reopening the app, including on Poco / Xiaomi.
- Posting a check-in and replying takes three taps or fewer.
- Photos pin to places, so the map doubles as a memory diary.
- The app is lightweight: small install, low memory and battery use, fast start.
- A reusable design system keeps the UI consistent as features grow.
- Runs on my home server at no recurring cost.

**Non-goals (v1)**

- iOS, web, or more than two users.
- Public sharing, social feed, ads or paid tiers.
- Driving reports, crash detection or emergency dispatch.

### 5. Users

Two users, me and my girlfriend, each on the same phone model, a Poco X8 (Xiaomi HyperOS). Both consent to always-on sharing; either can pause. Because both phones are Xiaomi, the Autostart and battery setup guide is needed on both.

### 6. Decisions made

- **Always-on sharing** between the two of us, with a quiet pause toggle either person can use.
- **Status shown** for the partner: location, battery level and speed.
- **Arrival alerts** such as "arrived home" are a must-have for v1.
- **Check-in photos expire after 24 hours,** like Instagram stories.
- **Highlights:** either person can save a check-in photo permanently before it expires; saved highlights stay in the shared memory timeline and on the map.
- **Map:** a custom-styled map made for OURS, matching the design system.
- **Backend:** Rust on my home server, reached through a public address with no VPN app on her phone.
- **Location history:** 30 days proposed, still to be confirmed.

### 7. User stories

- I see the other's live location, battery level and speed on a map.
- I post a photo with a short note saying where I am.
- I reply to a check-in with a message or a photo.
- I save a check-in as a highlight before it expires.
- I browse our highlights on a map and as a timeline.
- I get a notification when the other arrives home or at a saved place.
- I pause sharing quietly when I need privacy.
- I send a "thinking of you" tap that buzzes the other phone.

### 8. Functional requirements

| Area | Requirement | Priority |
| --- | --- | --- |
| Pairing | Link the two phones with a one-time invite code; only two accounts allowed. | Must |
| Live location | Share continuously; map shows the other's position, speed, last-updated time and battery level. | Must |
| Background reliability | Foreground service, battery exemption, Xiaomi setup guide, and a server push fallback when a phone goes quiet. | Must |
| Check-in | Post a photo with a note and a location tag; the other replies with text or a photo. Expires after 24 hours. | Must |
| Highlights | Save a check-in permanently before it expires; saved items live in the memory timeline and map. | Must |
| Notifications | Push for new check-ins and replies; place alerts such as "arrived home" for arriving and leaving saved places. | Must |
| Custom map | A map styled for OURS, showing both people, saved places and highlight pins. | Must |
| Pause sharing | Quiet toggle either person can use, with a clear "paused" state on the other side. | Should |
| Thinking of you | One tap sends a haptic buzz notification. | Could |
| Location history | Keep route history for 30 days (proposed, undecided), then delete automatically. | Could |

### 9. UI / UX requirements

- Map-first home screen showing the partner's position, speed, last-updated time and battery, with check-in reachable in one tap.
- Few screens: Map, Memories (highlights as a timeline and map), Chat and replies, and Settings (places, pause, setup guide).
- Build the design system first: tokens for color, type, spacing, radius and motion, plus a small component set (buttons, cards, bottom sheet, avatar, status chip).
- A custom map style that matches the design system.
- One action color, consistent icons, clear headlines, loading states and animated feedback on send and upload, following the saved design and animation lists.
- Fast, simple motion: ease-out entrances, 150 to 250 ms, no bounce on everyday UI.
- Large touch targets and few choices per screen (Fitts's, Hick's and Miller's laws).
- First-launch onboarding: pair phones with an invite code, grant location permissions step by step, then the Xiaomi / HyperOS guide (Autostart, battery saver "No restrictions", lock in recents).

### 10. Technical requirements

**Lightweight**

- Small install size, low memory use, minimal battery drain and fast cold start.
- Few dependencies; no heavy SDKs, ads or analytics libraries.
- Adaptive location sampling: frequent while moving, sparse while stationary.
- Photos compressed and resized on the phone before upload; thumbnails in lists and on the map.

**Front end (Android)**

- Native Android with Kotlin and Jetpack Compose, for light weight and direct access to the foreground service, geofencing and battery APIs.
- Map: a lightweight, style-able map SDK, likely OpenStreetMap-based, with a custom style. Provider still to be chosen.
- Local storage: a small on-device database for cached locations, check-ins and an upload queue.
- Background: foreground service for location, geofences for place alerts, and a high-priority push as the wake-up fallback.
- Networking: HTTPS with token auth, a WebSocket for live updates, and background upload with retry.

**Back end (Rust)**

- Rust, preferred for low memory use and speed on a home server; a framework such as Axum is a likely fit.
- SQLite for users, locations, places, check-ins and replies; photos stored as files on local disk with thumbnails generated on upload.
- WebSocket endpoint for partner location and new messages; REST for everything else.
- Auth: two paired accounts, invite-code pairing, and short-lived tokens over HTTPS.
- Wake-up pushes: the server tracks last-seen time per phone and sends a high-priority push when a phone has been quiet too long. Firebase Cloud Messaging is the easiest route and can be used for wake-ups alone.
- Jobs: delete expired check-in photos after 24 hours unless saved as highlights, and delete location history past the retention period.
- Deployment: a single small binary on the home server, reached through Cloudflare Tunnel or similar, with automatic backups of the database and highlight photos.

**Development environment**

- I code on my laptop; the VPS is on the same local network and reached by its local address (a 192.168.x.x address, still to be filled in).
- Local only: phones away from home use the public tunnel address instead.
- Flow: run the Rust server on the VPS, point the Android debug build at the local address, then switch to the tunnel address for real use.
- An AI agent helps generate code; this PRD is the source of truth for what it should build.

### 11. Privacy and security

- Only two accounts; all data stays on my server and is never sold or shared.
- Login tokens over HTTPS; photos and locations are never publicly reachable.
- Either person can pause sharing, and the paused state is visible.
- Expired photos and old location history are deleted automatically.

### 12. Success metrics

- The other person's location is never more than a few minutes old over a full week without opening the app.
- A check-in is posted in under 10 seconds from opening the app.
- Battery use from the app stays modest during a normal day.

### 13. Risks and open questions

- Xiaomi / HyperOS may still kill the service even with all settings on; needs real-device testing over several days on both phones.
- A home server depends on home internet and power; plan backups.
- Map provider and custom style: to be chosen.
- Location history length: 30 days proposed, to be confirmed.
- VPS local address: to be filled in.
- Home screen layout and navigation: to be designed next.

### 14. Milestones

1. Design system and home screen mockups.
2. Rust backend on the home server with pairing and location endpoints.
3. Android app with live location and background reliability tests on both Poco phones.
4. Check-ins, replies, 24-hour expiry and highlights.
5. Place alerts, notifications and the pause toggle.
6. Custom map style, animations, loading states and the Xiaomi setup guide.
