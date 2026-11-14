# OURS Design System

## Design Read
Reading this as: private two-person location, photo check-in, and messenger companion app for couples, in an Apple HIG Light/Dark & iOS 26+ Liquid Glass style, dial `ENERGY 2 / RHYTHM 2 / MOTION 2`.

### Liveliness Dials
- **ENERGY 2 (Balanced)**: Apple HIG Light (`#F2F2F7` / `#FFFFFF`) and Dark (`#000000` / `#1C1C1E` / `#2C2C2E`) ground plane with Apple System Blue (`#0066CC` light / `#0A84FF` dark) reserved for primary CTAs, active navigation, and partner markers.
- **RHYTHM 2 (Consistent with intentional breaks)**: Full-bleed live Map and Memories Map views contrast against structured Timeline, Messenger, and Settings cards.
- **MOTION 2 (Purposeful choreography)**: Apple fluid easing (`CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`), `220ms` entrances, `160ms` exits, `0.97f` press micro-scale, and expanding Nudge ripple waves on the Map.

### Owner Override (R-37 / R-10)
Per explicit owner direction (Glassmorphism v2, dose-capped per R-10), **translucent Glassmorphism** is reserved exclusively for floating chrome (top headers, `OursFloatingBottomNav` capsule dock, floating Map action pills, bottom sheets, and the single-capsule Chat composer) over a clean vertical architectural backdrop (`OursGlassBackdrop`, R-01):
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
- **Light (`OursLightColors`)**: `canvas = #F2F2F7`, `surface = #FFFFFF`, `surfaceMuted = #E5E5EA`, `ink = #000000`, `inkSecondary = #3C3C43`, `inkMuted = #5C5C60`, `action = #0066CC`, `onAction = #FFFFFF`, `actionSoft = #E5F0FF`, `success = #196F43`, `successSoft = #E8F5EE`, `danger = #B83232`, `dangerSoft = #FCECEC`, `paused = #4B5563`, `pausedSoft = #E5E5EA`.
- **Dark (`OursDarkColors`)**: `canvas = #000000`, `surface = #1C1C1E`, `surfaceMuted = #2C2C2E`, `ink = #FFFFFF`, `inkSecondary = #AEAEB2`, `inkMuted = #8E8E93`, `action = #0A84FF`, `onAction = #FFFFFF`, `actionSoft = #0D2B4D`, `success = #45C486`, `successSoft = #142E21`, `danger = #E85D5D`, `dangerSoft = #2E1616`.
