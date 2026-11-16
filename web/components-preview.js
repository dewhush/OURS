"use strict";

(function () {
  const STATES = ["default", "pressed", "disabled", "focus"];

  function stateClass(st) {
    if (st === "pressed") return "is-pressed";
    if (st === "disabled") return "is-disabled";
    if (st === "focus") return "is-focused";
    return "";
  }

  function addHeader(grid) {
    const head = document.createElement("div");
    head.className = "cp-state-head";
    const blank = document.createElement("span");
    blank.textContent = "Variant";
    head.appendChild(blank);
    STATES.forEach((st) => {
      const s = document.createElement("span");
      s.textContent = st;
      head.appendChild(s);
    });
    grid.appendChild(head);
  }

  function addRow(grid, label, builder) {
    const row = document.createElement("div");
    row.className = "cp-row";
    const lbl = document.createElement("span");
    lbl.className = "cp-row-label";
    lbl.textContent = label;
    row.appendChild(lbl);
    STATES.forEach((st) => {
      const cell = document.createElement("div");
      cell.className = "cp-cell";
      cell.appendChild(builder(st));
      row.appendChild(cell);
    });
    grid.appendChild(row);
  }

  function renderButtons() {
    const grid = document.getElementById("grid-buttons");
    if (!grid) return;
    grid.replaceChildren();
    addHeader(grid);

    const variants = [
      { label: "primary", cls: "ours-btn--primary", text: "Check in" },
      { label: "frosted", cls: "ours-btn--frosted", text: "Nudge" },
      { label: "stone", cls: "ours-btn--stone", text: "Cancel" },
      { label: "danger", cls: "ours-btn--danger", text: "Hold SOS" },
      { label: "icon-fab", cls: "ours-btn--fab", icon: "/assets/icons/fab-camera.svg" },
      { label: "icon-circle", cls: "ours-btn--circle", text: "+" }
    ];

    variants.forEach((v) => {
      addRow(grid, v.label, (st) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = `ours-btn ${v.cls} ${stateClass(st)}`.trim();
        if (st === "disabled") btn.disabled = true;
        if (v.icon) {
          const img = document.createElement("img");
          img.src = v.icon;
          img.width = 42;
          img.height = 42;
          img.alt = v.label;
          btn.appendChild(img);
        } else {
          btn.textContent = v.text;
        }
        return btn;
      });
    });
  }

  function renderInputs() {
    const grid = document.getElementById("grid-inputs");
    if (!grid) return;
    grid.replaceChildren();
    addHeader(grid);

    const variants = [
      { label: "code-field", cls: "ours-input-wrap--code", val: "K 7 M · 4 P 9", ph: "000 · 000" },
      { label: "glass-input", cls: "ours-input-wrap--glass", val: "", ph: "Add a caption..." },
      { label: "standard-field", cls: "ours-input-wrap--standard", val: "Dewantoro", ph: "Your name" },
      { label: "search-pill", cls: "ours-input-wrap--search", val: "", ph: "Search chat..." }
    ];

    variants.forEach((v) => {
      addRow(grid, v.label, (st) => {
        const wrap = document.createElement("div");
        wrap.className = `ours-input-wrap ${v.cls} ${stateClass(st)}`.trim();
        const input = document.createElement("input");
        input.type = "text";
        input.className = "ours-input";
        input.placeholder = v.ph;
        input.value = v.val;
        if (st === "disabled") input.disabled = true;
        wrap.appendChild(input);
        return wrap;
      });
    });
  }

  function renderChips() {
    const grid = document.getElementById("grid-chips");
    if (!grid) return;
    grid.replaceChildren();
    addHeader(grid);

    addRow(grid, "status-live", (st) => {
      const el = document.createElement("span");
      el.className = `ours-chip ours-chip--live ${stateClass(st)}`.trim();
      const dot = document.createElement("span");
      dot.className = "ours-chip-dot";
      el.appendChild(dot);
      el.appendChild(document.createTextNode("Live"));
      return el;
    });

    addRow(grid, "status-paused", (st) => {
      const el = document.createElement("span");
      el.className = `ours-chip ours-chip--paused ${stateClass(st)}`.trim();
      el.textContent = "Paused";
      return el;
    });

    addRow(grid, "peek-tab", (st) => {
      const el = document.createElement("span");
      el.className = `ours-chip ours-chip--peek-active ${stateClass(st)}`.trim();
      el.textContent = "All";
      return el;
    });

    addRow(grid, "segment-switch", (st) => {
      const el = document.createElement("div");
      el.className = `ours-chip ours-chip--segment ${stateClass(st)}`.trim();
      const t1 = document.createElement("span");
      t1.className = "ours-chip-thumb is-active";
      t1.textContent = "EN";
      const t2 = document.createElement("span");
      t2.className = "ours-chip-thumb";
      t2.textContent = "ID";
      el.appendChild(t1);
      el.appendChild(t2);
      return el;
    });

    addRow(grid, "media-pill", (st) => {
      const el = document.createElement("span");
      el.className = `ours-chip ours-chip--media-dark ${stateClass(st)}`.trim();
      el.textContent = "23h left";
      return el;
    });
  }

  function renderAvatars() {
    const grid = document.getElementById("grid-avatars");
    if (!grid) return;
    grid.replaceChildren();
    addHeader(grid);

    const variants = [
      { label: "map-partner", cls: "ours-avatar--map-partner", text: "M" },
      { label: "map-self", cls: "ours-avatar--map-self", text: "D" },
      { label: "header-with-dot", cls: "ours-avatar--header", text: "M", dot: true },
      { label: "island-partner", cls: "ours-avatar--island", text: "M" },
      { label: "nudge-orb", cls: "ours-avatar--nudge", core: true }
    ];

    variants.forEach((v) => {
      addRow(grid, v.label, (st) => {
        const av = document.createElement("div");
        av.className = `ours-avatar ${v.cls} ${stateClass(st)}`.trim();
        if (v.core) {
          const core = document.createElement("span");
          core.className = "ours-avatar-core";
          av.appendChild(core);
        } else {
          av.textContent = v.text;
        }
        if (v.dot) {
          const dot = document.createElement("span");
          dot.className = "ours-avatar-online";
          av.appendChild(dot);
        }
        return av;
      });
    });
  }

  function renderBadges() {
    const grid = document.getElementById("grid-badges");
    if (!grid) return;
    grid.replaceChildren();
    addHeader(grid);

    addRow(grid, "battery-chip", (st) => {
      const el = document.createElement("span");
      el.className = `ours-badge ours-badge--battery ${stateClass(st)}`.trim();
      el.textContent = "84%";
      return el;
    });

    addRow(grid, "speed-battery-pill", (st) => {
      const el = document.createElement("span");
      el.className = `ours-badge ours-badge--speed ${stateClass(st)}`.trim();
      el.textContent = "24 km/h · 84%";
      return el;
    });

    addRow(grid, "dwell-pill", (st) => {
      const el = document.createElement("span");
      el.className = `ours-badge ours-badge--dwell ${stateClass(st)}`.trim();
      el.textContent = "At Kemang · 2h";
      return el;
    });

    addRow(grid, "statusbar-battery", (st) => {
      const el = document.createElement("div");
      el.className = `ours-badge ours-badge--statusbar ${stateClass(st)}`.trim();
      const fill = document.createElement("span");
      fill.className = "ours-badge-batt-fill";
      const tip = document.createElement("span");
      tip.className = "ours-badge-batt-tip";
      el.appendChild(fill);
      el.appendChild(tip);
      return el;
    });

    addRow(grid, "expiry-countdown", (st) => {
      const el = document.createElement("span");
      el.className = `ours-badge ours-badge--expiry ${stateClass(st)}`.trim();
      el.textContent = "23h left";
      return el;
    });
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
    setTheme("light");
  });
})();
