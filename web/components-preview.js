"use strict";

(function () {
  const STATES = ["default", "pressed", "disabled", "focus"];

  function el(tag, cls, text) {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function img(src, w, h, alt) {
    const i = document.createElement("img");
    i.src = src;
    i.width = w;
    i.height = h;
    i.alt = alt || "";
    return i;
  }

  function stateClass(st) {
    if (st === "pressed") return "is-pressed";
    if (st === "disabled") return "is-disabled";
    if (st === "focus") return "is-focused";
    return "";
  }

  function cls(base, st) {
    return `${base} ${stateClass(st)}`.trim();
  }

  function getGrid(id, wide) {
    const grid = document.getElementById(id);
    if (!grid) return null;
    grid.replaceChildren();
    if (wide) grid.classList.add("cp-grid--wide");
    return grid;
  }

  function addHeader(grid) {
    const head = el("div", "cp-state-head");
    head.appendChild(el("span", "", "Variant"));
    STATES.forEach((st) => head.appendChild(el("span", "", st)));
    grid.appendChild(head);
  }

  function addRow(grid, label, builder) {
    const row = el("div", "cp-row");
    row.appendChild(el("span", "cp-row-label", label));
    STATES.forEach((st) => {
      const cell = el("div", "cp-cell");
      cell.appendChild(builder(st));
      row.appendChild(cell);
    });
    grid.appendChild(row);
  }

  function addStackRow(grid, label, builder, stage) {
    const row = el("div", "cp-row cp-row--stack");
    row.appendChild(el("span", "cp-row-label", label));
    const states = el("div", "cp-states");
    STATES.forEach((st) => {
      const cell = el("div", "cp-state-cell");
      cell.appendChild(el("span", "", st));
      const holder = stage ? el("div", "cp-stage") : cell;
      holder.appendChild(builder(st));
      if (stage) cell.appendChild(holder);
      states.appendChild(cell);
    });
    row.appendChild(states);
    grid.appendChild(row);
  }

  function disable(node, st) {
    if (st === "disabled") node.setAttribute("aria-disabled", "true");
    return node;
  }

  /* 1. Buttons */
  function renderButtons() {
    const grid = getGrid("grid-buttons");
    if (!grid) return;
    addHeader(grid);
    const variants = [
      { label: "primary", c: "ours-btn--primary", text: "Check in" },
      { label: "primary-lg", c: "ours-btn--primary ours-btn--lg", text: "Continue with Google" },
      { label: "frosted", c: "ours-btn--frosted", text: "Nudge" },
      { label: "stone", c: "ours-btn--stone", text: "Cancel" },
      { label: "danger", c: "ours-btn--danger", text: "Hold SOS" },
      { label: "icon-fab", c: "ours-btn--fab", icon: "/assets/icons/fab-camera.svg" },
      { label: "icon-circle", c: "ours-btn--circle", text: "+" }
    ];
    variants.forEach((v) => {
      addRow(grid, v.label, (st) => {
        const btn = el("button", cls(`ours-btn ${v.c}`, st));
        btn.type = "button";
        if (st === "disabled") btn.disabled = true;
        if (v.icon) btn.appendChild(img(v.icon, 42, 42, v.label));
        else btn.textContent = v.text;
        return btn;
      });
    });
  }

  /* 2. Inputs */
  function renderInputs() {
    const grid = getGrid("grid-inputs", true);
    if (!grid) return;
    addHeader(grid);
    const variants = [
      { label: "code-field", c: "ours-input-wrap--code", val: "K 7 M · 4 P 9", ph: "000 · 000" },
      { label: "glass-input", c: "ours-input-wrap--glass", val: "", ph: "Add a caption..." },
      { label: "standard-field", c: "ours-input-wrap--standard", val: "Dewantoro", ph: "Your name" },
      { label: "search-pill", c: "ours-input-wrap--search", val: "", ph: "Search chat..." }
    ];
    variants.forEach((v) => {
      addRow(grid, v.label, (st) => {
        const wrap = el("div", cls(`ours-input-wrap ${v.c}`, st));
        const input = el("input", "ours-input");
        input.type = "text";
        input.placeholder = v.ph;
        input.value = v.val;
        input.setAttribute("aria-label", v.label);
        if (st === "disabled") input.disabled = true;
        wrap.appendChild(input);
        return wrap;
      });
    });
  }

  /* 3. Chips */
  function renderChips() {
    const grid = getGrid("grid-chips");
    if (!grid) return;
    addHeader(grid);
    addRow(grid, "status-live", (st) => {
      const c = el("span", cls("ours-chip ours-chip--live", st));
      c.appendChild(el("span", "ours-chip-dot"));
      c.appendChild(document.createTextNode("Live"));
      return c;
    });
    addRow(grid, "status-paused", (st) => el("span", cls("ours-chip ours-chip--paused", st), "Paused"));
    addRow(grid, "peek-tab", (st) => el("span", cls("ours-chip ours-chip--peek-active", st), "All"));
    addRow(grid, "peek-tab-idle", (st) => el("span", cls("ours-chip ours-chip--peek", st), "People"));
    addRow(grid, "segment-switch", (st) => {
      const c = el("div", cls("ours-chip ours-chip--segment", st));
      c.appendChild(el("span", "ours-chip-thumb is-active", "EN"));
      c.appendChild(el("span", "ours-chip-thumb", "ID"));
      return c;
    });
    addRow(grid, "media-pill-dark", (st) => el("span", cls("ours-chip ours-chip--media-dark", st), "23h left"));
    addRow(grid, "media-pill-light", (st) => el("span", cls("ours-chip ours-chip--media-light", st), "Save"));
  }

  /* 4. Avatars */
  function renderAvatars() {
    const grid = getGrid("grid-avatars");
    if (!grid) return;
    addHeader(grid);
    const variants = [
      { label: "map-partner", c: "ours-avatar--map-partner", text: "M" },
      { label: "map-self", c: "ours-avatar--map-self", text: "D" },
      { label: "header-with-dot", c: "ours-avatar--header", text: "M", dot: true },
      { label: "island-partner", c: "ours-avatar--island", text: "M" },
      { label: "nudge-orb", c: "ours-avatar--nudge", core: true }
    ];
    variants.forEach((v) => {
      addRow(grid, v.label, (st) => {
        const a = el("div", cls(`ours-avatar ${v.c}`, st));
        if (v.core) a.appendChild(el("span", "ours-avatar-core"));
        else a.textContent = v.text;
        if (v.dot) a.appendChild(el("span", "ours-avatar-online"));
        return a;
      });
    });
  }

  /* 5. Badges */
  function renderBadges() {
    const grid = getGrid("grid-badges");
    if (!grid) return;
    addHeader(grid);
    addRow(grid, "battery-chip", (st) => {
      const b = el("span", cls("ours-badge ours-badge--battery", st));
      b.appendChild(el("span", "ours-badge-dot"));
      b.appendChild(document.createTextNode("84%"));
      return b;
    });
    addRow(grid, "speed-battery-pill", (st) => {
      const b = el("span", cls("ours-badge ours-badge--speed", st));
      b.appendChild(el("span", "ours-badge-dot"));
      b.appendChild(document.createTextNode("24 km/h"));
      return b;
    });
    addRow(grid, "dwell-pill", (st) => el("span", cls("ours-badge ours-badge--dwell", st), "Here for 19m"));
    addRow(grid, "statusbar-battery", (st) => {
      const b = el("div", cls("ours-badge ours-badge--statusbar", st));
      b.appendChild(el("span", "ours-badge-batt-fill"));
      b.appendChild(el("span", "ours-badge-batt-tip"));
      return b;
    });
    addRow(grid, "expiry-countdown", (st) => el("span", cls("ours-badge ours-badge--expiry", st), "23h left"));
  }

  /* Shared composite builders */
  function bubble(kind, text, time, st) {
    const b = el("div", cls(`ours-bubble ours-bubble--${kind}`, st));
    b.appendChild(el("div", "ours-bubble-text", text));
    b.appendChild(el("div", "ours-bubble-time", time));
    return b;
  }

  function photoCard(st) {
    const c = el("div", cls("ours-photo-card", st));
    c.appendChild(el("div", "ours-photo-art"));
    c.appendChild(el("span", "ours-chip ours-chip--media-dark", "23h left"));
    c.appendChild(el("span", "ours-chip ours-chip--media-light", "Save"));
    c.appendChild(el("div", "ours-photo-cap", "Iced oat latte at Kemang"));
    c.appendChild(el("div", "ours-photo-time", "14:19 · Tap to open"));
    return c;
  }

  function coupleCard(st) {
    const c = el("div", cls("ours-card ours-card--couple", st));
    c.appendChild(el("div", "ours-avatar ours-avatar--header", "M"));
    const copy = el("div", "ours-card-copy");
    copy.appendChild(el("div", "ours-card-title", "Dewantoro & Marchanda"));
    copy.appendChild(el("div", "ours-card-sub", "Paired · 214 days"));
    c.appendChild(copy);
    c.appendChild(el("span", "ours-chip ours-chip--live", "Live"));
    return c;
  }

  function navBar(active, st, withAlert) {
    const nav = el("div", cls("ours-bottom-nav", st));
    nav.setAttribute("role", "tablist");
    [
      ["Map", "/assets/icons/nav-map.svg", 14, 14],
      ["Chat", "/assets/icons/nav-chat.svg", 15, 12],
      ["Memories", "/assets/icons/nav-memories.svg", 13, 13],
      ["Settings", "/assets/icons/nav-settings.svg", 13, 13]
    ].forEach(([name, src, w, h]) => {
      const tab = el("button", `ours-nav-tab${name === active ? " is-active" : ""}`);
      tab.type = "button";
      tab.setAttribute("role", "tab");
      tab.setAttribute("aria-selected", String(name === active));
      tab.appendChild(img(src, w, h, ""));
      tab.appendChild(el("span", "", name));
      if (withAlert && name === "Chat") tab.appendChild(el("span", "ours-nav-alert"));
      nav.appendChild(tab);
    });
    return nav;
  }

  function topBarChat(st) {
    const bar = el("div", cls("ours-top-bar", st));
    const back = el("button", "ours-btn ours-btn--circle", "<");
    back.type = "button";
    back.setAttribute("aria-label", "Back");
    bar.appendChild(back);
    const av = el("div", "ours-avatar ours-avatar--header", "M");
    av.appendChild(el("span", "ours-avatar-online"));
    bar.appendChild(av);
    const copy = el("div", "ours-top-bar-copy");
    copy.appendChild(el("div", "ours-top-bar-title", "Marchanda"));
    copy.appendChild(el("div", "ours-top-bar-sub", "Live · Kemang Raya"));
    bar.appendChild(copy);
    const act = el("button", "ours-btn ours-btn--circle", "+");
    act.type = "button";
    act.setAttribute("aria-label", "Action");
    bar.appendChild(act);
    return bar;
  }

  function topBarMap(st) {
    const bar = el("div", cls("ours-top-bar ours-top-bar--map", st));
    const fabS = el("button", "ours-btn ours-btn--fab");
    fabS.type = "button";
    fabS.appendChild(img("/assets/icons/fab-settings.svg", 42, 42, "Settings"));
    const sw = el("div", "ours-center-switcher");
    sw.appendChild(el("span", "is-active", "Us"));
    sw.appendChild(el("span", "", "Marchanda"));
    const fabA = el("button", "ours-btn ours-btn--fab");
    fabA.type = "button";
    fabA.appendChild(img("/assets/icons/fab-alerts.svg", 42, 42, "Alerts"));
    bar.appendChild(fabS);
    bar.appendChild(sw);
    bar.appendChild(fabA);
    return bar;
  }

  function topBarOnboarding(st) {
    const bar = el("div", cls("ours-top-bar ours-top-bar--onboarding", st));
    const back = el("button", "ours-btn ours-btn--stone", "Back");
    back.type = "button";
    bar.appendChild(back);
    bar.appendChild(el("div", "ours-top-bar-title", "Join with code"));
    return bar;
  }

  function sheet(kind, st) {
    const s = el("div", cls("ours-sheet", st));
    s.appendChild(el("div", "ours-sheet-handle"));
    if (kind === "peek") {
      s.appendChild(el("h3", "ours-sheet-title", "Nearby"));
      const row = el("div", "ours-list-row ours-list-row--peek-d");
      row.appendChild(el("div", "ours-avatar ours-avatar--header", "M"));
      const copy = el("div", "ours-row-copy");
      copy.appendChild(el("div", "ours-row-title", "Marchanda"));
      copy.appendChild(el("div", "ours-row-sub", "Kemang Raya · 2m ago"));
      row.appendChild(copy);
      s.appendChild(row);
      return s;
    }
    s.appendChild(el("h3", "ours-sheet-title", kind === "focus" ? "Marchanda is live" : kind === "bubble" ? "Privacy bubble" : "Check in"));
    const inner = el("div", "ours-sheet-inner");
    if (kind === "checkin") inner.appendChild(el("div", "ours-sheet-photo"));
    else inner.appendChild(el("div", "ours-row-sub", kind === "focus" ? "24 km/h · 84% · At Kemang" : "Hide location within 200 m"));
    s.appendChild(inner);
    const actions = el("div", "ours-sheet-actions");
    actions.appendChild(el("button", "ours-btn ours-btn--primary", kind === "checkin" ? "Send" : "Nudge"));
    actions.appendChild(el("button", "ours-btn ours-btn--stone", "Close"));
    s.appendChild(actions);
    return s;
  }

  function toast(kind, st) {
    const t = el("div", cls(`ours-toast ours-toast--${kind}`, st));
    t.setAttribute("role", "status");
    if (kind === "arrival") {
      t.appendChild(el("div", "ours-avatar ours-avatar--island", "M"));
      t.appendChild(el("div", "ours-toast-title", "Marchanda arrived at Home"));
      t.appendChild(el("div", "ours-toast-sub", "Home · Just now"));
    } else if (kind === "nudge") {
      const orb = el("div", "ours-avatar ours-avatar--nudge");
      orb.appendChild(el("span", "ours-avatar-core"));
      t.appendChild(orb);
      t.appendChild(el("div", "ours-toast-title", "Nudge from Marchanda"));
      t.appendChild(el("div", "ours-toast-sub", "Thinking of you · 14:24"));
    } else if (kind === "info") {
      t.appendChild(el("div", "ours-avatar ours-avatar--header", "i"));
      t.appendChild(el("div", "ours-toast-title", "Check-in saved"));
      t.appendChild(el("div", "ours-toast-sub", "Added to Memories"));
    } else {
      t.appendChild(el("div", "ours-avatar ours-avatar--header", "!"));
      t.appendChild(el("div", "ours-toast-title", "Could not send"));
      t.appendChild(el("div", "ours-toast-sub", "Check your connection and retry"));
    }
    t.appendChild(el("span", "ours-toast-dot"));
    return t;
  }

  function listRow(kind, st) {
    const r = el("div", cls(`ours-list-row ours-list-row--${kind}`, st));
    r.setAttribute("role", "button");
    r.tabIndex = 0;
    if (kind === "peek-m" || kind === "peek-d") {
      const av = el("div", "ours-avatar ours-avatar--header", kind === "peek-m" ? "M" : "D");
      av.appendChild(el("span", "ours-avatar-online"));
      r.appendChild(av);
      const copy = el("div", "ours-row-copy");
      copy.appendChild(el("div", "ours-row-title", kind === "peek-m" ? "Marchanda" : "Dewantoro"));
      copy.appendChild(el("div", "ours-row-sub", "Kemang Raya · 2m ago"));
      if (kind === "peek-m") copy.appendChild(el("span", "ours-badge ours-badge--speed", "24 km/h"));
      r.appendChild(copy);
      r.appendChild(el("span", "ours-row-meta", "84%"));
    } else if (kind === "layer") {
      const copy = el("div", "ours-row-copy");
      copy.appendChild(el("div", "ours-row-title", "24h trail"));
      r.appendChild(copy);
      const sw = el("span", "ours-switch is-on");
      sw.setAttribute("role", "switch");
      sw.setAttribute("aria-checked", "true");
      r.appendChild(sw);
    } else if (kind === "waiting") {
      r.appendChild(el("span", "ours-pulse-dot"));
      r.appendChild(el("div", "ours-row-sub", "Waiting for Marchanda to join"));
    } else {
      const copy = el("div", "ours-row-copy");
      copy.appendChild(el("div", "ours-row-title", "Language"));
      r.appendChild(copy);
      r.appendChild(el("span", "ours-row-value", "English"));
      r.appendChild(el("span", "ours-row-chevron"));
    }
    return r;
  }

  /* 6-11 renderers */
  function renderCards() {
    const grid = getGrid("grid-cards");
    if (!grid) return;
    addStackRow(grid, "ivory-card", (st) => {
      const c = el("div", cls("ours-card", st));
      c.style.width = "358px";
      c.appendChild(el("div", "ours-card-title", "Saved places"));
      c.appendChild(el("div", "ours-card-sub", "Home, Studio, Kemang Raya"));
      return c;
    });
    addStackRow(grid, "glass-card", (st) => {
      const c = el("div", cls("ours-card ours-card--glass", st));
      c.style.width = "358px";
      c.appendChild(el("div", "ours-card-title", "Quiet pause"));
      c.appendChild(el("div", "ours-card-sub", "Marchanda sees you are on a break"));
      return c;
    }, true);
    addStackRow(grid, "bubble-partner", (st) => bubble("partner", "Just left the studio!", "14:12", st), true);
    addStackRow(grid, "bubble-me", (st) => bubble("me", "Grab iced latte on the way?", "14:14 ✓✓", st), true);
    addStackRow(grid, "bubble-photo-24h", photoCard, true);
    addStackRow(grid, "couple-header-card", coupleCard);
  }

  function renderNav() {
    const grid = getGrid("grid-nav");
    if (!grid) return;
    addStackRow(grid, "map-active", (st) => navBar("Map", st, false), true);
    addStackRow(grid, "chat-active-alert", (st) => navBar("Chat", st, true), true);
  }

  function renderTop() {
    const grid = getGrid("grid-top");
    if (!grid) return;
    addStackRow(grid, "frosted-chat-header", topBarChat, true);
    addStackRow(grid, "map-top-controls", topBarMap, true);
    addStackRow(grid, "onboarding-top-bar", topBarOnboarding);
  }

  function renderSheets() {
    const grid = getGrid("grid-sheets");
    if (!grid) return;
    ["peek", "focus", "bubble", "checkin"].forEach((k) => {
      addStackRow(grid, `${k}-sheet`, (st) => sheet(k, st), true);
    });
  }

  function renderToasts() {
    const grid = getGrid("grid-toasts");
    if (!grid) return;
    ["arrival", "nudge", "info", "error"].forEach((k) => {
      addStackRow(grid, `${k}-toast`, (st) => toast(k, st), true);
    });
  }

  function renderRows() {
    const grid = getGrid("grid-rows");
    if (!grid) return;
    ["peek-m", "peek-d", "layer", "waiting", "settings"].forEach((k) => {
      addStackRow(grid, `${k}-row`, (st) => listRow(k, st));
    });
  }

  /* Components_Sheet 1:1 reference panel (#24540) */
  function place(parent, node, x, y, w, h) {
    node.classList.add("cs-abs");
    node.style.left = `${x}px`;
    node.style.top = `${y}px`;
    if (w) node.style.width = `${w}px`;
    if (h) node.style.height = `${h}px`;
    parent.appendChild(node);
    return node;
  }

  function renderSheetPanel() {
    const panel = document.getElementById("panel-components-sheet");
    if (!panel) return;
    panel.replaceChildren();

    place(panel, el("h3", "cs-title", "Liquid Glass Web App Components"), 28, 28, 480, 24);
    place(panel, el("div", "cs-label", "ELECTRIC BLUE CTAS & FROSTED GLASS CHIPS"), 28, 72);

    const bp = el("button", "ours-btn ours-btn--primary", "Check in");
    bp.type = "button";
    place(panel, bp, 28, 98, 154, 44);
    const bf = el("button", "ours-btn ours-btn--frosted", "Nudge");
    bf.type = "button";
    place(panel, bf, 194, 98, 140, 44);
    const cl = el("span", "ours-chip ours-chip--live");
    cl.appendChild(el("span", "ours-chip-dot"));
    cl.appendChild(document.createTextNode("Live"));
    place(panel, cl, 346, 104, 74, 32);
    place(panel, el("span", "ours-chip ours-chip--paused", "Paused"), 428, 104, 84, 32);

    place(panel, el("div", "cs-label", "CUSTOM MAP PINS (ELECTRIC BLUE HALO)"), 28, 166);
    const stage = place(panel, el("div", "cs-pin-stage"), 28, 192, 484, 160);

    const pin = place(stage, el("div"), 28, 20, 128, 128);
    place(pin, el("div", "cs-cone"), 26, -20, 58, 56);
    place(pin, el("div", "cs-pulse-outer"), 28, 0, 72, 72);
    place(pin, el("div", "cs-pulse-inner"), 36, 8, 56, 56);
    place(pin, el("div", "cs-tail"), 58, 62, 12, 9);
    place(pin, el("div", "ours-avatar ours-avatar--map-partner", "M"), 41, 13);
    const pb = el("span", "ours-badge ours-badge--battery");
    pb.appendChild(el("span", "ours-badge-dot"));
    pb.appendChild(document.createTextNode("84%"));
    place(pin, pb, 40, 48);
    const sp = el("span", "ours-badge ours-badge--speed");
    sp.appendChild(el("span", "ours-badge-dot"));
    sp.appendChild(document.createTextNode("24 km/h"));
    place(pin, sp, 10, 75);
    place(pin, el("span", "ours-badge ours-badge--dwell", "Here for 19m"), 12, 104);

    const home = place(stage, el("div"), 184, 14, 130, 130);
    place(home, el("div", "cs-geofence"), 0, 0, 130, 130);
    const hp = place(home, el("div", "cs-home-pill"), 24, 48, 82, 32);
    const hd = el("span", "cs-home-dot");
    hd.appendChild(el("span", "cs-home-core"));
    hp.appendChild(hd);
    hp.appendChild(document.createTextNode("Home"));

    const hl = place(stage, el("div"), 364, 44, 56, 64);
    place(hl, el("div", "cs-hl-frame"), 0, 0, 56, 56);
    place(hl, el("div", "cs-hl-photo"), 4, 4, 48, 48);
    place(hl, el("div", "cs-hl-stem"), 25, 55, 6, 6);

    place(panel, el("div", "cs-label", "LIQUID GLASS CHAT BUBBLES (TEXT + 24H PHOTO)"), 28, 376);
    place(panel, bubble("partner", "Just left the studio!", "14:12", ""), 28, 404);
    place(panel, bubble("me", "Grab iced latte on the way?", "14:14 ✓✓", ""), 222, 476);
    place(panel, photoCard(""), 28, 550);

    place(panel, el("div", "cs-label", "REFRACTIVE CAPSULE DOCK OVER MAP"), 28, 786);
    const wrap = place(panel, el("div", "cs-nav-wrap"), 28, 814, 484, 108);
    place(wrap, el("div", "cs-nav-orb"), 160, -20, 160, 160);
    place(wrap, navBar("Map", "", false), 98, 23);
  }

  function setTheme(theme) {
    if (window.OursAtoms) window.OursAtoms.setTheme(theme);
    else document.documentElement.setAttribute("data-theme", theme);
    const lightBtn = document.getElementById("cp-theme-light");
    const darkBtn = document.getElementById("cp-theme-dark");
    if (lightBtn) lightBtn.setAttribute("aria-pressed", String(theme === "light"));
    if (darkBtn) darkBtn.setAttribute("aria-pressed", String(theme === "dark"));
  }

  document.addEventListener("DOMContentLoaded", () => {
    const lightBtn = document.getElementById("cp-theme-light");
    const darkBtn = document.getElementById("cp-theme-dark");
    if (lightBtn) lightBtn.addEventListener("click", () => setTheme("light"));
    if (darkBtn) darkBtn.addEventListener("click", () => setTheme("dark"));
    renderButtons();
    renderInputs();
    renderChips();
    renderAvatars();
    renderBadges();
    renderCards();
    renderNav();
    renderTop();
    renderSheets();
    renderToasts();
    renderRows();
    renderSheetPanel();
    setTheme("light");
  });
})();
