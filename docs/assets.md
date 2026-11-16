# OURS Fonts, Icons & Assets (Slice 2)

Extracted from [OURS-Design.figcraft](./OURS-Design.figcraft) (`01 Design System`: `Brand_Icon_DynamicIsland`, `UI_UX_Asset_Library`, `Components_Sheet`, `04 Mobile Map`).

## 1. Self-Hosted Fonts (`web/fonts/`)

| Font Family | File | Format | Weights | Source |
| :--- | :--- | :--- | :--- | :--- |
| `Inter` | `web/fonts/Inter-VF.woff2` | Variable WOFF2 | `400` (Regular), `600` (SemiBold), `700` (Bold) | `/home/dewhush/.local/share/fonts/sentra/Inter.ttf` |
| `Geist Mono` | `web/fonts/GeistMono-VF.woff2` | Variable WOFF2 | `500` (Medium), `600` (SemiBold), `700` (Bold) + `tnum` | `/home/dewhush/.local/share/fonts/sentra/GeistMono.ttf` |

## 2. Brand Marks & Illustrations (`web/assets/icons/`)

| Filename | FigCraft Node | Dimensions | Usage |
| :--- | :--- | :--- | :--- |
| `ours-icon-dark.svg` | `24622` (`OursIcon_Dark`) | `120x120` | Dark app icon / splash squircle mark |
| `ours-icon-light.svg` | `24629` (`OursIcon_Light`) | `88x88` | Light app icon / onboarding header mark |
| `nudge-wave.svg` | `24668` (`Stage_NudgeWave`) | `200x104` | Map pulse + travel arc illustration |
| `compass-rose.svg` | `24675` (`Stage_CompassRose`) | `200x104` | Always-on bearing cone illustration |
| `safety-shield.svg` | `24682` (`Stage_SafetyShield`) | `200x104` | Security #S1-#S8 + SOS shield illustration |
| `check-in-frame.svg` | `24688` (`Stage_CheckInFrame`) | `200x104` | 24h camera viewfinder illustration |
| `empty-memories-art.svg` | `24695` (`Stage_EmptyMemoriesArt`) | `200x104` | Memories empty state artwork |
| `pair-invite-art.svg` | `24702` (`Stage_PairInviteArt`) | `200x104` | Google + 6-char pair illustration |
| `empty-chat-art.svg` | `24708` (`Stage_EmptyChatArt`) | `200x104` | Messenger empty thread artwork |

## 3. UI, Navigation & Map FAB Icons (`web/assets/icons/`)

| Filename | FigCraft Node | Dimensions | Usage |
| :--- | :--- | :--- | :--- |
| `fab-settings.svg` | `25276` (`MapFabSettings`) | `42x42` | Map top-left settings FAB |
| `fab-alerts.svg` | `25283` (`MapFabAlerts`) | `42x42` | Map top-right alerts bell FAB |
| `fab-camera.svg` | `25286` (`MapFabCheckIn`) | `42x42` | Map right-rail camera check-in FAB |
| `fab-layers.svg` | `25289` (`MapFabLayers`) | `42x42` | Map right-rail layers FAB |
| `fab-recenter.svg` | `25292` (`MapFabRecenter`) | `42x42` | Map right-rail recenter FAB |
| `fab-bubble.svg` | `25294` (`MapFabBubble`) | `42x42` | Map right-rail privacy bubble FAB |
| `nav-map.svg` | `25327` (`NavIcon_Map`) | `14x14` | Floating bottom dock Map icon |
| `nav-chat.svg` | `25330` (`NavIcon_Chat`) | `15x12` | Floating bottom dock Chat icon |
| `nav-memories.svg` | `25334` (`NavIcon_Memories`) | `13x13` | Floating bottom dock Memories icon |
| `nav-settings.svg` | `25337` (`NavIcon_Settings`) | `13x13` | Floating bottom dock Settings icon |

## 4. Map Style Assets (`web/assets/`)

| Filename | FigCraft Source | Palette | Usage |
| :--- | :--- | :--- | :--- |
| `map-style-light.json` | `25199` (`CustomMapCanvas` Light) | Land `#F4F3EE`, Parks `#DFECE4`, Water `#CFE2F5`, Roads `#FFFFFC` / `#F0EFE9`, Labels `#373B44` | MapLibre Light style (OpenFreeMap vector tiles) |
| `map-style-dark.json` | `26537` (`CustomMapCanvas` Dark) | Land `#0C1018`, Parks `#10211D`, Water `#112238`, Roads `#232B3D` / `#181E2C`, Labels `#8E96A8` | MapLibre Dark style (OpenFreeMap vector tiles) |

## 5. Assets Not Obtained

none (`imageIndex: []` in `OURS-Design.figcraft`; 100% of fonts, brand icons, illustrations, UI/nav/FAB SVGs, and Light/Dark map styles obtained cleanly).
