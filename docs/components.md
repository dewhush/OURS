# OURS Components (Slice 3A + 3B)

Reusable CSS + JS atomic and composite primitives built strictly from Slice 1 tokens (`web/styles.css`) and Slice 2 fonts/SVGs (`web/fonts/*`, `web/assets/icons/*`).

## Atomic Components (1-5)

| Component | Class | Variants | States |
| :--- | :--- | :--- | :--- |
| Buttons | `.ours-btn` | `primary` (`14px`), `primary-lg` (`302x54 r=27 15px`), `frosted`, `stone`, `danger`, `fab` (`42x42`), `circle` (`36x36`) | `default`, `pressed` (`scale(0.97)`), `disabled` (`opacity: 0.45`), `focus` (`2px solid var(--action)`) |
| Inputs | `.ours-input-wrap` / `.ours-input` | `code` (`302x68 r=18 Geist Mono 26px`), `glass`, `standard`, `search` | `default`, `pressed`, `disabled`, `focus` (`border-color: var(--action)` + `--focus-ring`) |
| Chips | `.ours-chip` | `live`, `paused`, `peek-active`, `peek`, `segment`, `media-dark`, `media-light` | `default`, `pressed`, `disabled`, `focus` |
| Avatars | `.ours-avatar` | `map-partner` (`46x46`), `map-self` (`44x44`), `header` (`40x40` `#0055FF` + online dot), `island` (`44x44`), `nudge` (`44x44`) | `default`, `pressed`, `disabled`, `focus` |
| Badges | `.ours-badge` | `battery` (`48x17` `#146C43` dot + `Geist Mono 9px`), `speed` (`108x25` `10px`), `dwell` (`104x22` `10px`), `statusbar`, `expiry` (`tnum`) | `default`, `pressed`, `disabled`, `focus` |

## Composite Components (6-11)

| Component | Class | Variants | States |
| :--- | :--- | :--- | :--- |
| Cards | `.ours-card`, `.ours-bubble`, `.ours-photo-card` | `ivory-card`, `glass-card`, `bubble-partner` (`290x62`), `bubble-me` (`290x62`), `bubble-photo-24h` (`250x214`), `couple-header-card` (`358x66`) | `default`, `pressed`, `disabled`, `focus` |
| Bottom Nav | `.ours-bottom-nav` | `FloatingBottomNav` (`288x62 r=31`, 4 tabs `62x48`, active ElectricTint pill, alert dot) | `default`, `pressed`, `disabled`, `focus` |
| Top Bar | `.ours-top-bar` | `frosted-chat-header` (`358x64`), `map-top-controls` (`MapFabSettings`, `MapCenterSwitcher` `192x36`, `MapFabAlerts`), `onboarding-top-bar` | `default`, `pressed`, `disabled`, `focus` |
| Sheets | `.ours-sheet` | `peek`, `focus`, `bubble`, `checkin` (`370px`, handle `30x6`, concentric `28px`/`22px`/`12px` radii) | `default`, `pressed`, `disabled`, `focus` |
| Toasts | `.ours-toast` | `arrival` (`414x68` CapsuleDark), `nudge` (`414x68` ElectricDark), `info` (CapsuleLight), `error` | `default`, `pressed`, `disabled`, `focus` |
| List Rows | `.ours-list-row` | `peek-m` (`342x86`), `peek-d` (`342x74`), `layer` (`330x54` + `.ours-switch`), `waiting` (`302x36`), `settings` (`358x56`) | `default`, `pressed`, `disabled`, `focus` |

## Preview

Served at `/components-preview.html` (includes all 11 component matrices across 4 states + Light/Dark toggle, plus the 1:1 `#panel-components-sheet` `540x1060` reference matching `#24540`).
