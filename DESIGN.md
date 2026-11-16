# OURS Design System

## Design Read
Reading this as: private two-person location, photo check-in, and messenger companion app for couples, in an Apple HIG Light/Dark & iOS 26+ Liquid Glass style, dial `ENERGY 2 / RHYTHM 2 / MOTION 2`.

### Liveliness Dials
- **ENERGY 2 (Balanced)**: Apple Liquid Glass Light (**Broken White** `#FAF9F5` / `#FFFFFC` / `#F0EFE9`) and Dark (**Deep Obsidian** `#090B10` / `#131722` / `#1E2433`) ground plane with **Electric Blue** (`#0055FF` light / `#4D8BFF` dark) reserved for primary CTAs, active navigation, and partner markers (zero orange).
- **RHYTHM 2 (Consistent with intentional breaks)**: Full-bleed live Map and Memories Map views contrast against structured Timeline, Messenger, and Settings cards.
- **MOTION 2 (Purposeful choreography)**: Apple fluid easing (`CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`), `220ms` entrances, `160ms` exits, `0.97f` press micro-scale, and expanding Nudge ripple waves on the Map.

### Owner Override (R-37 / R-10)
Per explicit owner direction (Apple Liquid Glass, dose-capped per R-10), **translucent Glassmorphism** (`backdrop-filter: blur(28px) saturate(190%)` with top specular rim `inset 0 1px 0`) is reserved exclusively for floating chrome (top headers, `OursFloatingBottomNav` capsule dock, floating Map partner card, bottom sheets, and modal overlays) over a subtle ambient radial bloom (`radial-gradient(circle at 20% 10%, rgba(0, 85, 255, 0.09), transparent 55%)`):
- **Translucent refractive surface gradient**: `Brush.linearGradient` from luminous top-left frost (`#BDFFFFFF` light / `#A61E2B45` dark) to translucent sky/slate tint bottom-right (`#85E8F1FF` light / `#730F172A` dark) rendered via `drawWithCache` + `ClipOp.Difference`.
- **Directional specular rim border**: `1.dp` `Brush.linearGradient` with crisp top-left highlight (`#F2FFFFFF` light / `#73FFFFFF` dark) fading to subtle bottom-right refractive edge (`#4D93C5FD` light / `#2660A5FA` dark).
- **Crisp foreground content hierarchy**: Content cards (`OursCard`, default `frosted = false`) and Chat bubbles use solid `surface` / `actionSoft` with `1.dp` `border`, so scrolling content slides cleanly underneath the translucent floating chrome. Search/filter bars in `ChatScreen` and `MemoriesScreen` collapse behind a `44.dp` header search icon toggle (R-05 / C-3).

### Concentric Corner Radii (R-11)
`R_outer = R_inner + padding`:
- `chip = 10.dp`, `inner = 12.dp`, `control = 14.dp`, `card = 20.dp`, `mediaCard = 24.dp` (`12.dp` inner + `12.dp` padding), `sheet = 24.dp`.

### Typography (R-06)
Clean weight-contrasted sentence/title case without wide-tracked uppercase template headers. Display (`26.sp`, `-0.03em`) and Headline (`20.sp`, `-0.03em`) anchor hierarchy; `tabular` (`tnum`) aligns speeds, distances, battery, and timestamps.

### WCAG AA Color Tokens (R-25)
All normal-text foreground/background pairings clear `4.5:1`:
- **Light (`OursLightColors`)**: `canvas = #FAF9F5`, `surface = #FFFFFC`, `surfaceMuted = #F0EFE9`, `ink = #111318`, `inkSecondary = #373B44`, `inkMuted = #5A606D`, `action = #0055FF`, `actionPressed = #0046D6`, `onAction = #FFFFFF`, `actionSoft = #EBF1FF`, `success = #196F43`, `successSoft = #E8F5EE`, `danger = #B83232`, `dangerSoft = #FCECEC`, `paused = #4B5563`, `pausedSoft = #F0EFE9`.
- **Dark (`OursDarkColors`)**: `canvas = #090B10`, `surface = #131722`, `surfaceMuted = #1E2433`, `ink = #FAF9F5`, `inkSecondary = #C5C8D4`, `inkMuted = #8E95A5`, `action = #4D8BFF`, `actionPressed = #3377FF`, `onAction = #060B16`, `actionSoft = #13203B`, `success = #30D158`, `successSoft = #142E21`, `danger = #FF453A`, `dangerSoft = #2E1616`.

### Jakob's Law & Familiar Mental Models
- **Navigation & Bottom Dock**: Four universally familiar primary destinations (`Map`, `Chat`, `Memories`, `Settings`) rendered in the floating bottom dock on mobile and persistent left rail on desktop, with a distinct **Map Pin** glyph for the `Map` destination and a distinct **Navigation / Crosshair** glyph strictly for `Center map`.
- **Progressive Map Disclosure (`4.1` Overview vs. `4.2` Focus)**: The live map overview prioritizes the canvas (`Settings`, `Pair · Live` switcher, `Alerts`, right-edge map utility stack, `Hold 2s SOS`, and the 3-detent Peek Sheet with `All / People / Places / 24h Trail`). Contextual quick-reaction chips (`Love ya!`, `On my way`, `Miss u`, `Check in`) appear when focusing on the partner (`< Back · Partner`) to match standard Find My / Life360 drill-down flows without crowding the overview map.
- **Consumer-First Language**: Everyday onboarding, map, chat, photo check-in, and settings views use natural human copy (`Private invite-only space`, `Camera EXIF metadata removed automatically`, `Privacy & Security`) instead of internal audit codes (`#S1`–`#S9`), while technical inspection lives inside the dedicated `Security Shield` dialog (`Settings -> Review security details`).
- **Messenger & List Ergonomics**: Received bubbles stay left-aligned, sent bubbles right-aligned in `actionSoft` with compact inline read receipts (`✓✓`), active reply banner directly above the rounded composer (`[Camera] [Message...] [↑]`), grouped inset rows with chevrons in `Settings`, and generous scroll clearance (`156px + safe-area-bottom`) above the floating bottom dock.
- **Keyboard & Accessible Semantics (R-03 / R-32 / R-39)**: Explicit `<label>` associations on every field, visible `:focus-visible` contrast rings (`>= 3:1`), `Escape` to close modals, native `<button>` elements (zero nested clickable `div`s), and `>= 44px` touch targets.
