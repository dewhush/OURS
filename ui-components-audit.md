# OURS Compose UI Components Library Audit

**Audit date:** 2026-11-20
**Scope:** `app.ours.ui.components.*` + `app.ours.ui.theme.*`
**Source of truth:** `docs/PRD.md` (merged current PRD)

---

## Components inventory (what actually exists)

| Requested | Found | File |
|---|---|---|
| OursAvatar | ✅ | `Components.kt:492` |
| OursBottomSheet | ✅ | `Components.kt:468` |
| OursButton | ✅ | `Components.kt:246` |
| OursCard | ✅ | `Components.kt:328` |
| OursGlassBackdrop | ✅ | `Components.kt:137` |
| OursIcons | ✅ | `OursIcons.kt` (separate file) |
| OursInterlockedRingsMark | ✅ | `Components.kt:844` |
| OursSegmentedControl | ✅ | `Components.kt:390` |
| OursStatusChip | ✅ | `Components.kt:535` |
| OursEmptyState | ✅ | `Components.kt:598` |
| OursErrorState | ✅ | `Components.kt:724` |
| OursLoadingState | ✅ | `Components.kt:572` |
| OursMapView | ✅ | `OursMapView.kt` (separate file) |
| OursStaggeredItem | ✅ | `Components.kt:798` |
| **OursGlassCard** | ✅ (extra) | `Components.kt:224` — thin wrapper over OursCard(frosted=true) |
| **OursNudgeWaveOverlay** | ✅ (extra) | `Components.kt:644` — internal nudge animation |
| **OursSendConfirmation** | ✅ (extra) | `Components.kt:753` — animated confirmation pill |
| **oursLiquidGlass** | ✅ (extra) | `Components.kt:156` — modifier, not a component, but core to the system |
| **rememberOursLayoutSpec / OursLayoutSpec / OursBreakpoint** | ✅ (extra) | `Components.kt:94-122` |
| **oursContentWidth / oursHeaderInsets** | ✅ (extra) | `Components.kt:124-134` |

Theme:
| Requested | Found | File |
|---|---|---|
| OursTheme | ✅ | `Theme.kt` |
| OursTypography | ✅ | `Tokens.kt:163` |
| OursSpacing | ✅ | `Tokens.kt:114` |
| OursRadius | ✅ | `Tokens.kt:125` |
| OursMotion | ✅ | `Tokens.kt:135` |
| OursColors | ✅ | `Tokens.kt:17` |

---

## Global theme architecture — good

- `OursTheme` wraps `MaterialTheme` and provides three `CompositionLocal`s: `LocalOursColors`, `LocalOursStrings`, `LocalOursIsDark`. Components read from `OursTheme.colors` / `OursTheme.strings` / `OursTheme.isDark`. This is the correct pattern.
- `OursColors` is a comprehensive token set covering canvas, surfaces, glass, borders, ink hierarchy, action, success/paused/danger/error families. Both light and dark palettes are defined.
- `OursTypography` has 7 semantic roles (display, headline, title, body, caption, label, tabular, tabularLarge). Good coverage.
- `OursSpacing` has 6 steps (xxs→xl). Good.
- `OursRadius` has 6 roles with the nested-radius rule documented. Good.
- `OursMotion` has spring specs, ease constants, enter/exit/move tween factories, and named constants (`ENTER_MS`, `EXIT_MS`, `STAGGER_MS`, `PRESS_SCALE`).

**One structural gap:** `OursColors` has `live`, `liveSoft`, `paused`, `pausedSoft`, `error`, `errorSoft`, `danger`, `dangerSoft`, `success`, `successSoft`. There is **no `warning` / `warningSoft` family**. `OursStatusChip` only handles Live/Paused/Muted/Error — if a warning state is ever added, there's no token for it. Low severity now, but the PRD mentions crash/SOS which may want a warning tone distinct from danger.

---

## Audit findings by component

### OursTheme (`Theme.kt`)

**F-1. Hardcoded `language` default and hardcoded string set selection**

`Theme.kt:34` — `language: String = "en"` and `Theme.kt:38` — `if (language == "id") OursIndonesianStrings else OursEnglishStrings`.

This is fine as a default, but the theme's `strings` local is set once at composition time and never reacts to runtime language changes from `OursViewModel.setLanguage()` unless the whole `OursTheme` is recomposed. In practice the app seems to recompose the whole tree when language changes (the ` OursUiState.language` flow), so this works, but it's fragile: any screen that wraps its own `LanguagePillToggle` / `OursSegmentedControl` reads `OursTheme.strings.current` through the local, which is correct. **No bug, but worth noting**: if you ever hoist `OursTheme` higher than the language-flow recomposition root, strings won't update. Currently OK.

**F-2. M3 color scheme maps `primary = action` but the app doesn't use M3 color tokens directly**

`Theme.kt:41-64` builds a proper M3 `ColorScheme` so material components (e.g. `ModalBottomSheet` container) pick up sensible defaults. Good. But several components use raw `Color.Transparent` + `oursLiquidGlass` instead of M3 `Surface` for translucent surfaces — that's a deliberate design choice, not a bug.

---

### OursColors / OursLightColors / OursDarkColors (`Tokens.kt`)

**F-3. Glass token set is over-specified and partly redundant with surface tokens**

`Tokens.kt:54-62` (light) and `:86-94` (dark) define `glassSurface`, `glassSurfaceStrong`, `glassGradientStart`, `glassGradientEnd`, `glassHighlightTopLeft`, `glassRimBottomRight`, `glassInnerHighlight`, `glassActionTint`, `glassBorder`.

`oursLiquidGlass` (`Components.kt:170-181`) uses `glassGradientStart/End`, `glassHighlightTopLeft`, `glassRimBottomRight`, `glassInnerHighlight`. `glassSurface`, `glassSurfaceStrong`, `glassActionTint`, `glassBorder` are **never read** in the current codebase. They're dead tokens. Either remove them or use them. Dead tokens mislead future work and inflate the token set.

**F-4. Hardcoded alpha values embedded in color literals rather than derived**

Examples:
- `glassHighlightTopLeft = Color(0x1C000000)` — alpha `0x1C` = 28% black. This is a token, so it's OK, but the semantic name says "highlight top left" and the value is really "subtle dark vignette". The name is slightly off.
- `glassInnerHighlight = Color(0xD9FFFFFF)` — 85% white. Used as a top-edge specular line (`Components.kt:207`). OK, but this is a **fixed alpha** — if you wanted to tune the glass look per-tier (card vs sheet vs nudge pill), you can't without adding more tokens.

Not a bug; a design-note. The glass system is a custom effect and the tokens are fit for purpose. Just flagging that it's a closed effect — you can't theme it beyond what's baked in.

**F-5. Dark `glassGradientStart` is lighter than `glassGradientEnd` — intended, but undocumented**

`OursDarkColors`: `glassGradientStart = Color(0xF21E2433)`, `glassGradientEnd = Color(0xEB131722)`. The gradient goes from slightly lighter (top) to darker (bottom). Light mode is the reverse: `glassGradientStart = 0xF5FFFFFC` (lighter), `glassGradientEnd = 0xEBFAF9F5` (darker). So both modes go light→dark top→bottom. Consistent. But `glassHighlightTopLeft` / `glassRimBottomRight` names imply a corner-based lighting model while the gradient is vertical. Minor naming inconsistency. Not a bug.

---

### OursSpacing (`Tokens.kt:114`)

**No findings.** 6 steps, well used. The only observation: `OursSpacing.xxl` / `OursSpacing.sm2` etc. don't exist — if a screen needs a 40dp or 48dp gap it either uses `xl` (32) and adds, or hardcodes. E.g. `MapScreen.kt:180` uses `padding(bottom = 140.dp)` for the nudge pill — that's a layout constant, not a spacing token, and it arguably shouldn't be one (it's screen-specific positioning). Fine.

---

### OursRadius (`Tokens.kt:125`)

**F-6. `mediaCard` is defined as `inner + OursSpacing.sm` = 22.dp, but it's used inconsistently**

`OursRadius.mediaCard = 22.dp`. In `ChatScreen.kt:761-762` (CheckInBubble) the top corners use `OursRadius.mediaCard` (22) and bottom corners use either `6.dp` or `OursRadius.mediaCard`. This is a deliberate asymmetric card (the bottom tucks under), not a bug.

But the PRD design tip #14 says "Nested radius: outer radius = inner radius + padding." `OursRadius` documents this at `Tokens.kt:124`. Check adherence:
- `OursCard` default radius = `OursRadius.card` (20). Content padding default = `OursSpacing.md` (16). Nested rule would predict outer = 20, inner content corner = 20 - 16 = 4. But the content isn't clipped to a smaller radius — the text/images inside aren't radius-clipped separately. So the nested rule is documented but not enforced by the card component. The card is one clipped surface. This is fine for a simple card, but if you put a clipped image inside with `OursRadius.inner` (12), you get 20 outer + 12 inner — that violates the nested rule (inner should be 20 - padding). This actually happens in `MemoriesScreen.kt:231`: `clip(RoundedCornerShape(OursRadius.inner))` on an image inside `OursCard(radius = 22.dp, padding = 12.dp)`. Expected inner = 22 - 12 = 10, but it's 12. **Minor visual violation of the documented nested-radius rule.** See fix below.

**F-7. `OursRadius.chip` = 10.dp is used for both status chips and filter chips and save/reply chips — consistent. Good.**

---

### OursMotion (`Tokens.kt:135`)

**F-8. `ENTER_MS = 240`, `EXIT_MS = 175` — PRD says "150 to 250 ms" for entrances and "exits about 20% shorter than entrances"**

240ms entrance is within 150-250. Exit 175ms vs entrance 240ms: 175/240 = 73%, i.e. exit is 27% shorter. PRD says ~20% shorter. 27% is close enough — not a bug, just slightly faster exits than the guideline. Acceptable.

**F-9. `STAGGER_MS = 28` vs PRD animation tip #3 "Stagger releases by about 30 ms"**

28 vs 30. Negligible. Fine.

**F-10. `PRESS_SCALE = 0.96f` vs PRD animation tip #4 "Scale to 0.97 on press"**

0.96 vs 0.97. One percent off. Not worth changing, but the PRD number and the token disagree. Pick one. Low priority.

**F-11. `springSnappy` stiffness 520, damping 0.80 — "no bounce on everyday UI" (PRD animation tip #13)**

Damping 0.80 is underdamped (will overshoot slightly then settle). For a "snappy" press scale that's intentional and looks good. The PRD "no bounce on everyday UI" refers to not using a bouncy spring for normal interactions — and the codebase uses `springBouncy` (damping 0.62) only for the nudge pulse (`MapScreen.kt:110`), which is an explicit delight moment. So the principle is respected. Good.

**F-12. Missing `moveSpec` usage consistency**

`OursMotion.moveSpec` exists (`Tokens.kt:159`) but is **never called** in the component library. All moves use `springSnappy`/`springFluid`. `moveSpec` is for ease-in-out tweens — if you add a panel slide or sheet transition that should be eased (not springy), use it. Currently dormant. Not a bug.

---

### OursTypography (`Tokens.kt:163`)

**F-13. `label` and `caption` line-height ratio**

`label`: 12sp / 16sp line-height = 1.33. `caption`: 13sp / 18sp = 1.38. `body`: 15/21 = 1.40. `title`: 16/22 = 1.375. These are all reasonable. No issue.

**F-14. `tabular` and `tabularLarge` use `fontFeatureSettings = "tnum"` — correct for numbers. Good.** Used in `MapScreen.kt:253` (`style = OursTypography.tabular`) for the live status line, and in `SettingsScreen.kt:424` for coordinate text. Good adherence.

**F-15. Hardcoded `FontFamily.SansSerif` everywhere**

The PRD says "Use mono font only where needed" (tip #4) and "Use tabular numbers" (tip #18). The typography object uses `FontFamily.SansSerif` for everything. That's fine — the mono font is reserved for tabular numbers via `tnum` feature, but that's still the same sans font with a feature flag, not a separate mono typeface. If you want true tabular numerals you'd need a mono font family. Currently `tnum` on SansSerif may not give true monospaced digits on all platforms. Minor. Not a bug for v1.

**F-16. `display` font weight is `SemiBold` but `headline` is also `SemiBold` — only size differs**

`display`: 26sp SemiBold. `headline`: 20sp SemiBold. If you want display to feel more "headline-y" you'd make it Bold. Currently the weight hierarchy is: display=SemiBold, headline=SemiBold, title=Medium, body=Normal, caption=Normal, label=Medium. So `display` and `headline` have the same weight — only size separates them. PRD tip #9 "Make the headline specific" — the headline style could be bolder to stand out. Minor.

---

### OursAvatar (`Components.kt:492`)

**F-17. Hardcoded default `size = 40.dp`**

The default is 40dp. In `MapScreen.kt:216` it's used at default (40). In `ChatScreen.kt:422` it's used at 40. In `SettingsScreen.kt:194` it's used at 54.dp. The 54dp usage is a profile avatar and is larger — that's intentional. The default 40 is fine for list/row avatars. No bug. But there's no `OursAvatarSize` token or size variant — if you want a "small" (32dp) avatar for a tight row, you pass `size = 32.dp` ad hoc. A size token set (` OursAvatar.small/medium/large`) would be cleaner. Low priority.

**F-18. Hardcoded border widths: `1.5.dp` ring when `ringColor != null`, `0.75.dp` default border**

`Components.kt:511-512`. These should be tokens (e.g. `OursRadius` or a new `OursStroke` object). The 1.5dp ring is used for the "live" green ring in MapScreen (`MapScreen.kt:219`: `ringColor = if (statusTone == StatusTone.Live) colors.success else null`). The 1.5dp is a design choice for the live ring — it's thicker to feel alive. But it's a magic number. Move to a token.

**F-19. No accessible label / contentDescription on the avatar**

`OursAvatar` renders a `Text` (initial) and optionally an `AsyncImage` with `contentDescription = name`. But the avatar itself (the circle with initial) is not clickable and has no semantics label. If used as a tap target (e.g. Settings avatar that opens the camera picker — actually that's a `Box` with `clickable` wrapping the avatar, `SettingsScreen.kt:187-191`), the parent provides the role. So the avatar component itself doesn't need a label, but when it's the visual of a tappable area, the tappable parent should label it. In Settings, the parent `Box` has `role = Role.Button` but no `contentDescription` — the avatar's initial is the visual, but the semantics node says "Button" with no label. **Accessibility bug:** `SettingsScreen.kt:187-191` — the avatar tap target has `role = Role.Button` but no `contentDescription`. A screen reader announces "Button" with no name. Fix: add `contentDescription = "${strings.takePhoto} for ${meName}"` to the clickable box, or add `semantics { contentDescription = ... }` on the avatar.

**F-20. Initial fallback uses `name.trim().firstOrNull()?.uppercaseChar()` — correct, but if name is blank the avatar shows "?"**

`Components.kt:501`. Good fallback. In Settings, `meName` can be `strings.youFallback` ("You") if `state.me?.name` is blank — so the avatar shows "Y". Fine.

**F-21. `AsyncImage` loading state — no placeholder/shimmer**

When `photoUrl` is non-blank, `AsyncImage` loads asynchronously. During loading, the avatar shows the initial on the muted background, which is a reasonable placeholder. No shimmer, but the initial acts as the placeholder. Acceptable.

**F-22. `accent` flag swaps background/foreground to `actionSoft`/`action`**

`Components.kt:502-503`. Used in MapScreen (`accent = true`) and Settings (`accent = true`). In MapScreen the ring is also overridden to `colors.success` for live. So the avatar is action-colored with a green ring when live. Visually: blue avatar, green ring. That's a lot of color for one avatar. The PRD says "one action color" — using action as the avatar fill when `accent=true` is consistent with that. The green ring is a status indicator, separate. Acceptable, but verify it doesn't feel oversaturated. No code bug.

---

### OursButton (`Components.kt:246`)

**F-23. `defaultMinSize(minHeight = 48.dp)` — good, meets 44dp minimum touch target (PRD Fitts's law).** Confirmed.

**F-24. `role = Role.Button` on the clickable — good for accessibility.** Confirmed.

**F-25. Hardcoded loading indicator size `18.dp` and `strokeWidth = 2.dp`**

`Components.kt:307-309`. These are magic numbers. The 18dp spinner inside a 48dp button is reasonable, but should be a token or at least a named constant. Low priority.

**F-26. ` OursTypography.title.copy(fontWeight = FontWeight.SemiBold)` for button label**

Button uses `title` style (16sp Medium) but overrides to SemiBold. That's a slight weight bump on the title style. The PRD says "Emphasize the CTA" (tip #18). Button labels should be SemiBold — good. But the base `title` style is Medium; if every button copies to SemiBold, consider making a dedicated `button` text style in `OursTypography` (16sp SemiBold, same line-height as title). Currently it's a copy each time. Minor.

**F-27. ` OursSpacing.md` horizontal + `OursSpacing.sm` vertical padding inside the button — consistent. Good.**

**F-28. `Surface` color for Ghost variant is `Color.Transparent` — correct, but the clickable area still covers the full min-size box.** For a ghost button, the label + icon + padding define the visual, but the touch target is the full 48dp min box. That's correct behavior. Good.

**F-29. No visual disabled state differentiation beyond `effectiveAlpha = 0.42f`**

`Components.kt:276`. Disabled = 42% alpha on the whole button (including the label). That's a standard disabled treatment. Good. The PRD doesn't specify a disabled style; 42% is reasonable.

**F-30. Press scale animation applies to the whole `Surface` via `graphicsLayer` — good, no layout shift.** Confirmed.

**F-31. `leadingIcon` spacing uses `OursSpacing.xs` between icon and text — consistent.** Confirmed.

---

### OursCard (`Components.kt:328`)

**F-32. Default `elevation = 2.dp` but `Surface` `shadowElevation = 0.dp` always — elevation param is unused**

`Components.kt:333` — `elevation: Dp = 2.dp`. `Components.kt:379` — `shadowElevation = 0.dp`. The `elevation` parameter is **never used** to set the shadow. The card uses `oursLiquidGlass` for the frosted variant (which has its own elevation param that IS used — `Components.kt:368` passes `elevation = elevation` to `oursLiquidGlass`), but for the non-frosted variant, elevation is ignored. **Bug/inconsistency:** the `elevation` param on `OursCard` only does something when `frosted = true`. For a plain card, it's a no-op with a misleading default of 2.dp. Either remove the param, or apply it as a `shadowElevation` for non-frosted cards. Fix: for non-frosted, set `shadowElevation = elevation` and remove the hardcoded 0.dp, OR document that elevation is frosted-only. Currently misleading.

**F-33. `border` width differs by theme: `0.5.dp` dark, `0.75.dp` light**

`Components.kt:376-377`. This is a deliberate dark/light differentiation. Good. But the border width is a magic number — should be a token (e.g. `OursStroke.border`). Low priority.

**F-34. Press scale on clickable cards via `graphicsLayer` — good.** Confirmed.

**F-35. `OursCard` clickable uses `role = Role.Button` — correct for a tappable card.** Confirmed.

**F-36. `OursGlassCard` is a 1:1 wrapper that forwards all params to `OursCard(frosted = true)` — good, no duplication.** Confirmed. But it hardcodes `frosted = true` and doesn't add anything. That's fine as a semantic alias. Could be deprecated in favor of `OursCard(frosted = true)` but keeping it is OK for readability.

**F-37. Hardcoded `padding` default `OursSpacing.md` — good, uses token.** Confirmed.

---

### OursGlassBackdrop (`Components.kt:137`)

**F-38. Simple `Box(fillMaxSize, background = colors.canvas)` — correct.** This is the app background. Good. Used in `MemoriesScreen.kt:125`, `SettingsScreen.kt:158`, `OnboardingScreen.kt:192`, `ChatScreen.kt:203`. Consistent.

**F-39. No accessibility role — it's a background, no role needed.** Correct.

---

### OursGlassCard (`Components.kt:224`) — covered under OursCard.

---

### oursLiquidGlass modifier (`Components.kt:156`)

**F-40. The rim brush uses `glassHighlightTopLeft` → `glassRimBottomRight` names but draws a full rounded-rect stroke — naming implies a corner gradient, actual is a full perimeter stroke.** The `rimBrush` is a vertical gradient drawn as a `Stroke` around the entire rounded rect (`Components.kt:213-217`). The token names say "topLeft" and "bottomRight" which suggests a diagonal, but the gradient is vertical (top→bottom). The visual is a subtle light-to-dark rim around the whole shape. The token names are slightly misleading. Not a bug, but if someone adds a new glass variant they might misunderstand.

**F-41. The inner highlight line is drawn at `strokePx` (0.75.dp) from the top, only when `size.width > rPx * 1.2f`**

`Components.kt:205-211`. This is a conditional specular line. Good detail. The `0.75.dp` stroke width is a magic number (same as the border width discussed in F-33). Could be a token.

**F-42. Shadow loops: 3 steps, alpha formula `(0.04f * (steps - i + 1)) / steps`**

`Components.kt:187-198`. This renders a soft ambient shadow outside the clip path. The `0.04f` base alpha is a magic number. Low priority.

**F-43. `elevation.toPx()` and `radius.toPx()` computed inside `drawWithCache` — correct, cached per size/elevation/radius.** Good performance practice. Confirmed.

**F-44. The modifier takes an explicit `colors: OursColors` param rather than reading `OursTheme.colors` inside — this is CORRECT and good practice.** It lets the caller pass the right colors (e.g. tinted vs untinted, or a different color set). Good. Confirmed.

**F-45. `tinted` param: when true, uses `actionSoft → glassGradientEnd` gradient instead of `glassGradientStart → glassGradientEnd`.** This tints the glass with the action color. Used in `MapScreen.kt:184` for the nudge pill. Good. But `actionSoft` is the gradient start, and `glassGradientEnd` is the end — so a tinted glass goes from action-soft to the normal glass-end. That's a nice effect. Confirmed.

---

### OursBottomSheet (`Components.kt:468`)

**F-46. Uses M3 `ModalBottomSheet` with `RoundedCornerShape(topStart = OursRadius.sheet, topEnd = OursRadius.sheet)` — good, uses token.** Confirmed. `OursRadius.sheet = 26.dp`.

**F-47. `skipPartiallyExpanded = true` — correct for a bottom sheet that should snap to hidden or fully open.** Good.

**F-48. Sheet content padding: `horizontal = OursSpacing.md`, `bottom = OursSpacing.lg` — good, uses tokens.** Confirmed.

**F-49. No handle/grip indicator** — M3 `ModalBottomSheet` renders its own handle. Good.

**F-50. `containerColor = colors.surface`, `contentColor = colors.ink` — good, uses tokens.** Confirmed.

**F-51. Accessibility: M3 ModalBottomSheet provides semantics. Good.**

---

### OursSegmentedControl (`Components.kt:390`)

**F-52. Height `heightIn(min = 44.dp)` — meets 44dp touch target. Good.** Confirmed.

**F-53. `minSegmentWidth = 54.dp` default — good, gives each segment ≥ 54dp which at 44dp height is a comfortable touch target.** Confirmed.

**F-54. Thumb background uses `colors.action` — good, one action color.** Confirmed.

**F-55. Segment text animates between `onAction` (selected) and `inkSecondary` (unselected) — good.** Confirmed.

**F-56. Semantics: `role = Role.Tab`, `selected = isSelected` on each segment — correct.** Confirmed.

**F-57. The clickable on each segment creates a new `MutableInteractionSource()` inside the `forEach` — this is recreated on every recomposition of the row.** `Components.kt:447`. Since it's inside the `forEach` over options, each segment gets a fresh interaction source on recomposition. That's OK for press detection (it's remembered per composable frame), but if you wanted to preserve press state across recompositions you'd `remember` it by the value. Currently it works because Compose re-composes the whole row on selection change anyway. Minor inefficiency, not a bug.

**F-58. The thumb `offset` animation uses `animateDpAsState` with `springSnappy` — good.** Confirmed.

**F-59. The segmented control's outer box has `widthIn(min = minSegmentWidth * count + 6.dp)` — the `+ 6.dp` accounts for the 3.dp padding on each side (total 6.dp).** `Components.kt:404`. This is correct but a magic number derived from the padding. If padding changes, this needs to change. Could compute from the same padding token. Low priority.

**F-60. The thumb `height = 38.dp` is hardcoded while the outer box is `44.dp` min — the thumb is 6dp shorter than the track.** `Components.kt:421`. This gives the thumb a slightly inset look. It's a design choice. The 38dp is a magic number. Low priority.

**F-61. `OursSegmentedControl` is used for: theme toggle (3 options), language toggle (2 options), map/timeline toggle (2 options), pair mode (2 options).** All good uses. Confirmed.

**F-62. In `OnboardingScreen.kt:321`, the pair mode segmented control uses `true to strings.createPair, false to strings.joinWithCode` — the `true/false` values are `Boolean`, and `selected = pairModeCreate` is a `Boolean`. Works. But the generic `<T>` is instantiated as `Boolean` here — fine.**

---

### OursStatusChip (`Components.kt:535`)

**F-63. Token-based: `background = bg`, `foreground = fg`, `dot = dot` from `OursColors` per tone. Good.** Confirmed.

**F-64. `horizontal = OursSpacing.xs, vertical = OursSpacing.xxs` padding — uses tokens. Good.** Confirmed.

**F-65. Dot size `7.dp` is hardcoded — should be a token.** `Components.kt:558`. Low priority.

**F-66. `Arrangement.spacedBy(6.dp)` between dot and label — hardcoded. Should be a token.** `Components.kt:554`. Low priority.

**F-67. `OursTypography.label` for the chip text — good, uses token.** Confirmed.

**F-68. No contentDescription on the chip — it's a decorative status indicator, not interactive, so no role needed.** Correct.

**F-69. `StatusTone.Muted` uses `surfaceMuted` bg + `inkSecondary` fg + `inkMuted` dot — the dot is `inkMuted` which on light mode is `0xFF5A606D` — a muted gray. On dark mode `inkMuted = 0xFF8E95A5`. The dot color for Muted is the inkMuted color, which is a text color, not a status color. That's intentional (muted = no status). Good.** Confirmed.

**F-70. `StatusTone` enum: Live, Paused, Muted, Error. No `Warning`.** Discussed in F-3. If SOS/crash needs a warning state distinct from danger, add `Warning` + tokens. Low priority for now.

---

### OursEmptyState (`Components.kt:598`)

**F-71. Uses `OursSpacing.xl` padding, `OursSpacing.xs` between elements — tokens. Good.** Confirmed.

**F-72. `illustrationRes` size `64.dp` hardcoded — should be a token.** `Components.kt:618`. Low priority.

**F-73. `actionLabel` + `onAction` renders an `OursButton(Secondary)` — good, reuses button.** Confirmed.

**F-74. `contentDescription = null` on the illustration Image — correct if the illustration is purely decorative.** But if the illustration conveys meaning (e.g. "no memories" icon), it should have a description. Currently `contentDescription = null` means "no description" which screen readers skip. If the illustration is decorative, null is correct. If it's informative, add a description. The empty state already has a title and detail text, so the illustration is supplementary — null is acceptable. But consider `contentDescription = "${title}, ${detail}"` or mark it decorative explicitly with `semantics { hidden() }`. Minor.

**F-75. `OursEmptyState` used in MemoriesScreen (no memories, no results) and ChatScreen (say hello, no results).** Good reuse. Confirmed.

---

### OursErrorState (`Components.kt:724`)

**F-76. Uses `OursCard` + `OursButton(Secondary)` — reuses components. Good.** Confirmed.

**F-77. Title uses `strings.couldNotLoadTitle` — good, localizes the error title.** Confirmed.

**F-78. `OursSpacing.xxs` between title and message, `OursSpacing.sm` before button — tokens. Good.** Confirmed.

**F-79. The error message `Text` uses `OursTypography.caption` + `colors.inkSecondary` — good.** Confirmed.

**F-80. `OursErrorState` used in MapScreen (`MapScreen.kt:308`) and MemoriesScreen (`MemoriesScreen.kt:173`) and ChatScreen (`ChatScreen.kt:223`).** Good reuse. Confirmed.

**F-81. No icon/illustration on the error state — just text.** The PRD says "clear headlines, loading states" — an error state with an icon (e.g. alert icon) would be more recognizable. Currently it's a card with "Could not load" + message + try again. Functional but no error icon. Low priority. Consider adding an `OursIcons.Alert` icon. (OursIcons.Alert exists — `OursIcons.kt:265`.)

---

### OursLoadingState (`Components.kt:572`)

**F-82. `CircularProgressIndicator` size `24.dp`, `strokeWidth = 2.5.dp`, color `colors.action` — uses token for color. Good.** Confirmed.

**F-83. `24.dp` size and `2.5.dp` stroke are hardcoded — should be tokens.** Low priority.

**F-84. Message uses `OursTypography.caption` + `colors.inkSecondary` — good.** Confirmed.

**F-85. `OursSpacing.xl` vertical padding — token. Good.** Confirmed.

**F-86. `OursLoadingState` used in MemoriesScreen and ChatScreen.** Good reuse. Confirmed.

---

### OursMapView (`OursMapView.kt`)

This is a large component. Audit the Compose/UI layer, not the MapLibre integration per se.

**F-87. `partnerName: String = "Partner"` default — good fallback.** Confirmed.

**F-88. `styleUri = if (isDark) "asset://ours_map_style_dark.json" else "asset://ours_map_style.json"` — uses theme to pick map style. Good.** Confirmed. The map styles themselves are assets — not audited here.

**F-89. `map.uiSettings.isAttributionEnabled = false`, `isLogoEnabled = false` — good, removes clutter.** Confirmed.

**F-90. `nudgeProgress` animation uses `OursMotion.enterSpec(1050)` — 1050ms is much longer than the 150-250ms guideline.** `OursMapView.kt:329`. This is a nudge camera flyover animation — it's a special spectacle moment, not everyday UI. The PRD animation tips are for "everyday UI"; a map flyover is a showcase moment and can be longer. Not a bug, but note the deviation from the token's default `ENTER_MS = 240`. It uses `enterSpec(1050)` explicitly. Acceptable for this context.

**F-91. Partner marker movement animation uses `OursMotion.enterSpec(450)` — 450ms.** `OursMapView.kt:198`. Same note — a marker glide is a special animation, 450ms is reasonable. Not everyday UI.

**F-92. `partnerAvatarPin` uses `isLive` to pick green (success) vs gray — uses `Color.parseColor("#1D7A3B")` and `Color.parseColor("#68686E")` hardcoded.** `MapMarkerFactory.kt:33`. These should use `OursColors.success` and a muted gray token. But `MapMarkerFactory` is a plain Android `Canvas` bitmap generator, not a Compose component — it doesn't have access to `OursTheme`. **This is a real architectural issue:** the bitmap factory hardcodes hex colors that should come from the design tokens. The factory takes a `Context` but not a colorset. To use tokens it would need to either (a) take colors as params, or (b) read them from a shared source. Currently:
- `partnerAvatarPin`: `ringColor = if (isLive) Color.parseColor("#1D7A3B") else Color.parseColor("#68686E")` — `#1D7A3B` is the light-mode `success` color. But in dark mode, `success = 0xFF30D158` (a brighter green). The bitmap will always use the light-mode green even in dark mode. **Bug:** live ring color is hardcoded to light-mode success, wrong in dark mode.
- `partnerAvatarPin`: `fillColor = Color.parseColor("#0055FF")` — this is light-mode `action`. In dark mode, `action = 0xFF4D8BFF`. The bitmap always uses light-mode action. **Bug:** avatar fill is light-mode action, wrong in dark mode.
- `placePin`: `bgPaint color = Color.parseColor("#1C1C1E")` — near-black. In light mode this is fine (dark pin on light map). In dark mode the map is dark, and a `#1C1C1E` pin may be invisible. **Potential bug:** place pin background is a fixed near-black, may have poor contrast on the dark map style.
- `highlightPin`: `accentPaint color = Color.parseColor("#0055FF")` — light-mode action. Same dark-mode bug as F-92.
- `myLocationPin`: `green = Color.parseColor("#1D7A3B")` — light-mode success. Same dark-mode bug.

**This is the most significant finding in the audit:** `MapMarkerFactory` hardcodes light-mode hex colors and doesn't adapt to dark mode. The map markers will look wrong in dark mode (wrong action color, wrong success color, possible low contrast for place pins).

**F-93. `partnerAvatarPin` size `52f * density` — hardcoded.** Should this match `OursAvatar` sizes? The marker is a MapLibre marker, rendered as a bitmap, so it's in px. 52dp-equivalent is reasonable for a map marker. Not a bug.

**F-94. `placePin` text size `11.5f * density`, `heightPx = 28f * density` — hardcoded.** Fine for a map marker label.

**F-95. `highlightPin` size `32f * density` — hardcoded.** Fine.

**F-96. `myLocationPin` size `64f * density` — hardcoded.** Fine.

**F-97. `myLocationPin` azimuth cone: `canvas.rotate(bucketDeg.toFloat(), cx, cy)` and draws a 60° arc from -120° to -60°. The cone is green with alpha 65.** `MapMarkerFactory.kt:151-158`. Good visual. The green is hardcoded (F-92).

**F-98. `onMapLongClick` and `onHighlightClick` semantics — the map view itself has no accessibility label.** `OursMapView` is a `Box` with an `AndroidView` (MapLibre). MapLibre's accessibility depends on the SDK. The Compose wrapper doesn't add semantics. For a map screen, the partner info card (above the map) provides the accessible partner info. The map itself is a complex visual — full accessibility is hard. Not a bug for v1, but note it.

**F-99. `nudgeProgress` animation and the nudge canvas overlay — the overlay draws on a `Canvas(modifier = Modifier.fillMaxSize())` on top of the map.** `OursMapView.kt:379`. This is the nudge flight animation. It reads `colors.action` from `OursTheme.colors` — good, uses token. The animation uses hardcoded sizes: `12.dp`, `44.dp`, `24.dp`, `7.dp`, `3.dp`, `16.dp`, `68.dp`, `14.dp`, `46.dp`. Many magic numbers. These are animation-stage-specific sizes; could be documented constants but are acceptable for a one-off animation. Low priority.

**F-100. `cameraMoveTick` is read but marked `@Suppress("UNUSED_EXPRESSION")` — it's used as a recomposition trigger so the canvas redraws on camera move.** `OursMapView.kt:381`. Clever but hacky — using a state value as a side-effect trigger via suppress. A cleaner approach would be to use `LaunchedEffect` with the camera move listener to invalidate. Not a bug, but a code smell.

---

### OursStaggeredItem (`Components.kt:798`)

**F-101. Uses `OursMotion.STAGGER_MS` (28ms) and `OursMotion.enterSpec()` / `OursMotion.springFluid()` — tokens. Good.** Confirmed.

**F-102. `index.coerceIn(0, 6)` — stagger is capped at index 6 (i.e. 6 * 28 = 168ms max delay).** `Components.kt:807`. This means items after index 6 don't get additional stagger delay. For a long list (e.g. 50 memories), items 7+ all animate at the same time as item 6. This is a deliberate cap to avoid excessive delay on long lists. Reasonable. Not a bug.

**F-103. `alpha` animates 0→1, `offsetY` animates 14f→0f.** `Components.kt:803-804`. The 14f initial offset is hardcoded. Should be a token (e.g. `OursSpacing.sm` = 12, or a stagger-specific token). Low priority.

**F-104. `LaunchedEffect(Unit)` — both effects use `Unit` as key, so they run once per composition.** `Components.kt:806,811`. This is correct for a "run once on appear" animation. But if the item recomposes (e.g. data changes), the animation won't re-run. That's the intended behavior for a staggered enter animation. Good.

**F-105. `graphicsLayer { alpha = alpha.value; translationY = offsetY.value * density }` — uses `density` from the `graphicsLayer` scope.** `Components.kt:818-820`. Correct. Confirmed.

---

### OursInterlockedRingsMark (`Components.kt:844`)

**F-106. This is a custom canvas drawing of two interlocked rings with progress + converge progress.** Used in `SettingsScreen.kt:??` (need to check — actually it's imported in `SettingsScreen.kt:82` but I need to find the usage). Let me check. It's imported but the usage isn't in the files I've read so far. It's likely used in the pairing/onboarding ring animation. The component itself:

**F-107. All sizes are relative to `size.minDimension / 100f` (the `s` scale factor).** `Components.kt:853`. This makes the drawing scale to any size. Good practice for a canvas icon.

**F-108. `progress: Float = 1f` (0..1) controls the arc sweep. `convergeProgress: Float = 1f` controls how much the rings converge.** `Components.kt:849-850`. Good params.

**F-109. Default colors: `leftRingColor = OursTheme.colors.ink`, `rightRingColor = OursTheme.colors.action`, `backgroundColor = OursTheme.colors.canvas`.** `Components.kt:846-848`. These read `OursTheme.colors` directly — correct for a composable that's always used inside a theme. Good.

**F-110. The "converge" effect: when `progress > 0.7f`, it draws a background-colored arc over part of each ring to create a "cut" effect, then redraws the ring arc on top.** `Components.kt:886-923`. This is a clever masking technique. Good.

**F-111. `stroke = 6.5f * s`, `cutStroke = 10.5f * s` — these are relative to size, good.** Confirmed.

**F-112. No accessibility label — it's a decorative marking.** Correct if decorative. If it conveys progress (e.g. pairing progress), it should have a semantics label. Depends on usage. The component has no semantics block. If used to show pairing progress, add `semantics { contentDescription = "Pairing progress: $progress" }`. Low priority.

---

### OursIcons (`OursIcons.kt`)

**F-113. Icons use `ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)` — standard 24dp icon size. Good.** Confirmed.

**F-114. `stroke = SolidColor(Color.Black)` — all icons use a black stroke.** `OursIcons.kt:21`. This means the icons are always black, regardless of theme. In light mode, black icons on light canvas need a tint (the caller tints them). In dark mode, black icons on dark canvas would be invisible unless tinted. The callers do tint them: e.g. `MapScreen.kt:377` uses `tint = colors.action` for Crosshair, `MapScreen.kt:298` uses `tint = colors.paused` for ChevronRight. So the icons are tinted at use sites. **But** if an icon is used without a tint in dark mode, it will be black-on-dark = invisible. This is a latent bug. The icon set should either (a) use a tintable stroke (e.g. `LocalContentColor.current`), or (b) document that all uses must tint. Currently it works because all uses tint, but it's fragile. **Recommendation:** change `stroke = SolidColor(Color.Black)` to read `LocalContentColor.current` or make the stroke color a parameter. Low priority (no current bug, but fragile).

**F-115. `ChevronRight` uses `strokeLineWidth = 2.0f` while all others use `1.8f`.** `OursIcons.kt:294`. Inconsistent stroke width. Standardize to 1.8f or 2.0f. Minor.

**F-116. Icon set coverage: Map, Memories, Chat, Settings, Camera, Send, Heart, Crosshair, Close, Trash, Plus, Compass, Alert, Search, ChevronRight.** 15 icons. Good for v1. The PRD says "consistent icons" — they're all 24dp, all 1.8f stroke (except ChevronRight), all round caps/joins. Consistent. Good.

**F-117. `Heart` icon exists but is it used?** I don't see it used in the screens I've read. It may be unused. Check: `ChatScreen.kt` doesn't use it. `SettingsScreen.kt` doesn't. `MapScreen.kt` doesn't. `MemoriesScreen.kt` doesn't. `OnboardingScreen.kt` doesn't. `OursWebShellScreen.kt` doesn't. **Unused icon: `OursIcons.Heart`.** Either use it (e.g. for a "thinking of you" feature) or remove it. Low priority.

**F-118. `Send` icon used in ChatScreen for the send button — correct.** Confirmed.

**F-119. `Camera` icon used in ChatScreen for the camera button and in Settings for the avatar camera picker — correct.** Confirmed.

**F-120. `Close` icon used in search bars — correct.** Confirmed.

**F-121. `Search` icon used in search bars — correct.** Confirmed.

---

### OursNudgeWaveOverlay (`Components.kt:644`) — internal

**F-122. This component draws the nudge ring animation on a canvas.** It's used in... let me check. It's not obviously used in the screens I've read. `MapScreen.kt` uses `nudgePulseKey` + `nudgeRingScale`/`nudgeRingAlpha` for a ring around the nudge button, but that's implemented inline in `MapScreen.kt:340-367`, not via `OursNudgeWaveOverlay`. So `OursNudgeWaveOverlay` may be unused. **Check:** search for usages. Actually I should verify. The component is public (not private), so it might be used elsewhere. For now, flag as "verify usage."

**F-123. The overlay uses `remember(nudgeKey) { Animatable(0f) }` — keyed by nudgeKey, so each nudge gets its own animation.** Good. Confirmed.

**F-124. `LaunchedEffect(nudgeKey)` blocks — three separate LaunchedEffects with the same key.** `Components.kt:654,657,661`. This works but is slightly odd — three effects with the same key will all fire on nudgeKey change. Could be one effect with three animations. Minor.

**F-125. Hardcoded animation durations: `900` for ring1/ring2, `850` for travel.** `Components.kt:655,659,662`. These exceed the 150-250ms guideline but are for a decorative nudge effect — acceptable as a special moment.

**F-126. Hardcoded geometry: origin `size.width * 0.5f, size.height * 0.62f`, target `size.width * 0.5f, size.height * 0.34f`, maxR `size.minDimension * 0.36f`.** `Components.kt:670-672`. These are animation-specific proportions. Acceptable.

**F-127. `style = Stroke(width = 3.dp.toPx())` and `2.dp.toPx()` — hardcoded stroke widths.** Low priority.

---

### OursSendConfirmation (`Components.kt:753`)

**F-128. Animated visibility with `fadeIn + scaleIn` enter, `fadeOut + scaleOut` exit, using `OursMotion.enterSpec()` / `exitSpec()` / `springSnappy`.** Good, uses tokens. Confirmed.

**F-129. `initialScale = 0.92f`, `targetScale = 0.92f` — hardcoded.** PRD animation tip #10 "Start popovers at 0.95 scale" — 0.92 is close. Minor deviation. Low priority.

**F-130. Surface: `RoundedCornerShape(OursRadius.control)` + `colors.successSoft` + `BorderStroke(1.dp, colors.success.copy(alpha = 0.35f))`.** Uses tokens for radius and colors. The `1.dp` border width is a magic number. Low priority.

**F-131. Inner dot: `size = 8.dp`, `colors.success`.** `Components.kt:783`. Hardcoded size. Low priority.

**F-132. Text: `OursTypography.label.copy(fontWeight = FontWeight.SemiBold)`, `colors.success`.** Good. Confirmed.

**F-133. Padding: `horizontal = OursSpacing.md, vertical = OursSpacing.sm`.** Tokens. Good. Confirmed.

**F-134. Used in `OursViewModel.showConfirmation()` flow — the confirmation message is shown via this component.** Confirmed in `MapScreen.kt`? Actually the confirmation is shown via `OursSendConfirmation` — but I need to find where it's rendered. It's likely in a base screen layout. Let me check. It's imported in... I haven't seen it imported in the screens. It may be rendered in a shared layout or in `OursWebShellScreen`. Actually, the confirmation is shown via `OursSendConfirmation` which needs to be in a Compose hierarchy. Let me check if it's used. I'll flag for verification.

---

### rememberOursLayoutSpec / OursLayoutSpec / OursBreakpoint (`Components.kt:94-122`)

**F-135. Breakpoints: Compact < 360dp, Standard < 600dp, Expanded ≥ 600dp.** Good.

**F-136. `horizontalPadding`: 14dp / 16dp / 24dp.** These are hardcoded per breakpoint. Should they be tokens? They're layout-specific, so hardcoding per breakpoint is acceptable. But `14.dp` for compact is not a spacing token value (tokens are 4,8,12,16,24,32). 14 is a custom value. Minor inconsistency with the spacing scale. Low priority.

**F-137. `maxContentWidth = 560.dp` for Expanded.** Hardcoded. Acceptable as a layout constant. Good.

**F-138. `oursHeaderInsets()` adds `windowInsetsPadding(statusBars + displayCutout)` + `padding(top = 12.dp)`.** `Components.kt:131-134`. The `12.dp` top padding is a magic number (should be a token, e.g. `OursSpacing.sm` = 12 — actually it IS 12 = `OursSpacing.sm`. So it could be `OursSpacing.sm`. Minor — use the token for clarity.

**F-139. `oursContentWidth` constrains width to `spec.maxContentWidth` if set.** Good.

---

### OursViewModel + screens using components

**F-140. `OursErrorState` is used with `message = state.errorMessage!!` — the `!!` is safe because the `when` branches check `isNullOrBlank()` first.** `MapScreen.kt:309`. Correct.

**F-141. `MapScreen` partner card: `OursCard(radius = 22.dp, padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), frosted = true, elevation = 6.dp)`. The `radius = 22.dp` and `padding = 14.dp/10.dp` are hardcoded — not tokens.** `MapScreen.kt:203-206`. The 22dp radius is `OursRadius.mediaCard` (22). So it should be `radius = OursRadius.mediaCard`. The 14dp horizontal padding is not a token (closest is `md` = 16). The 10dp vertical is not a token (between `xs`=8 and `sm`=12). These should be tokens or the card should use defaults. Low priority.

**F-142. `MapScreen` paused banner card: `radius = 18.dp, padding = 14.dp/10.dp` — same issue.** `MapScreen.kt:279-282`. 18dp is not a radius token. Should be a token.

**F-143. `MemoriesScreen` card: `radius = 22.dp, padding = 12.dp` — 22dp = `OursRadius.mediaCard`, 12dp = `OursSpacing.sm`. Should use tokens.** `MemoriesScreen.kt:219-221`. Actually `padding = PaddingValues(12.dp)` — should be `PaddingValues(OursSpacing.sm)`. And `radius = 22.dp` should be `OursRadius.mediaCard`.

**F-144. `MemoriesScreen` staggered item image: `clip(RoundedCornerShape(OursRadius.inner))` — `OursRadius.inner = 12.dp`, but the card radius is 22dp with 12dp padding, so nested radius should be 22 - 12 = 10dp. Using `inner` (12) violates the nested rule.** `MemoriesScreen.kt:231`. Fix: use `RoundedCornerShape(10.dp)` or add a token `OursRadius.mediaCardImage = OursRadius.mediaCard - OursSpacing.sm`.

**F-145. `ChatScreen` message bubble: `widthIn(max = 288.dp)` and `CheckInBubble: widthIn(max = 284.dp)` — hardcoded max widths.** `ChatScreen.kt:668,767`. These are chat-bubble max widths. Should be tokens (e.g. `OursChatBubbleMaxWidth`). Low priority.

**F-146. `ChatScreen` reply chip: `sizeIn(minWidth = 44.dp, minHeight = 44.dp)` — 44dp touch target. Good.** Confirmed.

**F-147. `ChatScreen` save/reply/delete chips use `OursRadius.chip` (10dp) — token. Good.** Confirmed.

**F-148. `ChatScreen` message bubble `shape` uses custom corner radii (20dp, 8dp, 5dp) — these are bubble-specific radii, not tokens.** `ChatScreen.kt:652-657`. The 20dp matches `OursRadius.control` (14)? No, 20 ≠ 14. The 8dp and 5dp are custom. These are chat-bubble corner radii — they could be a small token set (`OursBubbleRadius.tip`, `OursBubbleRadius.bottom`). Low priority.

**F-149. `SettingsScreen` avatar: `size = 54.dp` — hardcoded, not a token.** `SettingsScreen.kt:197`. Should be a token (e.g. `OursAvatar.large = 54.dp`). Low priority.

**F-150. `SettingsScreen` places list item: `RoundedCornerShape(OursRadius.inner)` (12dp) on a `Surface` with `colors.surfaceMuted` — uses token. Good.** Confirmed.

**F-151. `SettingsScreen` Xiaomi steps card and other cards use default `OursCard` radius (20dp) — token. Good.** Confirmed.

**F-152. `OnboardingScreen` progress bar: `height = 4.dp` — hardcoded.** `OnboardingScreen.kt:668`. Should be a token. Low priority.

**F-153. `OnboardingScreen` progress bar use `OursRadius.chip` (10dp) for the bar shape — token. Good.** Confirmed.

---

## Accessibility summary

| Component | A11y status | Notes |
|---|---|---|
| OursButton | ✅ | `role = Role.Button`, `onClick` provided. Label from `text` param (Text inside). Good. |
| OursCard (clickable) | ✅ | `role = Role.Button`, `onClick`. But no `contentDescription` — the card's content provides the label. OK if content is text. |
| OursSegmentedControl | ✅ | `role = Role.Tab`, `selected`. Good. |
| OursAvatar | ⚠️ | No label on the avatar itself. When used as a tap target, the parent must label it. Settings avatar tap target has `role = Button` but no `contentDescription` — **bug**. |
| OursStatusChip | ✅ | Decorative, no role needed. Good. |
| OursBottomSheet | ✅ | M3 provides semantics. Good. |
| OursSegmentedControl | ✅ | Tab role + selected. Good. |
| OursMapView | ⚠️ | No semantics label on the map. Complex visual. Partner info card above provides accessible info. Acceptable for v1. |
| OursIcons (in buttons) | ✅ | When used in a button with `contentDescription`, good. When used with `contentDescription = null`, they're decorative. Check each use. |
| OursSendConfirmation | ✅ | Surface with text — text provides label. But the surface itself has no `role` or `semantics`. If it's announced, add `semantics { contentDescription = message }`. Minor. |
| OursInterlockedRingsMark | ⚠️ | No semantics. If it conveys progress, add contentDescription. |

**Specific a11y bug to fix:**
- `SettingsScreen.kt:187-191` — avatar tap target: `Box(modifier = Modifier.clickable(role = Role.Button) { ... })` has no `contentDescription`. Screen reader announces "Button" with no name. Fix: add `semantics { contentDescription = "${strings.takePhoto} · ${meName}" }` or set `contentDescription` on the clickable.

**Specific a11y improvement:**
- `OursSendConfirmation` — add `semantics { contentDescription = message }` so the confirmation pill is announced.
- `OursInterlockedRingsMark` — if used for pairing progress, add `semantics { contentDescription = "Pairing progress" }`.

---

## Reusability summary

| Component | Reusability | Notes |
|---|---|---|
| OursButton | ✅ Excellent | Used in MapScreen, MemoriesScreen, ChatScreen, SettingsScreen, OnboardingScreen. 4 variants cover all needs. |
| OursCard | ✅ Excellent | Used everywhere. `frosted` variant + `OursGlassCard` alias. |
| OursAvatar | ✅ Good | Used in MapScreen, ChatScreen, SettingsScreen. Size param allows flexibility. |
| OursStatusChip | ✅ Good | Used in MapScreen, ChatScreen, SettingsScreen, OnboardingScreen. |
| OursSegmentedControl | ✅ Good | Used for theme, language, map/timeline, pair mode. Generic `<T>`. |
| OursBottomSheet | ✅ Good | Used in SettingsScreen, OnboardingScreen. |
| OursEmptyState | ✅ Good | Used in MemoriesScreen, ChatScreen. |
| OursErrorState | ✅ Good | Used in MapScreen, MemoriesScreen, ChatScreen. |
| OursLoadingState | ✅ Good | Used in MemoriesScreen, ChatScreen. |
| OursGlassBackdrop | ✅ Good | Used in MemoriesScreen, SettingsScreen, ChatScreen, OnboardingScreen. |
| OursIcons | ✅ Excellent | 15 icons, used across all screens. |
| OursStaggeredItem | ✅ Good | Used in MemoriesScreen. |
| OursInterlockedRingsMark | ⚠️ Verify | Imported in SettingsScreen — verify actual usage. |
| OursNudgeWaveOverlay | ⚠️ Verify | May be unused — the nudge animation is implemented inline in MapScreen. |
| OursSendConfirmation | ⚠️ Verify | May be rendered in a shared layout — verify. |
| OursMapView | ✅ Good | Used in MapScreen and MemoriesScreen (map mode). |
| oursLiquidGlass | ✅ Excellent | Used in MapScreen, MemoriesScreen, ChatScreen, SettingsScreen, OnboardingScreen. |

---

## Hardcoded values that should be tokens — consolidated list

These are the actionable "magic number → token" fixes, ranked by importance:

### P1 (should fix — affects correctness or cross-theme correctness)

1. **MapMarkerFactory hardcodes light-mode hex colors** (`MapMarkerFactory.kt:33,34,93,125,147`). The live ring (`#1D7A3B`), avatar fill (`#0055FF`), place pin bg (`#1C1C1E`), highlight accent (`#0055FF`), my-location green (`#1D7A3B`) are all light-mode values. In dark mode these are wrong. **Fix:** either pass a colorset into the factory, or read from a shared token source. Easiest: add a `MapMarkerColors` data class param that the caller fills from `OursTheme.colors`. Or, if the factory is always called from one place, read the colors from a static that's set by the theme. Best: change the factory to accept colors as params.

2. **`OursCard.elevation` param is unused for non-frosted cards** (`Components.kt:333,379`). Either apply it as `shadowElevation` or remove it. **Fix:** for non-frosted, set `shadowElevation = elevation` and remove the hardcoded `0.dp`; or remove the param and document that elevation is frosted-only.

### P2 (should fix — accessibility)

3. **Settings avatar tap target has no contentDescription** (`SettingsScreen.kt:187-191`). **Fix:** add `contentDescription`.

4. **`OursSendConfirmation` should have a semantics label** (`Components.kt:753`). **Fix:** add `semantics { contentDescription = message }`.

### P3 (should fix — tokenize magic numbers, medium impact)

5. **OursAvatar border widths** (`Components.kt:511-512`): `1.5.dp` and `0.75.dp` → add to a stroke token object (e.g. `OursStroke.avatarRing`, `OursStroke.avatarBorder`).

6. **OursButton loading indicator** (`Components.kt:307-309`): `18.dp` size, `2.dp` stroke → tokens or named constants.

7. **OursCard border width** (`Components.kt:376-377`): `0.5.dp` / `0.75.dp` → `OursStroke.borderLight` / `OursStroke.border`.

8. **OursStatusChip dot size** (`Components.kt:558`): `7.dp` → token.

9. **OursStatusChip spacing** (`Components.kt:554`): `6.dp` → token.

10. **OursEmptyState illustration size** (`Components.kt:618`): `64.dp` → token.

11. **OursLoadingState indicator size/Stroke** (`Components.kt:585-587`): `24.dp`, `2.5.dp` → tokens.

12. **oursLiquidGlass inner highlight stroke** (`Components.kt:182,210`): `0.75.dp` → token (same as border).

13. **oursLiquidGlass shadow alpha base** (`Components.kt:190`): `0.04f` → named constant.

14. **OursSegmentedControl thumb height** (`Components.kt:421`): `38.dp` → token or derive from track height.

15. **OursSegmentedControl outer width padding** (`Components.kt:404`): `+ 6.dp` → derive from padding token.

16. **OursNudgeWaveOverlay / OursMapView nudge animation strokes** (`Components.kt:679,687,710` and `OursMapView.kt:411,435,454,460,474,477,484`): multiple `X.dp.toPx()` → document as constants.

17. **OursInterlockedRingsMark strokes** (`Components.kt:854-856`): relative to size, OK. But `stroke = 6.5f * s` and `cutStroke = 10.5f * s` — these are fine as relative.

### P4 (should fix — low impact, consistency)

18. **Onboarding progress bar height** (`OnboardingScreen.kt:668,676`): `4.dp` → token.

19. **Chat bubble max widths** (`ChatScreen.kt:668,767`): `288.dp`, `284.dp` → tokens.

20. **Chat bubble corner radii** (`ChatScreen.kt:652-657`): `20.dp`, `8.dp`, `5.dp` → small token set.

21. **Settings avatar size** (`SettingsScreen.kt:197`): `54.dp` → token ` OursAvatar.large`.

22. **MapScreen partner card radius/padding** (`MapScreen.kt:203-206`): `22.dp` → `OursRadius.mediaCard`; `14.dp/10.dp` → tokens.

23. **MapScreen paused banner radius/padding** (`MapScreen.kt:279-282`): `18.dp` → token; `14.dp/10.dp` → tokens.

24. **MemoriesScreen card radius/padding** (`MemoriesScreen.kt:219-221`): `22.dp` → `OursRadius.mediaCard`; `12.dp` → `OursSpacing.sm`.

25. **MemoriesScreen image corner radius vs nested rule** (`MemoriesScreen.kt:231`): `OursRadius.inner` (12) should be `22 - 12 = 10` per nested rule. Fix to `10.dp` or add `OursRadius.mediaCardImage = OursRadius.mediaCard - OursSpacing.sm`.

26. **OursTypography button label** — add a dedicated `button` text style (16sp SemiBold) to avoid copying `title` + `FontWeight.SemiBold` everywhere. Currently `OursButton` and others copy it. Minor.

27. **OursMotion.PRESS_SCALE** — reconcile 0.96 (token) with PRD's 0.97. Pick one. Minor.

28. **OursIcons.ChevronRight stroke width** — standardize to 1.8f (or 2.0f). Minor.

29. **OursIcons stroke color** — use `LocalContentColor.current` or a param instead of hardcoded `Color.Black`, so icons are tint-safe. Minor but prevents a latent dark-mode bug.

30. **OursColors: remove or use dead glass tokens** (`glassSurface`, `glassSurfaceStrong`, `glassActionTint`, `glassBorder`). Either use them or remove them.

31. **OursNudgeWaveOverlay usage** — verify if it's used. If not, remove or move to a shared nudge animation.

32. **OursSendConfirmation usage** — verify where it's rendered. If it's not in any screen, it's dead code.

33. **OursIcons.Heart usage** — verify if used. If not, remove or assign to a feature.

---

## Visual design vs PRD spec

**PRD design requirements (from `docs/PRD.md`):**

- "Apple HIG Light/Dark palette (`#F2F2F7` / `#FFFFFF` light, `#000000` / `#1C1C1E` / `#2C2C2E` dark, `#0066CC` / `#006EE6` Apple System Blue)" — The OURS palette is **not** exactly Apple's. Ours uses `#FAF9F5` canvas (warm off-white), `#FFFFFC` surface, `#0055FF` action (a slightly deeper blue than Apple's `#0066CC`). This is a deliberate departure — OURS has its own warm palette. The PRD says "Apple HIG palette" but the actual tokens are a custom warm variant. **This is a design decision, not a bug.** The PRD's Apple palette reference seems to be a starting point that was customized. As long as the custom palette is consistent and accessible, it's fine. **Note:** `#0055FF` (OURS action) vs `#0066CC` (Apple blue) — OURS blue is more saturated. Check contrast: on white, `#0055FF` has contrast ratio ~5.3:1 against white text? Actually `onAction = 0xFFFFFFFF` on `action = 0xFF0055FF` — contrast ratio is ~5.3:1. That passes WCAG AA for large text (4.5:1) but not AAA (7:1). For normal text on a button, the label is 16sp SemiBold — at 16sp SemiBold, 5.3:1 passes AA. OK.

- "tokens for color, type, spacing, radius, and motion, plus a small component set (buttons, cards, bottom sheet, avatar, status chip)" — ✅ All present.

- "One action color" — ✅ `action` is the single action color. Used consistently.

- "large touch targets" — ✅ 48dp buttons, 44dp segmented control, 44dp chips.

- "few choices per screen" — ✅ The screens have few actions. Good.

- "Fast, simple motion: ease-out entrances, 150 to 250 ms, no bounce on everyday UI." — ✅ `enterSpec` uses `EaseOut`, 240ms default. `springSnappy` (damping 0.80) for press — slight overshoot but not "bounce". `springBouncy` only for nudge pulse. Good.

- "20 design tips" — Let me check a few:
  - Tip #1 "Delete filler badges" — No filler badges observed. ✅
  - Tip #2 "Drop redundant labels" — Status chips label themselves. Avatar has initial. Good. ✅
  - Tip #3 "Cut unnecessary sizes and random borders" — Borders are token-driven (mostly). Some random borders remain (P3 list). Partial. ⚠️
  - Tip #4 "Use mono font only where needed" — Tabular numbers use `tnum` feature. ✅
  - Tip #5 "Use a design system" — ✅ This audit is about the design system. ✅
  - Tip #11 "Add an action color" — ✅ `action` color. ✅
  - Tip #12 "Keep icons consistent" — ✅ 24dp, consistent stroke. ✅
  - Tip #14 "Nested radius: outer = inner + padding" — Documented but not always followed (F-6, F-144). ⚠️
  - Tip #15 "Improve text contrast" — Action label on action bg: 5.3:1, passes AA. Other text uses ink/inkSecondary on canvas/surface — high contrast. ✅
  - Tip #18 "Use tabular numbers" — ✅ `tabular` and `tabularLarge` styles. ✅

- "20 animation tips" — Let me check a few:
  - Tip #1 "Ease-out for entrances" — ✅ `EaseOut`. ✅
  - Tip #2 "Keep dropdowns between 150 and 250 ms" — Exit is 175ms. ✅
  - Tip #3 "Stagger releases by about 30 ms" — 28ms. ✅
  - Tip #4 "Scale to 0.97 on press" — Token is 0.96. Close. ⚠️
  - Tip #7 "Use arcs for object movement" — Nudge animation uses a quadratic Bézier arc. ✅
  - Tip #9 "Ease-in-out for moves" — `moveSpec` exists but unused. ⚠️
  - Tip #13 "No bounce on everyday UI" — ✅ `springBouncy` only for nudge. ✅
  - Tip #14 "Don't tween while held" — Press scale uses a spring that settles; when held, the scale stays at 0.96. No tweening while held. ✅
  - Tip #15 "Make exits about 20% shorter than entrances" — 175 vs 240 = 27% shorter. Close. ⚠️

---

## Summary

**Strengths:**
- Comprehensive, well-organized token system (colors, type, spacing, radius, motion).
- Correct CompositionLocal pattern for theme access.
- Strong component reuse — buttons, cards, chips, empty/error/loading states are all reused across screens.
- Glass morphism effect is well-implemented with GPU-cached drawing.
- Accessibility roles are mostly correct (Button, Tab, selected).
- 44/48dp touch targets throughout.
- Map marker factory exists and produces good-looking markers.

**The one real bug:** `MapMarkerFactory` hardcodes light-mode colors and doesn't adapt to dark mode. This will make markers look wrong (wrong action color, wrong live-ring color, possible low contrast for place pins) when the app is in dark mode.

**The one real accessibility bug:** Settings avatar tap target has `Role.Button` but no `contentDescription`.

**The one misleading API:** `OursCard.elevation` param does nothing for non-frosted cards.

**Many P3/P4 tokenization opportunities** — the component library is mostly token-driven, but a few magic numbers remain (border widths, dot sizes, illustration sizes, loading indicator sizes, some corner radii, some padding values). These are low-severity but should be cleaned up for a polished design system.

**A few dead/unkempt items to verify:** `OursNudgeWaveOverlay` usage, `OursSendConfirmation` usage, `OursIcons.Heart` usage, dead glass tokens.

---

*End of audit.*
