"use strict";

(function () {
  const SWATCHES = [
    { name: "Broken White", varName: "--swatch-broken-white", hex: "#FAF9F5" },
    { name: "Midnight Ink", varName: "--swatch-midnight-ink", hex: "#111318" },
    { name: "Warm Ivory", varName: "--swatch-warm-ivory", hex: "#FFFFFC" },
    { name: "Soft Stone", varName: "--swatch-soft-stone", hex: "#F0EFE9" },
    { name: "Electric Blue", varName: "--swatch-electric-blue", hex: "#0055FF" },
    { name: "Pressed Blue", varName: "--swatch-pressed-blue", hex: "#0044D6" },
    { name: "Luminous Blue", varName: "--swatch-luminous-blue", hex: "#4D8BFF" },
    { name: "Electric Tint", varName: "--swatch-electric-tint", hex: "#EBF1FF" },
    { name: "Deep Obsidian", varName: "--swatch-deep-obsidian", hex: "#090B10" },
    { name: "Elevated Dark", varName: "--swatch-elevated-dark", hex: "#131722" },
    { name: "Live Green", varName: "--swatch-live-green", hex: "#146C43" },
    { name: "SOS Red", varName: "--swatch-sos-red", hex: "#C9182B" }
  ];

  const SPACING = [
    { name: "xxs 4", px: 4 },
    { name: "xs 8", px: 8 },
    { name: "sm 12", px: 12 },
    { name: "md 16", px: 16 },
    { name: "lg 24", px: 24 },
    { name: "xl 32", px: 32 }
  ];

  function renderSwatches() {
    const container = document.getElementById("tp-swatches");
    if (!container) return;
    container.replaceChildren();
    SWATCHES.forEach((s) => {
      const card = document.createElement("div");
      card.className = "tp-swatch";

      const chip = document.createElement("div");
      chip.className = "tp-swatch-chip";
      chip.style.background = `var(${s.varName})`;

      const info = document.createElement("div");
      info.className = "tp-swatch-info";

      const title = document.createElement("strong");
      title.textContent = s.name;

      const hex = document.createElement("span");
      hex.textContent = s.hex;

      info.appendChild(title);
      info.appendChild(hex);
      card.appendChild(chip);
      card.appendChild(info);
      container.appendChild(card);
    });
  }

  function renderSpacing() {
    const container = document.getElementById("tp-spacing");
    if (!container) return;
    container.replaceChildren();
    SPACING.forEach((sp, idx) => {
      const row = document.createElement("div");
      row.className = "tp-spacing-row";
      row.dataset.idx = String(idx);

      const label = document.createElement("span");
      label.className = "tp-spacing-label";
      label.textContent = sp.name;

      const bar = document.createElement("div");
      bar.className = "tp-spacing-bar";
      bar.style.width = `${sp.px * 5}px`;

      row.appendChild(label);
      row.appendChild(bar);
      container.appendChild(row);
    });
  }

  function setTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    const lightBtn = document.getElementById("theme-light");
    const darkBtn = document.getElementById("theme-dark");
    if (lightBtn) lightBtn.setAttribute("aria-pressed", String(theme === "light"));
    if (darkBtn) darkBtn.setAttribute("aria-pressed", String(theme === "dark"));
  }

  document.addEventListener("DOMContentLoaded", () => {
    const lightBtn = document.getElementById("theme-light");
    const darkBtn = document.getElementById("theme-dark");
    if (lightBtn) lightBtn.addEventListener("click", () => setTheme("light"));
    if (darkBtn) darkBtn.addEventListener("click", () => setTheme("dark"));
    renderSwatches();
    renderSpacing();
    setTheme("light");
  });
})();
