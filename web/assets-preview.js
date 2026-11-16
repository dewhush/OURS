"use strict";

(function () {
  const ASSET_CARDS = [
    {
      name: "NudgeWave",
      desc: "Map pulse + travel arc",
      src: "/assets/icons/nudge-wave.svg"
    },
    {
      name: "CompassRose",
      desc: "Always-on bearing cone",
      src: "/assets/icons/compass-rose.svg"
    },
    {
      name: "SafetyShield",
      desc: "Security #S1-#S8 + SOS",
      src: "/assets/icons/safety-shield.svg"
    },
    {
      name: "CheckInFrame",
      desc: "24h camera viewfinder",
      src: "/assets/icons/check-in-frame.svg"
    },
    {
      name: "EmptyMemoriesArt",
      desc: "Memories empty state",
      src: "/assets/icons/empty-memories-art.svg"
    },
    {
      name: "PairInviteArt",
      desc: "Google + 6-char pair",
      src: "/assets/icons/pair-invite-art.svg"
    },
    {
      name: "EmptyChatArt",
      desc: "Messenger empty thread",
      src: "/assets/icons/empty-chat-art.svg"
    }
  ];

  const TRAY_ICONS = [
    { name: "fab-settings.svg", src: "/assets/icons/fab-settings.svg", w: 42, h: 42 },
    { name: "fab-alerts.svg", src: "/assets/icons/fab-alerts.svg", w: 42, h: 42 },
    { name: "fab-camera.svg", src: "/assets/icons/fab-camera.svg", w: 42, h: 42 },
    { name: "fab-layers.svg", src: "/assets/icons/fab-layers.svg", w: 42, h: 42 },
    { name: "fab-recenter.svg", src: "/assets/icons/fab-recenter.svg", w: 42, h: 42 },
    { name: "fab-bubble.svg", src: "/assets/icons/fab-bubble.svg", w: 42, h: 42 },
    { name: "nav-map.svg", src: "/assets/icons/nav-map.svg", w: 14, h: 14 },
    { name: "nav-chat.svg", src: "/assets/icons/nav-chat.svg", w: 15, h: 12 },
    { name: "nav-memories.svg", src: "/assets/icons/nav-memories.svg", w: 13, h: 13 },
    { name: "nav-settings.svg", src: "/assets/icons/nav-settings.svg", w: 13, h: 13 }
  ];

  function renderAssets() {
    const grid = document.getElementById("ap-asset-grid");
    if (!grid) return;
    grid.replaceChildren();
    ASSET_CARDS.forEach((item, idx) => {
      const card = document.createElement("div");
      card.className = "ap-asset-card";
      card.dataset.idx = String(idx);

      const stage = document.createElement("div");
      stage.className = "ap-asset-stage";

      const img = document.createElement("img");
      img.src = item.src;
      img.width = 200;
      img.height = 104;
      img.alt = item.name;
      stage.appendChild(img);

      const title = document.createElement("strong");
      title.textContent = item.name;

      const desc = document.createElement("span");
      desc.textContent = item.desc;

      card.appendChild(stage);
      card.appendChild(title);
      card.appendChild(desc);
      grid.appendChild(card);
    });
  }

  function renderTray() {
    const tray = document.getElementById("ap-icons-tray");
    if (!tray) return;
    tray.replaceChildren();
    TRAY_ICONS.forEach((ic) => {
      const cell = document.createElement("div");
      cell.className = "ap-tray-item";

      const img = document.createElement("img");
      img.src = ic.src;
      img.width = ic.w;
      img.height = ic.h;
      img.alt = ic.name;

      const label = document.createElement("span");
      label.textContent = `${ic.name} (${ic.w}x${ic.h})`;

      cell.appendChild(img);
      cell.appendChild(label);
      tray.appendChild(cell);
    });
  }

  function setTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    const lightBtn = document.getElementById("ap-theme-light");
    const darkBtn = document.getElementById("ap-theme-dark");
    if (lightBtn) lightBtn.setAttribute("aria-pressed", String(theme === "light"));
    if (darkBtn) darkBtn.setAttribute("aria-pressed", String(theme === "dark"));
  }

  document.addEventListener("DOMContentLoaded", () => {
    const lightBtn = document.getElementById("ap-theme-light");
    const darkBtn = document.getElementById("ap-theme-dark");
    if (lightBtn) lightBtn.addEventListener("click", () => setTheme("light"));
    if (darkBtn) darkBtn.addEventListener("click", () => setTheme("dark"));
    renderAssets();
    renderTray();
    setTheme("light");
  });
})();
