# OURS Design System

## Design Read
Reading this as: private two-person location, photo check-in, and messenger companion app for couples, in an iOS 26+ Liquid Glass + Apple Cobalt Blue & Crisp Pearl Glass style, dial `ENERGY 2 / RHYTHM 2 / MOTION 2`.

### Liveliness Dials
- **ENERGY 2 (Balanced)**: Crisp Pearl Glass (`#F5F7FA`) and Midnight Slate (`#0B0E14`) ground plane with a single deliberate Apple Cobalt Blue focal accent (`#005EC4` light / `#5EA8FF` dark) reserved for primary CTAs, active navigation, and partner markers.
- **RHYTHM 2 (Consistent with intentional breaks)**: Full-bleed live Map and Memories Map views contrast against structured Timeline, Messenger, and Settings cards.
- **MOTION 2 (Purposeful choreography)**: Apple fluid easing (`CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`), `220ms` entrances, `160ms` exits, `0.97f` press micro-scale, and expanding Nudge ripple waves on the Map.

### Owner Override (R-37 / R-10)
Per explicit owner direction (Glassmorphism v2), **lightweight translucent Glassmorphism** is applied across floating chrome, capsule bottom dock, cards, pills, and single-capsule composer on all screens over a GPU-cached `OursGlassBackdrop` (3 soft radial ambient color orbs):
- **Translucent refractive surface gradient**: `Brush.linearGradient` from luminous top-left frost (`#BDFFFFFF` light / `#A61E2B45` dark) to translucent sky/slate tint bottom-right (`#85E8F1FF` light / `#730F172A` dark), allowing ambient color orbs and live map streets to refract visibly through every panel.
- **Directional specular rim border**: `1.dp` `Brush.linearGradient` with crisp top-left highlight (`#F2FFFFFF` light / `#73FFFFFF` dark) fading to subtle bottom-right refractive edge (`#4D93C5FD` light / `#2660A5FA` dark).
- **Top-edge inner specular highlight + ambient shadow hierarchy (R-12)**: Subtle `1.dp` top-edge inset luminance paired with calibrated ambient elevation (`8.dp` floating capsule dock, `6.dp` floating headers/composer, `4.dp` cards, `0.dp` flat rows) rendered via `drawWithCache` + `ClipOp.Difference`.

### Concentric Corner Radii (R-11)
`R_outer = R_inner + padding`:
- `chip = 10.dp`, `inner = 12.dp`, `control = 14.dp`, `card = 20.dp`, `mediaCard = 24.dp` (`12.dp` inner + `12.dp` padding), `sheet = 24.dp`.

### Typography (R-06)
Clean weight-contrasted sentence/title case without wide-tracked uppercase template headers. Display (`26.sp`, `-0.03em`) and Headline (`20.sp`, `-0.03em`) anchor hierarchy; `tabular` (`tnum`) aligns speeds, distances, battery, and timestamps.

### WCAG AA Color Tokens (R-25)
All normal-text foreground/background pairings clear `4.5:1`:
- **Light (`OursLightColors`)**: `canvas = #F5F7FA`, `surface = #FFFFFF`, `surfaceMuted = #EBEEF3`, `ink = #0F141C`, `inkSecondary = #3E4856`, `inkMuted = #525D6D`, `action = #005EC4`, `onAction = #FFFFFF`, `actionSoft = #E6F0FC`, `success = #196F43`, `successSoft = #E8F5EE`, `danger = #B83232`, `dangerSoft = #FCECEC`, `paused = #4B5563`, `pausedSoft = #EBEEF3`.
- **Dark (`OursDarkColors`)**: `canvas = #0B0E14`, `surface = #131822`, `surfaceMuted = #1C2330`, `ink = #F5F7FA`, `inkSecondary = #B8C2D0`, `inkMuted = #8E99AA`, `action = #5EA8FF`, `onAction = #08101C`, `actionSoft = #122742`, `success = #45C486`, `successSoft = #142E21`, `danger = #E85D5D`, `dangerSoft = #2E1616`.
