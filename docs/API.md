# OURS server API (v1)

The contract between the Android app and the Rust server. Any change after the first
commit is announced to `@android` on the board.

## Conventions

- Base URL: `http://192.168.100.92:8080` on the LAN, the Cloudflare Tunnel hostname elsewhere. All API paths start with `/api`.
- Request and response bodies are JSON (`Content-Type: application/json`) unless marked multipart. Field names are `snake_case`.
- Timestamps are unix epoch **milliseconds** (`i64`). IDs are `i64` except photo IDs, which are opaque strings.
- Optional fields are always present in responses and set to `null` when empty (never omitted).
- Auth: every `/api` route except `/api/auth/refresh`, `/api/pair/create`, `/api/pair/join` needs
  `Authorization: Bearer <access_token>`. The WebSocket takes the token as a query parameter instead.
- Errors: non-2xx responses carry

  ```json
  {"error": "code", "message": "human readable text"}
  ```

  | HTTP | `error` codes |
  | --- | --- |
  | 400 | `bad_request` (malformed JSON, missing or out-of-range field) |
  | 401 | `unauthorized` (missing/invalid/expired access token), `invalid_refresh_token` |
  | 403 | `forbidden` (bad setup key, not your resource) |
  | 404 | `not_found`, `invite_not_found` (unknown, used or expired invite code) |
  | 409 | `already_set_up`, `pair_full`, `already_paired`, `no_partner` |
  | 410 | `expired` (check-in already expired) |
  | 413 | `too_large` |
  | 500 | `internal` |

  On `401 unauthorized` the client refreshes once and retries; if refresh fails with
  `invalid_refresh_token` the user must pair again.

## Objects

**User**
```json
{"id": 1, "name": "Dewantoro", "created_at": 1731500000000}
```

**Tokens**
```json
{
  "access_token": "opaque string",
  "access_expires_at": 1731503600000,
  "refresh_token": "opaque string",
  "refresh_expires_at": 1747052000000
}
```
Access tokens live 1 hour. Refresh tokens live 180 days and rotate on every use: the old
refresh token is invalid once `/api/auth/refresh` succeeds, so persist the new one before using it.

**LocationPoint** (what the phone uploads, and what history returns)
```json
{
  "lat": -6.2, "lon": 106.8,
  "accuracy_m": 12.5,
  "speed_mps": 1.4,
  "bearing": 270.0,
  "battery_pct": 81,
  "charging": false,
  "recorded_at": 1731500000000
}
```
`lat` in [-90, 90], `lon` in [-180, 180], `accuracy_m >= 0`, `speed_mps >= 0`,
`bearing` is `null` or [0, 360), `battery_pct` in [0, 100] (integer), `charging` bool.

**PartnerLocation**
```json
{
  "user_id": 2,
  "paused": false,
  "last_seen_at": 1731500005000,
  "lat": -6.2, "lon": 106.8, "accuracy_m": 12.5, "speed_mps": 1.4, "bearing": 270.0,
  "battery_pct": 81, "charging": false, "recorded_at": 1731500000000
}
```
`last_seen_at` is the last time the partner's phone made any authenticated request (or `null`).
When `paused` is `true`, or the partner has never sent a location, every field from `lat`
to `recorded_at` is `null`.

**Place**
```json
{"id": 3, "name": "Home", "lat": -6.2, "lon": 106.8, "radius_m": 100.0, "created_by": 1, "created_at": 1731500000000}
```
Places are shared by both users. `radius_m` in [20, 5000].

**Reply**
```json
{"id": 9, "checkin_id": 5, "author_id": 2, "text": "cute!", "photo_id": null, "created_at": 1731500100000}
```
At least one of `text` / `photo_id` is non-null.

**ChatMessage**
```json
{
  "id": 12,
  "author_id": 1,
  "text": "on my way!",
  "photo_id": null,
  "reply_to_message_id": null,
  "reply_to_checkin_id": null,
  "created_at": 1731500200000
}
```
At least one of `text` / `photo_id` is non-null. `reply_to_message_id` and `reply_to_checkin_id` are optional quoted-reply targets (`null` when not replying).

**CheckIn**
```json
{
  "id": 5,
  "author_id": 1,
  "photo_id": "3f9c0a...",
  "note": "coffee",
  "lat": -6.2, "lon": 106.8,
  "place_name": "Kopi Kenangan",
  "created_at": 1731500000000,
  "expires_at": 1731586400000,
  "highlighted": false,
  "highlighted_by": null,
  "highlighted_at": null,
  "replies": [Reply, ...]
}
```
`expires_at` is `created_at + 24h` and stays the same when highlighted; a highlighted check-in is
simply never deleted. `note`, `lat`, `lon`, `place_name` may be `null`. `replies` are oldest first.

**Event**
```json
{"id": 42, "type": "checkin", "created_at": 1731500000000, "actor_id": 1, "data": {...}}
```
Events are addressed to the partner of the actor (you never get your own events).
Inbox events have increasing `id`s; the client keeps the highest id it has seen as its cursor.
Inbox events are kept for 7 days.

| `type` | `data` | In inbox? |
| --- | --- | --- |
| `location` | `PartnerLocation` | **No.** WebSocket only, `id` is `0`. Do not move the cursor for it. |
| `checkin` | `{"checkin": CheckIn}` | yes |
| `reply` | `{"reply": Reply}` | yes |
| `chat_message` | `{"message": ChatMessage}` | yes |
| `highlight` | `{"checkin_id": 5, "highlighted": true}` | yes |
| `sharing` | `{"paused": true}` | yes |
| `nudge` | `{}` | yes |
| `place` | `{"place_id": 3, "place_name": "Home", "transition": "enter" \| "exit", "lat": -6.2, "lon": 106.8}` | yes |

## Health

`GET /health` (no auth) -> `200 {"status": "ok", "version": "0.1.0"}`

## Auth and pairing

### `POST /api/pair/create`
First phone only. Header `X-Setup-Key: <OURS_SETUP_KEY>`. Only works while zero users exist.
```json
{"name": "Dewantoro"}
```
`name`: 1-40 chars after trimming.
-> `201`
```json
{"user": User, "tokens": Tokens, "invite_code": "K7M3QX", "invite_expires_at": 1731586400000}
```
Errors: `403 forbidden` (missing/wrong key), `409 already_set_up`.

Invite codes: 6 chars from `ABCDEFGHJKMNPQRSTUVWXYZ23456789` (no `0 O 1 I L`), valid 24h, single use.
Codes are matched case-insensitively, surrounding spaces ignored.

### `GET /api/pair/invite` (auth)
Issues a fresh invite code (old ones stop working) while the partner has not joined.
-> `200 {"invite_code": "...", "invite_expires_at": 1731586400000}`. Error: `409 already_paired`.

### `POST /api/pair/join`
```json
{"code": "K7M3QX", "name": "Marchanda"}
```
-> `201 {"user": User, "tokens": Tokens}`
Errors: `404 invite_not_found`, `409 pair_full` (two users already exist; there can never be more).

### `POST /api/auth/refresh`
```json
{"refresh_token": "..."}
```
-> `200 Tokens`. Error: `401 invalid_refresh_token` (unknown, already used, or expired).

### `GET /api/me` (auth)
-> `200`
```json
{"me": User, "partner": User | null, "sharing_paused": false, "partner_paused": false, "server_time": 1731500000000}
```

### `POST /api/push-token` (auth)
```json
{"token": "fcm registration token"}
```
-> `204`. Replaces the caller's previous token.

## Location

### `POST /api/locations?after_event_id=<id>` (auth)
Body: JSON array of `LocationPoint`, oldest first, 1-500 items.
-> `200`
```json
{"accepted": 3, "paused": false, "events": [Event, ...]}
```
- Every call updates the caller's `last_seen_at`, even when paused.
- While the caller is paused the points are dropped: `accepted` is `0` and `paused` is `true`.
- The point with the greatest `recorded_at` becomes the caller's latest location if it is newer than the stored one.
  All points go to history when `OURS_HISTORY_DAYS > 0` (default 30 days).
- The server checks place enter/exit on the points (see Places) and pushes a `location` event to the partner's WebSocket.
- `events` is the piggyback inbox: the caller's inbox events with `id > after_event_id`, oldest first, at most 100.
  If `after_event_id` is absent, `events` is `[]`.

### `GET /api/partner/location` (auth)
-> `200 PartnerLocation`. Error: `409 no_partner`.

### `GET /api/partner/history?since=<ms>&until=<ms>` (auth)
`since` required; `until` defaults to now. -> `200 {"points": [LocationPoint, ...]}`, oldest first,
at most 5000 (narrow the window to see more). While the partner is paused, returns `[]`.

### `POST /api/sharing` (auth)
```json
{"paused": true}
```
-> `200 {"paused": true}`. Sends a `sharing` event to the partner when the value changes.
Pausing also clears the caller's latest location on the server.

## Places

Enter/exit is detected on the server from incoming locations. A user is **inside** a place when the
distance to its center is `<= radius_m`, and only counts as **outside** again when the distance is
`> radius_m + 50`. Points with `accuracy_m > 200` are ignored for places. The first point after a place
is created only records the state, without an event. Each transition creates a `place` event for the partner.

- `GET /api/places` -> `200 {"places": [Place, ...]}`
- `POST /api/places` `{"name": "Home", "lat": -6.2, "lon": 106.8, "radius_m": 100}` -> `201 Place`
  (`name` 1-40 chars)
- `DELETE /api/places/{id}` -> `204` (either user)

## Check-ins and replies

### `POST /api/checkins` (auth, `multipart/form-data`)
| part | type | required |
| --- | --- | --- |
| `photo` | JPEG file, max 10 MB | yes |
| `note` | text, max 500 chars | no |
| `lat`, `lon` | decimal text | no (both or neither) |
| `place_name` | text, max 100 chars | no |

-> `201 CheckIn`. The server stores the original and generates a ~400 px (longest side) JPEG thumbnail.
Errors: `400 bad_request` (missing or non-JPEG photo), `413 too_large`. Sends a `checkin` event.

### `GET /api/checkins` (auth)
-> `200 {"checkins": [CheckIn, ...]}`: every check-in by either user that has not expired yet
(created in the last 24h), highlighted or not, newest first, with replies inline.
Older highlighted check-ins are served by `/api/memories`.

### `GET /api/checkins/{id}` (auth)
-> `200 CheckIn`. Expired non-highlighted check-ins are `404 not_found` (even before the hourly sweep deletes them).

### `DELETE /api/checkins/{id}` (auth, author only)
-> `204`. Deletes the check-in, its replies and photos. `403 forbidden` for the partner.

### `POST /api/checkins/{id}/highlight` / `DELETE /api/checkins/{id}/highlight` (auth, either user)
-> `200 CheckIn`. Highlighting an expired check-in fails with `410 expired`. Removing the highlight from a
check-in older than 24h makes it expire (it disappears on the next sweep). Sends a `highlight` event when
the value changes.

### `POST /api/checkins/{id}/replies` (auth, `multipart/form-data`)
Parts: `text` (max 1000 chars) and/or `photo` (JPEG, max 10 MB); at least one.
-> `201 Reply`. Replies are deleted with their check-in and kept while it is highlighted.
Replying to an expired check-in: `410 expired`. Sends a `reply` event.

### `GET /api/memories?before=<ms>&limit=<n>` (auth)
Highlighted check-ins ordered by `created_at` newest first, with `created_at < before` (default: now),
`limit` default 50, max 200.
-> `200 {"items": [CheckIn, ...], "next_before": 1731400000000 | null}` (`null` when there are no more).

## Chat messages

Plain text / photo messages for the messenger thread (merged on the client with 24h check-ins by `created_at`).

### `POST /api/messages` (auth, JSON or `multipart/form-data`)
- **JSON** (`Content-Type: application/json`):
  ```json
  {"text": "on my way!", "reply_to_message_id": null, "reply_to_checkin_id": null}
  ```
- **Multipart** (`Content-Type: multipart/form-data`):
  Parts: `text` (max 2000 chars), `photo` (JPEG, max 10 MB), `reply_to_message_id` (integer text), `reply_to_checkin_id` (integer text).
At least one of `text` or `photo` is required.
-> `201 ChatMessage`. Sends a `chat_message` event to the partner.

### `GET /api/messages?before=<ms>&limit=<n>` (auth)
Messages ordered **newest first** (`created_at DESC, id DESC`), with `created_at < before` (default: now),
`limit` default 50, max 200.
-> `200 {"items": [ChatMessage, ...], "next_before": 1731400000000 | null}` (`null` when there are no more).

## Photos

`GET /api/photos/{photo_id}?size=thumb|full` (auth, default `full`) -> `200` JPEG bytes,
`Content-Type: image/jpeg`, `Cache-Control: private, max-age=31536000, immutable`.
`404 not_found` once the check-in is gone. Photos are never served without a valid token.
Glide/Coil: add the `Authorization` header to image requests.

## Nudge

`POST /api/nudge` (auth, empty body) -> `204`. Sends a `nudge` event ("thinking of you").

## Inbox

`GET /api/events?after=<id>` (auth) -> `200 {"events": [Event, ...]}`: inbox events with `id > after`
(default 0), oldest first, at most 100. Call again with the last id while you get 100.

## WebSocket

`GET /api/ws?token=<access_token>` (upgrade). An invalid token is rejected with `401` before upgrade.

- Server -> client: text frames, each one `Event` JSON (same shape as the inbox), including `location`
  events (`id` 0) whenever the partner uploads a newer location or pauses.
- The server sends a WebSocket ping every 30 s; the client may send `{"type":"ping"}` text frames and
  gets `{"type":"pong"}` back. Other client messages are ignored.
- The socket is not closed when the access token expires, but the client should reconnect with a fresh
  token after network changes.
- After (re)connecting, call `GET /api/events?after=<cursor>` to catch up on anything missed.

## Push (FCM)

Optional; the server works without it (the app then relies on the WebSocket and the piggyback inbox).
All messages are **data-only, high priority**; every value is a string:

| `type` | other keys | meaning |
| --- | --- | --- |
| `wake` | none | your phone has been quiet for `OURS_STALE_MINUTES`; restart the location service and upload now |
| `checkin` | `event_id`, `title`, `body`, `checkin_id` | partner posted a check-in |
| `reply` | `event_id`, `title`, `body`, `checkin_id` | partner replied |
| `chat_message` | `event_id`, `title`, `body`, `message_id` | partner sent a chat message |
| `nudge` | `event_id`, `title`, `body` | thinking of you |
| `place` | `event_id`, `title`, `body`, `place_id`, `transition` | "Marchanda arrived at Home" / "Marchanda left Home" |
| `highlight` | `event_id`, `title`, `body`, `checkin_id` | partner saved a highlight |

`wake` is sent at most once per stale window, and never to a paused phone.
