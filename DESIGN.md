# OURS Design System

## Design Read
Reading this as: private two-person location, photo check-in, and messenger companion app for couples, in an iOS 26+ Liquid Glass + Warm Paper & Terracotta style, dial `ENERGY 2 / RHYTHM 2 / MOTION 2`.

### Liveliness Dials
- **ENERGY 2 (Balanced)**: Calm Warm Paper (`#FAF8F4`) and Jet Black (`#0F0F0E`) ground plane with a single deliberate Warm Terracotta focal accent (`#B24422` light / `#E67B58` dark) reserved for primary CTAs, active navigation, and partner markers.
- **RHYTHM 2 (Consistent with intentional breaks)**: Full-bleed live Map and Memories Map views contrast against structured Timeline, Messenger, and Settings cards.
- **MOTION 2 (Purposeful choreography)**: Apple fluid easing (`CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`), `220ms` entrances, `160ms` exits, `0.97f` press micro-scale, and expanding Nudge ripple waves on the Map.

### Owner Override (R-37 / R-10)
Per explicit owner direction (Audit Option 9A), **iOS 26+ Liquid Glass** is applied across floating chrome, bottom sheets, cards, pills, and input bars on all screens:
- **Multi-stop diagonal refractive surface gradient**: `Brush.linearGradient` from luminous top-left frost (`#F5FFFFFF` light / `#EB262623` dark) to translucent warm tint bottom-right (`#CCFAF8F4` light / `#CC161614` dark).
- **Directional specular rim border**: `1.dp` `Brush.linearGradient` with crisp top-left highlight (`#E6FFFFFF` light / `#66FFFFFF` dark) fading to subtle bottom-right refractive edge (`#4DFFFFFF` light / `#1AFFFFFF` dark).
- **Top-edge inner specular highlight + ambient shadow hierarchy (R-12)**: Subtle `1.dp` top-edge inset luminance paired with calibrated ambient elevation (`8.dp` floating chrome/dock, `4.dp` cards/inputs, `0.dp` flat rows).

### Concentric Corner Radii (R-11)
`R_outer = R_inner + padding`:
- `chip = 10.dp`, `inner = 12.dp`, `control = 14.dp`, `card = 20.dp`, `mediaCard = 24.dp` (`12.dp` inner + `12.dp` padding), `sheet = 24.dp`.

### Typography (R-06)
Clean weight-contrasted sentence/title case without wide-tracked uppercase template headers. Display (`26.sp`, `-0.03em`) and Headline (`20.sp`, `-0.03em`) anchor hierarchy; `tabular` (`tnum`) aligns speeds, distances, battery, and timestamps.

### WCAG AA Color Tokens (R-25)
All normal-text foreground/background pairings clear `4.5:1`:
- **Light (`OursLightColors`)**: `canvas = #FAF8F4`, `surface = #FFFFFF`, `surfaceMuted = #F2EFEA`, `ink = #0F0F0E`, `inkSecondary = #4A4844`, `inkMuted = #5C5954`, `action = #B24422`, `onAction = #FAF8F4`, `actionSoft = #FBECE6`, `success = #196F43`, `successSoft = #E8F5EE`, `danger = #B83232`, `dangerSoft = #FCECEC`, `paused = #54524D`, `pausedSoft = #F0EDE7`.
- **Dark (`OursDarkColors`)**: `canvas = #0F0F0E`, `surface = #181816`, `surfaceMuted = #232320`, `ink = #FAF8F4`, `inkSecondary = #BFBDB8`, `inkMuted = #96948E`, `action = #E67B58`, `onAction = #0F0F0E`, `actionSoft = #332019`, `success = #45C486`, `successSoft = #142E21`, `danger = #E85D5D`, `dangerSoft = #2E1616`.
