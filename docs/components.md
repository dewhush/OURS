# OURS Atomic Components (Slice 3A)

Reusable CSS + JS atomic primitives built strictly from Slice 1 tokens (`web/styles.css`) and Slice 2 fonts/SVGs (`web/fonts/*`, `web/assets/icons/*`).

## Components Inventory

| Component | Class | Variants | States |
| :--- | :--- | :--- | :--- |
| Buttons | `.ours-btn` | `primary`, `frosted`, `stone`, `danger`, `fab` (`42x42`), `circle` (`36x36`) | `default`, `pressed` (`scale(0.97)`), `disabled` (`opacity: 0.45`), `focus` (`2px solid var(--action)`) |
| Inputs | `.ours-input-wrap` / `.ours-input` | `code` (`26px`/`22px` Geist Mono), `glass`, `standard`, `search` | `default`, `pressed`, `disabled`, `focus` (`border-color: var(--action)` + `--focus-ring`) |
| Chips | `.ours-chip` | `live`, `paused`, `peek-active`, `peek`, `segment`, `media-dark`, `media-light` | `default`, `pressed`, `disabled`, `focus` |
| Avatars | `.ours-avatar` | `map-partner` (`46x46`), `map-self` (`44x44`), `header` (`40x40` + online dot), `island` (`44x44`), `nudge` (`44x44`) | `default`, `pressed`, `disabled`, `focus` |
| Badges | `.ours-badge` | `battery`, `speed`, `dwell`, `statusbar`, `expiry` (`tnum`) | `default`, `pressed`, `disabled`, `focus` |

## Preview

Served at `/components-preview.html` (backed by `web/components.css`, `web/components.js`, `web/components-preview.js`).
