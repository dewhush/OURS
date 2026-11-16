# OURS Design Tokens (Slice 1)

Extracted verbatim from [OURS-Design.figcraft](./OURS-Design.figcraft) (`01 Design System`: `Tokens_Colors_Glass`, `Tokens_Type_Radius`, `OURS Tokens` variable collection, and `Glass/*` effect styles).

## 1. Color Tokens (Light & Dark)

| CSS Token | FigCraft Variable / Node | Light Value | Dark Value | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `--bg-canvas` / `--canvas` | `VariableID:1` `bg/canvas` (`24419`) | `#FAF9F5` | `#090B10` | Primary ground canvas (Broken White / Deep Obsidian) |
| `--bg-surface` / `--surface` | `VariableID:2` `bg/surface` (`24427`) | `#FFFFFC` | `#131722` | Solid foreground card & bubble surface |
| `--bg-surface-muted` / `--surface-muted` | `VariableID:3` `bg/muted` (`24431`) | `#F0EFE9` | `#1E2433` | Muted surface, segmented track, secondary fill |
| `--ink` | `VariableID:4` `ink/primary` (`24423`) | `#111318` | `#FAF9F5` | Primary headline and body text |
| `--ink-secondary` | `VariableID:5` `ink/secondary` | `#373B44` | `#C5CAD6` | Supporting copy and metadata |
| `--ink-muted` | `VariableID:6` `ink/muted` | `#555B68` | `#8E96A8` | Captions, placeholders, inactive labels |
| `--line` | `24419` stroke | `#E2E0D8` | `#283044` | Hairline solid divider and card border |
| `--action` / `--action-btn` | `VariableID:7` `accent/electric` (`24435`/`24443`) | `#0055FF` | `#4D8BFF` | Primary CTA, active nav, electric focal accent |
| `--action-hover` / `--action-pressed` | `24439` (`Pressed Blue`) | `#0044D6` | `#3C7EFF` | Hover and pressed state for primary action |
| `--action-soft` | `VariableID:8` `accent/soft` (`24447`) | `#EBF1FF` | `#132240` | Soft electric blue tint fill |
| `--on-action` / `--on-action-btn` | `onAction` | `#FFFFFF` | `#060B16` | Foreground text/icon on `--action` |
| `--success` | `VariableID:9` `status/live` (`24459`) | `#146C43` | `#3CD084` | Live status, safety shield, paired indicator |
| `--success-soft` | `status/liveSoft` | `#E4F4EC` | `#0F271E` | Soft live badge background |
| `--danger` | `VariableID:10` `status/danger` (`24463`) | `#C9182B` | `#FF6B78` | SOS, destructive action, error state |
| `--danger-soft` | `status/dangerSoft` | `#FCECEE` | `#2C1217` | Soft danger badge background |
| `--paused` | `status/paused` | `#555B68` | `#C5CAD6` | Quiet pause state text |
| `--paused-soft` | `status/pausedSoft` | `#ECEAE2` | `#1E2433` | Quiet pause badge background |
| `--map-canvas-bg` | `24468` `GlassBackdrop` | `#F4F3EE` | `#0C1018` | Map canvas fallback ground |
| `--orb-top` | `24469` `RefrOrb1` | `rgba(0, 85, 255, 0.14)` | `rgba(77, 139, 255, 0.18)` | Ambient refraction orb (top-left) |
| `--orb-mid` | `24470` `RefrOrb2` | `rgba(124, 178, 255, 0.16)` | `rgba(37, 99, 235, 0.15)` | Ambient refraction orb (mid) |
| `--orb-bot` | `24472` `RefrOrb3` | `rgba(0, 85, 255, 0.10)` | `rgba(77, 139, 255, 0.12)` | Ambient refraction orb (bottom) |
| `--pin-ring` | Map pin halo | `#FFFFFF` | `#090B10` | Crisp outer ring around map markers |
| `--quote-bg` | Quoted reply fill | `rgba(255, 255, 252, 0.68)` | `rgba(19, 23, 34, 0.72)` | Quoted reply preview surface |

## 2. Liquid Glass Presets (`S:1` - `S:5`, `24474` - `24488`)

| Preset Token | FigCraft Style | Surface Gradient | Directional Specular Rim | Frost / Refraction |
| :--- | :--- | :--- | :--- | :--- |
| `--glass-capsule` (Light) | `S:1` `Glass/CapsuleLight` (`24474`) | `linear-gradient(135deg, rgba(255, 255, 252, 0.722), rgba(242, 245, 255, 0.62))` | `linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.565) 45%, rgba(0, 85, 255, 0.2) 100%)` | `frost: 14px`, `refraction: 0.88`, `dispersion: 0.55`, `depth: 20` |
| `--glass-card` (Light) | `S:2` `Glass/CardLight` (`24477`) | `linear-gradient(135deg, rgba(255, 255, 252, 0.808), rgba(247, 248, 245, 0.729))` | `linear-gradient(135deg, rgba(255, 255, 255, 0.949) 0%, rgba(0, 85, 255, 0.141) 100%)` | `frost: 16px`, `refraction: 0.72`, `dispersion: 0.40`, `depth: 18` |
| `--glass-electric` (Light) | `S:3` `Glass/ElectricTint` (`24480`) | `linear-gradient(135deg, rgba(237, 243, 255, 0.839), rgba(220, 232, 255, 0.761))` | `linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(0, 85, 255, 0.271) 45%, rgba(0, 85, 255, 0.4) 100%)` | `frost: 14px`, `refraction: 0.88`, `dispersion: 0.55`, `depth: 20` |
| `--glass-capsule` (Dark) | `S:4` `Glass/CapsuleDark` (`24483`) | `linear-gradient(135deg, rgba(21, 27, 40, 0.722), rgba(14, 19, 30, 0.651))` | `linear-gradient(135deg, rgba(255, 255, 255, 0.322) 0%, rgba(255, 255, 255, 0.102) 50%, rgba(77, 139, 255, 0.271) 100%)` | `frost: 15px`, `refraction: 0.85`, `dispersion: 0.50`, `depth: 20` |
| `--glass-electric` (Dark) | `S:5` `Glass/ElectricDark` (`24486`) | `linear-gradient(135deg, rgba(22, 42, 82, 0.8), rgba(15, 29, 58, 0.753))` | `linear-gradient(135deg, rgba(139, 184, 255, 0.459) 0%, rgba(77, 139, 255, 0.165) 50%, rgba(77, 139, 255, 0.333) 100%)` | `frost: 15px`, `refraction: 0.85`, `dispersion: 0.50`, `depth: 20` |

## 3. Typography Scale (`24489` `Tokens_Type_Radius`)

| Token | Font Family | Size | Weight | Line Height | Letter Spacing | FigCraft Node |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `--font-sans` | `Inter, -apple-system, BlinkMacSystemFont, sans-serif` | - | - | - | - | Sans stack |
| `--font-mono` | `'Geist Mono', ui-monospace, SFMono-Regular, monospace` | - | - | - | - | Tabular numerals (`tnum`) |
| Display / Title | `--font-sans` | `24px` (`--text-display`) | `600` (SemiBold) | `29px` (`--leading-display`) | `-0.03em` (`--tracking-display`) | `24491` - `24493` |
| Heading | `--font-sans` | `20px` (`--text-heading`) | `600` (SemiBold) | `24px` (`--leading-heading`) | `-0.02em` (`--tracking-heading`) | `24494` - `24496` |
| Card Title | `--font-sans` | `16px` (`--text-title`) | `600` (SemiBold) | `19px` (`--leading-title`) | `-0.01em` | `24497` - `24499` |
| Body | `--font-sans` | `15px` (`--text-body`) | `400` (Regular) | `21px` (`--leading-body`) | `0` | `24500` - `24502` |
| Caption / Meta | `--font-sans` | `12px` (`--text-caption`) | `600` (SemiBold) | `15px` (`--leading-caption`) | `0` | `24503` - `24505` |
| Tabular Mono | `--font-mono` (`font-variant-numeric: tabular-nums`) | `13.5px` (`--text-mono`) | `600` (SemiBold) | `18px` (`--leading-mono`) | `0` | `24506` - `24508` |

## 4. Spacing Scale (`24515` `SpacingBox`)

| CSS Token | Value | FigCraft Node | Usage |
| :--- | :--- | :--- | :--- |
| `--space-xxs` | `4px` | `24516` - `24517` | Micro icon/label gaps |
| `--space-xs` | `8px` | `24518` - `24519` | Compact inline element gaps |
| `--space-sm` | `12px` | `24520` - `24521` | Card internal padding & concentric offset |
| `--space-md` | `16px` | `24522` - `24523` | Standard container padding & vertical rhythm |
| `--space-lg` | `24px` | `24524` - `24525` | Section spacing |
| `--space-xl` | `32px` | `24526` - `24527` | Major layout separation |

## 5. Concentric Corner Radii (`24510` - `24513`: `outer = inner + padding`)

| CSS Token | Value | FigCraft Node | Usage |
| :--- | :--- | :--- | :--- |
| `--radius-chip` | `10px` | `24513` | Status chips, filter tags |
| `--radius-inner` | `12px` | `24511` - `24513` | Nested media/photo frames (`12px` padding -> `24px` outer) |
| `--radius-control` | `14px` | `24513` | Buttons, inputs, squircle badges |
| `--radius-card` | `22px` | `24513` | Content cards and floating headers |
| `--radius-media-card` | `24px` | `24510` - `24513` | `inner (12px) + padding (12px) = 24px` |
| `--radius-sheet` | `28px` | `24513` | Modal bottom sheets |
| `--radius-pill` | `999px` | Capsule nav & pills | Floating capsule dock, action pills |

## 6. Shadows, Borders, Opacity, Icons, Z-Layers, Motion & Viewports

### Shadows
- `--shadow-capsule` (Light): `0 10px 24px -2px rgba(17, 19, 24, 0.102)` (`S:1`, `S:3`)
- `--shadow-capsule` (Dark): `0 12px 28px -2px rgba(0, 0, 0, 0.451)` (`S:4`, `S:5`)
- `--shadow-card` (Light): `0 8px 22px -2px rgba(17, 19, 24, 0.078)` (`S:2`)
- `--shadow-sm`: `0 2px 10px rgba(17, 19, 24, 0.05)` (Light) / `0 2px 12px rgba(0, 0, 0, 0.35)` (Dark)
- `--shadow-md`: `0 10px 24px -4px rgba(17, 19, 24, 0.10)` (Light) / `0 12px 28px -2px rgba(0, 0, 0, 0.56)` (Dark)
- `--shadow-cta`: `0 8px 20px -4px rgba(0, 85, 255, 0.36)` (Light) / `0 8px 22px -4px rgba(77, 139, 255, 0.36)` (Dark)

### Borders & Frost
- `--border-hairline`: `1px`
- `--border-control`: `1.25px`
- `--border-emphasis`: `1.5px`
- `--glass-frost-capsule`: `14px` (Light) / `15px` (Dark)
- `--glass-frost-card`: `16px`
- `--opacity-disabled`: `0.45`
- `--opacity-scrim`: `0.52`

### Icon Sizes
- `--icon-xs`: `14px`
- `--icon-sm`: `17px`
- `--icon-md`: `19px`
- `--icon-lg`: `24px`
- `--icon-xl`: `34px`
- `--icon-tap`: `44px` (minimum interactive touch target)

### Z-Layers
- `--z-base`: `0` (Map / Backdrop)
- `--z-content`: `10` (Scrollable feed / Timeline)
- `--z-chrome`: `30` (Floating glass header / capsule dock / composer)
- `--z-sheet`: `50` (Bottom sheets / dialogs)
- `--z-toast`: `70` (Send confirmations / alerts)

### Motion (`24528` `MotionSpec`)
- `--spring`: `cubic-bezier(0.22, 1, 0.36, 1)`
- `--motion-enter`: `200ms`
- `--motion-exit`: `160ms`
- `--motion-press-scale`: `0.97`

### Exact Viewports & Breakpoints (`24530` - `24539`)
- `--frame-mobile-w`: `390px`
- `--frame-mobile-h`: `844px`
- `--frame-desktop-w`: `1280px`
- `--frame-desktop-h`: `820px`
- Mobile PWA: `< 600px` (Full width)
- Tablet Glass: `600px - 1023px` (`480px` centered column)
- Desktop Web: `>= 1024px` (Split layout)

- `--radius-badge-sm`: `8px`
- `--radius-chip`: `10px`
- `--radius-input-md`: `16px`
- `--radius-input-lg`: `18px`
- `--focus-ring`: `0 0 0 3px var(--action-soft)`
- `--focus-outline`: `2px solid var(--action)`
- `--shadow-btn-primary`: `0 8px 20px rgba(0, 85, 255, 0.24)`

### Fixed Specimen Swatch Tokens (Palette Reference)
- `--swatch-broken-white`: `#FAF9F5`
- `--swatch-midnight-ink`: `#111318`
- `--swatch-warm-ivory`: `#FFFFFC`
- `--swatch-soft-stone`: `#F0EFE9`
- `--swatch-electric-blue`: `#0055FF`
- `--swatch-pressed-blue`: `#0044D6`
- `--swatch-luminous-blue`: `#4D8BFF`
- `--swatch-electric-tint`: `#EBF1FF`
- `--swatch-deep-obsidian`: `#090B10`
- `--swatch-elevated-dark`: `#131722`
- `--swatch-live-green`: `#146C43`
- `--swatch-sos-red`: `#C9182B`
