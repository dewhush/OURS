# Life360 UI/UX Design System & Feature Benchmark (ADB Inspection -> OURS Adaptation)

> **Source**: Live UI/UX & accessibility tree observation of `com.life360.android.safetymapd` running on `1268x2756 px` (`520 dpi`, scale `3.25x` -> **`390.2 x 848.0 dp`** logical viewport).  
> **Scope**: UI layout metrics, component anatomy, interaction patterns, navigation hierarchy, and complete feature inventory (no personal coordinates/names retained; no proprietary assets copied).

---

## 1. Visual Design System & Measured Metrics (`1 dp = 3.25 px`)

### 1.1 Viewport & Global Shell Geometry
- **Logical mobile viewport**: `390.2 x 848.0 dp` (nearly 1:1 with iOS `390x844`).
- **Floating Top Chrome (`y = 55.1 dp` top offset)**:
  - Left & Right **circular floating icon buttons**: `42.2 x 42.2 dp` (`#FFFFFF` surface + soft multi-directional shadow, borderless).
  - Top-right **vertical FAB stack**: Alerts/Inbox (`y = 55.1 dp`) + Chat/Messaging (`y = 107.1 dp`, tight vertical separation).
  - Top-center **Capsule Switcher pill**: `195.1 x 35.1 dp`, pill radius (`999 dp`), displays active pair/group name + chevron.
- **Map Right-Edge Floating Controls**:
  - Stacked `42.2 x 42.2 dp` circular white FABs on right rail: `+` / Bubble / Share (`y = 264.3 dp`), contextual Recenter crosshair (appears only when map is panned away), `Path History`, and `Map Type / Layers` (`y = 316.6 dp`).
- **Floating Quick Pill Above Sheet (`x = 24.0 dp`, `y = 316.9 dp`)**:
  - Compact `36.0 dp`-tall pill button (`SOS` / primary safety shortcut) anchored cleanly above the sheet top rim.
- **Draggable Pillar Bottom Sheet (`sliding_panel_layout`)**:
  - Peek Y-anchor ~`369 dp` (`43%` down viewport); rounded top corners (~`24 dp`).
  - Centered drag handle pill: `30.2 x 6.2 dp` (list sheet) and `29.5 x 3.4 dp` (member detail sheet).
  - 4 horizontal sheet category icon pills (`People` active: `83.1 x 44.0 dp`; `Pets`, `Items`, `Places`: `70.5 x 40.0 dp`).
  - Member list row height: `98.2 dp` full-bleed row; Settings menu row height: `72.0 dp`; Places geofence row height: `72.9 dp`.
- **Bottom Navigation Bar**:
  - Total height `80.0 dp` (`64.0 dp` icon+label bar + `16.0 dp` system gesture inset).
  - `4` even columns (`91.4 dp` tap width each), `icon (24dp) + 4dp gap + label (11-12sp)`.

### 1.2 Life360 Color Taste (and OURS Translation)
Life360 relies on **soft shadow elevation instead of dark 1px borders**, high-saturation violet/purple branding, and amber/orange accents for warnings & battery:

| Role in Life360 | Observed Life360 Hex | Verdict for OURS | OURS Replacement (Apple Liquid Glass) |
| --- | --- | --- | --- |
| Primary Brand Accent | `#7F54F6` (Violet) | Replace (brand identity) | `#0055FF` Light / `#4D8BFF` Dark (**Electric Blue**) |
| Brand Soft Tint | `#F0EAFD` (Lavender Mist) | Adopt pattern in Blue | `#EBF1FF` Light / `#13203B` Dark (**Electric Soft**) |
| Dark Contrast Accent | `#200244` / `#3B3745` / `#44404E` | Adapt to Obsidian | `#111318` ink & `#090B10` Dark obsidian |
| Tier / Low-Battery Amber | `#F09235` (**Orange**) | **STRICTLY BANNED** | `#0055FF` active, `#5A606D` muted, `#B83232` <15% critical red |
| Upsell CTA Highlight | `#FDF56E` (Neon Yellow) | **Reject** (noisy upsell) | Solid Electric Blue primary pill CTA |
| Surface Canvas / Cards | `#FFFFFF` & `#F6F5F8` band | Upgrade to Broken White + Glass | `#FAF9F5` / `#FFFFFC` / `#F0EFE9` + `blur(28px)` glass chrome |
| List Hairline Divider | `#EDEBF0` | Adopt subtle 1px hairline | `rgba(17, 19, 24, 0.08)` Light / `rgba(250, 249, 245, 0.10)` Dark |
| Sheet Handle / Inactive Chip | `#DBD6E1` / `#EAEAEC` | Adopt neutral density | `rgba(17, 19, 24, 0.16)` / `#F0EFE9` |

### 1.3 Signature Layout & Component Taste Discovered via ADB
1. **Avatar Map Pin Anatomy**:
   - Circular profile photo (`48-56 dp` scaled on map) with crisp white outer ring + purple/blue accent ring + small triangular pointer tail at bottom.
   - Satellite status badge: circular pin icon anchored at top-right of avatar (`+28 deg`) OR floating white dwell pill **`"Here for X mins"`** attached alongside the pin + directional uncertainty cone fan.
   - **Edge-Docked Off-Screen Pins**: When panning/zooming away from a member, their circular avatar docks to the **screen edge** so direction stays visible without hunting.
2. **Row-Attached Micro Telemetry**:
   - In the sheet member row, **battery % (`tnum`)** sits inside a crisp white pill capsule overlapping the bottom edge of the circular avatar (zero clutter inside text columns).
   - **Connectivity warning** sits inline right next to the member's name (`Wi-Fi/offline` glyph), keeping address + `"Since HH:MM"` lines uncluttered.
   - **Direct Heart / React Button** on the right edge of every member row (`click=true`) enables 1-tap emotional feedback without leaving the map.
3. **Focus Mode (`Member Detail` Sheet)**:
   - Tapping a member switches to a dedicated map camera + `< Name / "Last updated X min. ago"` header + manual **Force Refresh** icon button top-right.
   - **Floating Quick-Reaction Pills (`Love ya!`, `Slow down`, `Pick me up!`)** hover as horizontally scrollable white glass pills *just above* the sheet's top curve.
   - Dragging the sheet expands from condensed summary (`35sp` ExtraBold Name + top-right `[battery 47%]` badge + 3D/vector stationary dwell illustration + `Get Place Alerts` inline toggle card) into full weekly telemetry & history.
   - **Shared-Element Transition Feel**: When expanded, the circular avatar anchors across the sheet's top seam (`straddling Y = top edge of sheet`).
   - **Docked Bottom Action Pills**: Horizontal scrollable white pills at the very bottom (`Distance / Route`, `Call`, `Message`, `Create Bubble`, `Alert Status`) + coachmark tooltip (`"Get directions"`).
4. **Split-Tint Hero Card Pattern (`Safety` & `Settings` Submenus)**:
   - Upper white card contains Icon (left) + Bold Title + Plan/Status Badge pill + Top-right Switch; attached bottom strip (`border-radius-bottom: 16dp`) uses a translucent pastel footer (`#F0EAFD` -> OURS `#EBF1FF`) with concise action copy and `>`.
   - Sub-pages (`Location Sharing`, `Circle Management`) use a horizontal carousel explainer hero card with illustration (left) + summary copy (right) + pagination dots underneath before the grouped `72 dp` list items.
5. **Thumbnail Map Type Selector Sheet**:
   - Top row of 3 square/rounded-`14dp` **visual map preview cards** (`Auto`, `Street`, `Satellite`) where the active card shows a `2dp` primary accent ring + colored label, above toggleable map overlay switches.
6. **1-Tap Per-Place Bell Toggle (`Places` Screen)**:
   - List rows (`72.9 dp`) put contextual icon badge on left -> bold place name -> **`×` quick remove** + **circular Bell icon toggle** (filled accent circle = alerts active, outlined circle = muted) on the right.

---

## 2. Complete Life360 Feature Inventory vs. OURS Alignment

| Domain | Life360 Feature | Observed UI Flow | Keep / Skip / Adapt for **OURS** (2-Person Private Space) |
| --- | --- | --- | --- |
| **Live Map & Switching** | Circle Switcher & Multi-Group (`Create / Join`) | Top pill dropdown -> overlay sheet with checkboxes + Create/Join CTA | **Simplify**: OURS is strictly 1 Pair; use top pill for **Live Sync Status / Quick Mode Switcher** (`Partner & Me · Live 18ms`) |
| **Partner Presence** | Pin + Accuracy Cone + `"Here for X mins"` Dwell Badge | Live dot + spinner + dwell duration chip on pin | **Adopt**: Huge upgrade to OURS map feel with Liquid Glass pills |
| **Off-Screen Orientation** | Edge-Docked Partner Pins + Contextual Recenter FAB | Appears only when panned away from partner | **Adopt**: Clean spatial awareness on full-bleed MapLibre canvas |
| **Quick Reactions** | Floating horizontal chips (`Love ya!`, etc.) + Row Heart | Scrollable pill strip above sheet + right row heart icon | **Adopt**: Maps cleanly to OURS *"Thinking of you / Nudge"* + quick preset chips (`Love ya!`, `On my way`, `Safe here`, `Pick me up`) |
| **Privacy Mode** | **Bubbles** (`Create Bubble` + Duration + Access control) & **Temporary Circle** (`Meetup · Duration picker`) | Settings toggle per member + Bottom pill `Create Bubble` + Overlay modal (`Name`, `Duration`, expiry timestamp) | **Adopt Bubble Concept**: Give couples a **Soft Privacy Bubble** option (`Approx. 1km radius · 2h / 6h`, auto-bursts on SOS) alongside **Quiet Pause** |
| **Places & Geofences** | Saved Places list with `×` & Bell mute/unmute + Nearby suggestions search | Search address -> `Locate on Map` -> suggested nearby places -> instant Bell toggle | **Adopt**: Upgrade OURS Places modal with single-tap Bell mute/unmute + visual radius picker + nearby labels |
| **Daily History & Trips** | `Path Trip` button -> `Daily History` (`< Today >`) map route + stop cards | Map button beside Layers opens day-by-day trail + empty/trip states | **Adopt**: Show clean 24h partner route/check-in trail toggle on OURS Map + Memories Map |
| **Driving Safety (Lite)** | Weekly Driving Report chips (`Top Speed`, `Drives`, `Dist`), Speeding/Braking/Distracted pills | Horizontal period chips (`This week`, `Last week`), 4 pastel stat pills, Bento summary cards | **Adopt Lite (No Paywall)**: Optional compact `Trip Telemetry` strip (`Top Speed`, `Trips today`, `Live moving/stopped`) without corporate surveillance feel |
| **Emergency & SOS** | Floating `Set Up SOS` pill + onboarding stepper + `Crash Alerts` + `Emergency Dispatch` | Shield-style stepper checklist + silent emergency broadcast | **Adapt**: Keep OURS `Crash Detection` + **Hold-to-SOS modal** + direct WhatsApp / Push alert (zero subscription gate) |
| **Maps Visual Layer** | `Auto` / `Street` / `Satellite` visual card picker sheet | Modal sheet with 3 map thumbnail previews & overlay toggles | **Adopt**: Visual thumbnail switcher on OURS Map for Light Broken-White vector, Dark Obsidian vector, & Satellite |
| **Built-In Messaging** | External chooser (`SMS / WhatsApp`) + Inbox (`All / Alerts / Offers / Tips`) | Pushes to external SMS/WhatsApp; Inbox mixed with ads | **OURS Advantage**: OURS already has native in-app **Chat + 24h Photo Check-ins + Saved Memories Diary**! Merge Alerts tab cleanly into Notification banner instead of ads |
| **Commercial Bloat** | Tile Hardware Store ads, Amazon Music offers, 3-tier Silver/Gold/Platinum paywall (`Rp 99.000/mo`), Subscription Tracker, ID Theft | Sticky ad banner inside bottom sheet, locked features on Driving/Safety/History | **STRICTLY REJECT**: Zero ads, zero subscriptions, zero locked paywall badges (`OURs is 100% private & free on user's own VPS`) |

---

## 3. Concrete Upgrade Blueprint for OURS (`OURS-Design.figcraft` -> `web/` -> `server/` -> `android/`)

Applying **Life360's spatial ergonomics** inside **OURS's Broken White (`#FAF9F5`) + Electric Blue (`#0055FF` / `#4D8BFF`) Apple Liquid Glass** aesthetic + full security hardening (`#S1`–`#S8`):

1. **Map Screen (`web/` & Android Compose)**:
   - **Floating Liquid Glass Top Pill** (`36dp` tall centered status pill: live sync latency + partner status) flanked by `42dp` frosted Settings & Camera Check-In buttons.
   - **Right Floating Glass Rail**: `Bubble / Privacy Radius`, contextual `Recenter` (when panned), `24h Path Trail` toggle, and `Map Style` thumbnail picker (`Broken White`, `Obsidian`, `Satellite`).
   - **Enhanced Map Markers**: Partner avatar pin with bottom pointer triangle, tabular battery pill overlapping bottom edge, pulsing live freshness dot, directional cone, **`"Here for Xm"` dwell glass badge**, and **edge-docked indicator** when panned off-screen.
   - **3-Detent Draggable Liquid Glass Sheet**:
     - *Collapsed (`peek`)*: Compact Partner + Me rows (`avatar + battery pill`, `place + since`, quick **Heart / Nudge** button) + horizontal quick-reaction pills (`Love ya!`, `On my way`, `Miss u`, `Check-in`).
     - *Mid (`focus`)*: Dwell status + **Trip / Speed summary** + **Bubble Privacy slider** (`Exact` vs `Approx radius 500m-5km`) + Saved Places quick Bell toggles.
     - *Expanded (`history`)*: Today's stops, 24h photo check-in cards, and SOS shortcut.
   - **Bottom Liquid Glass Capsule Dock**: Floating frosted capsule (`Map`, `Memories`, `Chat`, `Settings`) with subtle Electric Blue active indicator and unread badge.
2. **Memories, Chat & Settings Refinements**:
   - **Sentimental Core (Life360 doesn't have!)**: Keep 24h photo check-ins, pinned photo markers on Map/Memories, one-tap `Save` to Memories, and threaded in-app Messenger replies front-and-center.
   - **Places Sheet Upgrade**: `72dp` rows with category icon + instant **Bell mute/unmute toggle** + radius preview slider (`100m–2000m`).
   - **Settings Split-Tint Hero Cards**: Elevated hero explainer header (`Google Verified`, `Security Shield #S1-#S8`, `VPS mnemonic.my.id Live`) above clean hairline-separated grouped controls (`Theme: System / Light / Dark`, `EN / ID`, `Bubbles & Quiet Pause`, `Crash Detection & SOS`, `Reload Latest Web UI`).
