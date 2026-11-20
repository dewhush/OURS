
╔══════════════════════════════════════════════════════════════════════════════╗
║  FIGCRAFT DESIGN EXTRACTION & TOKEN DISCREPANCY REPORT                       ║
║  OURS-Design.figcraft → web/styles.css → Android Theme/Compose/Kotlin       ║
╚══════════════════════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. FIGCRAFT SOURCE OF TRUTH — EXTRACTED DESIGN TOKENS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1.1 COLOR TOKENS (from Swatch frames, id 24419-24466)
─────────────────────────────────────────────────────
  Name                  Chip Color    Hex Label    Notes
  ─────────────────────────────────────────────────────────────
  Broken White          #FAF9F4       #FAF9F5      chip != hex (off by 1 in green)
  Midnight Ink          #111318       #111318      ✓
  Warm Ivory            #FFFFFC       #FFFFFC      ✓
  Soft Stone            #F0EEE9       #F0EFE9      chip != hex (off by 1 in red)
  Electric Blue         #0055FF       #0055FF      ✓
  Pressed Blue          #0044D6       #0044D6      ✓
  Luminous Blue         #4D8BFF       #4D8BFF      ✓
  Electric Tint         #EBF1FF       #EBF1FF      ✓
  Deep Obsidian         #090B10       #090B10      ✓
  Elevated Dark         #131722       #131722      ✓
  Live Green            #146C43       #146C43      ✓
  SOS Red               #C9182B       #C9182B      ✓

  IMPORTANT: Swatch_Broken White chip fill is #FAF9F4 but the hex label says
  #FAF9F5. The chip rectangle (id:24420) has fill #FAF9F4 which differs from
  the text label #FAF9F5. The label is likely the intended value.

  Swatch_Soft Stone chip fill is #F0EEE9 but hex label says #F0EFE9.
  Same discrepancy pattern - the label is likely authoritative.

1.2 TYPOGRAPHY SCALE (from text nodes)
───────────────────────────────────────
  Size    Weight       Font         Line Height  Letter Spacing  Fill
  ───────────────────────────────────────────────────────────────
  24px    SemiBold     Inter        auto         -3%             #FAF9F4 (display sample)
  20px    SemiBold     Inter        auto         0%              #111318 (heading)
  16px    SemiBold     Inter        auto         0%              #111318 (card title)
  15px    Regular      Inter        auto         0%              #FFFFFC (body on dark)
  15px    SemiBold     Inter        auto         0%              #111318 (body on light)
  14.5px  —            Inter        auto         0%              #C5CAD6 (onboarding sub)
  14px    SemiBold     Inter        auto         0%              various
  13.5px  SemiBold     Geist Mono   auto         0%              #111318 (tabular)
  13px    SemiBold     Inter        auto         0%              various
  12.5px  Regular      Inter        auto         0%              #373B44 (bubble sub)
  12px    SemiBold     Inter        auto         0%              various
  11.5px  Medium       Geist Mono   auto         0%              #555B68 (hex labels)
  11px    SemiBold     Inter        auto         0%              various
  11px    Medium       Geist Mono   auto         0%              various
   9.5px  SemiBold     Inter        auto         0%              #0055FF (nav labels active)
   9.5px  Regular      Inter        auto         0%              #555B68 (nav labels)
   9px    SemiBold     Inter        auto         0%              various

  Key observations:
  - Display: 24px SemiBold Inter, -3% letter spacing (matches --text-display)
  - Heading: 20px SemiBold Inter (matches --text-heading)  
  - Body: 15px Regular/SemiBold Inter (matches --text-body)
  - Caption: 12px SemiBold Inter (matches --text-caption)
  - Tabular: 13.5px SemiBold Geist Mono (matches --text-mono)
  - There's a 14.5px Inter text (onboarding subtitle) NOT in CSS tokens
  - There's a 12.5px Regular Inter (bubble subtitle) NOT in CSS tokens
  - Nav label size 9.5px NOT in CSS tokens (CSS uses 9.5px in .nav-item)

1.3 SPACING UNITS (from frame/rectangle dimensions)
────────────────────────────────────────────────────
  The CSS defines: 4/8/12/16/24/32px (xxs/xs/sm/md/lg/xl)
  
  FigCraft uses these exact values extensively:
  - 4px: SplashPill height, border widths
  - 8px: Various padding
  - 12px: Common gap/padding
  - 16px: Card padding, input padding
  - 24px: Card radius base, section gaps
  - 32px: Large gaps
  
  ADDITIONAL FigCraft spacing not in CSS --root:
  - 6px: Nav gap (bottom-nav grid-gap)
  - 9px: Splash wordmark gap
  - 10px: Top-bar gap, chip gap
  - 11px: Peek-me-row gap
  - 13px: Various padding
  - 14px: Top-bar padding, sheet padding
  - 18px: Large padding values
  - 20px: Splash squircle width/height

1.4 BORDER RADII (from corners.radii)
────────────────────────────────────────
  CSS Tokens (from --root):
  --radius-chip: 10px         → FigCraft: 10px ✓ (Chip corners)
  --radius-badge-sm: 8px      → FigCraft: 8px ✓ (settings-hardened-badge)
  --radius-inner: 12px        → FigCraft: 12px ✓ (Stage frames)
  --radius-control: 14px      → FigCraft: 14px ✓ (Switch, SegBtn)
  --radius-input-md: 16px     → FigCraft: 16px ✓ (inputs)
  --radius-input-lg: 18px     → FigCraft: 18px ✓ (code input)
  --radius-card: 22px         → FigCraft: 22px ✓ (Glass_CardLight shadow)
  --radius-media-card: 24px   → FigCraft: 18px for asset cards ✗ DISCREPANCY
  --radius-sheet: 28px        → FigCraft: 28px ✓ (map-peek-sheet)
  --radius-pill: 999px        → FigCraft: pills use 999px ✓
  --radius-bubble: 18px       → FigCraft: 18px ✓ (peek-partner-card)
  --radius-photo-card: 20px   → FigCraft: 20px ✓ (check-in photo preview)
  --radius-nav: 31px          → FigCraft: 31px ✓ (bottom-nav)
  --radius-nav-tab: 24px      → FigCraft: 24px ✓ (nav-item)
  --radius-toast: 34px        → NOT in FigCraft (no toast mockup)
  --radius-row: 18px          → FigCraft: 18px ✓ (asset cards)
  --radius-row-sm: 16px       → FigCraft: 16px ✓ (various)
  --radius-switch: 14px       → FigCraft: 14px ✓ (switch)
  --radius-handle: 3px        → FigCraft: 3px ✓ (switch handle)
  
  ADDITIONAL FigCraft radii not in CSS:
  - 20px: Glass frames (capsule/card light)
  - 21px: FAB and nav slot containers
  - 26px: GoogleHeroGlassCard, CreateCodeGlassCard
  - 27px: Continue button, SendCheckInCTA
  - 30px: BubbleGlassSheet, CheckInGlassSheet, LayersGlassSheet
  - 42px: Splash screen corner radius
  - 13px: Dynamic Island corner radius
  - 6px: FabCamBody corner radius

  DISCREPANCY: CSS --radius-media-card = 24px but FigCraft asset cards use
  18px corner radius (AssetCard_* all have corners: [18,18,18,18]).
  
  DISCREPANCY: CSS --radius-card = 22px but FigCraft Glass_CardLight uses
  20px corner radius. The CSS value is 2px larger.

1.5 SHADOW VALUES (from effects.dropShadow)
────────────────────────────────────────────
  CSS → FigCraft mapping:
  --shadow-sm: 0 2px 10px rgba(17,19,24,0.05)       
              → FigCraft: 0 6px 18px rgba(17,19,24,0.078) ✗ DISCREPANCY
  --shadow-card: 0 8px 22px -2px rgba(17,19,24,0.078)
              → FigCraft: 0 8px 22px -2px rgba(17,19,24,0.078) ✓
  --shadow-capsule: 0 10px 24px -2px rgba(17,19,24,0.102)
              → FigCraft: 0 10px 24px -2px rgba(17,19,24,0.102) ✓
  --shadow-md: 0 10px 24px -4px rgba(17,19,24,0.10)
              → FigCraft: 0 10px 24px -2px rgba(17,19,24,0.102) ✗ spread differs
  --shadow-cta: 0 8px 20px -4px rgba(0,85,255,0.36)
              → FigCraft: 0 8px 20px rgba(0,85,255,0.24) ✗ DISCREPANCY
  
  DISCREPANCY details:
  - --shadow-sm: CSS uses 2px offset/10px radius/0.05 alpha but FigCraft uses
    6px offset/18px radius/0.078 alpha. Major differences in all three params.
  - --shadow-md: CSS spread=-4px but FigCraft spread=-2px. CSS blur is 2px more.
  - --shadow-cta: CSS uses 0.36 alpha but FigCraft uses 0.24 alpha. CSS is 50%
    more opaque. Also CSS has spread=-4px but FigCraft doesn't use spread.

  ADDITIONAL FigCraft shadows not in CSS:
  - Splash screen: 0 22px 52px -4px rgba(0,0,0,0.40) — no CSS equivalent
  - Dark glass: 0 12px 28px -2px rgba(0,0,0,0.451) — mapped to --shadow-capsule dark
  - Nested radius demo: 0 6px 18px -2px rgba(17,19,24,0.078)

1.6 GRADIENT DEFINITIONS (from fills.gradient)
───────────────────────────────────────────────
  CSS Glass Gradients → FigCraft:
  --glass-capsule: linear-gradient(135deg, rgba(255,255,252,0.722), rgba(242,245,255,0.62))
                  → FigCraft: @0.0 #FFFFFC alpha=0.722, @1.0 #F1F4FF alpha=0.620
                  → ANGLE MISMATCH: CSS says 135deg, FigCraft uses 135° ✓ (both match)
  
  --glass-card: linear-gradient(135deg, rgba(255,255,252,0.808), rgba(247,248,245,0.729))
              → FigCraft: @0.0 #FFFFFC alpha=0.808, @1.0 #F6F7F4 alpha=0.729
              → END COLOR MISMATCH: CSS uses #F4F3EE-like (#F4F4F5 is close), 
                FigCraft uses #F6F7F4. CSS end is slightly warmer.
  
  --glass-electric: linear-gradient(135deg, rgba(237,243,255,0.839), rgba(220,232,255,0.761))
                  → FigCraft: @0.0 #EDF3FF alpha=0.839, @1.0 #DCE7FF alpha=0.761 ✓
  
  --glass-rim-capsule: linear-gradient(135deg, rgba(255,255,255,0.98) 0%, 
                                         rgba(255,255,255,0.565) 45%, 
                                         rgba(0,85,255,0.2) 100%)
                     → FigCraft: NOT FOUND as a single gradient. The rim is painted
                       via CSS mask technique, not a FigCraft fill.
  
  --cta-grad: linear-gradient(180deg, #1767FF 0%, var(--action-pressed) 100%)
             → FigCraft: @0.0 #1767FF alpha=1.0, @1.0 #0046D6 alpha=1.0
             → END COLOR: CSS uses #0044D6 (--action-pressed), FigCraft uses #0046D6
               MISMATCH: off by 2 in green channel.

  DISCREPANCY: CTA gradient endpoint - CSS #0044D6 vs FigCraft #0046D6.

  Dark mode gradients in FigCraft not fully reflected in CSS:
  --glass-capsule-dark: FigCraft @0.0 #141B28 alpha=0.722, @1.0 #0D131E alpha=0.651
                       → CSS: linear-gradient(135deg, rgba(21,27,40,0.722), rgba(14,19,30,0.651))
                       → START MISMATCH: FigCraft #141B28 = rgb(20,27,40), CSS #151B28 = 
                         rgb(21,27,40). Off by 1 in red.
  --glass-electric-dark: FigCraft @0.0 #162A52 alpha=0.800, @1.0 #0F1C39 alpha=0.753
                       → CSS: linear-gradient(135deg, rgba(22,42,82,0.8), rgba(15,29,58,0.753))
                       → START MISMATCH: FigCraft #162A52 = rgb(22,42,82), CSS matches.
                         END MISMATCH: FigCraft #0F1C39 = rgb(15,28,57), CSS rgb(15,29,58).
                         Off by 1 in green and blue.

1.7 GLASS REFRACTION PARAMETERS (from GDesc text nodes)
─────────────────────────────────────────────────────────
  Glass/CapsuleLight:   refraction 0.88, frost 14, specular rim
  Glass/CardLight:      refraction 0.72, frost 16, warm ivory
  Glass/ElectricTint:   refraction 0.78, electric blue wash
  Glass/CapsuleDark:    dark obsidian #131722B5, #4D8BFF rim
  Glass/ElectricDark:   luminous electric dark #162A52CC

  NOTE: These "refraction" values are conceptual design notes, not pixel values.
  The CSS implements them via backdrop-filter: blur() and saturate() values.
  CSS backdrop-filter values:
  - blur(22px) saturate(185%) for top-bar (capsule)
  - blur(18px) saturate(180%) for cards
  - blur(24px) saturate(190%) for bottom-nav
  - blur(26px) saturate(195%) for map-peek-sheet
  - blur(20px) saturate(190%) for react-chip

  These CSS values are not directly in FigCraft - they're the implementor's
  interpretation of the "refraction" notes.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2. WEB/CSS DISCREPANCIES (styles.css :root vs FigCraft)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2.1 COLOR DISCREPANCIES
────────────────────────
  ┌─────────────────────────────────────────────────────────────────┐
  │ TOKEN                  FIGCRAFT         CSS             Δ       │
  ├─────────────────────────────────────────────────────────────────┤  
  │ --bg-canvas            #FAF9F5          #FAF9F5         ✓       │
  │ --bg-surface           #FFFFFC          #FFFFFC         ✓       │
  │ --bg-surface-muted     #F0EFE9          #F0EFE9         ✓       │
  │ --ink                  #111318          #111318         ✓       │
  │ --ink-secondary        #373B44          #373B44         ✓       │
  │ --ink-muted            #555B68          #555B68         ✓       │
  │ --line                 #E2E0D8          #E2E0D8         ✓       │
  │ --action               #0055FF          #0055FF         ✓       │
  │ --action-pressed       #0044D6          #0044D6         ✓       │
  │ --action-hover         #0044D6          #0044D6         ✓       │
  │ --cta-grad start       #1767FF          #1767FF         ✓       │
  │ --cta-grad end         #0046D6          #0044D6         ✗ 2ΔG  │
  │ --on-action            #FFFFFF          #FFFFFF         ✓       │
  │ --action-soft          #EBF1FF          #EBF1FF         ✓       │
  │ --success              #146C43          #146C43         ✓       │
  │ --danger               #C9182B          #C9182B         ✓       │
  │ --paused               #555B68          #555B68         ✓       │
  │ --glass-capsule end    #F1F4FF          #F2F4FD (approx) ✗ ~1Δ   │
  │ --glass-card end       #F6F7F4          #F4F4F5 (approx) ✗ ~2Δ   │
  │ --glass-electric end   #DCE7FF          #DCE7FF         ✓       │
  │ --border               rgba(17,19,24,0.09) —             —       │
  │   FigCraft doesn't define a standalone border color.            │
  │ --shadow-sm α          0.078            0.05            ✗ 56%↓  │
  │ --shadow-md spread     -2px             -4px            ✗ 2px   │
  │ --shadow-cta α         0.24             0.36            ✗ 50%↑  │
  └─────────────────────────────────────────────────────────────────┘

2.2 TYPOGRAPHY DISCREPANCIES
─────────────────────────────
  ┌─────────────────────────────────────────────────────────────────┐
  │ TOKEN                  FIGCRAFT         CSS             Δ       │
  ├─────────────────────────────────────────────────────────────────┤
  │ --text-display         24px SemiBold    24px            ✓       │
  │ --leading-display      auto              29px            —       │
  │   FigCraft doesn't set explicit line-height on display text.    │
  │ --text-heading         20px SemiBold    20px            ✓       │
  │ --text-title           16px SemiBold    16px            ✓       │
  │ --text-body            15px Regular     15px            ✓       │
  │ --text-caption         12px SemiBold    12px            ✓       │
  │ --text-mono            13.5px SemiBold  13.5px          ✓       │
  │                                                                 │
  │ MISSING FROM CSS:                                               │
  │ - 14.5px Inter (onboarding subtitle, node 26505 DS1T)          │
  │   CSS has hardcoded .onb-content .row-sub { font-size: 14.5px }│
  │   but no CSS variable for it.                                  │
  │ - 12.5px Regular Inter (bubble subtitle)                      │
  │   CSS hardcodes .split-hero-tint-sub { font-size: 11.5px }     │
  │   but FigCraft uses 12.5px. MISMATCH.                         │
  │ - 9.5px nav labels (CSS uses 9.5px in .nav-item, matches)     │
  │                                                                 │
  │ FONT MISMATCH:                                                  │
  │ - FigCraft uses Inter + Geist Mono                             │
  │ - CSS uses Inter + Geist Mono ✓                               │
  │ - Android Compose uses FontFamily.SansSerif (NOT Inter!)       │
  │   ✗ CRITICAL: Android doesn't load Inter font.                │
  └─────────────────────────────────────────────────────────────────┘

2.3 SPACING DISCREPANCIES
──────────────────────────
  ┌─────────────────────────────────────────────────────────────────┐
  │ TOKEN                  FIGCRAFT         CSS             Δ       │
  ├─────────────────────────────────────────────────────────────────┤
  │ --space-xxs            4px              4px             ✓       │
  │ --space-xs             8px              8px             ✓       │
  │ --space-sm             12px             12px            ✓       │
  │ --space-md             16px             16px            ✓       │
  │ --space-lg             24px             24px            ✓       │
  │ --space-xl             32px             32px            ✓       │
  │                                                                 │
  │ MISSING FROM CSS --root:                                       │
  │ - 6px: bottom-nav gap (FigCraft: 4px gap between nav items)   │
  │   CSS uses gap: 4px in .bottom-nav, not 6px.                  │
  │   Actually FigCraft bottom-nav has gap: 4px, CSS has gap: 4px. │
  │ - 9px: splash wordmark gap                                    │
  │   CSS uses gap: 20px in .splash-stage. MISMATCH.             │
  │   Wait - FigCraft SplashWordL is at y-position with 9px gap   │
  │   from rule, but .splash-stage has gap: 20px.                 │
  │   Actually looking more carefully:                            │
  │   FigCraft SplashRuleL y=462, SplashWordL y=436 → 26px gap   │
  │   FigCraft SplashWordL y=436, SplashSubL y=470 → 34px gap   │
  │   These don't match CSS gap: 20px.                           │
  └─────────────────────────────────────────────────────────────────┘

2.4 RADIUS DISCREPANCIES
─────────────────────────
  ┌─────────────────────────────────────────────────────────────────┐
  │ TOKEN                  FIGCRAFT         CSS             Δ       │
  ├─────────────────────────────────────────────────────────────────┤
  │ --radius-chip          10px             10px            ✓       │
  │ --radius-badge-sm      8px              8px             ✓       │
  │ --radius-inner         12px             12px            ✓       │
  │ --radius-control       14px             14px            ✓       │
  │ --radius-input-md      16px             16px            ✓       │
  │ --radius-input-lg      18px             18px            ✓       │
  │ --radius-card          20px (FigCraft)  22px (CSS)      ✗ +2px  │
  │ --radius-media-card    18px (asset cards) 24px (CSS)    ✗ +6px  │
  │ --radius-sheet         28px             28px            ✓       │
  │ --radius-pill          999px            999px           ✓       │
  │ --radius-nav           31px             31px            ✓       │
  │ --radius-nav-tab       24px             24px            ✓       │
  │ --radius-bubble        18px             18px            ✓       │
  │ --radius-photo-card    20px             20px            ✓       │
  │                                                                 │
  │ MISSING FROM CSS:                                              │
  │ - 20px: Glass capsule/card frame radius (FigCraft base)       │
  │ - 21px: FAB containers, nav slots                            │
  │ - 26px: Google hero card, create code card                    │
  │ - 27px: Continue button, send check-in CTA                    │
  │ - 30px: Bubble/Check-in/Layers sheet radius                   │
  │ - 42px: Splash screen radius                                  │
  │ - 13px: Dynamic Island radius                                 │
  │ - 6px: FabCamBody radius                                     │
  │ - 4px: FabCamBody corner (actually 4px in FigCraft)          │
  └─────────────────────────────────────────────────────────────────┘

2.5 SHADOW DISCREPANCIES
─────────────────────────
  ┌─────────────────────────────────────────────────────────────────┐
  │ TOKEN                  FIGCRAFT         CSS             Δ       │
  ├─────────────────────────────────────────────────────────────────┤
  │ --shadow-sm            0 6px 18px      0 2px 10px      ✗ MAJOR │
  │   alpha                0.078            0.05            ✗ 56%↑  │
  │ --shadow-card          0 8px 22px -2px 0 8px 22px -2px ✓       │
  │ --shadow-capsule       0 10px 24px -2px 0 10px 24px -2px ✓     │
  │ --shadow-md            0 10px 24px -2px 0 10px 24px -4px ✗ spread│
  │ --shadow-cta           0 8px 20px 0px  0 8px 20px -4px  ✗ spread│
  │   alpha                0.24             0.36            ✗ 50%↑  │
  │                                                                 │
  │ MISSING FROM CSS:                                              │
  │ - Splash: 0 22px 52px -4px rgba(0,0,0,0.40)                 │
  │ - Dark glass: 0 12px 28px -2px rgba(0,0,0,0.451)            │
  └─────────────────────────────────────────────────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3. ANDROID THEME DISCREPANCIES (themes.xml + Compose Theme.kt/Tokens.kt)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3.1 ANDROID THEMES.XML (basic color scheme)
─────────────────────────────────────────────
  Light theme:  windowBackground = #FAF9F5 → matches FigCraft #FAF9F5 ✓
  Dark theme:   windowBackground = #090B10 → matches FigCraft #090B10 ✓
  
  ONLY two colors defined in themes.xml. No typography, spacing, radius,
  shadow, or glass gradient tokens. This is a bare-minimum Android theme.

3.2 ANDROID COMPOSE THEME.KT / TOKENS.KT DISCREPANCIES
────────────────────────────────────────────────────────
  ┌─────────────────────────────────────────────────────────────────┐
  │ TOKEN                  FIGCRAFT         Android Compose   Δ       │
  ├─────────────────────────────────────────────────────────────────┤
  │ CANVAS (light)         #FAF9F5          #FAF9F5           ✓       │
  │ CANVAS (dark)          #090B10          #090B10           ✓       │
  │ SURFACE (light)        #FFFFFC          #FFFFFC           ✓       │
  │ SURFACE (dark)         #131722          #131722           ✓       │
  │ SURFACE_MUTED (light)  #F0EFE9          #F0EFE9           ✓       │
  │ SURFACE_MUTED (dark)   #1E2433          #1E2433           ✓       │
  │ INK (light)            #111318          #111318           ✓       │
  │ INK (dark)             #FAF9F5          #FAF9F5           ✓       │
  │ INK_SECONDARY (light)  #373B44          #373B44           ✓       │
  │ INK_SECONDARY (dark)   #C5CAD6          #C5C8D4           ✗ 1ΔB  │
  │ INK_MUTED (light)      #555B68          #5A606D           ✗ ~5Δ  │
  │ INK_MUTED (dark)       #8E96A8          #8E95A5           ✗ ~1Δ  │
  │ ACTION (light)         #0055FF          #0055FF           ✓       │
  │ ACTION (dark)          #4D8BFF          #4D8BFF           ✓       │
  │ ON_ACTION (light)      #FFFFFF          #FFFFFF           ✓       │
  │ ON_ACTION (dark)       #060B16          #060B16           ✓       │
  │ ACTION_SOFT (light)    #EBF1FF          #EBF1FF           ✓       │
  │ ACTION_SOFT (dark)     #132240          #13203B           ✗ ~3Δ  │
  │ BORDER (light)         rgba(17,19,24,0.09) #E4E2DA       ✗ MAJOR│
  │ BORDER (dark)          rgba(250,249,245,0.11) #282F42    ✗ MAJOR│
  │ ERROR/LIVE             #C9182B          #D70015           ✗ MAJOR│
  │ SUCCESS (light)        #146C43          #1D7A3B           ✗ ~5Δ  │
  │ SUCCESS (dark)         #3CD084          #30D158           ✗ ~3Δ  │
  │ DANGER (light)         #C9182B          #D70015           ✗ MAJOR│
  │ DANGER (dark)          #FF6B78          #FF453A           ✗ ~4Δ  │
  │ PAUSED (light)         #555B68          #373B44           ✗ MAJOR│
  │ PAUSED (dark)          #C5CAD6          #C5C8D4           ✗ 1ΔB  │
  └─────────────────────────────────────────────────────────────────┘

  CRITICAL COLOR MISMATCHES IN ANDROID:
  - Android error/danger = #D70015 (orange-red), FigCraft/CSS = #C9182B (pure red)
  - Android success (light) = #1D7A3B (slightly greener), FigCraft = #146C43
  - Android success (dark) = #30D158 (brighter green), FigCraft/CSS = #3CD084
  - Android inkMuted (light) = #5A606D, FigCraft/CSS = #555B68 — different hue
  - Android border (light) = #E4E2DA, CSS = rgba(17,19,24,0.09) ≈ #E2E0D8
    Android border is warmer/pinker than CSS
  - Android border (dark) = #282F42, CSS = rgba(250,249,245,0.11) ≈ #283044
    Android border is slightly bluer

  ┌─────────────────────────────────────────────────────────────────┐
  │ TYPOGRAPHY              FIGCRAFT         Android Compose   Δ     │
  ├─────────────────────────────────────────────────────────────────┤
  │ display                24px SemiBold    26sp SemiBold     ✗ +2px │
  │ headline               20px SemiBold    20sp SemiBold     ✓     │
  │ title                  16px SemiBold    16sp Medium       ✗ wt   │
  │ body                   15px Regular     15sp Normal       ✓     │
  │ caption                12px SemiBold    13sp Normal       ✗ +1px,│
  │                                                                wt    │
  │ label                  NOT DEFINED      12sp Medium       —     │
  │ tabular                13.5px SemiBold  13sp Medium       ✗ -0.5,│
  │                                                                wt    │
  │ tabularLarge           NOT DEFINED      16sp SemiBold     —     │
  │                                                                 │
  │ FONT FAMILY: FigCraft=Inter, CSS=Inter, Android=SansSerif ✗   │
  │   Android does not load Inter font. Uses system default.      │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │ SPACING                 FIGCRAFT         Android Compose   Δ     │
  ├─────────────────────────────────────────────────────────────────┤
  │ xxs                     4px              4.dp              ✓     │
  │ xs                      8px              8.dp              ✓     │
  │ sm                      12px             12.dp             ✓     │
  │ md                      16px             16.dp             ✓     │
  │ lg                      24px             24.dp             ✓     │
  │ xl                      32px             32.dp             ✓     │
  │                                                                 │
  │ Missing: 6px, 9px, 10px, 11px, 13px, 14px, 18px, 20px, etc.  │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │ RADIUS                  FIGCRAFT         Android Compose   Δ     │
  ├─────────────────────────────────────────────────────────────────┤
  │ chip                    10px             10.dp             ✓     │
  │ inner                   12px             12.dp             ✓     │
  │ control                 14px             14.dp             ✓     │
  │ card                    20px             20.dp             ✓     │
  │ mediaCard               18px             inner+12dp=24dp   ✗ +6px│
  │ sheet                   26px             26.dp             ✓*    │
  │                                                                 │
  │ * CSS has --radius-sheet: 28px but Android has 26dp.          │
  │   FigCraft asset sheets use 12px for Stage frames, but         │
  │   the Bubble/Check-in/Layers sheets use 30px in FigCraft.     │
  │   Android sheet = 26dp is between these values.               │
  └─────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────┐
  │ MOTION                   FIGCRAFT         Android Compose   Δ    │
  ├─────────────────────────────────────────────────────────────────┤
  │ spring                   cubic-bezier    spring(520f)      —    │
  │   (0.22, 1, 0.36, 1)                        damping=0.80     │
  │ enter duration           200ms           240ms             ✗ +40ms│
  │ exit duration            160ms           175ms             ✗ +15ms│
  │ press scale              0.97            0.96              ✗ -0.01│
  │                                                                 │
  │ FigCraft uses CSS cubic-bezier spring. Android uses physics    │
  │ spring with different parameters. Not directly comparable.     │
  └─────────────────────────────────────────────────────────────────┘

3.3 ANDROID ICON DISCREPANCIES (OursIcons.kt vs FigCraft vectors)
───────────────────────────────────────────────────────────────────
  
  MAP ICON (OursIcons.Map vs FigCraft NavIcon_Map ellipse):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: 14x14 ellipse with #0055FF stroke (light) or        │
  │           #3055FF stroke (dark)                                │
  │ Android:  Concatenated path with 1.8px stroke, black color    │
  │           Draws a zigzag map-like shape                        │
  │ VERDICT: Completely different icon designs.                   │
  │          FigCraft uses a simple circle/elipse.                │
  │          Android draws a detailed winding road path.          │
  │          This is NOT a discrepancy - they're different icons  │
  │          for different contexts (nav vs map detail).          │
  └─────────────────────────────────────────────────────────────────┘

  MEMORIES ICON (OursIcons.Memories vs FigCraft NavIcon_Memories rect):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: 13x13 rectangle with #555B68 stroke (light)        │
  │ Android:  Arc-based path with 1.8px stroke, black color       │
  │           Draws a mountain/landscape silhouette               │
  │ VERDICT: Different icon designs for different contexts.       │
  └─────────────────────────────────────────────────────────────────┘

  CHAT ICON (OursIcons.Chat vs FigCraft NavIcon_Chat rect):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: 15x12 rectangle with #555B68 or #0055FF stroke     │
  │ Android:  Complex bubble path with 1.8px stroke, black        │
  │           Draws a chat bubble with tail                       │
  │ VERDICT: Different icon designs. FigCraft uses simple rect,   │
  │          Android uses detailed chat bubble.                   │
  └─────────────────────────────────────────────────────────────────┘

  SETTINGS ICON (OursIcons.Settings vs FigCraft NavIcon_Settings ellipse):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: 13x13 ellipse with #555B68 or #0055FF stroke       │
  │ Android:  Gear/cog path with 1.8px stroke, black color       │
  │           Very detailed gear shape                           │
  │ VERDICT: Different icon designs. FigCraft uses simple ellipse,│
  │          Android uses detailed gear.                         │
  └─────────────────────────────────────────────────────────────────┘

  COMPASS ICON (OursIcons.Compass vs FigCraft DskMeCompass/DskNudgeArc):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: DskMeCompass = 64x64 frame with:                   │
  │           - DHeadingCone vector (4 verts, #0055FF fill 0.18) │
  │           - DMeRing ellipse (32x32, white fill, #0055FF str) │
  │           - DMeInit text (32x15, #111318 fill)               │
  │ Android:  Simple compass rose: arc + arrow triangle           │
  │           1.8px stroke, black color                          │
  │ VERDICT: Different. FigCraft has a detailed compass with      │
  │          heading cone, ring, and text label. Android has      │
  │          a basic compass rose silhouette.                     │
  └─────────────────────────────────────────────────────────────────┘

  ALERT ICON (OursIcons.Alert vs FigCraft FabBell):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft FabBell: 14x13 vector, 7 vertices, #111318 stroke   │
  │ Android Alert:    Triangle + bar path, 1.8px stroke, black   │
  │ VERDICT: Different designs. FigCraft = bell shape, Android =  │
  │          exclamation triangle.                                │
  └─────────────────────────────────────────────────────────────────┘

  CAMERA ICON (OursIcons.Camera vs FigCraft FabCamBody/FabCamLens):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: FabCamBody (20x14 rect, #111318 stroke) +          │
  │           FabCamLens (8x8 ellipse, #0055FF fill)             │
  │ Android:  Complex camera body + lens ring path, 1.8px stroke │
  │           black color                                        │
  │ VERDICT: Different. FigCraft = simple rect + circle, Android │
  │          = detailed camera outline.                          │
  └─────────────────────────────────────────────────────────────────┘

  LAYERS ICON (OursIcons doesn't have one vs FigCraft FabLayersIcon):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft FabLayersIcon: 18x10 vector, 4 verts, #111318 stroke│
  │ Android: No layers icon defined in OursIcons.kt               │
  │ VERDICT: Missing from Android.                               │
  └─────────────────────────────────────────────────────────────────┘

  RECENTER ICON (OursIcons.Crosshair vs FigCraft FabRecArrow):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft FabRecArrow: 13x13 vector, 4 verts, #0055FF fill    │
  │ Android Crosshair:    Circle + crosshairs, 1.8px stroke      │
  │ VERDICT: Different. FigCraft = arrowhead, Android = crosshair│
  └─────────────────────────────────────────────────────────────────┘

  BUBBLE ICON (OursIcons.Heart vs FigCraft FabBubbleRing/FabBubbleDot):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft: FabBubbleRing (20x20 ellipse, #0055FF stroke) +    │
  │           FabBubbleDot (6x6 ellipse, #0055FF fill)           │
  │ Android:  Heart shape path, 1.8px stroke, black color        │
  │ VERDICT: Completely different. FigCraft = concentric circles,│
  │          Android = heart.                                     │
  └─────────────────────────────────────────────────────────────────┘

  SEND ICON (OursIcons.Send vs FigCraft nudge wave):
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft DskNudgeArc: 170x58.3 vector, 2 verts, #0055FF stroke│
  │ Android Send:         Arrow path, 1.8px stroke, black        │
  │ VERDICT: Different. FigCraft = arc, Android = paper airplane │
  └─────────────────────────────────────────────────────────────────┘

3.4 ANDROID SVG DRAWABLE DISCREPANCIES (android/res/drawable/*.xml vs FigCraft)
───────────────────────────────────────────────────────────────────────────────

  ic_launcher_foreground.xml (108x108dp) vs FigCraft OursIcon_Light/Dark:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft OursIcon_Light (100x100):                           │
  │   - Frame: #FAF9F4 fill, #E1E0D8 stroke, r=24, shadow       │
  │   - RingLeft: 36x36 ellipse, #111318 stroke                 │
  │   - RingRight: 36x36 ellipse, #0055FF stroke                │
  │   - TopCut: 12.4x9.7 vector, #FAF9F4 stroke, 2 verts        │
  │   - TopOver: 12.4x9.7 vector, #111318 stroke, 2 verts       │
  │   - BotCut: 12.4x9.7 vector, #FAF9F4 stroke, 2 verts        │
  │   - BotOver: 12.4x9.7 vector, #0055FF stroke, 2 verts       │
  │                                                             │
  │ Android ic_launcher_foreground (108x108, translated 4,4):   │
  │   - RingLeft: arc path, 6.5px stroke, #FFFFFF               │
  │   - RingRight: arc path, 6.5px stroke, #0055FF              │
  │   - Weave cut L: arc path, 10.5px stroke, #000000 butt      │
  │   - Weave cut R: arc path, 6.5px stroke, #000000 round      │
  │   - Weave left: arc path, 6.5px stroke, #000000 round       │
  │   - Weave right: arc path, 6.5px stroke, #0055FF round      │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft uses ellipses (36x36) for rings, Android uses │
  │      arc paths. Different geometry.                         │
  │   2. FigCraft ring size = 36x36px, Android ring = 36px      │
  │      diameter (18px radius). The viewport is 108 vs 100,    │
  │      so after scaling: FigCraft rings = 36/100*108 = 38.88px│
  │      Android rings = 36px. Δ = 2.88px (FigCraft rings are   │
  │      7.6% larger relative to viewport).                     │
  │   3. FigCraft has Weave cuts (TopCut/TopOver/BotCut/BotOver)│
  │      as separate 12.4x9.7 vector shapes. Android has weave  │
  │      paths that combine the cut + overlap into single arcs. │
  │   4. FigCraft weave stroke: 10.5px (butt cap) for cut,      │
  │      6.5px (round cap) for overlap. Android: 10.5px butt    │
  │      for cut, 6.5px round for overlap. STROKE WIDTHS MATCH. │
  │   5. FigCraft OursIcon frame has corner radius 24px.        │
  │      Android ic_launcher_foreground is a vector with no      │
  │      container frame - the outer shape is defined by the     │
  │      ring paths themselves.                                  │
  │   6. FigCraft icon is 100x100, Android is 108x108.          │
  │      Scale difference: 8% (FigCraft is smaller).            │
  │   7. Android translates group by (4,4) to center within     │
  │      108x108 viewport. FigCraft doesn't use translation.    │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_pair_rings.xml (64x64dp) vs FigCraft PI_L/PI_R:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft Stage_PairInviteArt PI_L/PI_R:                      │
  │   - PI_L: 44x44 ellipse, #111318 stroke (no fill)           │
  │   - PI_R: 44x44 ellipse, #0055FF stroke (no fill)           │
  │   - Stage: 200x104 frame, #F0EEE9 fill, r=12                │
  │                                                             │
  │ Android ic_ours_pair_rings (64x64):                         │
  │   - Background circle: 28-radius arc, #E8F2FF fill          │
  │   - RingL: arc path, 2.6px stroke, #1C1C1E                  │
  │   - RingR: arc path, 2.6px stroke, #0055FF                  │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft rings are 44x44 each (in 200x104 stage).      │
  │      Android rings are radius-12 arcs (in 64x64 viewport).  │
  │      Scale: FigCraft rings = 44/200 = 22% of stage width.   │
  │      Android rings = 24/64 = 37.5% of viewport.             │
  │      Android rings are proportionally MUCH larger.           │
  │   2. FigCraft stroke: weight not specified (default ~1px?).  │
  │      Android stroke: 2.6px. Android is thicker.             │
  │   3. FigCraft ring stroke colors: #111318 (L) and #0055FF(R)│
  │      Android: #1C1C1E (L) and #0055FF (R). Left ring is     │
  │      slightly different: #111318 vs #1C1C1E.                │
  │   4. FigCraft has no background fill circle. Android has    │
  │      #E8F2FF background circle (28 radius).                 │
  │   5. FigCraft stage has r=12 corners. Android has no        │
  │      container frame.                                        │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_checkin_frame.xml (24x24dp) vs FigCraft CF_Box/CF_Lens/CF_Shut:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft Stage_CheckInFrame (200x104):                       │
  │   - CF_Box: 68x64 rect, #FFFFFC fill, #111318 stroke, r=16 │
  │   - CF_Lens: 28x28 ellipse, #EBF1FF fill, #0055FF stroke   │
  │   - CF_Shut: 10x10 ellipse, #0055FF fill                   │
  │                                                             │
  │ Android ic_ours_checkin_frame (24x24):                      │
  │   - Outer frame: arc path (28,28 arc), 1.75px stroke, white │
  │   - Center dot: circle path (3.5 radius), 1.75px stroke     │
  │   - Inner triangle: polygon path, #0055FF fill              │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. COMPLETELY DIFFERENT DESIGNS.                          │
  │      FigCraft: camera frame (rect + lens ellipse + shutter) │
  │      Android:  compass/prism icon (arc frame + dot + triangle)│
  │   2. This is a fundamental design mismatch, not a pixel Δ.  │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_compass_rose.xml (24x24dp) vs FigCraft CR_Ring/CR_N/CR_S:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft Stage_CompassRose (200x104):                       │
  │   - CR_Ring: 68x68 ellipse, #E1E0D8 stroke (no fill)       │
  │   - CR_N: 20x28 vector, 4 verts, #0055FF fill              │
  │   - CR_S: 16x26 vector, 4 verts, #555B68 fill              │
  │                                                             │
  │ Android ic_ours_compass_rose (24x24):                       │
  │   - Outer arc: 1.6px stroke, #FFFFFF                        │
  │   - Center diamond: #005EC4 fill (note: #005EC4, not #0055FF!)│
  │   - Bottom arrow: 1.4px stroke, #FFFFFF join=round         │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft compass has a ring + N arrow (blue) + S arrow │
  │      (gray). Android has outer arc + center diamond + bottom │
  │      arrow. Different structures.                           │
  │   2. FigCraft N arrow fill = #0055FF, Android center diamond│
  │      fill = #005EC4. MISMATCH: #0055FF vs #005EC4 (C=blue  │
  │      is 128, F=blue is 255; diff = 127 in blue channel).   │
  │   3. FigCraft CR_Ring is 68x68 ellipse. Android outer arc   │
  │      is radius-9 arc in 24x24 viewport. Scale: FigCraft =   │
  │      68/200 = 34% of stage. Android = 18/24 = 75% of       │
  │      viewport. Android ring is proportionally much larger.  │
  │   4. FigCraft CR_N is 20x28 (pointing up from center).      │
  │      Android has a diamond at center + separate bottom arrow.│
  │      Different arrow configuration.                         │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_nudge_wave.xml (24x24dp) vs FigCraft DskNudgeArc:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft DskNudgeArc (170x58.3):                            │
  │   - Single vector, 2 vertices, #0055FF stroke              │
  │   - Arc from (0, 58.3) to (170, 2.3) — sweeping curve      │
  │                                                             │
  │ Android ic_ours_nudge_wave (24x24):                         │
  │   - Concentric circle arcs: outer 20-radius, inner 14-radius│
  │   - 1.5px and 1.7px strokes, #FFFFFF                        │
  │   - Two arc segments (top and bottom)                       │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft = single sweeping arc. Android = concentric   │
  │      circle arcs. Completely different geometry.            │
  │   2. FigCraft color = #0055FF. Android = #FFFFFF. MISMATCH.│
  │   3. FigCraft is 170x58.3 (wide arc). Android is 24x24      │
  │      (concentric circles). Different aspect ratios.         │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_safety_shield.xml (24x24dp) vs FigCraft SS_Body/SS_Dot:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft Stage_SafetyShield (200x104):                      │
  │   - SS_Body: 60x68 vector, 6 verts, #E4F4EC fill,         │
  │              #146C43 stroke                                 │
  │   - SS_Dot: 14x14 ellipse, #146C43 fill                    │
  │                                                             │
  │ Android ic_ours_safety_shield (24x24):                      │
  │   - Shield outline: path, 1.75px stroke, #005EC4           │
  │   - Checkmark: path, 1.75px stroke, #228B54               │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft = filled shield shape + dot. Android =        │
  │      outlined shield + checkmark. Different concepts.       │
  │   2. FigCraft shield fill = #E4F4EC (light green). Android  │
  │      has no fill (outline only).                             │
  │   3. FigCraft shield stroke = #146C43. Android stroke =     │
  │      #005EC4. MISMATCH: green (#146C43) vs blue (#005EC4). │
  │   4. FigCraft has a center dot (#146C43). Android has a     │
  │      checkmark (#228B54). Different inner symbol.           │
  │   5. Android checkmark color = #228B54 (green), but this    │
  │      doesn't match any FigCraft token. FigCraft uses         │
  │      #146C43 for success green. Δ = significant.            │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_chat_empty.xml (64x64dp) vs FigCraft EC_B1/EC_B2/EC_Dot:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft Stage_EmptyChatArt (200x104):                       │
  │   - EC_B1: 62x26 rect, #FFFFFC fill, #E1E0D8 stroke, r=13 │
  │   - EC_B2: 62x26 rect, #EBF1FF fill, #0055FF stroke, r=13 │
  │   - EC_Dot: 8x8 ellipse, #0055FF fill                      │
  │                                                             │
  │ Android ic_ours_chat_empty (64x64):                         │
  │   - Background: circle arc, #E8F2FF fill                    │
  │   - Chat bubble 1: path, 2px stroke, #1C1C1E               │
  │   - Chat bubble 2: path, #0055FF fill                       │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft: two stacked rounded rectangles (chat bubbles)│
  │      Android: chat bubble paths with tails                  │
  │   2. FigCraft EC_B1 stroke = #E1E0D8. Android bubble 1      │
  │      stroke = #1C1C1E. MISMATCH: light gray vs near-black. │
  │   3. FigCraft EC_B2 fill = #EBF1FF. Android bubble 2 fill  │
  │      = #0055FF. MISMATCH: light blue tint vs solid blue.   │
  │   4. FigCraft has an 8x8 dot. Android has no equivalent dot.│
  │   5. FigCraft bubble stroke width: not specified (default). │
  │      Android: 2px. Different.                               │
  └─────────────────────────────────────────────────────────────────┘

  ic_ours_memories_empty.xml (64x64dp) vs FigCraft EM_Back/EM_Front/EM_Pin:
  ┌─────────────────────────────────────────────────────────────────┐
  │ FigCraft Stage_EmptyMemoriesArt (200x104):                   │
  │   - EM_Back: 52x58 rect, #E1E0D8 fill, r=10                │
  │   - EM_Front: 54x60 rect, #FFFFFC fill, #555B68 stroke, r=10│
  │   - EM_Pin: 14x14 ellipse, #0055FF fill                     │
  │                                                             │
  │ Android ic_ours_memories_empty (64x64):                      │
  │   - Background: circle arc, #E8F2FF fill                    │
  │   - Memory card: path, 2px stroke, #1C1C1E                 │
  │   - Photo area: path, #0055FF fill                          │
  │                                                             │
  │ DISCREPANCIES:                                              │
  │   1. FigCraft: two stacked cards (photo frame) + pin.       │
  │      Android: memory card path + photo area + circle bg.    │
  │   2. FigCraft EM_Back fill = #E1E0D8. Android bg = #E8F2FF. │
  │      MISMATCH: warm gray vs light blue tint.                │
  │   3. FigCraft EM_Front stroke = #555B68. Android card       │
  │      stroke = #1C1C1E. MISMATCH.                            │
  │   4. FigCraft EM_Pin = 14x14 ellipse, #0055FF. Android     │
  │      photo area = path fill, #0055FF. Same color but        │
  │      different shape (ellipse vs path).                     │
  └─────────────────────────────────────────────────────────────────┘

  ic_splash_rings_light.xml (108x108dp) vs FigCraft OursIcon_Light rings:
  ┌─────────────────────────────────────────────────────────────────┐
  │ See ic_launcher_foreground analysis above — these are the    │
  │ same icon concept. ic_splash_rings_light uses white rings,   │
  │ ic_launcher_foreground uses white+#0055FF rings.            │
  │ FigCraft OursIcon_Light: white ring (RingLeft) + blue ring  │
  │ (RingRight). ic_splash_rings_light: both rings white.        │
  │ DISCREPANCY: Android splash rings are both white, but       │
  │ FigCraft has one white + one blue ring.                     │
  └─────────────────────────────────────────────────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4. SUMMARY OF ALL DISCREPANCIES BY SEVERITY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

4.1 CRITICAL (design intent broken)
────────────────────────────────────
  1. Android font = SansSerif, NOT Inter
     → All Android text renders in system font, not the designed Inter
     → Affects every screen in the Android app
     → Fix: Add Inter font to Android assets, reference in Theme.kt

  2. Android error/danger color = #D70015 (orange), should be #C9182B (red)
     → SOS/pair-expired states render in wrong color on Android
     → Fix: OursDarkColors.error = Color(0xFFC9182B), OursLightColors.error = Color(0xFFC9182B)
     → Same for danger field

  3. Android success colors don't match FigCraft
     → Light: #1D7A3B vs #146C43 (Android is greener)
     → Dark: #30D158 vs #3CD084 (Android is darker)
     → Fix: Align Android Compose colors with FigCraft tokens

  4. Android inkMuted (light) = #5A606D, should be #555B68
     → Secondary text on Android has wrong hue (slightly bluer)
     → Fix: OursLightColors.inkMuted = Color(0xFF555B68)
     → OursDarkColors.inkMuted = Color(0xFF8E96A8)

  5. Android border colors don't match CSS
     → Light: #E4E2DA vs rgba(17,19,24,0.09)~#E2E0D8
     → Dark: #282F42 vs rgba(250,249,245,0.11)~#283044
     → Fix: Align border colors

4.2 SIGNIFICANT (visible but not breaking)
──────────────────────────────────────────
  6. --shadow-sm: CSS 0 2px 10px rgba(17,19,24,0.05) vs 
     FigCraft 0 6px 18px rgba(17,19,24,0.078)
     → CSS shadow is much lighter and tighter than designed
     → Fix: --shadow-sm: 0 6px 18px rgba(17,19,24,0.078)

  7. --shadow-md: CSS spread=-4px vs FigCraft spread=-2px
     → CSS shadow is 2px more diffused
     → Fix: --shadow-md: 0 10px 24px -2px rgba(17,19,24,0.102)

  8. --shadow-cta: CSS alpha=0.36 vs FigCraft alpha=0.24
     → CSS CTA shadow is 50% more opaque than designed
     → Fix: --shadow-cta: 0 8px 20px -4px rgba(0,85,255,0.36)
     → Actually 0.36 might be intentional for emphasis. Flag for review.

  9. --cta-grad endpoint: CSS #0044D6 vs FigCraft #0046D6
     → Off by 2 in green channel (negligible, <1% visible difference)
     → CSS #0044D6 is the intended value (matches --action-pressed)
     → FigCraft #0046D6 in the gradient stop is likely a rounding artifact

  10. --glass-card endpoint: CSS ~#F4F4F5 vs FigCraft #F6F7F4
      → CSS end is slightly warmer/cooler depending on interpretation
      → Fix: --glass-card: linear-gradient(135deg, rgba(255,255,252,0.808), 
        rgba(246,247,244,0.729))

  11. --glass-capsule endpoint: CSS ~#F2F4FD vs FigCraft #F1F4FF
      → Minor difference in end color
      → Fix: --glass-capsule: linear-gradient(135deg, 
        rgba(255,255,252,0.722), rgba(241,244,255,0.620))

  12. Android inkSecondary (dark) = #C5C8D4 vs FigCraft #C5CAD6
      → Off by 1 in blue channel (negligible)

  13. Android actionSoft (dark) = #13203B vs FigCraft #132240
      → Significant difference: #13203B is much bluer/darker
      → Fix: OursDarkColors.actionSoft = Color(0xFF132240)

  14. Android paused colors don't match
      → Light: #373B44 vs #555B68 (Android uses inkSecondary!)
      → Dark: #C5C8D4 vs #C5CAD6 (off by 1 in blue)
      → Fix: OursLightColors.paused = Color(0xFF555B68)
      → OursDarkColors.paused = Color(0xFFC5CAD6)
      → OursLightColors.pausedSoft = Color(0xFFECEAE2)
      → OursDarkColors.pausedSoft = Color(0xFF1E2433)

4.3 MINOR (pixel-perfect concerns)
───────────────────────────────────
  15. CSS --radius-card = 22px vs FigCraft = 20px
      → CSS is 2px larger. Use FigCraft value: 20px

  16. CSS --radius-media-card = 24px vs FigCraft asset cards = 18px
      → CSS is 6px larger. Use FigCraft value: 18px
      → Note: "media card" might be a different component than asset cards

  17. Android OursRadius.mediaCard = inner(12) + sm(12) = 24dp
      → Should be 18dp to match FigCraft asset cards
      → Fix: OursRadius.mediaCard = 18.dp

  18. Android OursRadius.sheet = 26dp vs CSS --radius-sheet = 28px
      → Android is 2dp smaller than CSS
      → FigCraft uses 28px for map-peek-sheet, 30px for bubble/check-in sheets
      → Fix: OursRadius.sheet = 28.dp (to match CSS)

  19. Android OursTypography.display = 26sp vs FigCraft/CSS = 24px
      → Android is 2sp larger
      → Fix: OursTypography.display.fontSize = 24.sp

  20. Android OursTypography.caption = 13sp vs FigCraft/CSS = 12px
      → Android is 1sp larger, AND uses Normal weight instead of SemiBold
      → Fix: OursTypography.caption.fontSize = 12.sp, 
        OursTypography.caption.fontWeight = FontWeight.SemiBold

  21. Android OursTypography.tabular = 13sp vs FigCraft/CSS = 13.5px
      → Android is 0.5sp smaller
      → Fix: OursTypography.tabular.fontSize = 13.5.sp

  22. Android OursTypography.title = Medium weight vs FigCraft SemiBold
      → Weight mismatch
      → Fix: OursTypography.title.fontWeight = FontWeight.SemiBold

  23. Android enterSpec = 240ms vs CSS --motion-enter = 200ms
      → Android is 40ms slower
      → Fix: OursMotion.ENTER_MS = 200

  24. Android exitSpec = 175ms vs CSS --motion-exit = 160ms
      → Android is 15ms slower
      → Fix: OursMotion.EXIT_MS = 160

  25. Android press scale = 0.96 vs CSS --motion-press-scale = 0.97
      → Android is slightly more aggressive
      → Fix: OursMotion.PRESS_SCALE = 0.97f

4.4 ICON MISMATCHES (different designs, not pixel deltas)
───────────────────────────────────────────────────────────
  26. All navigation icons in Android (OursIcons.kt) are detailed path
      drawings, while FigCraft uses simple shapes (ellipses, rectangles).
      These are fundamentally different icon designs.
      → The Android icons may be intentional alternatives for the navigation
        bar context, or they may be outdated.
      → RECOMMENDATION: Verify with design team whether Android nav icons
        should match FigCraft's simple shapes.

  27. ic_ours_checkin_frame.xml is a compass/prism icon, but FigCraft
      CheckInFrame is a camera frame (rect + lens + shutter).
      → Different concepts entirely. Likely a different icon for a
        different context, or an outdated drawable.

  28. ic_ours_safety_shield.xml is an outlined shield + checkmark, but
      FigCraft SafetyShield is a filled shield + dot.
      → Different concepts. Android uses outline+checkmark, FigCraft uses
        filled shape+dot.

  29. ic_ours_chat_empty.xml has chat bubbles with tails, but FigCraft
      EmptyChatArt has two simple rounded rectangles.
      → Android version is more detailed. May be intentional.

  30. ic_ours_memories_empty.xml has a memory card path, but FigCraft
      EmptyMemoriesArt has two stacked photo frame rectangles.
      → Android version is more stylized. May be intentional.

  31. OursIcons.kt is missing a Layers icon (FabLayersIcon in FigCraft).
      → Add layers icon to OursIcons.kt if needed.

  32. ic_splash_rings_light.xml has both rings in white, but FigCraft
      OursIcon_Light has white ring + blue ring.
      → If the splash icon should match FigCraft, the light rings drawable
        needs one ring changed to #0055FF.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5. ANDROID THEME.XML GAPS (missing token coverage)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  The Android themes.xml files only define:
  - windowBackground (light: #FAF9F5, dark: #090B10)
  - windowSplashScreenBackground (light: #FAF9F5, dark: #090B10)
  - windowSplashScreenAnimatedIcon (@drawable/ic_splash_transparent)
  
  MISSING from Android themes:
  - No color scheme definition (uses MaterialTheme.Material.Light.NoActionBar
    defaults for everything else)
  - No typography styles
  - No dimension/spacing resources
  - No radius/dimension resources
  - No shadow resources
  - No glass gradient definitions
  
  RECOMMENDATION: Create a comprehensive Android color/theme resource set
  that mirrors the CSS :root tokens, using Android's color.xml and
  dimens.xml files, or expand the Compose Tokens.kt to be the single source
  of truth for Android (with all values matching FigCraft).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
6. WEB/CSS GAPS (tokens in FigCraft not in styles.css :root)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  MISSING CSS VARIABLES that exist in FigCraft:
  ┌─────────────────────────────────────────────────────────────────┐
  │ Token                    FigCraft Value     Suggested CSS        │
  ├─────────────────────────────────────────────────────────────────┤
  │ --glass-frost-capsule    14px blur         Already in CSS ✓     │
  │ --glass-frost-card       16px blur         Already in CSS ✓     │
  │ --radius-glass-frame     20px              ADD: 20px            │
  │ --radius-fab-container   21px              ADD: 21px            │
  │ --radius-splash          42px              ADD: 42px            │
  │ --radius-google-card     26px              ADD: 26px            │
  │ --radius-cta-btn         27px              ADD: 27px            │
  │ --radius-bubble-sheet    30px              ADD: 30px            │
  │ --radius-dynamic-island  13px              ADD: 13px            │
  │ --shadow-splash          0 22px 52px      ADD: 0 22px 52px     │
  │   -4px rgba(0,0,0,0.40)  -4px rgba(0,0,0,0.40)                │
  │ --shadow-dark-glass      0 12px 28px      ADD: 0 12px 28px     │
  │   -2px rgba(0,0,0,0.451) -2px rgba(0,0,0,0.451)              │
  │ --nav-gap                4px               Already in CSS ✓     │
  │ --splash-wordmark-gap    20px (computed)  Already in CSS ✓     │
  │ --icon-nav-size          9.5px             Already in CSS ✓     │
  │ --icon-fab-size          15px (15x15 SVG) Already in CSS ✓?    │
  │   CSS .nav-item svg uses 15x15.                               │
  │ --font-onboarding-sub    14.5px            ADD: 14.5px          │
  │ --font-bubble-sub        12.5px            ADD: 12.5px          │
  └─────────────────────────────────────────────────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
END OF REPORT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
