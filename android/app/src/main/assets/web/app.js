(() => {
  "use strict";

  const STRINGS = {
    en: {
      tabMap: "Map",
      tabMemories: "Memories",
      tabChat: "Chat",
      tabSettings: "Settings",
      checkIn: "Check in",
      nudge: "Nudge",
      center: "Center",
      places: "Places",
      nudgeSent: "Nudge sent",
      sharingPaused: "Sharing paused",
      partnerPaused: "Partner paused sharing",
      noFixYet: "Waiting for location",
      timeline: "Timeline",
      map: "Map",
      all: "All",
      photos: "Photos",
      save: "Save",
      saved: "Saved",
      reply: "Reply",
      del: "Delete",
      searchPlaceholder: "Search notes or places",
      noMemories: "No highlights yet. Save a check-in to keep it here.",
      noResults: "No matching items",
      clearFilters: "Clear filters",
      typeMessage: "Message",
      send: "Send",
      settings: "Settings",
      appearance: "Appearance",
      themeSystem: "System",
      themeLight: "Light",
      themeDark: "Dark",
      language: "Language",
      locationPrivacy: "Location & Privacy",
      pauseSharing: "Pause location sharing",
      savedPlaces: "Saved places",
      bgReliability: "Background reliability",
      safetySos: "Safety & SOS",
      crashDetection: "Crash detection",
      emergencyWhatsapp: "Emergency WhatsApp contact",
      triggerSos: "Trigger SOS",
      serverSection: "Server",
      serverUrl: "Server URL",
      testConnection: "Test connection",
      reloadUi: "Reload latest web UI",
      signOut: "Sign out & unpair",
      welcomeTitle: "Private space for two",
      welcomeSub: "Live location, 24-hour photo check-ins, and shared memories on your own server.",
      yourName: "Your name",
      yourEmail: "Email (optional)",
      setupKey: "Setup key (if configured)",
      createPair: "Create pair",
      joinPair: "Join with code",
      inviteCode: "Invite code",
      waitingPartner: "Share this code with your partner",
      continueApp: "Open OURS",
      cancel: "Cancel",
      confirm: "Confirm",
      close: "Close",
      noteOptional: "Note (optional)",
      placeOptional: "Place name (optional)",
      pickPhoto: "Choose photo",
      addPlace: "Add place",
      placeName: "Place name",
      radiusM: "Radius (m)",
      sosTitle: "Send SOS alert?",
      sosBody: "This immediately alerts your partner with your current coordinates.",
      connected: "Connected",
      unreachable: "Unreachable",
      today: "Today",
      yesterday: "Yesterday",
      repliesCount: "replies",
      replyingTo: "Replying to check-in",
      continueGoogle: "Continue with Google",
      googleVerified: "Google verified",
      googleSignedInAs: "Signed in as",
      googleSwitch: "Switch",
      orManual: "or continue with name",
      googleModalTitle: "Continue with Google",
      googleModalSub: "Enter your Google account email to sign in or restore your OURS session.",
      googleEmailRequired: "Valid Google email is required"
    },
    id: {
      tabMap: "Peta",
      tabMemories: "Kenangan",
      tabChat: "Obrolan",
      tabSettings: "Pengaturan",
      checkIn: "Check in",
      nudge: "Sapa",
      center: "Pusat",
      places: "Tempat",
      nudgeSent: "Sapaan terkirim",
      sharingPaused: "Berbagi dijeda",
      partnerPaused: "Pasangan menjeda berbagi",
      noFixYet: "Menunggu lokasi",
      timeline: "Linimasa",
      map: "Peta",
      all: "Semua",
      photos: "Foto",
      save: "Simpan",
      saved: "Tersimpan",
      reply: "Balas",
      del: "Hapus",
      searchPlaceholder: "Cari catatan atau tempat",
      noMemories: "Belum ada sorotan. Simpan check-in untuk menyimpannya di sini.",
      noResults: "Tidak ada hasil",
      clearFilters: "Hapus filter",
      typeMessage: "Pesan",
      send: "Kirim",
      settings: "Pengaturan",
      appearance: "Tampilan",
      themeSystem: "Sistem",
      themeLight: "Terang",
      themeDark: "Gelap",
      language: "Bahasa",
      locationPrivacy: "Lokasi & Privasi",
      pauseSharing: "Jeda berbagi lokasi",
      savedPlaces: "Tempat tersimpan",
      bgReliability: "Keandalan latar belakang",
      safetySos: "Keamanan & SOS",
      crashDetection: "Deteksi tabrakan",
      emergencyWhatsapp: "Kontak WhatsApp darurat",
      triggerSos: "Kirim SOS",
      serverSection: "Server",
      serverUrl: "URL Server",
      testConnection: "Uji koneksi",
      reloadUi: "Muat ulang UI terbaru",
      signOut: "Keluar & putuskan",
      welcomeTitle: "Ruang privat berdua",
      welcomeSub: "Lokasi langsung, check-in foto 24 jam, dan kenangan bersama di server sendiri.",
      yourName: "Nama kamu",
      yourEmail: "Email (opsional)",
      setupKey: "Setup key (jika ada)",
      createPair: "Buat pasangan",
      joinPair: "Gabung dengan kode",
      inviteCode: "Kode undangan",
      waitingPartner: "Bagikan kode ini ke pasanganmu",
      continueApp: "Buka OURS",
      cancel: "Batal",
      confirm: "Konfirmasi",
      close: "Tutup",
      noteOptional: "Catatan (opsional)",
      placeOptional: "Nama tempat (opsional)",
      pickPhoto: "Pilih foto",
      addPlace: "Tambah tempat",
      placeName: "Nama tempat",
      radiusM: "Radius (m)",
      sosTitle: "Kirim peringatan SOS?",
      sosBody: "Ini langsung mengirim peringatan ke pasangan beserta koordinatmu.",
      connected: "Terhubung",
      unreachable: "Tidak terjangkau",
      today: "Hari ini",
      yesterday: "Kemarin",
      repliesCount: "balasan",
      replyingTo: "Membalas check-in",
      continueGoogle: "Lanjutkan dengan Google",
      googleVerified: "Terverifikasi Google",
      googleSignedInAs: "Masuk sebagai",
      googleSwitch: "Ganti",
      orManual: "atau lanjutkan dengan nama",
      googleModalTitle: "Lanjutkan dengan Google",
      googleModalSub: "Masukkan email akun Google untuk masuk atau memulihkan sesi OURS.",
      googleEmailRequired: "Email Google yang valid wajib diisi"
    }
  };

  const native = typeof window.OursNative === "object" && window.OursNative !== null ? window.OursNative : null;

  const state = {
    tab: "map",
    lang: localStorage.getItem("ours_lang") || "en",
    theme: localStorage.getItem("ours_theme") || "system",
    serverUrl: localStorage.getItem("ours_server_url") || window.location.origin,
    accessToken: localStorage.getItem("ours_access_token") || "",
    refreshToken: localStorage.getItem("ours_refresh_token") || "",
    deviceId: localStorage.getItem("ours_device_id") || (crypto.randomUUID ? crypto.randomUUID() : String(Date.now())),
    sharingPaused: localStorage.getItem("ours_sharing_paused") === "true",
    crashDetectionEnabled: localStorage.getItem("ours_crash_enabled") === "true",
    emergencyContact: localStorage.getItem("ours_emergency_contact") || "",
    me: null,
    partner: null,
    partnerPaused: false,
    meLocation: null,
    partnerLocation: null,
    places: [],
    checkins: [],
    memories: [],
    messages: [],
    lastEventId: Number(localStorage.getItem("ours_last_event_id") || "0"),
    memoriesMode: "timeline",
    memoriesFilter: "all",
    memoriesSearchOpen: false,
    memoriesQuery: "",
    chatSearchOpen: false,
    chatQuery: "",
    replyToCheckInId: null,
    onboardingMode: "create",
    inviteCode: "",
    googleAuth: null,
    serverStatusText: "",
    serverStatusOk: null,
    photoUrls: new Map()
  };

  let mapInstance = null;
  let memoriesMapInstance = null;
  let meMarker = null;
  let partnerMarker = null;
  let ws = null;
  let wsPingTimer = null;
  let wsReconnectTimer = null;
  let refreshPromise = null;
  let pairPollTimer = null;

  function t(key) {
    return (STRINGS[state.lang] && STRINGS[state.lang][key]) || STRINGS.en[key] || key;
  }

  function haptic() {
    try { native?.hapticTick?.(); } catch (_) {}
  }

  function persistAndSyncNative() {
    localStorage.setItem("ours_lang", state.lang);
    localStorage.setItem("ours_theme", state.theme);
    localStorage.setItem("ours_server_url", state.serverUrl);
    localStorage.setItem("ours_access_token", state.accessToken);
    localStorage.setItem("ours_refresh_token", state.refreshToken);
    localStorage.setItem("ours_device_id", state.deviceId);
    localStorage.setItem("ours_sharing_paused", String(state.sharingPaused));
    localStorage.setItem("ours_crash_enabled", String(state.crashDetectionEnabled));
    localStorage.setItem("ours_emergency_contact", state.emergencyContact);
    localStorage.setItem("ours_last_event_id", String(state.lastEventId));

    if (native?.syncAuth) {
      try {
        native.syncAuth(JSON.stringify({
          serverUrl: state.serverUrl,
          accessToken: state.accessToken,
          refreshToken: state.refreshToken,
          deviceId: state.deviceId,
          sharingPaused: state.sharingPaused,
          crashDetectionEnabled: state.crashDetectionEnabled,
          emergencyContact: state.emergencyContact
        }));
      } catch (_) {}
    }
  }

  function applyTheme() {
    document.documentElement.setAttribute("data-theme", state.theme);
  }

  function inheritNativeState() {
    if (!native?.getNativeState) return;
    try {
      const raw = native.getNativeState();
      if (!raw) return;
      const n = JSON.parse(raw);
      const savedUrl = localStorage.getItem("ours_server_url") || "";
      if (n.serverUrl && (!savedUrl || savedUrl.includes(":8080") || savedUrl.includes("127.0.0.1") || savedUrl.startsWith("file:"))) {
        state.serverUrl = n.serverUrl;
        localStorage.setItem("ours_server_url", n.serverUrl);
      }
      if (n.accessToken && !state.accessToken) state.accessToken = n.accessToken;
      if (n.refreshToken && !state.refreshToken) state.refreshToken = n.refreshToken;
      if (n.deviceId) state.deviceId = n.deviceId;
      if (typeof n.sharingPaused === "boolean") state.sharingPaused = n.sharingPaused;
      if (typeof n.crashDetectionEnabled === "boolean") state.crashDetectionEnabled = n.crashDetectionEnabled;
      if (n.emergencyContact && !state.emergencyContact) state.emergencyContact = n.emergencyContact;
      if (typeof n.lat === "number" && typeof n.lon === "number" && !state.meLocation) {
        state.meLocation = {
          lat: n.lat,
          lon: n.lon,
          speed_mps: 0,
          battery_pct: typeof n.batteryPct === "number" ? n.batteryPct : 100,
          charging: Boolean(n.charging),
          recorded_at: Date.now()
        };
      }
    } catch (_) {}
  }

  function showToast(msg) {
    const root = document.getElementById("toast-container");
    if (!root) return;
    const el = document.createElement("div");
    el.className = "toast";
    el.textContent = msg;
    root.appendChild(el);
    setTimeout(() => {
      el.style.opacity = "0";
      setTimeout(() => el.remove(), 220);
    }, 2400);
  }

  function apiBase() {
    return (state.serverUrl || window.location.origin).trim().replace(/\/+$/, "");
  }

  async function refreshTokens() {
    if (refreshPromise) return refreshPromise;
    refreshPromise = (async () => {
      if (!state.refreshToken) throw new Error("No refresh token");
      const resp = await fetch(`${apiBase()}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refresh_token: state.refreshToken })
      });
      if (!resp.ok) {
        if (resp.status === 401) {
          state.accessToken = "";
          state.refreshToken = "";
          state.me = null;
          persistAndSyncNative();
          render();
        }
        throw new Error("Refresh failed");
      }
      const tokens = await resp.json();
      state.accessToken = tokens.access_token;
      state.refreshToken = tokens.refresh_token;
      persistAndSyncNative();
      return state.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
    return refreshPromise;
  }

  async function apiFetch(path, options = {}, retry = true) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) {
      headers.set("Authorization", `Bearer ${state.accessToken}`);
    }
    const resp = await fetch(`${apiBase()}${path}`, { ...options, headers });
    if (resp.status === 401 && retry && state.refreshToken) {
      await refreshTokens();
      return apiFetch(path, options, false);
    }
    if (!resp.ok) {
      let msg = `HTTP ${resp.status}`;
      try {
        const err = await resp.json();
        if (err.message) msg = err.message;
      } catch (_) {}
      throw new Error(msg);
    }
    if (resp.status === 204) return null;
    return resp.json();
  }

  async function loadPhotoObjectUrl(photoId, size = "thumb") {
    const key = `${photoId}_${size}`;
    if (state.photoUrls.has(key)) return state.photoUrls.get(key);
    const headers = new Headers();
    if (state.accessToken) headers.set("Authorization", `Bearer ${state.accessToken}`);
    let resp = await fetch(`${apiBase()}/api/photos/${photoId}?size=${size}`, { headers });
    if (resp.status === 401 && state.refreshToken) {
      await refreshTokens();
      headers.set("Authorization", `Bearer ${state.accessToken}`);
      resp = await fetch(`${apiBase()}/api/photos/${photoId}?size=${size}`, { headers });
    }
    if (!resp.ok) return "";
    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    state.photoUrls.set(key, url);
    return url;
  }

  function hydratePhotos() {
    document.querySelectorAll("img[data-photo-id]").forEach((img) => {
      const pid = img.getAttribute("data-photo-id");
      const size = img.getAttribute("data-photo-size") || "thumb";
      if (!pid || img.getAttribute("data-loaded") === pid) return;
      img.setAttribute("data-loaded", pid);
      loadPhotoObjectUrl(pid, size).then((url) => {
        if (url) img.src = url;
      });
    });
  }

  async function compressJpeg(file, maxDim = 1600, quality = 0.82) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error("Failed to read file"));
      reader.onload = () => {
        const img = new Image();
        img.onerror = () => reject(new Error("Invalid image"));
        img.onload = () => {
          let w = img.width;
          let h = img.height;
          if (Math.max(w, h) > maxDim) {
            const scale = maxDim / Math.max(w, h);
            w = Math.round(w * scale);
            h = Math.round(h * scale);
          }
          const canvas = document.createElement("canvas");
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext("2d");
          ctx.drawImage(img, 0, 0, w, h);
          canvas.toBlob(
            (blob) => {
              if (!blob) return reject(new Error("Compression failed"));
              resolve(blob);
            },
            "image/jpeg",
            quality
          );
        };
        img.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }

  function haversineM(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  function formatAgo(ms) {
    if (!ms) return t("noFixYet");
    const diff = Math.max(0, Math.floor((Date.now() - ms) / 1000));
    if (diff < 60) return `${diff}s`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    return `${Math.floor(diff / 86400)}d`;
  }

  function formatTime(ms) {
    return new Date(ms).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }

  function dayLabel(ms) {
    const d = new Date(ms);
    const now = new Date();
    const startOf = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime();
    const diffDays = Math.round((startOf(now) - startOf(d)) / 86400000);
    if (diffDays === 0) return t("today");
    if (diffDays === 1) return t("yesterday");
    return d.toLocaleDateString();
  }

  function escapeHtml(str) {
    return String(str || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  async function syncAllData() {
    if (!state.accessToken) return;
    try {
      const pairState = await apiFetch("/api/pair/state");
      state.me = pairState.me;
      state.partner = pairState.partner;
      state.sharingPaused = pairState.sharing_paused;
      state.partnerPaused = pairState.partner_paused;
      state.meLocation = pairState.me_location;
      state.partnerLocation = pairState.partner_location;

      const [placesRes, checkinsRes, memoriesRes, messagesRes] = await Promise.all([
        apiFetch("/api/places"),
        apiFetch("/api/checkins"),
        apiFetch("/api/memories?limit=50"),
        apiFetch("/api/messages?limit=50")
      ]);
      state.places = placesRes.places || [];
      state.checkins = checkinsRes.checkins || [];
      state.memories = memoriesRes.items || [];
      state.messages = messagesRes.items || [];
      persistAndSyncNative();
      render();
    } catch (err) {
      showToast(err.message);
    }
  }

  function connectWebSocket() {
    if (!state.accessToken) return;
    if (ws) {
      try { ws.close(); } catch (_) {}
    }
    const base = apiBase().replace(/^http/, "ws");
    ws = new WebSocket(`${base}/api/ws?token=${encodeURIComponent(state.accessToken)}`);
    ws.onopen = () => {
      if (wsPingTimer) clearInterval(wsPingTimer);
      wsPingTimer = setInterval(() => {
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: "ping" }));
        }
      }, 25000);
      catchUpEvents();
    };
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data);
        if (msg.type === "pong") return;
        handleIncomingEvent(msg);
      } catch (_) {}
    };
    ws.onclose = () => {
      if (wsPingTimer) clearInterval(wsPingTimer);
      if (state.accessToken) {
        clearTimeout(wsReconnectTimer);
        wsReconnectTimer = setTimeout(connectWebSocket, 3500);
      }
    };
  }

  async function catchUpEvents() {
    if (!state.accessToken) return;
    try {
      const res = await apiFetch(`/api/events?after=${state.lastEventId}`);
      for (const ev of res.events || []) {
        handleIncomingEvent(ev);
      }
    } catch (_) {}
  }

  function handleIncomingEvent(ev) {
    if (ev.id && ev.id > state.lastEventId) {
      state.lastEventId = ev.id;
      localStorage.setItem("ours_last_event_id", String(state.lastEventId));
    }
    switch (ev.type) {
      case "location":
        state.partnerLocation = ev.data;
        updateMapMarkers();
        if (state.tab === "map") renderMapOverlay();
        break;
      case "chat_message":
        if (ev.data?.message) {
          state.messages = [ev.data.message, ...state.messages.filter((m) => m.id !== ev.data.message.id)];
          if (state.tab === "chat") render();
        }
        break;
      case "checkin":
        if (ev.data?.checkin) {
          state.checkins = [ev.data.checkin, ...state.checkins.filter((c) => c.id !== ev.data.checkin.id)];
          render();
        }
        break;
      case "checkin_deleted":
        state.checkins = state.checkins.filter((c) => c.id !== ev.data?.checkin_id);
        state.memories = state.memories.filter((c) => c.id !== ev.data?.checkin_id);
        render();
        break;
      case "reply":
        syncAllData();
        break;
      case "highlight":
        syncAllData();
        break;
      case "nudge":
        haptic();
        showToast(`${state.partner?.name || "Partner"} nudged you`);
        break;
      case "sos":
        haptic();
        showToast(`SOS from ${state.partner?.name || "Partner"}`);
        break;
      case "places_changed":
        apiFetch("/api/places").then((r) => {
          state.places = r.places || [];
          render();
        }).catch(() => {});
        break;
      case "avatar":
        syncAllData();
        break;
      case "sharing":
        state.partnerPaused = Boolean(ev.data?.paused);
        render();
        break;
      default:
        break;
    }
  }

  function startBrowserLocationWatch() {
    if (native || !navigator.geolocation || !state.accessToken) return;
    navigator.geolocation.watchPosition(
      async (pos) => {
        if (state.sharingPaused || !state.accessToken) return;
        let batteryPct = 100;
        let charging = false;
        if (navigator.getBattery) {
          try {
            const b = await navigator.getBattery();
            batteryPct = Math.round(b.level * 100);
            charging = b.charging;
          } catch (_) {}
        }
        const pt = {
          lat: pos.coords.latitude,
          lon: pos.coords.longitude,
          accuracy_m: pos.coords.accuracy || 20,
          speed_mps: Math.max(0, pos.coords.speed || 0),
          bearing: Number.isFinite(pos.coords.heading) ? pos.coords.heading : null,
          battery_pct: batteryPct,
          charging,
          recorded_at: Date.now()
        };
        state.meLocation = { ...pt, paused: false, user_id: state.me?.id || 0 };
        updateMapMarkers();
        try {
          const res = await apiFetch(`/api/locations?after_event_id=${state.lastEventId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify([pt])
          });
          for (const ev of res.events || []) handleIncomingEvent(ev);
        } catch (_) {}
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 15000, timeout: 15000 }
    );
  }

  function navIcon(name) {
    const icons = {
      map: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l-6 3V6l6-3 6 3 6-3v15l-6 3-6-3z"/><path d="M9 3v15M15 6v15"/></svg>`,
      memories: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="4"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>`,
      chat: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>`,
      settings: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`
    };
    return icons[name] || "";
  }

  function googleGIcon() {
    return `<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"/><path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24Z"/><path fill="#FBBC05" d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14Z"/><path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96Z"/></svg>`;
  }

  async function submitGoogleAuthPayload(payload, options = {}) {
    const serverInput = document.getElementById("ob-server");
    if (serverInput && serverInput.value.trim()) {
      state.serverUrl = serverInput.value.trim();
    }
    const email = (payload?.email || "").trim();
    if (!email) {
      showToast(t("googleEmailRequired"));
      return;
    }
    const body = {
      email,
      name: (payload?.name || "").trim() || null,
      id_token: payload?.id_token || payload?.idToken || null,
      invite_code: options.inviteCode || null,
      auto_create: options.autoCreate === true ? true : null,
      setup_key: options.setupKey || null
    };
    const headers = { "Content-Type": "application/json" };
    if (options.setupKey) headers["X-Setup-Key"] = options.setupKey;
    const resp = await fetch(`${apiBase()}/api/auth/google`, {
      method: "POST",
      headers,
      body: JSON.stringify(body)
    });
    const data = await resp.json();
    if (!resp.ok) throw new Error(data.message || "Google sign-in failed");

    if (data.status === "authenticated" && data.tokens) {
      state.accessToken = data.tokens.access_token;
      state.refreshToken = data.tokens.refresh_token;
      state.me = data.user;
      state.partner = data.partner || null;
      state.inviteCode = data.invite_code || "";
      state.googleAuth = {
        email: data.user?.email || email,
        name: data.user?.name || body.name || "",
        idToken: body.id_token || null,
        provider: payload?.provider || "google"
      };
      persistAndSyncNative();
      connectWebSocket();
      startBrowserLocationWatch();
      if (state.inviteCode && !state.partner) {
        render();
      } else {
        await syncAllData();
      }
      return;
    }

    state.googleAuth = {
      email: data.email || email,
      name: data.name || body.name || email.split("@")[0],
      idToken: body.id_token || null,
      provider: payload?.provider || "google"
    };
    render();
  }

  function openBrowserGoogleModal() {
    const preEmail = state.googleAuth?.email || "";
    const preName = state.googleAuth?.name || "";
    openSheet(`
      <div style="font-size:18px;font-weight:700;">${escapeHtml(t("googleModalTitle"))}</div>
      <div class="row-sub">${escapeHtml(t("googleModalSub"))}</div>
      <input class="input" id="gm-email" type="email" placeholder="name@gmail.com" value="${escapeHtml(preEmail)}" />
      <input class="input" id="gm-name" placeholder="${escapeHtml(t("yourName"))}" value="${escapeHtml(preName)}" />
      <div style="display:flex;gap:10px;">
        <button class="btn btn-secondary" id="gm-cancel" style="flex:1;">${escapeHtml(t("cancel"))}</button>
        <button class="btn btn-primary" id="gm-submit" style="flex:1;">${escapeHtml(t("confirm"))}</button>
      </div>
    `);
    document.getElementById("gm-cancel").onclick = closeSheet;
    document.getElementById("gm-submit").onclick = async () => {
      const email = document.getElementById("gm-email").value.trim();
      const name = document.getElementById("gm-name").value.trim();
      if (!email || !email.includes("@")) return showToast(t("googleEmailRequired"));
      try {
        await submitGoogleAuthPayload({ email, name, provider: "google-web" });
        closeSheet();
      } catch (err) {
        showToast(err.message);
      }
    };
  }

  window.onOursGoogleAuth = async (payload) => {
    try {
      await submitGoogleAuthPayload(payload || {});
    } catch (err) {
      showToast(err.message);
    }
  };

  window.onOursGoogleAuthFallback = () => {
    openBrowserGoogleModal();
  };

  function renderOnboarding() {
    const app = document.getElementById("app");
    if (state.inviteCode && !state.partner) {
      app.innerHTML = `
        <div class="onboarding-wrap">
          <div class="card" style="display:flex;flex-direction:column;gap:14px;">
            <div style="font-size:22px;font-weight:700;">${escapeHtml(t("waitingPartner"))}</div>
            <div class="invite-code-box tnum">${escapeHtml(state.inviteCode)}</div>
            <button class="btn btn-primary" id="btn-continue-app">${escapeHtml(t("continueApp"))}</button>
          </div>
        </div>
      `;
      document.getElementById("btn-continue-app").onclick = () => {
        clearInterval(pairPollTimer);
        syncAllData();
      };
      return;
    }

    const g = state.googleAuth;
    const prefillName = g?.name || "";
    const prefillEmail = g?.email || "";

    app.innerHTML = `
      <div class="onboarding-wrap">
        <div style="text-align:center;display:flex;flex-direction:column;gap:6px;">
          <div style="font-size:28px;font-weight:800;letter-spacing:-0.03em;">OURS</div>
          <div style="font-size:18px;font-weight:700;">${escapeHtml(t("welcomeTitle"))}</div>
          <div class="row-sub">${escapeHtml(t("welcomeSub"))}</div>
        </div>

        ${g ? `
          <div class="google-pill">
            <div class="google-pill-meta">
              <div class="google-pill-email">${escapeHtml(t("googleSignedInAs"))} ${escapeHtml(g.email)}</div>
              <div class="google-verified-badge">${googleGIcon()}<span>${escapeHtml(t("googleVerified"))}</span></div>
            </div>
            <button class="google-switch-btn" id="ob-google-switch">${escapeHtml(t("googleSwitch"))}</button>
          </div>
        ` : `
          <button class="btn-google" id="ob-google-btn">${googleGIcon()}<span>${escapeHtml(t("continueGoogle"))}</span></button>
          <div class="or-divider"><span>${escapeHtml(t("orManual"))}</span></div>
        `}

        <div class="segmented">
          <button class="seg-btn ${state.onboardingMode === "create" ? "active" : ""}" id="ob-mode-create">${escapeHtml(t("createPair"))}</button>
          <button class="seg-btn ${state.onboardingMode === "join" ? "active" : ""}" id="ob-mode-join">${escapeHtml(t("joinPair"))}</button>
        </div>

        <div class="card" style="display:flex;flex-direction:column;gap:10px;">
          <input class="input" id="ob-server" placeholder="${escapeHtml(t("serverUrl"))}" value="${escapeHtml(state.serverUrl)}" />
          <input class="input" id="ob-name" placeholder="${escapeHtml(t("yourName"))}" value="${escapeHtml(prefillName)}" />
          <input class="input" id="ob-email" type="email" placeholder="${escapeHtml(t("yourEmail"))}" value="${escapeHtml(prefillEmail)}" ${g ? "readonly" : ""} />
          ${
            state.onboardingMode === "create"
              ? `<input class="input" id="ob-key" placeholder="${escapeHtml(t("setupKey"))}" />`
              : `<input class="input" id="ob-code" placeholder="${escapeHtml(t("inviteCode"))}" style="text-transform:uppercase;letter-spacing:0.15em;" />`
          }
          <button class="btn btn-primary" id="ob-submit">${escapeHtml(state.onboardingMode === "create" ? t("createPair") : t("joinPair"))}</button>
        </div>
      </div>
    `;

    const googleBtn = document.getElementById("ob-google-btn");
    if (googleBtn) {
      googleBtn.onclick = () => {
        haptic();
        state.serverUrl = document.getElementById("ob-server").value.trim() || window.location.origin;
        persistAndSyncNative();
        if (native?.signInWithGoogle) {
          try {
            native.signInWithGoogle();
          } catch (_) {
            openBrowserGoogleModal();
          }
        } else {
          openBrowserGoogleModal();
        }
      };
    }
    const switchBtn = document.getElementById("ob-google-switch");
    if (switchBtn) {
      switchBtn.onclick = () => {
        haptic();
        if (native?.signInWithGoogle) {
          try {
            native.signInWithGoogle();
          } catch (_) {
            openBrowserGoogleModal();
          }
        } else {
          openBrowserGoogleModal();
        }
      };
    }

    document.getElementById("ob-mode-create").onclick = () => { state.onboardingMode = "create"; render(); };
    document.getElementById("ob-mode-join").onclick = () => { state.onboardingMode = "join"; render(); };
    document.getElementById("ob-submit").onclick = async () => {
      haptic();
      state.serverUrl = document.getElementById("ob-server").value.trim() || window.location.origin;
      const name = document.getElementById("ob-name").value.trim();
      const email = document.getElementById("ob-email").value.trim() || null;
      if (!name) return showToast("Name is required");
      try {
        if (state.googleAuth && email) {
          if (state.onboardingMode === "create") {
            const setupKey = document.getElementById("ob-key").value.trim();
            await submitGoogleAuthPayload(
              { email, name, id_token: state.googleAuth.idToken, provider: state.googleAuth.provider },
              { autoCreate: true, setupKey }
            );
          } else {
            const code = document.getElementById("ob-code").value.trim().toUpperCase();
            await submitGoogleAuthPayload(
              { email, name, id_token: state.googleAuth.idToken, provider: state.googleAuth.provider },
              { inviteCode: code }
            );
          }
          return;
        }
        if (state.onboardingMode === "create") {
          const setupKey = document.getElementById("ob-key").value.trim();
          const headers = { "Content-Type": "application/json" };
          if (setupKey) headers["X-Setup-Key"] = setupKey;
          const resp = await fetch(`${apiBase()}/api/pair/create`, {
            method: "POST",
            headers,
            body: JSON.stringify({ name, email })
          });
          const data = await resp.json();
          if (!resp.ok) throw new Error(data.message || "Failed");
          state.accessToken = data.tokens.access_token;
          state.refreshToken = data.tokens.refresh_token;
          state.me = data.user;
          state.inviteCode = data.invite_code;
          persistAndSyncNative();
          connectWebSocket();
          startBrowserLocationWatch();
          render();
        } else {
          const code = document.getElementById("ob-code").value.trim().toUpperCase();
          const resp = await fetch(`${apiBase()}/api/pair/join`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ code, name, email })
          });
          const data = await resp.json();
          if (!resp.ok) throw new Error(data.message || "Failed");
          state.accessToken = data.tokens.access_token;
          state.refreshToken = data.tokens.refresh_token;
          state.me = data.user;
          persistAndSyncNative();
          connectWebSocket();
          startBrowserLocationWatch();
          await syncAllData();
        }
      } catch (err) {
        showToast(err.message);
      }
    };
  }

  function renderMapScreen() {
    const partner = state.partner;
    const pLoc = state.partnerLocation;
    const mLoc = state.meLocation;
    let subtitle = t("noFixYet");
    if (state.partnerPaused || pLoc?.paused) {
      subtitle = t("partnerPaused");
    } else if (pLoc?.lat != null) {
      const speedKmh = Math.round((pLoc.speed_mps || 0) * 3.6);
      const bat = pLoc.battery_pct != null ? `${pLoc.battery_pct}%` : "";
      let dist = "";
      if (mLoc?.lat != null) {
        const d = haversineM(mLoc.lat, mLoc.lon, pLoc.lat, pLoc.lon);
        dist = d < 1000 ? `${Math.round(d)}m` : `${(d / 1000).toFixed(1)}km`;
      }
      subtitle = [formatAgo(pLoc.recorded_at), `${speedKmh} km/h`, bat, dist].filter(Boolean).join(" · ");
    }

    return `
      <div class="screen map-screen">
        <div class="map-partner-card">
          <div style="display:flex;align-items:center;gap:10px;min-width:0;">
            <div class="avatar-circle">
              ${partner?.avatar_photo_id
                ? `<img data-photo-id="${escapeHtml(partner.avatar_photo_id)}" data-photo-size="thumb" alt="" />`
                : escapeHtml((partner?.name || "P").slice(0, 1).toUpperCase())}
            </div>
            <div style="min-width:0;">
              <div style="font-weight:700;font-size:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(partner?.name || "Partner")}</div>
              <div class="row-sub tnum" id="map-partner-sub">${escapeHtml(subtitle)}</div>
            </div>
          </div>
          <span class="chip ${state.sharingPaused ? "chip-paused" : "chip-success"}">${escapeHtml(state.sharingPaused ? t("sharingPaused") : "Live")}</span>
        </div>

        <div class="map-canvas-wrap">
          <div id="maplibre-map" class="map-container"></div>
        </div>

        <div class="map-actions-bar">
          <button class="btn btn-primary" id="btn-map-checkin">${escapeHtml(t("checkIn"))}</button>
          <button class="btn btn-glass" id="btn-map-nudge">${escapeHtml(t("nudge"))}</button>
          <button class="btn btn-glass" id="btn-map-center">${escapeHtml(t("center"))}</button>
          <button class="btn btn-glass" id="btn-map-places">${escapeHtml(t("places"))}</button>
        </div>
      </div>
    `;
  }

  function renderMapOverlay() {
    const el = document.getElementById("map-partner-sub");
    if (!el) return;
    const pLoc = state.partnerLocation;
    const mLoc = state.meLocation;
    if (state.partnerPaused || pLoc?.paused) {
      el.textContent = t("partnerPaused");
      return;
    }
    if (pLoc?.lat != null) {
      const speedKmh = Math.round((pLoc.speed_mps || 0) * 3.6);
      const bat = pLoc.battery_pct != null ? `${pLoc.battery_pct}%` : "";
      let dist = "";
      if (mLoc?.lat != null) {
        const d = haversineM(mLoc.lat, mLoc.lon, pLoc.lat, pLoc.lon);
        dist = d < 1000 ? `${Math.round(d)}m` : `${(d / 1000).toFixed(1)}km`;
      }
      el.textContent = [formatAgo(pLoc.recorded_at), `${speedKmh} km/h`, bat, dist].filter(Boolean).join(" · ");
    }
  }

  function initOrUpdateMap() {
    const container = document.getElementById("maplibre-map");
    if (!container) return;
    if (typeof maplibregl === "undefined") {
      container.innerHTML = `<div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;color:var(--ink-muted);">Map offline</div>`;
      return;
    }
    const isDark =
      state.theme === "dark" ||
      (state.theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    const tileUrl = isDark
      ? "https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png"
      : "https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png";

    const center = state.partnerLocation?.lat != null
      ? [state.partnerLocation.lon, state.partnerLocation.lat]
      : state.meLocation?.lat != null
      ? [state.meLocation.lon, state.meLocation.lat]
      : [106.8272, -6.1754];

    if (mapInstance) {
      try { mapInstance.remove(); } catch (_) {}
      mapInstance = null;
      meMarker = null;
      partnerMarker = null;
    }

    mapInstance = new maplibregl.Map({
      container,
      style: {
        version: 8,
        sources: {
          carto: { type: "raster", tiles: [tileUrl], tileSize: 256 }
        },
        layers: [{ id: "carto", type: "raster", source: "carto" }]
      },
      center,
      zoom: 13.5,
      attributionControl: false
    });
    updateMapMarkers();
  }

  function updateMapMarkers() {
    if (!mapInstance || typeof maplibregl === "undefined") return;
    if (state.meLocation?.lat != null) {
      if (!meMarker) {
        const el = document.createElement("div");
        el.className = "map-pin";
        el.innerHTML = `<div class="map-pin-dot" style="background:#196F43;">${escapeHtml((state.me?.name || "M").slice(0, 1).toUpperCase())}</div><div class="map-pin-label">Me</div>`;
        meMarker = new maplibregl.Marker({ element: el }).setLngLat([state.meLocation.lon, state.meLocation.lat]).addTo(mapInstance);
      } else {
        meMarker.setLngLat([state.meLocation.lon, state.meLocation.lat]);
      }
    }
    if (state.partnerLocation?.lat != null && !state.partnerLocation.paused) {
      if (!partnerMarker) {
        const el = document.createElement("div");
        el.className = "map-pin";
        el.innerHTML = `<div class="map-pin-dot" style="background:#0066CC;">${escapeHtml((state.partner?.name || "P").slice(0, 1).toUpperCase())}</div><div class="map-pin-label">${escapeHtml(state.partner?.name || "Partner")}</div>`;
        partnerMarker = new maplibregl.Marker({ element: el }).setLngLat([state.partnerLocation.lon, state.partnerLocation.lat]).addTo(mapInstance);
      } else {
        partnerMarker.setLngLat([state.partnerLocation.lon, state.partnerLocation.lat]);
      }
    } else if (partnerMarker) {
      partnerMarker.remove();
      partnerMarker = null;
    }
  }

  function renderMemoriesScreen() {
    const q = state.memoriesQuery.trim().toLowerCase();
    const items = state.memories.filter((c) => {
      if (state.memoriesFilter === "places" && !c.place_name) return false;
      if (q) {
        const hay = `${c.note || ""} ${c.place_name || ""}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });

    return `
      <div class="top-bar">
        <div class="top-title">${escapeHtml(t("tabMemories"))}</div>
        <div class="top-actions">
          <button class="icon-btn" id="btn-mem-search" aria-label="Search">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          </button>
        </div>
      </div>
      <div class="screen">
        <div class="page-pad">
          <div class="segmented">
            <button class="seg-btn ${state.memoriesMode === "timeline" ? "active" : ""}" id="seg-mem-timeline">${escapeHtml(t("timeline"))}</button>
            <button class="seg-btn ${state.memoriesMode === "map" ? "active" : ""}" id="seg-mem-map">${escapeHtml(t("map"))}</button>
          </div>
          ${state.memoriesSearchOpen ? `<input class="input" id="mem-search-input" placeholder="${escapeHtml(t("searchPlaceholder"))}" value="${escapeHtml(state.memoriesQuery)}" />` : ""}
          <div class="filter-row">
            <button class="filter-pill ${state.memoriesFilter === "all" ? "active" : ""}" data-mem-filter="all">${escapeHtml(t("all"))}</button>
            <button class="filter-pill ${state.memoriesFilter === "photos" ? "active" : ""}" data-mem-filter="photos">${escapeHtml(t("photos"))}</button>
            <button class="filter-pill ${state.memoriesFilter === "places" ? "active" : ""}" data-mem-filter="places">${escapeHtml(t("places"))}</button>
          </div>

          ${
            state.memoriesMode === "map"
              ? `<div class="card" style="padding:0;overflow:hidden;height:420px;"><div id="memories-map" style="width:100%;height:100%;"></div></div>`
              : items.length === 0
              ? `<div class="card" style="text-align:center;padding:28px 16px;">
                   <div class="row-sub">${escapeHtml(q ? t("noResults") : t("noMemories"))}</div>
                   ${q ? `<button class="btn btn-soft" style="margin-top:12px;" id="btn-mem-clear">${escapeHtml(t("clearFilters"))}</button>` : ""}
                 </div>`
              : items.map((c) => renderCheckInCard(c)).join("")
          }
        </div>
      </div>
    `;
  }

  function renderCheckInCard(c) {
    const isMine = c.author_id === state.me?.id;
    const authorName = isMine ? (state.me?.name || "Me") : (state.partner?.name || "Partner");
    return `
      <div class="memory-card">
        <img class="memory-photo" data-photo-id="${escapeHtml(c.photo_id)}" data-photo-size="full" data-lightbox="${escapeHtml(c.photo_id)}" alt="" />
        <div class="memory-body">
          <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
            <span style="font-weight:700;">${escapeHtml(authorName)}</span>
            <span class="row-sub tnum">${escapeHtml(formatTime(c.created_at))}</span>
          </div>
          ${c.place_name ? `<div class="chip chip-action" style="align-self:flex-start;">${escapeHtml(c.place_name)}</div>` : ""}
          ${c.note ? `<div>${escapeHtml(c.note)}</div>` : ""}
          ${
            c.replies && c.replies.length
              ? `<div style="display:flex;flex-direction:column;gap:4px;padding:8px 10px;border-radius:12px;background:var(--surface-muted);">
                  ${c.replies.map((r) => `<div style="font-size:13px;"><strong>${escapeHtml(r.author_id === state.me?.id ? (state.me?.name || "Me") : (state.partner?.name || "Partner"))}:</strong> ${escapeHtml(r.text || "[Photo]")}</div>`).join("")}
                </div>`
              : ""
          }
          <div class="memory-actions">
            <button class="pill-action ${c.highlighted ? "saved" : ""}" data-toggle-hl="${c.id}" data-hl="${c.highlighted}">
              ${escapeHtml(c.highlighted ? t("saved") : t("save"))}
            </button>
            <button class="pill-action" data-reply-checkin="${c.id}">${escapeHtml(t("reply"))}</button>
            ${isMine ? `<button class="pill-action" style="color:var(--danger);" data-del-checkin="${c.id}">${escapeHtml(t("del"))}</button>` : ""}
          </div>
        </div>
      </div>
    `;
  }

  function renderChatScreen() {
    const q = state.chatQuery.trim().toLowerCase();
    const combined = [
      ...state.messages.map((m) => ({ kind: "msg", created_at: m.created_at, item: m })),
      ...state.checkins.map((c) => ({ kind: "checkin", created_at: c.created_at, item: c }))
    ]
      .filter((entry) => {
        if (!q) return true;
        const txt = entry.kind === "msg" ? entry.item.text : `${entry.item.note || ""} ${entry.item.place_name || ""}`;
        return (txt || "").toLowerCase().includes(q);
      })
      .sort((a, b) => a.created_at - b.created_at);

    let lastDay = "";
    const rowsHtml = combined.map((entry) => {
      const dl = dayLabel(entry.created_at);
      const divider = dl !== lastDay ? `<div class="chat-day-divider">${escapeHtml(dl)}</div>` : "";
      lastDay = dl;

      if (entry.kind === "checkin") {
        return `${divider}<div style="margin:6px 0;">${renderCheckInCard(entry.item)}</div>`;
      }
      const m = entry.item;
      const mine = m.author_id === state.me?.id;
      return `
        ${divider}
        <div class="bubble-row ${mine ? "mine" : "theirs"}">
          <div class="bubble ${mine ? "mine" : "theirs"}">
            ${m.photo_id ? `<img data-photo-id="${escapeHtml(m.photo_id)}" data-photo-size="thumb" data-lightbox="${escapeHtml(m.photo_id)}" style="max-width:220px;border-radius:12px;display:block;margin-bottom:${m.text ? "6px" : "0"};cursor:pointer;" alt="" />` : ""}
            ${m.text ? escapeHtml(m.text) : ""}
          </div>
          <div class="bubble-meta tnum">${escapeHtml(formatTime(m.created_at))}</div>
        </div>
      `;
    }).join("");

    return `
      <div class="top-bar">
        <div class="top-title">${escapeHtml(state.partner?.name || t("tabChat"))}</div>
        <div class="top-actions">
          <button class="icon-btn" id="btn-chat-search" aria-label="Search">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          </button>
        </div>
      </div>
      <div class="screen chat-screen" id="chat-scroll">
        ${state.chatSearchOpen ? `<div class="page-pad" style="padding-bottom:0;"><input class="input" id="chat-search-input" placeholder="${escapeHtml(t("searchPlaceholder"))}" value="${escapeHtml(state.chatQuery)}" /></div>` : ""}
        <div class="chat-list">
          ${rowsHtml || `<div class="row-sub" style="text-align:center;padding:32px 0;">${escapeHtml(q ? t("noResults") : "Say hello")}</div>`}
        </div>
      </div>
      <div class="chat-composer">
        ${
          state.replyToCheckInId
            ? `<div class="reply-banner"><span>${escapeHtml(t("replyingTo"))} #${state.replyToCheckInId}</span><button class="icon-btn" id="btn-cancel-reply" style="min-width:36px;min-height:36px;">×</button></div>`
            : ""
        }
        <div class="composer-row">
          <button class="icon-btn" id="btn-chat-camera" aria-label="Camera">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
          </button>
          <input class="input" id="chat-input" placeholder="${escapeHtml(t("typeMessage"))}" />
          <button class="btn btn-primary" id="btn-chat-send">${escapeHtml(t("send"))}</button>
        </div>
      </div>
    `;
  }

  function renderSettingsScreen() {
    return `
      <div class="top-bar">
        <div class="top-title">${escapeHtml(t("settings"))}</div>
      </div>
      <div class="screen">
        <div class="page-pad">
          <div class="card" style="display:flex;align-items:center;gap:14px;">
            <label class="avatar-circle" style="width:56px;height:56px;cursor:pointer;">
              <input type="file" id="avatar-file-input" accept="image/*" class="hidden" />
              ${state.me?.avatar_photo_id
                ? `<img data-photo-id="${escapeHtml(state.me.avatar_photo_id)}" data-photo-size="thumb" alt="" />`
                : escapeHtml((state.me?.name || "U").slice(0, 1).toUpperCase())}
            </label>
            <div style="min-width:0;flex:1;">
              <div style="font-size:17px;font-weight:700;">${escapeHtml(state.me?.name || "")} &amp; ${escapeHtml(state.partner?.name || "...")}</div>
              <div class="row-sub">${escapeHtml(state.me?.email || "")}</div>
            </div>
          </div>

          <div class="section-label">${escapeHtml(t("appearance"))}</div>
          <div class="grouped-list">
            <div class="grouped-row" style="flex-direction:column;align-items:stretch;gap:8px;">
              <div style="font-weight:600;">${escapeHtml(t("appearance"))}</div>
              <div class="segmented">
                <button class="seg-btn ${state.theme === "system" ? "active" : ""}" data-theme-set="system">${escapeHtml(t("themeSystem"))}</button>
                <button class="seg-btn ${state.theme === "light" ? "active" : ""}" data-theme-set="light">${escapeHtml(t("themeLight"))}</button>
                <button class="seg-btn ${state.theme === "dark" ? "active" : ""}" data-theme-set="dark">${escapeHtml(t("themeDark"))}</button>
              </div>
            </div>
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("language"))}</div>
              <div class="segmented" style="width:140px;">
                <button class="seg-btn ${state.lang === "en" ? "active" : ""}" data-lang-set="en">EN</button>
                <button class="seg-btn ${state.lang === "id" ? "active" : ""}" data-lang-set="id">ID</button>
              </div>
            </div>
          </div>

          <div class="section-label">${escapeHtml(t("locationPrivacy"))}</div>
          <div class="grouped-list">
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("pauseSharing"))}</div>
              <button class="switch ${state.sharingPaused ? "on" : ""}" id="sw-pause" aria-pressed="${state.sharingPaused}"></button>
            </div>
            <div class="grouped-row" id="row-places" style="cursor:pointer;">
              <div style="font-weight:600;">${escapeHtml(t("savedPlaces"))}</div>
              <span class="row-sub">${state.places.length}</span>
            </div>
            <div class="grouped-row" id="row-bg-guide" style="cursor:pointer;">
              <div style="font-weight:600;">${escapeHtml(t("bgReliability"))}</div>
              <span class="row-sub">Xiaomi / HyperOS</span>
            </div>
          </div>

          <div class="section-label">${escapeHtml(t("safetySos"))}</div>
          <div class="grouped-list">
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("crashDetection"))}</div>
              <button class="switch ${state.crashDetectionEnabled ? "on" : ""}" id="sw-crash" aria-pressed="${state.crashDetectionEnabled}"></button>
            </div>
            <div class="grouped-row" style="flex-direction:column;align-items:stretch;gap:8px;">
              <div style="font-weight:600;">${escapeHtml(t("emergencyWhatsapp"))}</div>
              <input class="input" id="input-wa" placeholder="+62812..." value="${escapeHtml(state.emergencyContact)}" />
            </div>
            <div class="grouped-row">
              <button class="btn btn-danger" style="width:100%;" id="btn-open-sos">${escapeHtml(t("triggerSos"))}</button>
            </div>
          </div>

          <div class="section-label">${escapeHtml(t("serverSection"))}</div>
          <div class="grouped-list">
            <div class="grouped-row" style="flex-direction:column;align-items:stretch;gap:8px;">
              <input class="input" id="input-server-url" value="${escapeHtml(state.serverUrl)}" />
              <div style="display:flex;gap:8px;">
                <button class="btn btn-secondary" style="flex:1;" id="btn-test-conn">${escapeHtml(t("testConnection"))}</button>
                <button class="btn btn-soft" style="flex:1;" id="btn-reload-ui">${escapeHtml(t("reloadUi"))}</button>
              </div>
              ${
                state.serverStatusText
                  ? `<div style="font-size:13px;font-weight:600;color:${state.serverStatusOk ? "var(--success)" : "var(--danger)"};">${escapeHtml(state.serverStatusText)}</div>`
                  : ""
              }
            </div>
          </div>

          <button class="btn btn-danger" id="btn-logout">${escapeHtml(t("signOut"))}</button>
        </div>
      </div>
    `;
  }

  function openModal(html, centered = false) {
    const root = document.getElementById("modal-root");
    root.className = `modal-root ${centered ? "center" : ""}`;
    root.innerHTML = `<div class="modal-sheet">${html}</div>`;
    root.onclick = (e) => {
      if (e.target === root) closeModal();
    };
  }

  function closeModal() {
    const root = document.getElementById("modal-root");
    root.className = "modal-root hidden";
    root.innerHTML = "";
  }

  function openCheckInModal() {
    openModal(`
      <div style="font-size:18px;font-weight:700;">${escapeHtml(t("checkIn"))}</div>
      <input type="file" id="ci-file" accept="image/*" capture="environment" class="input" />
      <input type="text" id="ci-place" class="input" placeholder="${escapeHtml(t("placeOptional"))}" />
      <textarea id="ci-note" class="textarea" placeholder="${escapeHtml(t("noteOptional"))}"></textarea>
      <div style="display:flex;gap:8px;">
        <button class="btn btn-secondary" style="flex:1;" id="ci-cancel">${escapeHtml(t("cancel"))}</button>
        <button class="btn btn-primary" style="flex:1;" id="ci-submit">${escapeHtml(t("confirm"))}</button>
      </div>
    `);
    document.getElementById("ci-cancel").onclick = closeModal;
    document.getElementById("ci-submit").onclick = async () => {
      const file = document.getElementById("ci-file").files[0];
      if (!file) return showToast("Please choose a photo");
      const btn = document.getElementById("ci-submit");
      btn.disabled = true;
      try {
        const blob = await compressJpeg(file);
        const fd = new FormData();
        fd.append("photo", blob, "checkin.jpg");
        const note = document.getElementById("ci-note").value.trim();
        const place = document.getElementById("ci-place").value.trim();
        if (note) fd.append("note", note);
        if (place) fd.append("place_name", place);
        if (state.meLocation?.lat != null) {
          fd.append("lat", String(state.meLocation.lat));
          fd.append("lon", String(state.meLocation.lon));
        }
        const created = await apiFetch("/api/checkins", { method: "POST", body: fd });
        state.checkins = [created, ...state.checkins];
        haptic();
        closeModal();
        render();
      } catch (err) {
        btn.disabled = false;
        showToast(err.message);
      }
    };
  }

  function openPlacesModal() {
    const listHtml = state.places.map((p) => `
      <div class="grouped-row">
        <div>
          <div style="font-weight:600;">${escapeHtml(p.name)}</div>
          <div class="row-sub tnum">${p.lat.toFixed(4)}, ${p.lon.toFixed(4)} (${Math.round(p.radius_m)}m)</div>
        </div>
        <button class="btn btn-danger" data-del-place="${p.id}">${escapeHtml(t("del"))}</button>
      </div>
    `).join("");

    openModal(`
      <div style="font-size:18px;font-weight:700;">${escapeHtml(t("savedPlaces"))}</div>
      <div class="grouped-list">${listHtml || `<div class="grouped-row"><span class="row-sub">No saved places</span></div>`}</div>
      <input class="input" id="pl-name" placeholder="${escapeHtml(t("placeName"))}" />
      <input class="input" id="pl-radius" type="number" value="150" placeholder="${escapeHtml(t("radiusM"))}" />
      <div style="display:flex;gap:8px;">
        <button class="btn btn-secondary" style="flex:1;" id="pl-close">${escapeHtml(t("close"))}</button>
        <button class="btn btn-primary" style="flex:1;" id="pl-add">${escapeHtml(t("addPlace"))}</button>
      </div>
    `);

    document.getElementById("pl-close").onclick = closeModal;
    document.querySelectorAll("[data-del-place]").forEach((btn) => {
      btn.onclick = async () => {
        const id = Number(btn.getAttribute("data-del-place"));
        await apiFetch(`/api/places/${id}`, { method: "DELETE" });
        state.places = state.places.filter((p) => p.id !== id);
        openPlacesModal();
        render();
      };
    });
    document.getElementById("pl-add").onclick = async () => {
      const name = document.getElementById("pl-name").value.trim();
      const radius_m = Number(document.getElementById("pl-radius").value) || 150;
      const lat = state.meLocation?.lat ?? state.partnerLocation?.lat;
      const lon = state.meLocation?.lon ?? state.partnerLocation?.lon;
      if (!name || lat == null) return showToast("Name and current location required");
      const created = await apiFetch("/api/places", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, lat, lon, radius_m })
      });
      state.places.push(created);
      openPlacesModal();
      render();
    };
  }

  function openSosModal() {
    openModal(`
      <div style="font-size:18px;font-weight:700;color:var(--danger);">${escapeHtml(t("sosTitle"))}</div>
      <div class="row-sub">${escapeHtml(t("sosBody"))}</div>
      <div style="display:flex;gap:8px;">
        <button class="btn btn-secondary" style="flex:1;" id="sos-cancel">${escapeHtml(t("cancel"))}</button>
        <button class="btn btn-danger" style="flex:1;" id="sos-send">${escapeHtml(t("triggerSos"))}</button>
      </div>
    `, true);
    document.getElementById("sos-cancel").onclick = closeModal;
    document.getElementById("sos-send").onclick = async () => {
      try {
        await apiFetch("/api/sos", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            kind: "manual",
            lat: state.meLocation?.lat ?? null,
            lon: state.meLocation?.lon ?? null
          })
        });
        haptic();
        closeModal();
        showToast("SOS sent");
      } catch (err) {
        showToast(err.message);
      }
    };
  }

  function openLightbox(photoId) {
    openModal(`
      <img class="lightbox-img" data-photo-id="${escapeHtml(photoId)}" data-photo-size="full" alt="" />
      <button class="btn btn-secondary" id="lb-close">${escapeHtml(t("close"))}</button>
    `, true);
    hydratePhotos();
    document.getElementById("lb-close").onclick = closeModal;
  }

  function bindHandlers() {
    document.querySelectorAll("[data-tab]").forEach((btn) => {
      btn.onclick = () => {
        haptic();
        state.tab = btn.getAttribute("data-tab");
        render();
      };
    });

    if (state.tab === "map") {
      initOrUpdateMap();
      document.getElementById("btn-map-checkin").onclick = openCheckInModal;
      document.getElementById("btn-map-places").onclick = openPlacesModal;
      document.getElementById("btn-map-nudge").onclick = async () => {
        haptic();
        try {
          await apiFetch("/api/nudge", { method: "POST" });
          showToast(t("nudgeSent"));
        } catch (err) {
          showToast(err.message);
        }
      };
      document.getElementById("btn-map-center").onclick = () => {
        haptic();
        if (!mapInstance) return;
        const p = state.partnerLocation;
        const m = state.meLocation;
        if (p?.lat != null && !p.paused && m?.lat != null && haversineM(m.lat, m.lon, p.lat, p.lon) <= 100000) {
          const bounds = new maplibregl.LngLatBounds([m.lon, m.lat], [m.lon, m.lat]).extend([p.lon, p.lat]);
          mapInstance.fitBounds(bounds, { padding: 80, maxZoom: 15 });
        } else if (p?.lat != null && !p.paused) {
          mapInstance.flyTo({ center: [p.lon, p.lat], zoom: 14.5 });
        } else if (m?.lat != null) {
          mapInstance.flyTo({ center: [m.lon, m.lat], zoom: 14.5 });
        }
      };
    }

    if (state.tab === "memories") {
      document.getElementById("seg-mem-timeline").onclick = () => { state.memoriesMode = "timeline"; render(); };
      document.getElementById("seg-mem-map").onclick = () => { state.memoriesMode = "map"; render(); };
      document.getElementById("btn-mem-search").onclick = () => { state.memoriesSearchOpen = !state.memoriesSearchOpen; render(); };
      const si = document.getElementById("mem-search-input");
      if (si) si.oninput = (e) => { state.memoriesQuery = e.target.value; render(); };
      const cb = document.getElementById("btn-mem-clear");
      if (cb) cb.onclick = () => { state.memoriesQuery = ""; state.memoriesFilter = "all"; render(); };
      document.querySelectorAll("[data-mem-filter]").forEach((b) => {
        b.onclick = () => { state.memoriesFilter = b.getAttribute("data-mem-filter"); render(); };
      });
      if (state.memoriesMode === "map" && typeof maplibregl !== "undefined") {
        const c = document.getElementById("memories-map");
        if (c) {
          const mm = new maplibregl.Map({
            container: c,
            style: { version: 8, sources: { carto: { type: "raster", tiles: ["https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png"], tileSize: 256 } }, layers: [{ id: "carto", type: "raster", source: "carto" }] },
            center: [state.partnerLocation?.lon ?? 106.82, state.partnerLocation?.lat ?? -6.17],
            zoom: 12,
            attributionControl: false
          });
          state.memories.filter((m) => m.lat != null).forEach((m) => {
            new maplibregl.Marker({ color: "#0066CC" }).setLngLat([m.lon, m.lat]).addTo(mm);
          });
        }
      }
    }

    if (state.tab === "chat") {
      const sc = document.getElementById("chat-scroll");
      if (sc) sc.scrollTop = sc.scrollHeight;
      document.getElementById("btn-chat-search").onclick = () => { state.chatSearchOpen = !state.chatSearchOpen; render(); };
      const csi = document.getElementById("chat-search-input");
      if (csi) csi.oninput = (e) => { state.chatQuery = e.target.value; render(); };
      const cr = document.getElementById("btn-cancel-reply");
      if (cr) cr.onclick = () => { state.replyToCheckInId = null; render(); };
      document.getElementById("btn-chat-camera").onclick = openCheckInModal;
      document.getElementById("btn-chat-send").onclick = async () => {
        const inp = document.getElementById("chat-input");
        const text = inp.value.trim();
        if (!text) return;
        inp.value = "";
        haptic();
        try {
          if (state.replyToCheckInId) {
            const fd = new FormData();
            fd.append("text", text);
            await apiFetch(`/api/checkins/${state.replyToCheckInId}/replies`, { method: "POST", body: fd });
            state.replyToCheckInId = null;
            await syncAllData();
          } else {
            const msg = await apiFetch("/api/messages", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ text })
            });
            state.messages = [msg, ...state.messages];
            render();
          }
        } catch (err) {
          showToast(err.message);
        }
      };
    }

    if (state.tab === "settings") {
      document.getElementById("avatar-file-input").onchange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;
        try {
          const blob = await compressJpeg(file, 600, 0.85);
          const fd = new FormData();
          fd.append("photo", blob, "avatar.jpg");
          state.me = await apiFetch("/api/me/avatar", { method: "POST", body: fd });
          render();
        } catch (err) {
          showToast(err.message);
        }
      };
      document.querySelectorAll("[data-theme-set]").forEach((b) => {
        b.onclick = () => {
          state.theme = b.getAttribute("data-theme-set");
          applyTheme();
          persistAndSyncNative();
          render();
        };
      });
      document.querySelectorAll("[data-lang-set]").forEach((b) => {
        b.onclick = () => {
          state.lang = b.getAttribute("data-lang-set");
          persistAndSyncNative();
          render();
        };
      });
      document.getElementById("sw-pause").onclick = async () => {
        const next = !state.sharingPaused;
        try {
          await apiFetch("/api/sharing", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ paused: next })
          });
          state.sharingPaused = next;
          persistAndSyncNative();
          render();
        } catch (err) {
          showToast(err.message);
        }
      };
      document.getElementById("sw-crash").onclick = () => {
        state.crashDetectionEnabled = !state.crashDetectionEnabled;
        persistAndSyncNative();
        render();
      };
      document.getElementById("input-wa").onchange = (e) => {
        state.emergencyContact = e.target.value.trim();
        persistAndSyncNative();
      };
      document.getElementById("row-places").onclick = openPlacesModal;
      document.getElementById("row-bg-guide").onclick = () => {
        openModal(`
          <div style="font-size:18px;font-weight:700;">Xiaomi / HyperOS</div>
          <div class="row-sub">Enable Autostart, set Battery Saver to "No restrictions", and lock OURS in Recents for reliable background location updates.</div>
          <button class="btn btn-primary" id="bg-ok">${escapeHtml(t("close"))}</button>
        `, true);
        document.getElementById("bg-ok").onclick = closeModal;
      };
      document.getElementById("btn-open-sos").onclick = openSosModal;
      document.getElementById("btn-test-conn").onclick = async () => {
        state.serverUrl = document.getElementById("input-server-url").value.trim() || window.location.origin;
        persistAndSyncNative();
        try {
          const r = await fetch(`${apiBase()}/healthz`);
          if (!r.ok) throw new Error();
          state.serverStatusOk = true;
          state.serverStatusText = t("connected");
        } catch (_) {
          state.serverStatusOk = false;
          state.serverStatusText = t("unreachable");
        }
        render();
      };
      document.getElementById("btn-reload-ui").onclick = () => {
        const nextUrl = document.getElementById("input-server-url").value.trim() || window.location.origin;
        state.serverUrl = nextUrl;
        persistAndSyncNative();
        if (native?.reloadUpdatingUi) {
          native.reloadUpdatingUi();
        } else {
          window.location.href = `${apiBase()}/?t=${Date.now()}`;
        }
      };
      document.getElementById("btn-logout").onclick = async () => {
        try {
          await apiFetch("/api/auth/logout", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refresh_token: state.refreshToken })
          });
        } catch (_) {}
        state.accessToken = "";
        state.refreshToken = "";
        state.me = null;
        state.partner = null;
        persistAndSyncNative();
        render();
      };
    }

    document.querySelectorAll("[data-toggle-hl]").forEach((btn) => {
      btn.onclick = async () => {
        const id = Number(btn.getAttribute("data-toggle-hl"));
        const isHl = btn.getAttribute("data-hl") === "true";
        haptic();
        try {
          await apiFetch(`/api/checkins/${id}/highlight`, { method: isHl ? "DELETE" : "POST" });
          await syncAllData();
        } catch (err) {
          showToast(err.message);
        }
      };
    });
    document.querySelectorAll("[data-reply-checkin]").forEach((btn) => {
      btn.onclick = () => {
        state.replyToCheckInId = Number(btn.getAttribute("data-reply-checkin"));
        state.tab = "chat";
        render();
      };
    });
    document.querySelectorAll("[data-del-checkin]").forEach((btn) => {
      btn.onclick = async () => {
        const id = Number(btn.getAttribute("data-del-checkin"));
        try {
          await apiFetch(`/api/checkins/${id}`, { method: "DELETE" });
          await syncAllData();
        } catch (err) {
          showToast(err.message);
        }
      };
    });
    document.querySelectorAll("[data-lightbox]").forEach((img) => {
      img.onclick = () => openLightbox(img.getAttribute("data-lightbox"));
    });

    hydratePhotos();
  }

  function render() {
    const app = document.getElementById("app");
    if (!state.accessToken || (state.inviteCode && !state.partner && !state.me)) {
      renderOnboarding();
      return;
    }
    if (!state.accessToken) {
      renderOnboarding();
      return;
    }

    let bodyHtml = "";
    if (state.tab === "map") bodyHtml = renderMapScreen();
    else if (state.tab === "memories") bodyHtml = renderMemoriesScreen();
    else if (state.tab === "chat") bodyHtml = renderChatScreen();
    else bodyHtml = renderSettingsScreen();

    app.innerHTML = `
      ${bodyHtml}
      <nav class="bottom-nav" aria-label="Main navigation">
        <button class="nav-item ${state.tab === "map" ? "active" : ""}" data-tab="map">${navIcon("map")}<span>${escapeHtml(t("tabMap"))}</span></button>
        <button class="nav-item ${state.tab === "memories" ? "active" : ""}" data-tab="memories">${navIcon("memories")}<span>${escapeHtml(t("tabMemories"))}</span></button>
        <button class="nav-item ${state.tab === "chat" ? "active" : ""}" data-tab="chat">${navIcon("chat")}<span>${escapeHtml(t("tabChat"))}</span></button>
        <button class="nav-item ${state.tab === "settings" ? "active" : ""}" data-tab="settings">${navIcon("settings")}<span>${escapeHtml(t("tabSettings"))}</span></button>
      </nav>
    `;
    bindHandlers();
  }

  function boot() {
    inheritNativeState();
    applyTheme();

    if ("serviceWorker" in navigator && !native) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }

    setTimeout(() => {
      const splash = document.getElementById("splash");
      const app = document.getElementById("app");
      if (splash) splash.classList.add("fade-out");
      if (app) app.classList.remove("hidden");
      setTimeout(() => splash?.remove(), 320);
    }, 550);

    render();
    if (state.accessToken) {
      syncAllData();
      connectWebSocket();
      startBrowserLocationWatch();
    }
  }

  boot();
})();
