"use strict";

(function () {
  const SWATCHES = [
    { name: "Broken White", varName: "--bg-canvas", light: "#FAF9F5", dark: "#090B10" },
    { name: "Warm Ivory", varName: "--bg-surface", light: "#FFFFFC", dark: "#131722" },
    { name: "Soft Stone", varName: "--bg-surface-muted", light: "#F0EFE9", dark: "#1E2433" },
    { name: "Midnight Ink", varName: "--ink", light: "#111318", dark: "#FAF9F5" },
    { name: "Ink Secondary", varName: "--ink-secondary", light: "#373B44", dark: "#C5CAD6" },
    { name: "Ink Muted", varName: "--ink-muted", light: "#555B68", dark: "#8E96A8" },
    { name: "Electric Blue", varName: "--action", light: "#0055FF", dark: "#4D8BFF" },
    { name: "Pressed Blue", varName: "--action-pressed", light: "#0044D6", dark: "#3C7EFF" },
    { name: "Electric Tint", varName: "--action-soft", light: "#EBF1FF", dark: "#132240" },
    { name: "Live Green", varName: "--success", light: "#146C43", dark: "#3CD084" },
    { name: "SOS Red", varName: "--danger", light: "#C9182B", dark: "#FF6B78" },
    { name: "Hairline Line", varName: "--line", light: "#E2E0D8", dark: "#283044" }
  ];

  const SPACING = [
    { name: "xxs", px: 4 },
    { name: "xs", px: 8 },
    { name: "sm", px: 12 },
    { name: "md", px: 16 },
    { name: "lg", px: 24 },
    { name: "xl", px: 32 }
  ];

  function renderSwatches(theme) {
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
      hex.textContent = `${theme === "dark" ? s.dark : s.light} (${s.varName})`;

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
    SPACING.forEach((sp) => {
      const row = document.createElement("div");
      row.className = "tp-spacing-row";

      const label = document.createElement("span");
      label.className = "tp-spacing-label";
      label.textContent = `${sp.name} ${sp.px}px`;

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
    renderSwatches(theme);
  }

  document.addEventListener("DOMContentLoaded", () => {
    const lightBtn = document.getElementById("theme-light");
    const darkBtn = document.getElementById("theme-dark");
    if (lightBtn) lightBtn.addEventListener("click", () => setTheme("light"));
    if (darkBtn) darkBtn.addEventListener("click", () => setTheme("dark"));
    renderSpacing();
    setTheme("light");
  });
})();
