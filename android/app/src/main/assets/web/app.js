(() => {
  "use strict";

  const PRODUCTION_URL = "https://ours.mnemonic.my.id";
  const PHOTO_CACHE = new Map();

  const STRINGS = {
    en: {
      appTagline: "Private for two",
      onboardingWelcome: "Stay close, quietly.",
      onboardingSub: "Live location, 24h check-ins, and shared memories built for just the two of you.",
      continueGoogle: "Continue with Google",
      googleVerified: "Google verified",
      googleSignedInAs: "Signed in as",
      googleSwitch: "Switch",
      googleModalTitle: "Continue with Google",
      googleModalSub: "Sign in with your Google email to enter or restore your pair session.",
      googleEmailRequired: "Please enter a valid Google email",
      googleCancelled: "Sign-in cancelled",
      createPair: "Create pair",
      joinPair: "Join with code",
      createCta: "Create OURS Space",
      joinCta: "Join OURS Space",
      spaceNameLabel: "Space name (optional)",
      spaceNamePlaceholder: "Space name (e.g. Alex & Maya)",
      yourName: "Display name",
      displayNameHint: "Shown to your partner on map & chat",
      continueNoEmail: "Continue without email",
      quickSetupChip: "Space & display setup · No email needed",
      backBtn: "Back",
      waitingSpaceSub: "Space \"{space}\" is ready. Share this private invite code to pair.",
      partnerCode: "6-character invite code",
      setupKeyPlaceholder: "Server setup key",
      recoveryTitle: "Verify this new device",
      recoverySub: "We found an existing account for {email}. Confirm with your pair invite code or server setup key to link this device.",
      verifyDeviceBtn: "Verify & sign in",
      waitingTitle: "Waiting for your partner",
      waitingSub: "Share this private invite code with your partner to pair.",
      oneTimeCodeLbl: "YOUR ONE-TIME PAIR CODE",
      copyCode: "Copy pair code",
      codeCopied: "Invite code copied",
      alreadySetupHint: "This server already has an active pair. Sign in with the registered partner email or join with an invite code.",
      tabMap: "Map",
      tabMemories: "Memories",
      tabChat: "Chat",
      tabSettings: "Settings",
      tabPlaces: "Saved Places",
      liveNow: "Live",
      pausedLabel: "Paused",
      sharingPausedBanner: "Sharing paused",
      sharingPausedSub: "{name} sees you are on a quiet break",
      privacyBubble: "Privacy Bubble",
      privacyBubbleSub: "Your partner sees your general neighborhood instead of your exact GPS pin.",
      exactGps: "Exact GPS",
      bubbleRadius: "Bubble radius",
      autoExpireDur: "Auto-expire duration",
      bubbleSosNote: "Auto-clears to Exact GPS if Hold 2s SOS activates.",
      activateBubbleCta: "Activate {r} Privacy Bubble",
      approxZoneLbl: "Approx. {r} Zone",
      privacyOnToast: "Privacy bubble enabled (sharing paused)",
      privacyOffToast: "Live sharing resumed",
      offlineLabel: "Offline",
      waitingPartnerLoc: "Waiting for partner location",
      nudge: "Send Nudge",
      nudgeSent: "Nudge sent",
      checkIn: "Check in",
      centerPins: "Center Pins",
      places: "Places",
      route24h: "24h Path",
      route24hTitle: "Today · 24h Route",
      routePointsToast: "{n} route points shown",
      noRouteToast: "No recent 24h route points yet",
      dwellTripsLbl: "CHRONOLOGICAL DWELL & TRIPS",
      dwellAutoPruneNote: "24h local history auto-prunes · Zero cloud driving scores",
      holdSos: "Hold 2s SOS",
      holdSosLong: "Hold 2s for Emergency SOS",
      holdSosHint: "Press and hold 2 seconds to send SOS",
      sosSent: "SOS alert sent",
      sosTitle: "Send SOS Alert?",
      sosBody: "This immediately alerts your partner with your latest GPS coordinates.",
      cancel: "Cancel",
      confirm: "Confirm",
      close: "Close",
      send: "Send",
      save: "Save",
      saved: "Saved",
      reply: "Reply",
      del: "Delete",
      noteOptional: "Add a note for your partner...",
      placeOptional: "Place name (optional)",
      typeMessage: "Message...",
      replyingTo: "Replying to",
      searchMemoriesPlaceholder: "Search memories, places, notes...",
      searchChatPlaceholder: "Search messages...",
      searchPlacesPlaceholder: "Drop pin name at current location...",
      all: "All",
      photos: "Photos",
      highlights: "Highlights",
      timeline: "Timeline",
      mapPins: "Map Pins",
      noMemories: "No saved memories yet. Tap Save on any check-in or take one from the map.",
      noResults: "No matching items",
      clearFilters: "Clear filters",
      sayHello: "Start your private conversation",
      activityInbox: "Pair Activity",
      markAllRead: "Mark all read",
      newToday: "NEW · TODAY",
      noEventsYet: "No recent alerts or updates yet.",
      switcherTitle: "Map layers & style",
      switcherSub: "Visual style and live overlays on your map",
      mapTileTheme: "Map Appearance",
      mapTileAuto: "Match App",
      mapTileLight: "Broken White",
      mapTileDark: "Obsidian",
      mapTileTerrain: "Terrain",
      geofenceLayerTitle: "Saved Places Geofences",
      geofenceLayerSub: "{n} active radius rings on map",
      routeLayerTitle: "24h Route & Dwell Trail",
      routeLayerSub: "Breadcrumbs & stop times",
      checkinPinsToggle: "Photo Check-in Pins",
      checkinPinsSub: "Show 24h stories & saved pins",
      hereFor: "Here for {m}",
      movingSpeed: "{k} km/h",
      distanceApart: "{d} away",
      updatedNow: "Updated now",
      settings: "Settings",
      appearance: "Appearance",
      themeLabel: "Theme",
      themeSystem: "System",
      themeLight: "Light",
      themeDark: "Dark",
      language: "Language",
      locationPrivacy: "Location & Privacy",
      pauseSharing: "Quiet Pause mode",
      savedPlaces: "Saved Places",
      savedPlacesBells: "Saved Places & Bells",
      nearbySuggestions: "QUICK SUGGESTIONS",
      pairGeofences: "YOUR PAIR GEOFENCES (TAP BELL TO MUTE)",
      placesTipTitle: "Instant 1-Tap Arrival & Departure Alerts",
      placesTipSub: "Blue bells send private push notices only within your pair.",
      addPlaceBtn: "+ Add place",
      zonesCount: "{n} zones",
      placeName: "Place name (e.g. Studio)",
      radiusM: "Radius (m)",
      addPlace: "Add current location",
      bellOnToast: "Arrival alerts enabled",
      bellOffToast: "Arrival alerts muted",
      bellOnBadge: "Bell ON",
      bellMutedBadge: "Muted",
      bgReliability: "Background Reliability",
      bgGuideText: "Enable Autostart, set Battery Saver to No restrictions, and lock OURS in Recents on Xiaomi / HyperOS.",
      safetySos: "Safety & SOS",
      crashDetection: "Hold 2s SOS & Crash",
      emergencyWhatsapp: "WhatsApp contact",
      pickBtn: "Save",
      activeStatus: "Active",
      triggerSos: "Send SOS Alert",
      securityHeroBadge: "ALL PROTECTIONS ACTIVE",
      securityHeroTitle: "Privacy & Security",
      securityHeroSub: "Private invite link, automatic EXIF removal & origin lock active.",
      securityHeroCta: "Review security details",
      securityModalTitle: "Security Shield",
      serverSection: "Server & Sync",
      testConnection: "Test connection",
      reloadUi: "Reload Latest Web UI (Zero Cache)",
      connected: "Connected",
      unreachable: "Server unreachable",
      signOut: "Sign out & unpair this device",
      photoRequired: "Please select or capture a photo",
      quickReacts: ["Love ya!", "On my way", "Miss u", "Check in"],
      pairLivePill: "Pair · Live",
      pairPausedPill: "Quiet Pause",
      pairSoloPill: "Waiting · Solo",
      justNow: "just now",
      todayLabel: "Today",
      yesterdayLabel: "Yesterday",
      refreshPairStatus: "Check partner status",
      enterYourName: "Please enter your name",
      enterValidInviteCode: "Enter a valid invite code",
      nameLocationRequired: "Place name and location required",
      noSavedPlaces: "No saved places yet",
      nudgeReceived: "{name} nudged you",
      sosReceived: "{name} triggered SOS",
      arrivedAt: "{name} arrived at {place}",
      leftPlace: "{name} left {place}",
      partnerNudged: "Partner sent a nudge",
      sosTriggered: "SOS alert triggered",
      sentReaction: "Sent \"{msg}\"",
      centerMap: "Center map",
      mePin: "Me",
      livePin: "Live",
      radiusUnit: "{m}m radius",
      eventCheckin: "{name} shared a check-in{place}",
      eventMessage: "{name} sent a message",
      eventReply: "{name} replied to a check-in",
      eventHighlight: "{name} saved a memory",
      eventUnhighlight: "{name} removed a saved memory",
      eventPaused: "{name} paused location sharing",
      eventResumed: "{name} resumed location sharing",
      eventAvatar: "{name} updated their photo",
      badgePlace: "Reply",
      badgeChat: "Reply",
      badgeCheckin: "Save",
      badgePrivacy: "View",
      badgeNudge: "Send ♥",
      badgeSos: "SOS",
      stayCloseTitle: "Stay close, quietly.",
      stepProgressLabel: "STEP {n} OF 2 · SIGN IN",
      stepProgressPair: "STEP 2 OF 2 · PAIR SPACE",
      authBadgeLine: "Private invite-only space · Encrypted",
      footerConnLine: "Connected to {host} · End-to-end verified",
      pairedBadge: "Paired",
      peopleTab: "People",
      trail24hTab: "24h Trail",
      sharingLiveShort: "Sharing live",
      quietPauseTitle: "Your location is hidden",
      quietPauseBody: "No coordinates are recorded or sent while paused. Emergency SOS still bursts your exact pin if held.",
      resumeLiveBtn: "Resume live sharing",
      pickPhotoPrompt: "Tap canvas to snap or choose a photo",
      retakePhoto: "Retake photo",
      exifNote: "Camera EXIF location tags are removed automatically",
      sendCheckIn24h: "Send 24h check-in",
      quickCheckInTitle: "Quick photo check-in",
      quickCheckInSub: "Auto-expires in 24h unless saved to Memories",
      arrivalsTab: "Arrivals",
      checkinsTab: "Check-ins",
      reactionsTab: "Reactions",
      hardeningActive: "9 / 9 Hardening Controls Active",
      youLabel: "(You)",
      noStopsYet: "Recent GPS route trail will appear as you move.",
      getPlaceAlertsTitle: "Get {place} arrival alerts",
      getPlaceAlertsSub: "1-tap notification when partner arrives or leaves",
      todayStopsSummary: "Today · {n} route points logged",
      desktopInspTitle: "Saved Highlights & Context",
      desktopRecentMem: "RECENT MEMORIES ({n})",
      desktopLiveSyncTitle: "Live Sync Active",
      desktopLiveLine1: "Private invite-only pair space",
      desktopLiveLine2: "Live location & chat via WebSocket",
      desktopLiveLine3: "Check-ins auto-expire after 24h",
      desktopLiveLine4: "Photo EXIF metadata removed automatically",
      msgBtnShort: "Message",
      alertsBtnShort: "Alerts",
      bubbleBtnShort: "Bubble",
      hoursLeftLabel: "23h left"
    },
    id: {
      appTagline: "Privat berdua",
      onboardingWelcome: "Tetap dekat, dengan tenang.",
      onboardingSub: "Lokasi langsung, check-in foto 24 jam, dan kenangan bersama hanya untuk kalian berdua.",
      continueGoogle: "Lanjutkan dengan Google",
      googleVerified: "Terverifikasi Google",
      googleSignedInAs: "Masuk sebagai",
      googleSwitch: "Ganti",
      googleModalTitle: "Lanjutkan dengan Google",
      googleModalSub: "Masukkan email Google untuk masuk atau memulihkan sesi pasangan Anda.",
      googleEmailRequired: "Email Google yang valid wajib diisi",
      googleCancelled: "Masuk dibatalkan",
      createPair: "Buat pasangan",
      joinPair: "Gabung kode",
      createCta: "Buat Ruang OURS",
      joinCta: "Gabung Ruang OURS",
      spaceNameLabel: "Nama space (opsional)",
      spaceNamePlaceholder: "Nama space (mis. Rumah Kita)",
      yourName: "Nama tampilan",
      displayNameHint: "Ditampilkan ke pasangan di peta & obrolan",
      continueNoEmail: "Lanjut tanpa email",
      quickSetupChip: "Atur space & nama · Tanpa email",
      backBtn: "Kembali",
      waitingSpaceSub: "Space \"{space}\" siap. Bagikan kode undangan privat ini untuk berpasangan.",
      partnerCode: "Kode undangan 6 karakter",
      setupKeyPlaceholder: "Kunci setup server",
      recoveryTitle: "Verifikasi perangkat baru",
      recoverySub: "Akun {email} sudah terdaftar. Verifikasi dengan kode undangan pasangan atau kunci setup server.",
      verifyDeviceBtn: "Verifikasi & masuk",
      waitingTitle: "Menunggu pasangan bergabung",
      waitingSub: "Bagikan kode undangan privat ini ke pasangan Anda.",
      oneTimeCodeLbl: "KODE PASANGAN SATU KALI",
      copyCode: "Salin kode pasangan",
      codeCopied: "Kode undangan disalin",
      alreadySetupHint: "Server ini sudah memiliki pasangan aktif. Masuk dengan email terdaftar atau gabung dengan kode undangan.",
      tabMap: "Peta",
      tabMemories: "Kenangan",
      tabChat: "Obrolan",
      tabSettings: "Pengaturan",
      tabPlaces: "Tempat Tersimpan",
      liveNow: "Aktif",
      pausedLabel: "Dijeda",
      sharingPausedBanner: "Berbagi dijeda",
      sharingPausedSub: "{name} melihat Anda sedang istirahat sejenak",
      privacyBubble: "Gelembung Privasi",
      privacyBubbleSub: "Pasangan melihat area umum Anda alih-alih titik GPS persis.",
      exactGps: "GPS Persis",
      bubbleRadius: "Radius gelembung",
      autoExpireDur: "Durasi otomatis berakhir",
      bubbleSosNote: "Otomatis kembali ke GPS Persis jika SOS 2 detik diaktifkan.",
      activateBubbleCta: "Aktifkan Gelembung {r}",
      approxZoneLbl: "Area ~{r}",
      privacyOnToast: "Gelembung privasi aktif (lokasi dijeda)",
      privacyOffToast: "Berbagi lokasi dilanjutkan",
      offlineLabel: "Luring",
      waitingPartnerLoc: "Menunggu lokasi pasangan",
      nudge: "Kirim Colek",
      nudgeSent: "Colekan terkirim",
      checkIn: "Check in",
      centerPins: "Pusat Pin",
      places: "Tempat",
      route24h: "Rute 24j",
      route24hTitle: "Hari Ini · Rute 24j",
      routePointsToast: "{n} titik rute 24 jam ditampilkan",
      noRouteToast: "Belum ada riwayat rute 24 jam",
      dwellTripsLbl: "RIWAYAT SINGGAH & PERJALANAN",
      dwellAutoPruneNote: "Riwayat lokal 24j bersih otomatis · Tanpa skor berkendara",
      holdSos: "Tahan 2d SOS",
      holdSosLong: "Tahan 2d untuk SOS Darurat",
      holdSosHint: "Tahan selama 2 detik untuk kirim SOS",
      sosSent: "Peringatan SOS terkirim",
      sosTitle: "Kirim Peringatan SOS?",
      sosBody: "Mengirim notifikasi darurat beserta koordinat GPS terbaru ke pasangan.",
      cancel: "Batal",
      confirm: "Konfirmasi",
      close: "Tutup",
      send: "Kirim",
      save: "Simpan",
      saved: "Tersimpan",
      reply: "Balas",
      del: "Hapus",
      noteOptional: "Tambahkan catatan untuk pasangan...",
      placeOptional: "Nama tempat (opsional)",
      typeMessage: "Pesan...",
      replyingTo: "Membalas",
      searchMemoriesPlaceholder: "Cari kenangan, tempat, catatan...",
      searchChatPlaceholder: "Cari pesan...",
      searchPlacesPlaceholder: "Nama tempat di lokasi saat ini...",
      all: "Semua",
      photos: "Foto",
      highlights: "Sorotan",
      timeline: "Linimasa",
      mapPins: "Pin Peta",
      noMemories: "Belum ada kenangan tersimpan. Ketuk Simpan pada check-in atau ambil foto di peta.",
      noResults: "Tidak ada hasil yang cocok",
      clearFilters: "Hapus filter",
      sayHello: "Mulai obrolan privat kalian",
      activityInbox: "Aktivitas Pasangan",
      markAllRead: "Tandai terbaca",
      newToday: "BARU · HARI INI",
      noEventsYet: "Belum ada aktivitas terbaru.",
      switcherTitle: "Lapisan & gaya peta",
      switcherSub: "Gaya visual dan lapisan langsung di peta",
      mapTileTheme: "Tampilan Peta",
      mapTileAuto: "Ikuti Aplikasi",
      mapTileLight: "Broken White",
      mapTileDark: "Obsidian",
      mapTileTerrain: "Terrain",
      geofenceLayerTitle: "Zona Tempat Tersimpan",
      geofenceLayerSub: "{n} lingkaran radius aktif",
      routeLayerTitle: "Jejak & Rute 24 Jam",
      routeLayerSub: "Jejak perjalanan & durasi singgah",
      checkinPinsToggle: "Pin Foto Check-in",
      checkinPinsSub: "Tampilkan cerita 24j & sorotan",
      hereFor: "Di sini {m}",
      movingSpeed: "{k} km/j",
      distanceApart: "{d} jauhnya",
      updatedNow: "Diperbarui sekarang",
      settings: "Pengaturan",
      appearance: "Tampilan",
      themeLabel: "Tema",
      themeSystem: "Sistem",
      themeLight: "Terang",
      themeDark: "Gelap",
      language: "Bahasa",
      locationPrivacy: "Lokasi & Privasi",
      pauseSharing: "Mode Jeda Tenang",
      savedPlaces: "Tempat Tersimpan",
      savedPlacesBells: "Tempat & Lonceng",
      nearbySuggestions: "SARAN CEPAT",
      pairGeofences: "ZONA PASANGAN (KETUK LONCENG UNTUK BISUKAN)",
      placesTipTitle: "Notifikasi Kedatangan & Keberangkatan 1-Ketuk",
      placesTipSub: "Lonceng biru hanya mengirim pengingat privat untuk kalian berdua.",
      addPlaceBtn: "+ Tambah",
      zonesCount: "{n} zona",
      placeName: "Nama tempat (mis. Rumah)",
      radiusM: "Radius (m)",
      addPlace: "Tambah lokasi saat ini",
      bellOnToast: "Notifikasi tempat aktif",
      bellOffToast: "Notifikasi tempat dibisukan",
      bellOnBadge: "Aktif",
      bellMutedBadge: "Bisu",
      bgReliability: "Keandalan Latar Belakang",
      bgGuideText: "Aktifkan Autostart, atur Penghemat Baterai ke Tanpa Pembatasan, dan kunci OURS di Recents Xiaomi / HyperOS.",
      safetySos: "Keamanan & SOS",
      crashDetection: "Tahan 2d SOS & Benturan",
      emergencyWhatsapp: "Kontak WhatsApp",
      pickBtn: "Simpan",
      activeStatus: "Aktif",
      triggerSos: "Kirim Peringatan SOS",
      securityHeroBadge: "SEMUA PROTEKSI AKTIF",
      securityHeroTitle: "Privasi & Keamanan",
      securityHeroSub: "Tautan privat, penghapusan EXIF otomatis & kunci origin aktif.",
      securityHeroCta: "Tinjau detail keamanan",
      securityModalTitle: "Perisai Keamanan",
      serverSection: "Server & Sinkronisasi",
      testConnection: "Uji koneksi",
      reloadUi: "Muat Ulang Web App (Tanpa Cache)",
      connected: "Terhubung",
      unreachable: "Server tidak terjangkau",
      signOut: "Keluar & putuskan perangkat ini",
      photoRequired: "Pilih atau ambil foto terlebih dahulu",
      quickReacts: ["Love ya!", "Otw nih", "Kangen", "Check in"],
      pairLivePill: "Pair · Aktif",
      pairPausedPill: "Jeda Tenang",
      pairSoloPill: "Menunggu · Sendiri",
      justNow: "baru saja",
      todayLabel: "Hari ini",
      yesterdayLabel: "Kemarin",
      refreshPairStatus: "Periksa status pasangan",
      enterYourName: "Masukkan nama Anda",
      enterValidInviteCode: "Masukkan kode undangan yang valid",
      nameLocationRequired: "Nama tempat dan lokasi wajib diisi",
      noSavedPlaces: "Belum ada tempat tersimpan",
      nudgeReceived: "{name} mencolek Anda",
      sosReceived: "{name} mengirim SOS",
      arrivedAt: "{name} tiba di {place}",
      leftPlace: "{name} meninggalkan {place}",
      partnerNudged: "Pasangan mengirim colekan",
      sosTriggered: "Peringatan SOS dikirim",
      sentReaction: "Terkirim \"{msg}\"",
      centerMap: "Pusatkan peta",
      mePin: "Saya",
      livePin: "Aktif",
      radiusUnit: "Radius {m}m",
      eventCheckin: "{name} membagikan check-in{place}",
      eventMessage: "{name} mengirim pesan",
      eventReply: "{name} membalas check-in",
      eventHighlight: "{name} menyimpan kenangan",
      eventUnhighlight: "{name} menghapus kenangan tersimpan",
      eventPaused: "{name} menjeda lokasi",
      eventResumed: "{name} melanjutkan berbagi lokasi",
      eventAvatar: "{name} memperbarui foto profil",
      badgePlace: "Balas",
      badgeChat: "Balas",
      badgeCheckin: "Simpan",
      badgePrivacy: "Lihat",
      badgeNudge: "Kirim ♥",
      badgeSos: "SOS",
      stayCloseTitle: "Tetap dekat, dengan tenang.",
      stepProgressLabel: "LANGKAH {n} DARI 2 · MASUK",
      stepProgressPair: "LANGKAH 2 DARI 2 · PASANGKAN",
      authBadgeLine: "Ruang privat berdua · Terenkripsi",
      footerConnLine: "Terhubung ke {host} · Terverifikasi",
      pairedBadge: "Terhubung",
      peopleTab: "Pasangan",
      trail24hTab: "Rute 24j",
      sharingLiveShort: "Berbagi aktif",
      quietPauseTitle: "Lokasi Anda disembunyikan",
      quietPauseBody: "Koordinat tidak direkam saat jeda. SOS Darurat tetap mengirim titik persis jika ditahan.",
      resumeLiveBtn: "Lanjutkan berbagi lokasi",
      pickPhotoPrompt: "Ketuk area ini untuk ambil atau pilih foto",
      retakePhoto: "Ganti foto",
      exifNote: "Metadata lokasi EXIF dihapus otomatis",
      sendCheckIn24h: "Kirim check-in 24j",
      quickCheckInTitle: "Check-in foto cepat",
      quickCheckInSub: "Hilang otomatis dalam 24j kecuali disimpan ke Kenangan",
      arrivalsTab: "Kedatangan",
      checkinsTab: "Check-in",
      reactionsTab: "Reaksi",
      hardeningActive: "9 / 9 Kontrol Keamanan Aktif",
      youLabel: "(Anda)",
      noStopsYet: "Jejak rute GPS 24 jam akan muncul saat bergerak.",
      getPlaceAlertsTitle: "Aktifkan notif {place}",
      getPlaceAlertsSub: "Pemberitahuan instan saat pasangan tiba atau pergi",
      todayStopsSummary: "Hari ini · {n} titik rute tercatat",
      desktopInspTitle: "Sorotan Tersimpan & Konteks",
      desktopRecentMem: "KENANGAN TERBARU ({n})",
      desktopLiveSyncTitle: "Sinkronisasi Langsung Aktif",
      desktopLiveLine1: "Ruang pasangan privat dengan kode undangan",
      desktopLiveLine2: "Lokasi & obrolan langsung via WebSocket",
      desktopLiveLine3: "Check-in foto kedaluwarsa otomatis 24 jam",
      desktopLiveLine4: "Metadata EXIF foto dihapus otomatis",
      msgBtnShort: "Pesan",
      alertsBtnShort: "Aktivitas",
      bubbleBtnShort: "Gelembung",
      hoursLeftLabel: "23j tersisa"
    }
  };

  const SECURITY_CONTROLS = [
    ["#S1", "Google Auth & Device Hash", "SHA-256 device binding + rate-limited auth"],
    ["#S2", "Strict CSP & Frame Headers", "frame-ancestors none, nosniff, referrer guard"],
    ["#S3", "Zero Inline Script Handlers", "Strict DOM creation, zero eval/unsafe-inline"],
    ["#S4", "WebView Origin Allowlist", "Explicit server origin allowlist & bridge origin-gated"],
    ["#S5", "WebSocket Subprotocol Auth", "Subprotocol Auth · max 8 sockets/user · 64KB frame cap"],
    ["#S6", "EXIF-Stripped Media Cap", "JPEG capped 8192x8192 / EXIF stripped server-side"],
    ["#S7", "Filesystem Mode 0700 / 0600", "Owner-only DB, uploaded media & Android Keystore"],
    ["#S8", "Ephemeral Token & Access Pruning", "Sign-out revokes REST & WS immediately"],
    ["#S9", "Trusted-Proxy Rate & IP Lockout", "Anti-spoof proxy guards & email lockout protection"]
  ];

  const native = typeof window.OursNative !== "undefined" ? window.OursNative : null;

  function sanitizeServerUrl(raw) {
    const val = String(raw || "").trim().replace(/\/+$/, "");
    if (!val) return "";
    if (val.includes("trycloudflare.com")) return "";
    if (val.startsWith("http://") || val.startsWith("https://")) return val;
    return "";
  }

  function resolveDefaultServerUrl() {
    const stored = sanitizeServerUrl(localStorage.getItem("ours_server_url"));
    if (stored) return stored;
    if (window.location && (window.location.protocol === "https:" || window.location.protocol === "http:")) {
      return window.location.origin;
    }
    return PRODUCTION_URL;
  }

  function loadJsonStorage(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (_) {
      return fallback;
    }
  }

  function getOrCreateDeviceId() {
    const existing = localStorage.getItem("ours_device_id");
    if (existing && existing.length >= 8) return existing;
    const created = (typeof crypto !== "undefined" && crypto.randomUUID)
      ? crypto.randomUUID()
      : `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    localStorage.setItem("ours_device_id", created);
    return created;
  }

  const state = {
    tab: "map",
    lang: localStorage.getItem("ours_lang") || "en",
    theme: localStorage.getItem("ours_theme") || "system",
    mapTileStyle: localStorage.getItem("ours_map_style") || "auto",
    serverUrl: resolveDefaultServerUrl(),
    deviceId: getOrCreateDeviceId(),
    accessToken: localStorage.getItem("ours_access_token") || "",
    refreshToken: localStorage.getItem("ours_refresh_token") || "",
    me: null,
    partner: null,
    inviteCode: localStorage.getItem("ours_invite_code") || "",
    spaceName: localStorage.getItem("ours_space_name") || "",
    onboardingMode: "create",
    onbStep: 1,
    googleAuth: null,
    serverConfig: { google_client_id: null, setup_key_required: false, version: "" },
    sharingPaused: false,
    partnerPaused: false,
    meLocation: null,
    partnerLocation: null,
    partnerDwellKey: "",
    partnerDwellSince: 0,
    partnerOffscreen: null,
    partnerFocused: false,
    bubbleRadiusM: 1200,
    bubbleDurationH: 3,
    showRoute24h: false,
    showPlacesLayer: true,
    routePoints: [],
    sheetDetent: "peek", // "collapsed" | "peek" | "expanded"
    sheetTab: "all", // "all" | "people" | "places" | "trail"
    unreadAlerts: 0,
    alertsFilter: "all",
    placeBells: loadJsonStorage("ours_place_bells", {}),
    places: [],
    checkins: [],
    memories: [],
    messages: [],
    events: [],
    memoriesMode: "timeline",
    memoriesFilter: "all",
    memoriesQuery: "",
    selectedMemoryId: null,
    chatFilter: "all",
    chatQuery: "",
    replyToCheckInId: null,
    showCheckinPins: localStorage.getItem("ours_checkin_pins") !== "0",
    crashDetectionEnabled: localStorage.getItem("ours_crash") !== "0",
    emergencyContact: localStorage.getItem("ours_wa") || "",
    serverStatusOk: null,
    serverStatusText: "",
    waitingSocket: null,
    gisLoaded: false
  };

  let mapInstance = null;
  let meMarker = null;
  let partnerMarker = null;
  let placeMarkers = [];
  let checkinMapMarkers = [];
  let mapCurrentThemeKey = null;
  let savedMapCamera = null;
  let mapHasAutoFitPair = false;
  let mapUserMoved = false;
  let sosHoldTimer = null;
  let sosHolding = false;
  let deferredRenderAfterSos = false;

  function resetClientSession() {
    if (state.waitingSocket) {
      try { state.waitingSocket.close(); } catch (_) {}
      state.waitingSocket = null;
    }
    if (mapInstance) {
      try { mapInstance.remove(); } catch (_) {}
      mapInstance = null;
      meMarker = null;
      partnerMarker = null;
      placeMarkers = [];
      checkinMapMarkers = [];
      savedMapCamera = null;
      mapHasAutoFitPair = false;
      mapUserMoved = false;
    }
    for (const objUrl of PHOTO_CACHE.values()) {
      try { URL.revokeObjectURL(objUrl); } catch (_) {}
    }
    PHOTO_CACHE.clear();
    state.accessToken = "";
    state.refreshToken = "";
    state.me = null;
    state.partner = null;
    state.googleAuth = null;
    state.onbStep = 1;
    state.spaceName = "";
    state.inviteCode = "";
    state.meLocation = null;
    state.partnerLocation = null;
    state.partnerFocused = false;
    state.messages = [];
    state.checkins = [];
    state.memories = [];
    state.events = [];
    state.places = [];
    state.routePoints = [];
    state.unreadAlerts = 0;
  }

  function t(key, vars = {}) {
    const dict = STRINGS[state.lang] || STRINGS.en;
    let str = dict[key] !== undefined ? dict[key] : (STRINGS.en[key] ?? key);
    Object.keys(vars).forEach((k) => {
      str = String(str).replaceAll(`{${k}}`, String(vars[k]));
    });
    return str;
  }

  function escapeHtml(str) {
    return String(str ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function apiBase() {
    if (window.location && window.location.protocol !== "file:") {
      return sanitizeServerUrl(state.serverUrl) || window.location.origin;
    }
    return sanitizeServerUrl(state.serverUrl) || PRODUCTION_URL;
  }

  function haptic() {
    try { native?.hapticTick?.(); } catch (_) {}
  }

  function showToast(msg) {
    const root = document.getElementById("toast-root");
    if (!root || !msg) return;
    const el = document.createElement("div");
    el.className = "toast";
    el.textContent = msg;
    root.appendChild(el);
    setTimeout(() => el.remove(), 2600);
  }

  function persistState() {
    localStorage.setItem("ours_lang", state.lang);
    localStorage.setItem("ours_theme", state.theme);
    localStorage.setItem("ours_map_style", state.mapTileStyle);
    localStorage.setItem("ours_server_url", state.serverUrl);
    localStorage.setItem("ours_device_id", state.deviceId);
    localStorage.setItem("ours_access_token", state.accessToken || "");
    localStorage.setItem("ours_refresh_token", state.refreshToken || "");
    localStorage.setItem("ours_invite_code", state.inviteCode || "");
    localStorage.setItem("ours_space_name", state.spaceName || "");
    localStorage.setItem("ours_crash", state.crashDetectionEnabled ? "1" : "0");
    localStorage.setItem("ours_wa", state.emergencyContact || "");
    localStorage.setItem("ours_place_bells", JSON.stringify(state.placeBells || {}));
  }

  function persistAndSyncNative() {
    persistState();
    try {
      if (native?.syncAuth) {
        native.syncAuth(JSON.stringify({
          accessToken: state.accessToken,
          refreshToken: state.refreshToken,
          serverUrl: state.serverUrl,
          deviceId: state.deviceId,
          language: state.lang,
          themeMode: state.theme,
          sharingPaused: state.sharingPaused,
          crashDetectionEnabled: state.crashDetectionEnabled,
          emergencyContact: state.emergencyContact,
          userId: state.me?.id || 0,
          myName: state.me?.name || state.googleAuth?.name || "",
          myEmail: state.me?.email || state.googleAuth?.email || "",
          me: state.me,
          partner: state.partner
        }));
      }
    } catch (_) {}
  }

  function inheritNativeState() {
    try {
      if (!native?.getNativeState) return;
      const raw = native.getNativeState();
      if (!raw) return;
      const n = JSON.parse(raw);
      if (typeof n.safeTopPx === "number" && n.safeTopPx > 0) {
        document.documentElement.style.setProperty("--safe-top", `${n.safeTopPx}px`);
      }
      if (typeof n.safeBottomPx === "number" && n.safeBottomPx > 0) {
        document.documentElement.style.setProperty("--safe-bottom", `${n.safeBottomPx}px`);
      }
      if (n.deviceId && n.deviceId.length >= 8) state.deviceId = n.deviceId;
      if (n.language === "en" || n.language === "id") state.lang = n.language;
      if (n.themeMode === "system" || n.themeMode === "light" || n.themeMode === "dark") state.theme = n.themeMode;
      const cleanNativeUrl = sanitizeServerUrl(n.serverUrl);
      if (cleanNativeUrl) state.serverUrl = cleanNativeUrl;
      if (n.accessToken && n.refreshToken) {
        state.accessToken = n.accessToken;
        state.refreshToken = n.refreshToken;
      }
      if (typeof n.sharingPaused === "boolean") state.sharingPaused = n.sharingPaused;
      if (typeof n.crashDetectionEnabled === "boolean") state.crashDetectionEnabled = n.crashDetectionEnabled;
      if (typeof n.emergencyContact === "string" && n.emergencyContact) state.emergencyContact = n.emergencyContact;
      if (n.myEmail && !state.googleAuth) {
        state.googleAuth = { email: n.myEmail, name: n.myName || "", provider: "google-android" };
      }
      if (typeof n.lat === "number" && typeof n.lon === "number") {
        state.meLocation = {
          lat: n.lat,
          lon: n.lon,
          battery_pct: typeof n.batteryPct === "number" ? n.batteryPct : 100,
          charging: !!n.charging,
          speed_mps: 0,
          recorded_at: Date.now()
        };
      }
      persistState();
    } catch (_) {}
  }

  function resolveIsDark() {
    if (state.theme === "dark") return true;
    if (state.theme === "light") return false;
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  }

  function resolveMapThemeKey() {
    if (state.mapTileStyle === "terrain") return "terrain";
    if (state.mapTileStyle === "dark") return "dark";
    if (state.mapTileStyle === "light") return "light";
    return resolveIsDark() ? "dark" : "light";
  }

  function resolveMapDark() {
    return resolveMapThemeKey() === "dark";
  }

  function applyTheme() {
    document.documentElement.setAttribute("data-theme", state.theme || "system");
  }

  function formatRelative(ts) {
    if (!ts) return "";
    const diffSec = Math.max(0, Math.floor((Date.now() - ts) / 1000));
    if (diffSec < 60) return t("justNow");
    const mins = Math.floor(diffSec / 60);
    if (mins < 60) return `${mins}m`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h`;
    return `${Math.floor(hrs / 24)}d`;
  }

  function formatDwell(ms) {
    const mins = Math.max(1, Math.floor(ms / 60000));
    if (mins < 60) return `${mins}m`;
    const hrs = Math.floor(mins / 60);
    const rem = mins % 60;
    return rem ? `${hrs}h ${rem}m` : `${hrs}h`;
  }

  function formatTime(ts) {
    try {
      return new Date(ts).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    } catch (_) {
      return "";
    }
  }

  function formatDateShort(ts) {
    try {
      return new Date(ts).toLocaleDateString(state.lang === "id" ? "id-ID" : "en-US", { month: "short", day: "numeric" });
    } catch (_) {
      return "";
    }
  }

  function dayLabel(ts) {
    const d = new Date(ts);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) return t("todayLabel");
    const yest = new Date(Date.now() - 86400000);
    if (d.toDateString() === yest.toDateString()) return t("yesterdayLabel");
    return d.toLocaleDateString(state.lang === "id" ? "id-ID" : undefined, { month: "short", day: "numeric" });
  }

  function haversineM(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  function formatDist(meters) {
    if (meters < 1000) return `${Math.round(meters)} m`;
    return `${(meters / 1000).toFixed(1)} km`;
  }

  async function apiFetch(path, options = {}, retried = false) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) headers.set("Authorization", `Bearer ${state.accessToken}`);
    const resp = await fetch(`${apiBase()}${path}`, { ...options, headers });
    if (resp.status === 401 && !retried && state.refreshToken) {
      const ok = await refreshAccessToken();
      if (ok) return apiFetch(path, options, true);
    }
    if (!resp.ok) {
      let msg = `Request failed (${resp.status})`;
      let code = "";
      try {
        const j = await resp.json();
        if (j?.message) msg = j.message;
        if (j?.error) code = j.error;
      } catch (_) {}
      const err = new Error(msg);
      err.status = resp.status;
      err.code = code;
      throw err;
    }
    if (resp.status === 204) return null;
    const ct = resp.headers.get("content-type") || "";
    return ct.includes("application/json") ? resp.json() : resp.text();
  }

  async function refreshAccessToken() {
    try {
      const resp = await fetch(`${apiBase()}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refresh_token: state.refreshToken })
      });
      if (!resp.ok) return false;
      const data = await resp.json();
      if (data?.access_token) {
        state.accessToken = data.access_token;
        if (data.refresh_token) state.refreshToken = data.refresh_token;
        persistAndSyncNative();
        return true;
      }
      return false;
    } catch (_) {
      return false;
    }
  }

  async function loadServerConfig() {
    try {
      const prevSetup = state.serverConfig?.setup_key_required;
      const resp = await fetch(`${apiBase()}/api/config`);
      if (!resp.ok) return;
      const cfg = await resp.json();
      state.serverConfig = {
        google_client_id: cfg.google_client_id || null,
        setup_key_required: !!cfg.setup_key_required,
        version: cfg.version || ""
      };
      if (state.serverConfig.google_client_id && !native) {
        initGoogleGis();
      }
      if (!state.accessToken && prevSetup !== state.serverConfig.setup_key_required) {
        const nameEl = document.getElementById("onb-name");
        const typedName = nameEl ? nameEl.value : "";
        render();
        const nextNameEl = document.getElementById("onb-name");
        if (nextNameEl && typedName) nextNameEl.value = typedName;
      }
    } catch (_) {}
  }

  function initGoogleGis() {
    const cid = state.serverConfig?.google_client_id;
    if (!cid || native) return;
    if (window.google?.accounts?.id) {
      window.google.accounts.id.initialize({
        client_id: cid,
        callback: (response) => {
          if (response?.credential) {
            submitGoogleAuthPayload({ id_token: response.credential, provider: "google-gis" });
          }
        }
      });
      renderGisButtonSlot();
      return;
    }
    if (state.gisLoaded) return;
    state.gisLoaded = true;
    const s = document.createElement("script");
    s.src = "https://accounts.google.com/gsi/client";
    s.async = true;
    s.defer = true;
    s.onload = () => initGoogleGis();
    document.head.appendChild(s);
  }

  function renderGisButtonSlot() {
    const slot = document.getElementById("gis-btn-slot");
    if (!slot || !window.google?.accounts?.id || !state.serverConfig?.google_client_id) return;
    slot.innerHTML = "";
    window.google.accounts.id.renderButton(slot, {
      theme: resolveIsDark() ? "filled_black" : "outline",
      size: "large",
      shape: "pill",
      width: Math.min(360, slot.clientWidth || 320)
    });
  }

  function updatePartnerDwell(loc) {
    if (!loc || loc.paused || loc.lat == null || loc.lon == null) {
      state.partnerDwellKey = "";
      state.partnerDwellSince = 0;
      return;
    }
    const bucketKey = `${loc.lat.toFixed(3)},${loc.lon.toFixed(3)}`;
    if (state.partnerDwellKey !== bucketKey) {
      state.partnerDwellKey = bucketKey;
      state.partnerDwellSince = loc.recorded_at || Date.now();
    }
  }

  async function syncAllData() {
    if (!state.accessToken) return;
    try {
      const [pairState, placesData, checkinsData, memoriesData, messagesData, eventsData] = await Promise.all([
        apiFetch("/api/pair/state"),
        apiFetch("/api/places").catch(() => ({ places: [] })),
        apiFetch("/api/checkins").catch(() => ({ checkins: [] })),
        apiFetch("/api/memories").catch(() => ({ memories: [] })),
        apiFetch("/api/messages").catch(() => ({ messages: [] })),
        apiFetch("/api/events?after=0").catch(() => ({ events: [] }))
      ]);

      if (pairState) {
        state.me = pairState.me;
        state.partner = pairState.partner || null;
        if (pairState.space_name !== undefined) state.spaceName = pairState.space_name || "";
        state.sharingPaused = !!pairState.sharing_paused;
        state.partnerPaused = !!pairState.partner_paused;
        if (pairState.me_location) state.meLocation = pairState.me_location;
        if (pairState.partner_location) {
          state.partnerLocation = pairState.partner_location;
          updatePartnerDwell(pairState.partner_location);
        }
        if (state.partner) state.inviteCode = "";
        persistAndSyncNative();
      }
      state.places = placesData?.places || [];
      state.checkins = checkinsData?.checkins || [];
      state.memories = memoriesData?.items || memoriesData?.memories || [];
      state.messages = messagesData?.items || messagesData?.messages || [];
      state.events = eventsData?.events || [];

      if (state.showRoute24h) {
        await fetchRoute24h(true);
      }
      render();
    } catch (err) {
      if (err.status === 401) {
        state.accessToken = "";
        state.refreshToken = "";
        persistAndSyncNative();
        render();
      }
    }
  }

  async function fetchRoute24h(silent = false) {
    try {
      const since = Date.now() - 24 * 60 * 60 * 1000;
      const data = await apiFetch(`/api/partner/history?since=${since}`);
      state.routePoints = data?.points || [];
      if (!silent) {
        showToast(
          state.routePoints.length
            ? t("routePointsToast", { n: state.routePoints.length })
            : t("noRouteToast")
        );
      }
      updateRouteLayer();
    } catch (_) {
      state.routePoints = [];
    }
  }

  function connectWebSocket() {
    if (!state.accessToken) return;
    if (state.waitingSocket) {
      try { state.waitingSocket.close(); } catch (_) {}
      state.waitingSocket = null;
    }
    const base = apiBase().replace(/^http/, "ws");
    const ws = new WebSocket(`${base}/api/ws`, ["ours.v1", `bearer.${state.accessToken}`]);
    state.waitingSocket = ws;

    ws.onmessage = (e) => {
      try {
        const ev = JSON.parse(e.data);
        const pName = state.partner?.name || "Partner";
        if (ev.type === "location" && ev.data) {
          state.partnerLocation = ev.data;
          updatePartnerDwell(ev.data);
          updateMapMarkers();
          if (state.tab === "map") updatePeekCardValues();
        } else if (ev.type === "nudge") {
          state.unreadAlerts += 1;
          state.events = [{ id: ev.id || Date.now(), type: "nudge", created_at: ev.created_at || Date.now(), data: ev.data || {} }, ...state.events].slice(0, 50);
          showToast(t("nudgeReceived", { name: pName }));
          haptic();
          if (state.tab === "map") {
            triggerNudgeWave();
            render();
          }
        } else if (ev.type === "sos") {
          state.unreadAlerts += 1;
          state.events = [{ id: ev.id || Date.now(), type: "sos", created_at: ev.created_at || Date.now(), data: ev.data || {} }, ...state.events].slice(0, 50);
          showToast(t("sosReceived", { name: pName }));
          haptic();
          render();
        } else if (ev.type === "place" || ev.type === "place_enter" || ev.type === "place_exit") {
          const placeId = ev.data?.place_id;
          const muted = placeId != null && state.placeBells[String(placeId)] === false;
          const isExit = ev.type === "place_exit" || ev.data?.transition === "exit";
          state.events = [{ id: ev.id || Date.now(), type: ev.type, created_at: ev.created_at || Date.now(), data: ev.data || {} }, ...state.events].slice(0, 50);
          if (!muted) {
            state.unreadAlerts += 1;
            const pname = ev.data?.place_name || t("places");
            showToast(t(isExit ? "leftPlace" : "arrivedAt", { name: pName, place: pname }));
          }
          if (state.tab === "map") render();
        } else if (ev.type === "chat_message" && ev.data) {
          const msg = ev.data.message || ev.data;
          if (msg && msg.id && !state.messages.some((m) => m.id === msg.id)) {
            state.messages = [msg, ...state.messages];
          }
          state.events = [{ id: ev.id || Date.now(), type: "chat_message", created_at: ev.created_at || Date.now(), data: ev.data || {} }, ...state.events].slice(0, 50);
          if (state.tab === "chat") render();
        } else {
          syncAllData();
        }
      } catch (_) {}
    };

    ws.onclose = () => {
      if (state.waitingSocket === ws && state.accessToken) {
        setTimeout(connectWebSocket, 3500);
      }
    };
  }

  function startBrowserLocationWatch() {
    if (native || !navigator.geolocation || !state.accessToken) return;
    navigator.geolocation.watchPosition(
      (pos) => {
        const pt = {
          lat: pos.coords.latitude,
          lon: pos.coords.longitude,
          accuracy_m: pos.coords.accuracy || 10,
          speed_mps: pos.coords.speed != null && pos.coords.speed >= 0 ? pos.coords.speed : 0,
          bearing: pos.coords.heading != null && pos.coords.heading >= 0 && pos.coords.heading < 360 ? pos.coords.heading : null,
          activity: null,
          battery_pct: 100,
          charging: true,
          recorded_at: Date.now()
        };
        state.meLocation = pt;
        updateMapMarkers();
        if (!state.sharingPaused) {
          apiFetch("/api/locations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify([pt])
          }).catch(() => {});
        }
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 15000, timeout: 15000 }
    );
  }

  async function compressJpeg(file, maxDim = 1280, quality = 0.82) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(file);
      img.onload = () => {
        URL.revokeObjectURL(url);
        let w = img.width;
        let h = img.height;
        if (Math.max(w, h) > maxDim) {
          const r = maxDim / Math.max(w, h);
          w = Math.round(w * r);
          h = Math.round(h * r);
        }
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, w, h);
        canvas.toBlob((blob) => {
          if (blob) resolve(blob);
          else reject(new Error("Compression failed"));
        }, "image/jpeg", quality);
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error("Invalid image"));
      };
      img.src = url;
    });
  }

  async function hydratePhotos() {
    const imgs = document.querySelectorAll("img[data-photo-id]");
    for (const img of imgs) {
      const pid = img.getAttribute("data-photo-id");
      const size = img.getAttribute("data-photo-size") || "full";
      if (!pid || !state.accessToken) continue;
      const cacheKey = `${pid}:${size}`;
      if (PHOTO_CACHE.has(cacheKey)) {
        img.src = PHOTO_CACHE.get(cacheKey);
        img.classList.add("photo-loaded");
        continue;
      }
      try {
        const resp = await fetch(`${apiBase()}/api/photos/${encodeURIComponent(pid)}?size=${encodeURIComponent(size)}`, {
          headers: { Authorization: `Bearer ${state.accessToken}` }
        });
        if (!resp.ok) continue;
        const blob = await resp.blob();
        const objUrl = URL.createObjectURL(blob);
        PHOTO_CACHE.set(cacheKey, objUrl);
        img.onload = () => img.classList.add("photo-loaded");
        img.src = objUrl;
        if (img.complete) img.classList.add("photo-loaded");
      } catch (_) {}
    }
  }

  /* FigCraft Sliced Vectors & Illustrations */
  function oursRingsLogo(size = 44) {
    return `
      <svg viewBox="0 0 100 100" width="${size}" height="${size}" aria-hidden="true">
        <circle cx="39" cy="50" r="18" fill="none" stroke="var(--ink)" stroke-width="6.5" />
        <circle cx="61" cy="50" r="18" fill="none" stroke="var(--action)" stroke-width="6.5" />
        <path d="M42.7 32.4C48.122 33.552 52.711 37.137 55.14 42.12" fill="none" stroke="var(--canvas)" stroke-width="10.5" stroke-linecap="butt" />
        <path d="M42.7 32.4C48.122 33.552 52.711 37.137 55.14 42.12" fill="none" stroke="var(--ink)" stroke-width="6.5" stroke-linecap="round" />
        <path d="M57.24 67.62C51.818 66.468 47.229 62.883 44.8 57.9" fill="none" stroke="var(--canvas)" stroke-width="10.5" stroke-linecap="butt" />
        <path d="M57.24 67.62C51.818 66.468 47.229 62.883 44.8 57.9" fill="none" stroke="var(--action)" stroke-width="6.5" stroke-linecap="round" />
      </svg>
    `;
  }

  function figcraftEmptyArt(kind) {
    if (kind === "memories") {
      return `
        <svg viewBox="0 0 200 104" width="168" height="88" style="display:block;margin:0 auto 10px;" aria-hidden="true">
          <rect x="0" y="0" width="200" height="104" rx="12" fill="var(--surface-muted)" />
          <g transform="rotate(8 96 49)"><rect x="70" y="21" width="52" height="58" rx="10" fill="var(--line)" /></g>
          <rect x="84" y="22" width="54" height="60" rx="10" fill="var(--surface)" stroke="var(--ink-muted)" stroke-width="1.5" />
          <circle cx="111" cy="45" r="7" fill="var(--action)" />
        </svg>
      `;
    }
    if (kind === "checkinPreview") {
      return `
        <svg viewBox="0 0 314 174" preserveAspectRatio="xMidYMid slice" class="ci-art-placeholder" aria-hidden="true">
          <defs>
            <linearGradient id="fcSky" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#DCE8FC" />
              <stop offset="65%" stop-color="#A5C4FA" />
              <stop offset="100%" stop-color="#6EA2F8" />
            </linearGradient>
          </defs>
          <rect width="314" height="174" fill="url(#fcSky)" />
          <path d="M87 13H118C142.3 13 162 32.7 162 57V138H43V57C43 32.7 62.7 13 87 13Z" fill="rgba(255,255,252,0.72)" />
          <rect x="0" y="118" width="314" height="59" fill="#F0EFE9" />
          <ellipse cx="175" cy="60" rx="34" ry="6" fill="rgba(255,255,255,0.85)" />
          <rect x="141" y="59" width="69" height="73" rx="14" fill="rgba(255,255,252,0.90)" stroke="rgba(255,255,255,0.95)" stroke-width="2" />
          <rect x="177" y="30" width="5" height="45" rx="2.5" fill="#0055FF" />
        </svg>
      `;
    }
    return `
      <svg viewBox="0 0 200 104" width="168" height="88" style="display:block;margin:0 auto 10px;" aria-hidden="true">
        <rect x="0" y="0" width="200" height="104" rx="12" fill="var(--surface-muted)" />
        <rect x="56" y="26" width="62" height="26" rx="13" fill="var(--surface)" stroke="var(--line)" stroke-width="1.5" />
        <rect x="82" y="56" width="62" height="26" rx="13" fill="var(--action-soft)" stroke="var(--action)" stroke-opacity="0.3" stroke-width="1.5" />
        <circle cx="112" cy="69" r="4" fill="var(--action)" />
      </svg>
    `;
  }

  function googleGIcon() {
    return `<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
      <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"/>
      <path fill="#34A853" d="M12 24c3.3 0 6.08-1.09 8.1-2.96l-3.88-3.05c-1.1.74-2.5 1.18-4.22 1.18-3.24 0-5.98-2.18-6.96-5.12H1.04v3.14C3.05 21.2 7.21 24 12 24Z"/>
      <path fill="#FBBC05" d="M5.04 14.05c-.25-.74-.39-1.54-.39-2.35s.14-1.61.39-2.35V6.21H1.04C.38 7.53 0 9.02 0 11.7s.38 4.17 1.04 5.49l4-3.14Z"/>
      <path fill="#EA4335" d="M12 4.75c1.8 0 3.41.62 4.68 1.84l3.51-3.51C18.08 1.19 15.3 0 12 0 7.21 0 3.05 2.8 1.04 6.21l4 3.14c.98-2.94 3.72-5.12 6.96-5.12Z"/>
    </svg>`;
  }

  function svgIcon(name) {
    const map = {
      map: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 10c0 5.5-8 11.5-8 11.5S4 15.5 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.8" fill="currentColor" stroke="none"/></svg>`,
      memories: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="4" width="16" height="16" rx="4"/><circle cx="9.5" cy="9.5" r="1.5"/><path d="M20 15l-4.5-4.5L5 20"/></svg>`,
      chat: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="14" rx="7"/><path d="M5 17l-2 4 5-2"/></svg>`,
      settings: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.9"><circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.26.6.85 1 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`,
      bell: `<svg viewBox="0 0 20 20" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"><path d="M3 14.5H17L15 11.5V6.8C15 3.9 12.9 2 10 2C7.1 2 5 3.9 5 6.8V11.5L3 14.5Z"/><path d="M8.3 17a1.8 1.8 0 0 0 3.4 0"/></svg>`,
      bellOff: `<svg viewBox="0 0 20 20" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 14.5H17L15 11.5V6.8C15 3.9 12.9 2 10 2C7.1 2 5 3.9 5 6.8V11.5L3 14.5Z"/><line x1="2.5" y1="2.5" x2="17.5" y2="17.5"/></svg>`,
      camera: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="6" width="18" height="13" rx="4"/><circle cx="12" cy="12.5" r="3.4" fill="var(--action)" stroke="none"/></svg>`,
      layers: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"><path d="M12 4L3 9L12 14L21 9L12 4Z"/><path d="M3 15L12 20L21 15"/></svg>`,
      recenter: `<svg viewBox="0 0 18 18" width="17" height="17" fill="var(--action)"><path d="M2.5 9.5L15.5 2.5L10.5 15.5L8.5 10.5L2.5 9.5Z"/></svg>`,
      route: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="6" cy="18" r="2.6"/><path d="M8.6 18h7.8a3.2 3.2 0 0 0 0-6.4H7.6a3.2 3.2 0 0 1 0-6.4H15"/><circle cx="18" cy="5.2" r="2.6"/></svg>`,
      bubble: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3" fill="currentColor" stroke="none"/></svg>`,
      search: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7.5"/><line x1="20.5" y1="20.5" x2="16.5" y2="16.5"/></svg>`,
      pin: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`,
      home: `<svg viewBox="0 0 18 19" width="17" height="17" fill="currentColor"><path d="M0 8L9 0L18 8V19H12V12H6V19H0V8Z"/></svg>`,
      briefcase: `<svg viewBox="0 0 20 20" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5.5" width="16" height="11.5" rx="3"/><path d="M7 5.5V4a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 13 4v1.5"/></svg>`,
      shield: `<svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L20 5.5V12C20 17.5 12 22 12 22C12 22 4 17.5 4 12V5.5L12 2Z" fill="var(--success-soft)"/><circle cx="12" cy="12" r="3" fill="var(--success)" stroke="none"/></svg>`,
      sendArrow: `<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>`,
      heart: `<svg viewBox="0 0 20 20" width="15" height="15" fill="currentColor"><path d="M10 17.2C10 17.2 2.5 12.2 2.5 6.8A4.2 4.2 0 0 1 10 4.3A4.2 4.2 0 0 1 17.5 6.8C17.5 12.2 10 17.2 10 17.2Z"/></svg>`
    };
    return map[name] || "";
  }

  function placeCategoryIcon(name) {
    const lower = String(name || "").toLowerCase();
    if (lower.includes("home") || lower.includes("rumah")) return svgIcon("home");
    if (lower.includes("studio") || lower.includes("work") || lower.includes("office") || lower.includes("kantor") || lower.includes("gym")) return svgIcon("briefcase");
    return svgIcon("pin");
  }

  async function completePairAuthSuccess({ tokens, user, partner = null, inviteCode = "", spaceName = "" }) {
    state.accessToken = tokens.access_token;
    state.refreshToken = tokens.refresh_token;
    state.me = user;
    state.partner = partner || null;
    state.inviteCode = inviteCode || "";
    if (spaceName) state.spaceName = spaceName;
    persistAndSyncNative();
    closeModal();
    haptic();
    if (state.partner || !state.inviteCode) {
      await syncAllData();
      connectWebSocket();
      startBrowserLocationWatch();
    } else {
      connectWebSocket();
    }
    render();
  }

  async function submitDirectPair({ mode, name, spaceName, code, setupKey }) {
    const cleanName = (name || "").trim();
    if (!cleanName) throw new Error(t("enterYourName"));
    const headers = { "Content-Type": "application/json" };
    if (setupKey) headers["X-Setup-Key"] = setupKey;

    if (mode === "create") {
      const resp = await fetch(`${apiBase()}/api/pair/create`, {
        method: "POST",
        headers,
        body: JSON.stringify({
          name: cleanName,
          email: null,
          space_name: (spaceName || "").trim() || null
        })
      });
      const data = await resp.json().catch(() => ({}));
      if (!resp.ok) {
        if (resp.status === 409) {
          state.onboardingMode = "join";
          showToast(t("alreadySetupHint"));
          render();
          return;
        }
        throw new Error(data?.message || `Setup failed (${resp.status})`);
      }
      await completePairAuthSuccess({
        tokens: data.tokens,
        user: data.user,
        partner: null,
        inviteCode: data.invite_code || "",
        spaceName: data.space_name || spaceName || ""
      });
      return;
    }

    const cleanCode = (code || "").trim().toUpperCase();
    const resp = await fetch(`${apiBase()}/api/pair/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: cleanName, code: cleanCode, email: null })
    });
    const data = await resp.json().catch(() => ({}));
    if (!resp.ok) throw new Error(data?.message || `Join failed (${resp.status})`);
    await completePairAuthSuccess({
      tokens: data.tokens,
      user: data.user,
      partner: null,
      inviteCode: "",
      spaceName: data.space_name || ""
    });
  }

  async function submitGoogleAuthPayload(payload) {
    const email = (payload?.email || state.googleAuth?.email || "").trim();
    const idToken = payload?.id_token || state.googleAuth?.id_token || null;
    if (!email && !idToken) {
      state.onbStep = 2;
      render();
      return;
    }

    const reqBody = {
      email,
      name: (payload?.name || state.googleAuth?.name || "").trim() || null,
      space_name: (payload?.space_name || state.spaceName || "").trim() || null,
      id_token: idToken,
      device_id: state.deviceId || null,
      invite_code: payload?.invite_code || null,
      setup_key: payload?.setup_key || null,
      auto_create: !!payload?.auto_create
    };

    try {
      const resp = await fetch(`${apiBase()}/api/auth/google`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(reqBody)
      });
      const data = await resp.json().catch(() => ({}));
      if (!resp.ok) {
        if (resp.status === 401 && email) {
          state.googleAuth = {
            email,
            name: reqBody.name || "",
            id_token: idToken,
            provider: payload?.provider || "google"
          };
          openAccountRecoveryModal(email, data?.message || "");
          return;
        }
        if (resp.status === 409) {
          state.onboardingMode = "join";
          state.onbStep = 2;
          showToast(t("alreadySetupHint"));
          render();
          return;
        }
        throw new Error(data?.message || `Auth failed (${resp.status})`);
      }

      if (data.status === "authenticated" && data.tokens) {
        state.googleAuth = {
          email: data.email || data.user?.email || email,
          name: data.user?.name || reqBody.name || "",
          id_token: idToken,
          provider: payload?.provider || "google"
        };
        await completePairAuthSuccess({
          tokens: data.tokens,
          user: data.user,
          partner: data.partner || null,
          inviteCode: data.invite_code || "",
          spaceName: data.space_name || reqBody.space_name || ""
        });
        return;
      }

      state.googleAuth = {
        email: data.email || email,
        name: data.name || reqBody.name || "",
        id_token: idToken,
        provider: payload?.provider || "google"
      };
      state.onbStep = 2;
      closeModal();
      render();
    } catch (err) {
      showToast(err.message || "Google sign-in failed");
    }
  }

  function openAccountRecoveryModal(email, serverMsg) {
    openModal(`
      <div style="font-size:18px;font-weight:700;">${escapeHtml(t("recoveryTitle"))}</div>
      <div class="row-sub">${escapeHtml(t("recoverySub", { email }))}</div>
      ${serverMsg ? `<div class="chip chip-paused">${escapeHtml(serverMsg)}</div>` : ""}
      <input class="input" id="rec-invite" maxlength="6" placeholder="${escapeHtml(t("partnerCode"))}" />
      <input class="input" id="rec-setup-key" type="password" placeholder="${escapeHtml(t("setupKeyPlaceholder"))}" />
      <div style="display:flex;gap:8px;">
        <button class="btn btn-secondary" style="flex:1;" id="rec-cancel">${escapeHtml(t("cancel"))}</button>
        <button class="btn btn-primary" style="flex:1;" id="rec-submit">${escapeHtml(t("verifyDeviceBtn"))}</button>
      </div>
    `, true);
    document.getElementById("rec-cancel").onclick = closeModal;
    document.getElementById("rec-submit").onclick = async () => {
      const inviteCode = document.getElementById("rec-invite").value.trim().toUpperCase();
      const setupKey = document.getElementById("rec-setup-key").value.trim();
      if (!inviteCode && !setupKey) {
        return showToast("Enter your pair invite code or server setup key");
      }
      await submitGoogleAuthPayload({
        email,
        name: state.googleAuth?.name || "",
        id_token: state.googleAuth?.id_token || null,
        invite_code: inviteCode || null,
        setup_key: setupKey || null,
        provider: "google-recovery"
      });
    };
  }

  function proceedToSpaceSetupStep() {
    state.onbStep = 2;
    render();
    const targetInput = document.getElementById(state.onboardingMode === "create" ? "onb-space" : "onb-name");
    if (targetInput) targetInput.focus();
  }

  function triggerGoogleSignIn() {
    haptic();
    if (native?.signInWithGoogle) {
      native.signInWithGoogle();
      return;
    }
    if (window.google?.accounts?.id && state.serverConfig?.google_client_id) {
      window.google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          proceedToSpaceSetupStep();
        }
      });
      return;
    }
    proceedToSpaceSetupStep();
  }

  window.onOursGoogleAuth = async (payload) => {
    await submitGoogleAuthPayload(payload);
  };

  window.onOursGoogleAuthFallback = (reason) => {
    if (reason === "cancelled") {
      showToast(t("googleCancelled"));
      return;
    }
    proceedToSpaceSetupStep();
  };

  function renderOnboarding() {
    const app = document.getElementById("app");
    app.className = "app-shell";

    if (state.inviteCode && state.accessToken) {
      const gMail = state.me?.email || state.googleAuth?.email || "";
      const gInit = (state.me?.name || state.googleAuth?.name || "O").slice(0, 1).toUpperCase();
      app.innerHTML = `
        <div class="ambient-orbs" aria-hidden="true">
          <span class="ambient-orb orb-top"></span>
          <span class="ambient-orb orb-mid"></span>
          <span class="ambient-orb orb-bot"></span>
        </div>
        <main class="screen onboarding-wrap" style="background:transparent;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
            <div class="onb-seg-progress">
              <span class="onb-seg-step active"></span>
              <span class="onb-seg-step active"></span>
            </div>
            <div class="segmented" style="width:82px;height:28px;border-radius:14px;">
              <button class="seg-btn ${state.lang === "en" ? "active" : ""}" id="onb-lang-en" style="min-height:22px;font-size:11px;padding:0;">EN</button>
              <button class="seg-btn ${state.lang === "id" ? "active" : ""}" id="onb-lang-id" style="min-height:22px;font-size:11px;padding:0;">ID</button>
            </div>
          </div>

          ${
            gMail
              ? `<div class="google-pill">
                  <span class="google-pill-av">${escapeHtml(gInit)}</span>
                  <div class="google-pill-meta">
                    <span class="google-pill-email">${escapeHtml(gMail)}</span>
                  </div>
                  <button class="google-switch-btn" id="btn-reset-onboard">${escapeHtml(t("signOut"))}</button>
                </div>`
              : ""
          }

          <div style="display:flex;align-items:center;gap:14px;">
            <div class="onb-squircle">${oursRingsLogo(40)}</div>
            <div>
              <h1 style="margin:0;font-size:26px;font-weight:800;letter-spacing:-0.03em;">${escapeHtml(state.spaceName || t("waitingTitle"))}</h1>
              <div class="row-sub">${escapeHtml(state.spaceName ? t("waitingSpaceSub", { space: state.spaceName }) : t("waitingSub"))}</div>
            </div>
          </div>

          <div class="card" style="display:flex;flex-direction:column;gap:14px;">
            <div class="section-label" style="padding:0;">${escapeHtml(t("oneTimeCodeLbl"))}</div>
            <div class="invite-code-box tnum">${escapeHtml(state.inviteCode)}</div>
            <div class="waiting-pulse-row">
              <span class="waiting-pulse-dot"></span>
              <span>${escapeHtml(t("waitingTitle"))}</span>
            </div>
            <button class="btn btn-secondary" id="btn-copy-code">${escapeHtml(t("copyCode"))}</button>
            <button class="btn btn-primary" id="btn-check-partner">${escapeHtml(t("refreshPairStatus"))}</button>
          </div>
          ${!gMail ? `<button class="btn btn-danger" id="btn-reset-onboard">${escapeHtml(t("signOut"))}</button>` : ""}
        </main>
      `;
      document.getElementById("onb-lang-en").onclick = () => { state.lang = "en"; persistAndSyncNative(); render(); };
      document.getElementById("onb-lang-id").onclick = () => { state.lang = "id"; persistAndSyncNative(); render(); };
      document.getElementById("btn-copy-code").onclick = async () => {
        try { await navigator.clipboard.writeText(state.inviteCode); } catch (_) {}
        haptic();
        showToast(t("codeCopied"));
      };
      document.getElementById("btn-check-partner").onclick = syncAllData;
      const rstBtn = document.getElementById("btn-reset-onboard");
      if (rstBtn) {
        rstBtn.onclick = () => {
          resetClientSession();
          persistAndSyncNative();
          render();
        };
      }
      return;
    }

    const gEmail = state.googleAuth?.email || "";
    const gName = state.googleAuth?.name || "";
    const gInit = (gName || gEmail || "O").slice(0, 1).toUpperCase();
    const needsSetupKey = !!state.serverConfig?.setup_key_required;
    const isStep2 = state.onbStep === 2 || !!gEmail;

    app.innerHTML = `
      <div class="ambient-orbs" aria-hidden="true">
        <span class="ambient-orb orb-top"></span>
        <span class="ambient-orb orb-mid"></span>
        <span class="ambient-orb orb-bot"></span>
      </div>
      <main class="screen onboarding-wrap" style="background:transparent;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
          <div class="onb-seg-progress">
            <span class="onb-seg-step active"></span>
            <span class="onb-seg-step ${isStep2 ? "active" : ""}"></span>
          </div>
          <div class="segmented" style="width:82px;height:28px;border-radius:14px;">
            <button class="seg-btn ${state.lang === "en" ? "active" : ""}" id="onb-lang-en" style="min-height:22px;font-size:11px;padding:0;">EN</button>
            <button class="seg-btn ${state.lang === "id" ? "active" : ""}" id="onb-lang-id" style="min-height:22px;font-size:11px;padding:0;">ID</button>
          </div>
        </div>

        <div style="display:flex;align-items:center;gap:12px;">
          <div class="onb-squircle">${oursRingsLogo(40)}</div>
          <span class="chip chip-action" style="font-size:11px;letter-spacing:0.06em;">
            ${escapeHtml(isStep2 ? t("stepProgressPair") : t("stepProgressLabel", { n: 1 }))}
          </span>
        </div>

        <div>
          <h1 style="margin:0 0 4px;font-size:28px;font-weight:800;letter-spacing:-0.03em;">${escapeHtml(t("stayCloseTitle"))}</h1>
          <div class="row-sub">${escapeHtml(t("onboardingSub"))}</div>
        </div>

        <div class="card" style="display:flex;flex-direction:column;gap:13px;border-radius:26px;padding:18px;">
          <div style="display:flex;align-items:center;gap:8px;padding:9px 12px;border-radius:16px;background:var(--action-soft-grad);border:1px solid var(--border-glass);">
            <span class="live-dot"></span>
            <span style="font-size:12px;font-weight:700;color:var(--ink);">${escapeHtml(t("authBadgeLine"))}</span>
          </div>
          ${
            !isStep2
              ? `<button class="btn-google" id="btn-google-auth">${googleGIcon()}<span>${escapeHtml(t("continueGoogle"))}</span></button>
                 <div id="gis-btn-slot" style="display:flex;justify-content:center;"></div>
                 <button type="button" class="btn btn-secondary" id="btn-onb-no-email" style="min-height:42px;font-size:13px;">${escapeHtml(t("continueNoEmail"))}</button>`
              : `${
                  gEmail
                    ? `<div class="google-pill">
                        <span class="google-pill-av">${escapeHtml(gInit)}</span>
                        <div class="google-pill-meta">
                          <span class="google-verified-badge">${escapeHtml(t("googleVerified"))}</span>
                          <span class="google-pill-email">${escapeHtml(gEmail)}</span>
                        </div>
                        <button class="google-switch-btn" id="btn-google-switch">${escapeHtml(t("googleSwitch"))}</button>
                      </div>`
                    : `<div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:6px 10px;border-radius:14px;background:var(--surface-muted);">
                        <span class="row-sub" style="font-size:12px;font-weight:600;">${escapeHtml(t("quickSetupChip"))}</span>
                        <button type="button" class="google-switch-btn" id="btn-onb-back">${escapeHtml(t("backBtn"))}</button>
                      </div>`
                }
                <div class="segmented" style="height:40px;border-radius:20px;">
                  <button class="seg-btn ${state.onboardingMode === "create" ? "active-blue" : ""}" style="border-radius:16px;" id="seg-create">${escapeHtml(t("createPair"))}</button>
                  <button class="seg-btn ${state.onboardingMode === "join" ? "active-blue" : ""}" style="border-radius:16px;" id="seg-join">${escapeHtml(t("joinPair"))}</button>
                </div>
                ${
                  state.onboardingMode === "create"
                    ? `<label class="sr-only" for="onb-space">${escapeHtml(t("spaceNamePlaceholder"))}</label>
                       <input class="input" id="onb-space" maxlength="40" placeholder="${escapeHtml(t("spaceNamePlaceholder"))}" value="${escapeHtml(state.spaceName || "")}" />`
                    : ""
                }
                <label class="sr-only" for="onb-name">${escapeHtml(t("yourName"))}</label>
                <input class="input" id="onb-name" maxlength="40" placeholder="${escapeHtml(t("yourName"))}" value="${escapeHtml(gName)}" />
                ${
                  state.onboardingMode === "join"
                    ? `<label class="sr-only" for="onb-code">${escapeHtml(t("partnerCode"))}</label>
                       <input class="input" id="onb-code" maxlength="6" placeholder="${escapeHtml(t("partnerCode"))}" />`
                    : needsSetupKey
                    ? `<label class="sr-only" for="onb-setup-key">${escapeHtml(t("setupKeyPlaceholder"))}</label>
                       <input class="input" id="onb-setup-key" type="password" placeholder="${escapeHtml(t("setupKeyPlaceholder"))}" />`
                    : ""
                }
                <button class="btn btn-primary" id="btn-onb-submit">
                  ${escapeHtml(state.onboardingMode === "create" ? t("createCta") : t("joinCta"))}
                </button>`
          }
          <div style="border-top:1px solid var(--line);padding-top:10px;font-size:11px;color:var(--ink-muted);">
            ${escapeHtml(t("footerConnLine", { host: apiBase().replace(/^https?:\/\//, "") }))}
          </div>
        </div>
      </main>
    `;

    document.getElementById("onb-lang-en").onclick = () => { state.lang = "en"; persistAndSyncNative(); render(); };
    document.getElementById("onb-lang-id").onclick = () => { state.lang = "id"; persistAndSyncNative(); render(); };

    const gBtn = document.getElementById("btn-google-auth");
    if (gBtn) gBtn.onclick = triggerGoogleSignIn;
    const gSw = document.getElementById("btn-google-switch");
    if (gSw) gSw.onclick = triggerGoogleSignIn;
    const skipBtn = document.getElementById("btn-onb-no-email");
    if (skipBtn) skipBtn.onclick = proceedToSpaceSetupStep;
    const backBtn = document.getElementById("btn-onb-back");
    if (backBtn) backBtn.onclick = () => { state.onbStep = 1; render(); };
    renderGisButtonSlot();

    const segC = document.getElementById("seg-create");
    const segJ = document.getElementById("seg-join");
    if (segC) segC.onclick = () => { state.onboardingMode = "create"; render(); };
    if (segJ) segJ.onclick = () => { state.onboardingMode = "join"; render(); };

    const subBtn = document.getElementById("btn-onb-submit");
    if (subBtn) {
      subBtn.onclick = async () => {
        const name = document.getElementById("onb-name")?.value.trim() || gName;
        const spaceName = document.getElementById("onb-space")?.value.trim() || "";
        if (!name) return showToast(t("enterYourName"));

        subBtn.disabled = true;
        try {
          const setupKeyInput = document.getElementById("onb-setup-key")?.value?.trim() || null;
          const code = document.getElementById("onb-code")?.value?.trim()?.toUpperCase();
          if (state.onboardingMode === "join" && (!code || code.length < 4)) {
            subBtn.disabled = false;
            return showToast(t("enterValidInviteCode"));
          }

          if (gEmail) {
            if (state.onboardingMode === "create") {
              await submitGoogleAuthPayload({
                email: gEmail,
                name,
                space_name: spaceName || null,
                id_token: state.googleAuth?.id_token || null,
                setup_key: setupKeyInput,
                auto_create: true,
                provider: state.googleAuth?.provider || "google"
              });
            } else {
              await submitGoogleAuthPayload({
                email: gEmail,
                name,
                id_token: state.googleAuth?.id_token || null,
                invite_code: code,
                provider: state.googleAuth?.provider || "google"
              });
            }
          } else {
            await submitDirectPair({
              mode: state.onboardingMode,
              name,
              spaceName,
              code,
              setupKey: setupKeyInput
            });
          }
        } catch (err) {
          subBtn.disabled = false;
          showToast(err.message);
        }
      };
    }
  }

  function partnerStatusSummary() {
    const loc = state.partnerLocation;
    const paused = state.partnerPaused || loc?.paused;
    if (!state.partner) return { label: t("waitingTitle"), chipClass: "chip-action", dotClass: "action", live: false };
    if (paused) return { label: t("pausedLabel"), chipClass: "chip-paused", dotClass: "paused", live: false };
    if (!loc || loc.lat == null) return { label: t("offlineLabel"), chipClass: "chip-paused", dotClass: "paused", live: false };
    const staleMs = Date.now() - (loc.recorded_at || 0);
    const live = staleMs < 5 * 60 * 1000;
    return {
      label: live ? `${t("liveNow")} · ${formatRelative(loc.recorded_at)}` : formatRelative(loc.recorded_at),
      chipClass: live ? "chip-success" : "chip-paused",
      dotClass: live ? "" : "paused",
      live
    };
  }

  function partnerCurrentPlace() {
    const loc = state.partnerLocation;
    if (!loc?.lat || !state.places.length) return null;
    for (const pl of state.places) {
      const dist = haversineM(loc.lat, loc.lon, pl.lat, pl.lon);
      if (dist <= (Number(pl.radius_m) || 150) * 1.15) return pl;
    }
    return null;
  }

  function renderMapOverlayHtml() {
    const st = partnerStatusSummary();
    const loc = state.partnerLocation;
    const pName = state.partner?.name || "Partner";
    const pInit = pName.slice(0, 1).toUpperCase();
    const mName = state.me?.name || t("mePin");
    const mInit = mName.slice(0, 1).toUpperCase();
    const matchedPlace = partnerCurrentPlace();
    const hasDist = !!(state.meLocation?.lat != null && loc?.lat != null && !loc?.paused && !state.partnerPaused);
    const distTxt = hasDist ? formatDist(haversineM(state.meLocation.lat, state.meLocation.lon, loc.lat, loc.lon)) : "";
    const batTxt = loc?.battery_pct != null ? `${loc.battery_pct}%${loc.charging ? "⚡" : ""}` : "100%";
    const spdKmh = loc?.speed_mps > 0.6 ? Math.round(loc.speed_mps * 3.6) : 0;
    const dwellTxt = state.partnerDwellSince && spdKmh === 0 ? t("hereFor", { m: formatDwell(Date.now() - state.partnerDwellSince) }) : "";
    const switcherTxt = !state.partner
      ? t("pairSoloPill")
      : (state.sharingPaused ? t("pairPausedPill") : t("pairLivePill"));

    const detent = state.sheetDetent;
    const offscreen = state.partnerOffscreen;

    const partnerIsPaused = !!(state.partnerPaused || loc?.paused);
    const statusLine = partnerIsPaused
      ? st.label
      : matchedPlace
        ? `At ${matchedPlace.name}${dwellTxt ? ` · ${dwellTxt}` : ""}`
        : (loc?.lat != null ? st.label : t("waitingPartnerLoc"));

    const subLine = loc?.lat != null && !partnerIsPaused
      ? `${distTxt ? t("distanceApart", { d: distTxt }) : `${loc.lat.toFixed(4)}, ${loc.lon.toFixed(4)}`}${spdKmh ? ` · ${t("movingSpeed", { k: spdKmh })}` : ` · ${t("updatedNow")}`}`
      : (partnerIsPaused ? t("sharingPausedBanner") : t("waitingPartnerLoc"));

    // FigCraft 4.6 Quiet Pause overlay state
    const pauseSheetHtml = `
      <div class="map-peek-sheet">
        <button class="sheet-grabber-btn" id="btn-sheet-cycle" aria-label="Resize sheet"><span class="sheet-grabber-bar"></span></button>
        <div class="chip chip-paused" style="align-self:flex-start;">${escapeHtml(t("pairPausedPill"))}</div>
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("quietPauseTitle"))}</div>
        <div class="row-sub">${escapeHtml(t("quietPauseBody"))}</div>
        <button class="btn btn-primary" id="btn-peek-resume">${escapeHtml(t("resumeLiveBtn"))}</button>
        <button class="btn btn-danger hold-sos-pill" id="btn-hold-sos" style="width:100%;height:46px;border-radius:23px;justify-content:center;">
          <span class="hold-fill" id="sos-hold-fill"></span>
          <span class="live-dot danger"></span>
          <span>${escapeHtml(t("holdSosLong"))}</span>
        </button>
      </div>
    `;

    // FigCraft 4.2 Partner Focus Sheet
    const focusPlace = matchedPlace || state.places[0] || null;
    const focusBellOn = focusPlace ? state.placeBells[String(focusPlace.id)] !== false : true;
    const focusSheetHtml = `
      <div class="map-peek-sheet">
        <button class="sheet-grabber-btn" id="btn-sheet-cycle" aria-label="Close focus"><span class="sheet-grabber-bar"></span></button>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
          <div style="font-size:22px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(pName)}</div>
          <span class="chip chip-success tnum"><span class="live-dot"></span>${escapeHtml(batTxt)} · OK</span>
        </div>
        <div class="row-sub tnum">${escapeHtml(statusLine)} · ${escapeHtml(subLine)}</div>
        ${
          focusPlace
            ? `<div class="focus-alert-card">
                <div style="min-width:0;">
                  <div style="font-weight:700;font-size:14px;">${escapeHtml(t("getPlaceAlertsTitle", { place: focusPlace.name }))}</div>
                  <div class="row-sub" style="font-size:12px;">${escapeHtml(t("getPlaceAlertsSub"))}</div>
                </div>
                <button class="switch ${focusBellOn ? "on" : ""}" data-toggle-bell="${focusPlace.id}" role="switch" aria-checked="${focusBellOn}" aria-label="Place alert"></button>
              </div>`
            : ""
        }
        <div class="focus-dwell-card">
          <div style="font-weight:700;font-size:13px;">${escapeHtml(t("todayStopsSummary", { n: state.routePoints.length }))}</div>
          <div class="row-sub tnum" style="font-size:12px;margin-top:2px;">${escapeHtml(matchedPlace ? `${matchedPlace.name} (${dwellTxt || t("liveNow")})` : (distTxt || t("noStopsYet")))}</div>
        </div>
        <div class="focus-util-grid">
          <button class="focus-util-btn active" id="btn-focus-route">
            <span class="focus-util-icon tnum">${svgIcon("route")}</span>
            <span class="tnum">${escapeHtml(distTxt || t("route24h"))}</span>
          </button>
          <button class="focus-util-btn" id="btn-peek-checkin">
            <span class="focus-util-icon">${svgIcon("camera")}</span>
            <span>${escapeHtml(t("checkIn"))}</span>
          </button>
          <button class="focus-util-btn" id="btn-peek-chat">
            <span class="focus-util-icon">${svgIcon("chat")}</span>
            <span>${escapeHtml(t("msgBtnShort"))}</span>
          </button>
          <button class="focus-util-btn" id="btn-focus-bubble">
            <span class="focus-util-icon">${svgIcon("bubble")}</span>
            <span>${escapeHtml(t("bubbleBtnShort"))}</span>
          </button>
          <button class="focus-util-btn" id="btn-focus-alerts">
            <span class="focus-util-icon">${svgIcon("bell")}</span>
            <span>${escapeHtml(t("alertsBtnShort"))}</span>
          </button>
        </div>
      </div>
    `;

    const renderPeekPlacesSection = (includeAddBtn) => `
      <div style="display:flex;flex-direction:column;gap:8px;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
          <span class="section-label" style="margin:0;">${escapeHtml(t("savedPlaces"))}</span>
          <span class="row-sub tnum" style="font-size:11px;">${state.places.length}</span>
        </div>
        ${
          state.places.slice(0, 4).map((pl) => {
            const bellOn = state.placeBells[String(pl.id)] !== false;
            return `
              <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:7px 10px;border-radius:14px;background:var(--surface-muted);">
                <div style="min-width:0;">
                  <div style="font-weight:700;font-size:13px;">${escapeHtml(pl.name)}</div>
                  <div class="row-sub tnum" style="font-size:11px;">${escapeHtml(t("radiusUnit", { m: Math.round(pl.radius_m) }))}</div>
                </div>
                <button class="place-bell-btn ${bellOn ? "on" : ""}" data-toggle-bell="${pl.id}" aria-label="Toggle place alert">
                  ${bellOn ? svgIcon("bell") : svgIcon("bellOff")}
                </button>
              </div>
            `;
          }).join("") || `<div class="row-sub">${escapeHtml(t("noSavedPlaces"))}</div>`
        }
        ${includeAddBtn ? `<button class="btn btn-primary" style="min-height:40px;" id="btn-peek-manage-places">${escapeHtml(t("addPlaceBtn"))}</button>` : ""}
      </div>
    `;

    // FigCraft 4.1 Live + Peek Sheet
    const defaultPeekHtml = `
      <div class="map-peek-sheet">
        <button class="sheet-grabber-btn" id="btn-sheet-cycle" aria-label="Resize sheet">
          <span class="sheet-grabber-bar"></span>
        </button>
        <div class="peek-seg" role="tablist" aria-label="Sheet view">
          <button class="peek-seg-btn ${state.sheetTab === "all" ? "active" : ""}" data-sheet-tab="all">${escapeHtml(t("all"))}</button>
          <button class="peek-seg-btn ${state.sheetTab === "people" ? "active" : ""}" data-sheet-tab="people">${escapeHtml(t("peopleTab"))}</button>
          <button class="peek-seg-btn ${state.sheetTab === "places" ? "active" : ""}" data-sheet-tab="places">${escapeHtml(t("places"))}</button>
          <button class="peek-seg-btn ${state.sheetTab === "trail" ? "active" : ""}" data-sheet-tab="trail">${escapeHtml(t("trail24hTab"))}</button>
        </div>
        ${
          state.sheetTab === "places"
            ? renderPeekPlacesSection(true)
            : state.sheetTab === "trail"
            ? `<div style="display:flex;flex-direction:column;gap:8px;">
                <div class="row-sub">${escapeHtml(state.routePoints.length ? t("routePointsToast", { n: state.routePoints.length }) : t("noStopsYet"))}</div>
                <div style="display:flex;gap:8px;">
                  <button class="btn ${state.showRoute24h ? "btn-soft" : "btn-primary"}" style="flex:1;min-height:40px;" id="btn-peek-trail-toggle">${escapeHtml(t("route24h"))}</button>
                  <button class="btn btn-secondary" style="flex:1;min-height:40px;" id="btn-peek-route-modal">${escapeHtml(t("route24hTitle"))}</button>
                </div>
              </div>`
            : `<div class="peek-partner-card">
                <button type="button" class="peek-partner-main" id="peek-header-tap" aria-label="${escapeHtml(pName)}">
                  <div class="peek-partner-av-wrap">
                    <div class="peek-partner-av">
                      ${state.partner?.avatar_photo_id
                        ? `<img data-photo-id="${escapeHtml(state.partner.avatar_photo_id)}" data-photo-size="thumb" width="48" height="48" alt="" />`
                        : escapeHtml(pInit)}
                    </div>
                    <span class="peek-batt-pill tnum" id="peek-batt-val">${escapeHtml(batTxt)}</span>
                  </div>
                  <div style="min-width:0;flex:1;">
                    <div style="font-weight:700;font-size:17px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(pName)}</div>
                    <div id="peek-statusline" style="font-size:13px;font-weight:600;color:${st.live ? "var(--success)" : "var(--ink-muted)"};">${escapeHtml(statusLine)}</div>
                    <div class="row-sub tnum" id="peek-subline" style="font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(subLine)}</div>
                  </div>
                </button>
                <button type="button" class="peek-heart-btn" id="btn-peek-nudge" aria-label="${escapeHtml(t("nudge"))}">${svgIcon("heart")}</button>
              </div>
              ${
                detent !== "collapsed"
                  ? `<div class="peek-me-row">
                      <span class="peek-me-av">${escapeHtml(mInit)}</span>
                      <div style="min-width:0;flex:1;">
                        <div style="font-weight:700;">${escapeHtml(mName)} ${escapeHtml(t("youLabel"))}</div>
                        <div class="row-sub tnum" style="font-size:12px;">${state.sharingPaused ? escapeHtml(t("quietPauseTitle")) : escapeHtml(t("sharingLiveShort"))}${state.meLocation?.battery_pct != null ? ` · Battery ${state.meLocation.battery_pct}%` : ""}</div>
                      </div>
                    </div>`
                  : ""
              }
              ${
                detent === "expanded"
                  ? `<div class="peek-actions">
                      <button class="btn btn-primary" id="btn-peek-checkin">${escapeHtml(t("checkIn"))}</button>
                      <button class="btn btn-soft" id="btn-peek-manage-places">${escapeHtml(t("places"))}</button>
                      <button class="btn btn-secondary" id="btn-peek-chat">${escapeHtml(t("tabChat"))}</button>
                    </div>
                    ${renderPeekPlacesSection(false)}`
                  : ""
              }`
        }
      </div>
    `;

    return `
      <div class="map-topbar">
        ${
          state.partnerFocused
            ? `<button class="map-focus-back-pill" id="btn-focus-back">&lt;&nbsp;&nbsp;${escapeHtml(pName)} · ${escapeHtml(t("updatedNow"))}</button>
               <button class="glass-fab" id="btn-map-center" aria-label="${escapeHtml(t("centerMap"))}">${svgIcon("recenter")}</button>`
            : `<button class="glass-fab" id="btn-map-settings" aria-label="${escapeHtml(t("settings"))}">${svgIcon("settings")}</button>
               <button class="map-switcher-pill" id="btn-map-switcher">
                 <span class="live-dot ${state.sharingPaused ? "action" : (st.dotClass || "")}"></span>
                 <span>${escapeHtml(switcherTxt)}</span>
                 <span style="opacity:.55;font-size:11px;">▾</span>
               </button>
               <button class="glass-fab" id="btn-map-alerts" aria-label="${escapeHtml(t("activityInbox"))}">
                 ${svgIcon("bell")}
                 ${state.unreadAlerts > 0 ? `<span class="fab-badge-dot"></span>` : ""}
               </button>`
        }
      </div>

      ${
        state.sharingPaused && !state.partnerFocused
          ? `<div class="frosted-paused-banner">
              <span class="live-dot paused" style="width:10px;height:10px;"></span>
              <div style="min-width:0;">
                <div style="font-weight:700;font-size:14px;">${escapeHtml(t("sharingPausedBanner"))}</div>
                <div class="row-sub" style="font-size:12px;">${escapeHtml(t("sharingPausedSub", { name: pName }))}</div>
              </div>
            </div>`
          : ""
      }

      ${
        !state.partnerFocused
          ? `<div class="map-right-rail">
              <button class="glass-fab" id="btn-map-checkin" title="${escapeHtml(t("checkIn"))}" aria-label="${escapeHtml(t("checkIn"))}">${svgIcon("camera")}</button>
              <button class="glass-fab ${state.mapTileStyle !== "auto" ? "active" : ""}" id="btn-map-layers" title="${escapeHtml(t("mapTileTheme"))}" aria-label="${escapeHtml(t("mapTileTheme"))}">${svgIcon("layers")}</button>
              <button class="glass-fab" id="btn-map-center" title="${escapeHtml(t("centerMap"))}" aria-label="${escapeHtml(t("centerMap"))}">${svgIcon("recenter")}</button>
              <button class="glass-fab ${state.showRoute24h ? "active" : ""}" id="btn-map-route" title="${escapeHtml(t("route24h"))}" aria-label="${escapeHtml(t("route24h"))}">${svgIcon("route")}</button>
              <button class="glass-fab ${state.sharingPaused ? "active" : ""}" id="btn-map-bubble" title="${escapeHtml(t("privacyBubble"))}" aria-label="${escapeHtml(t("privacyBubble"))}">${svgIcon("bubble")}</button>
            </div>`
          : ""
      }

      ${
        offscreen && !state.partnerFocused && !state.sharingPaused
          ? `<button class="map-edge-pin" id="btn-edge-partner">
              <span class="map-edge-av">${escapeHtml(pInit)}</span>
              <span class="tnum">${escapeHtml(offscreen.dist)} ›</span>
            </button>`
          : ""
      }

      <div class="map-bottom-stack">
        ${
          !state.sharingPaused
            ? `${
                !state.partnerFocused
                  ? `<div class="map-floating-pills-row">
                      <button class="hold-sos-pill" id="btn-hold-sos" aria-label="${escapeHtml(t("holdSos"))}">
                        <span class="hold-fill" id="sos-hold-fill"></span>
                        <span class="live-dot danger"></span>
                        <span>${escapeHtml(t("holdSos"))}</span>
                      </button>
                    </div>`
                  : `<div class="quick-reactions" aria-label="Quick reactions">
                      ${(STRINGS[state.lang] || STRINGS.en).quickReacts.map((lbl, idx) => `
                        <button class="react-chip ${idx === 0 ? "primary" : ""}" data-quick-react="${escapeHtml(lbl)}">${escapeHtml(lbl)}</button>
                      `).join("")}
                    </div>`
              }`
            : ""
        }
        ${state.sharingPaused ? pauseSheetHtml : state.partnerFocused ? focusSheetHtml : defaultPeekHtml}
      </div>

      <!-- Desktop Floating Map Chrome (03 Desktop Web App) -->
      <div class="desktop-map-util">
        <span class="live-dot ${state.sharingPaused ? "paused" : (st.dotClass || "")}"></span>
        <span style="font-size:13px;font-weight:700;margin-right:4px;" id="dsk-switcher-open">${escapeHtml(switcherTxt)}</span>
        <button class="desktop-util-btn ${state.showRoute24h ? "active" : ""}" id="dsk-btn-route">${escapeHtml(t("route24h"))}</button>
        <button class="desktop-util-btn" id="dsk-btn-layers">${escapeHtml(t("mapTileTheme"))}</button>
        <button class="desktop-util-btn ${state.sharingPaused ? "active" : ""}" id="dsk-btn-bubble">${escapeHtml(t("bubbleBtnShort"))}</button>
        <button class="desktop-util-btn ${state.sharingPaused ? "active" : ""}" id="dsk-btn-pause">${escapeHtml(t("pairPausedPill"))}</button>
        <button class="desktop-util-btn" id="dsk-btn-alerts" aria-label="${escapeHtml(t("activityInbox"))}">${escapeHtml(t("alertsBtnShort"))}${state.unreadAlerts ? ` (${state.unreadAlerts})` : ""}</button>
        <button class="desktop-util-btn danger hold-sos-pill" id="dsk-btn-sos" style="border-radius:16px;"><span class="hold-fill" id="dsk-sos-fill"></span><span>${escapeHtml(t("holdSos"))}</span></button>
      </div>

      <div class="desktop-map-bottom-dock">
        <div class="quick-reactions" aria-label="Quick reactions">
          ${(STRINGS[state.lang] || STRINGS.en).quickReacts.map((lbl, idx) => `
            <button class="react-chip ${idx === 0 ? "primary" : ""}" data-quick-react="${escapeHtml(lbl)}">${escapeHtml(lbl)}</button>
          `).join("")}
        </div>
        <div class="desktop-dock-bar">
          <button class="desktop-dock-btn primary" id="dsk-dock-checkin">${escapeHtml(t("checkIn"))}</button>
          <button class="desktop-dock-btn" id="dsk-dock-nudge">${escapeHtml(t("nudge"))}</button>
          <button class="desktop-dock-btn" id="dsk-dock-center">${escapeHtml(t("centerPins"))}</button>
          <button class="desktop-dock-btn" id="dsk-dock-places">${escapeHtml(t("places"))} (${state.places.length})</button>
        </div>
      </div>
    `;
  }

  function renderMapScreen() {
    return `
      <main class="screen map-screen">
        <div class="map-canvas-wrap">
          <div id="ours-map" class="map-container"></div>
          <div id="map-overlay-root" class="map-overlay-root">
            ${renderMapOverlayHtml()}
          </div>
        </div>
      </main>
    `;
  }

  function triggerNudgeWave() {
    const wrap = document.querySelector(".map-canvas-wrap");
    if (!wrap) return;
    const wave = document.createElement("div");
    wave.className = "nudge-wave";
    wave.setAttribute("aria-hidden", "true");
    wrap.appendChild(wave);
    setTimeout(() => wave.remove(), 1100);
  }

  function updatePeekCardValues() {
    const sub = document.getElementById("peek-subline");
    const stEl = document.getElementById("peek-statusline");
    const batEl = document.getElementById("peek-batt-val");
    const loc = state.partnerLocation;
    if (!loc && !sub && !stEl) return;
    const paused = !!(loc?.paused || state.partnerPaused || loc?.lat == null);
    const st = partnerStatusSummary();
    const matchedPlace = paused ? null : partnerCurrentPlace();
    const spdKmh = loc?.speed_mps > 0.6 ? Math.round(loc.speed_mps * 3.6) : 0;
    const dwell = state.partnerDwellSince && spdKmh === 0 ? t("hereFor", { m: formatDwell(Date.now() - state.partnerDwellSince) }) : "";
    if (stEl) {
      stEl.textContent = (loc?.paused || state.partnerPaused)
        ? st.label
        : matchedPlace
          ? `At ${matchedPlace.name}${dwell ? ` · ${dwell}` : ""}`
          : (loc?.lat != null ? st.label : t("waitingPartnerLoc"));
      stEl.style.color = st.live && !paused ? "var(--success)" : "var(--ink-muted)";
    }
    if (batEl && loc?.battery_pct != null) {
      batEl.textContent = `${loc.battery_pct}%${loc.charging ? "⚡" : ""}`;
    }
    if (!sub || !loc) return;
    if (paused) {
      sub.textContent = (loc.paused || state.partnerPaused) ? t("sharingPausedBanner") : t("waitingPartnerLoc");
      return;
    }
    const dist = state.meLocation?.lat != null ? formatDist(haversineM(state.meLocation.lat, state.meLocation.lon, loc.lat, loc.lon)) : "";
    sub.textContent = `${dist ? t("distanceApart", { d: dist }) : `${loc.lat.toFixed(4)}, ${loc.lon.toFixed(4)}`}${spdKmh ? ` · ${t("movingSpeed", { k: spdKmh })}` : dwell ? ` · ${dwell}` : ` · ${t("updatedNow")}`}`;
  }

  function buildPinHtml(initial, batPct, speedTxt, dwellTxt, isMe, auraBubble, bearing = null) {
    const pillText = speedTxt || dwellTxt || (isMe ? t("mePin") : t("livePin"));
    const subText = speedTxt && dwellTxt ? dwellTxt : "";
    const auraClass = auraBubble ? "bubble" : (!isMe ? "live" : "");
    const rot = typeof bearing === "number" ? `transform:rotate(${Math.round(bearing)}deg);` : "";
    return `
      <div class="map-pin-aura ${auraClass}">
        ${speedTxt ? `<div class="map-pin-cone" style="${rot}"></div>` : ""}
        <div class="map-pin-dot ${isMe ? "me" : "partner"}">${escapeHtml(initial)}</div>
      </div>
      <div class="map-pin-tail ${isMe ? "me" : ""}"></div>
      ${batPct != null ? `<div class="map-pin-bat tnum"><span class="live-dot"></span><span>${batPct}%</span></div>` : ""}
      <div class="map-pin-label tnum">${!isMe ? `<span class="live-dot"></span>` : ""}${escapeHtml(pillText)}</div>
      ${subText ? `<div class="map-pin-sub tnum">${escapeHtml(subText)}</div>` : ""}
    `;
  }

  function checkPartnerEdgePin() {
    if (!mapInstance || !state.partnerLocation?.lat || state.partnerLocation?.paused || state.partnerPaused) {
      if (state.partnerOffscreen) {
        state.partnerOffscreen = null;
        renderMapControlsOnly();
      }
      return;
    }
    const bounds = mapInstance.getBounds();
    const lngLat = [state.partnerLocation.lon, state.partnerLocation.lat];
    const inside = bounds.contains(lngLat);
    if (inside && state.partnerOffscreen) {
      state.partnerOffscreen = null;
      const existing = document.getElementById("btn-edge-partner");
      if (existing) existing.remove();
    } else if (!inside && !state.partnerOffscreen) {
      const center = mapInstance.getCenter();
      const dist = formatDist(haversineM(center.lat, center.lng, state.partnerLocation.lat, state.partnerLocation.lon));
      state.partnerOffscreen = { dist };
      if (!document.getElementById("btn-edge-partner") && !state.sharingPaused) {
        const host = document.getElementById("map-overlay-root") || document.querySelector(".map-canvas-wrap");
        if (host) {
          const btn = document.createElement("button");
          btn.className = "map-edge-pin";
          btn.id = "btn-edge-partner";
          const pInit = (state.partner?.name || "P").slice(0, 1).toUpperCase();
          btn.innerHTML = `<span class="map-edge-av">${escapeHtml(pInit)}</span><span class="tnum">${escapeHtml(dist)} ›</span>`;
          btn.onclick = () => {
            if (mapInstance && state.partnerLocation?.lat != null) {
              mapInstance.flyTo({ center: [state.partnerLocation.lon, state.partnerLocation.lat], zoom: 15 });
            }
          };
          host.appendChild(btn);
        }
      }
    }
  }

  function renderMapControlsOnly() {
    const existing = document.getElementById("btn-edge-partner");
    if (existing && !state.partnerOffscreen) existing.remove();
  }

  /* OURS Vector Basemap (Instant Local Canvas + OpenFreeMap Vector Tiles, Zero Watermark) */
  function buildMapStyle(themeKey, centerLonLat) {
    const [lon, lat] = centerLonLat;
    const isDark = themeKey === "dark";
    const isTerrain = themeKey === "terrain";
    const bg = isDark ? "#0B0E14" : isTerrain ? "#E8F0E5" : "#FAF9F5";
    const res = isDark ? "#11161F" : isTerrain ? "#E2EBDF" : "#F1F0EA";
    const park = isDark ? "#13201B" : isTerrain ? "#CCE2C5" : "#E2EDE5";
    const water = isDark ? "#101E2E" : isTerrain ? "#BFD9F0" : "#D5E6F5";
    const waterway = isDark ? "#16293D" : isTerrain ? "#A9CCE9" : "#C2DAF0";
    const bld = isDark ? "#171D28" : isTerrain ? "#D5E0D5" : "#EAE8E0";
    const bldOut = isDark ? "#222B3A" : isTerrain ? "#C4D1C4" : "#DFDDD4";
    const roadCasing = isDark ? "#182030" : isTerrain ? "#C4D0C4" : "#D8DEE8";
    const roadMinor = isDark ? "#222B3A" : "#FFFFFF";
    const roadMajor = isDark ? "#2E3A4F" : "#FFFFFF";
    const textCol = isDark ? "#CBD5E1" : "#3E4856";
    const textHalo = isDark ? "#0B0E14" : "#FAF9F5";

    const streets = [];
    for (let i = -12; i <= 12; i++) {
      const d = i * 0.008;
      streets.push({
        type: "Feature",
        properties: { major: i % 3 === 0 ? 1 : 0 },
        geometry: { type: "LineString", coordinates: [[lon - 0.14, lat + d], [lon + 0.14, lat + d + 0.004]] }
      });
      streets.push({
        type: "Feature",
        properties: { major: i % 3 === 0 ? 1 : 0 },
        geometry: { type: "LineString", coordinates: [[lon + d, lat - 0.14], [lon + d - 0.004, lat + 0.14]] }
      });
    }
    const parksGeo = {
      type: "FeatureCollection",
      features: [
        { type: "Feature", geometry: { type: "Polygon", coordinates: [[[lon - 0.018, lat + 0.004], [lon - 0.005, lat + 0.004], [lon - 0.005, lat + 0.015], [lon - 0.018, lat + 0.015], [lon - 0.018, lat + 0.004]]] } },
        { type: "Feature", geometry: { type: "Polygon", coordinates: [[[lon + 0.006, lat - 0.014], [lon + 0.022, lat - 0.014], [lon + 0.022, lat - 0.003], [lon + 0.006, lat - 0.003], [lon + 0.006, lat - 0.014]]] } }
      ]
    };
    const waterGeo = {
      type: "FeatureCollection",
      features: [{
        type: "Feature",
        geometry: {
          type: "LineString",
          coordinates: [[lon - 0.06, lat - 0.012], [lon - 0.01, lat - 0.003], [lon + 0.02, lat - 0.007], [lon + 0.07, lat + 0.005]]
        }
      }]
    };

    return {
      version: 8,
      name: isDark ? "OURS Obsidian" : isTerrain ? "OURS Terrain" : "OURS Broken White",
      glyphs: "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
      sources: {
        "fc-parks": { type: "geojson", data: parksGeo },
        "fc-water": { type: "geojson", data: waterGeo },
        "fc-roads": { type: "geojson", data: { type: "FeatureCollection", features: streets } },
        openmaptiles: { type: "vector", url: "https://tiles.openfreemap.org/planet" }
      },
      layers: [
        { id: "fc-bg", type: "background", paint: { "background-color": bg } },
        { id: "fc-parks-layer", type: "fill", source: "fc-parks", paint: { "fill-color": park, "fill-opacity": 0.45 } },
        { id: "fc-water-layer", type: "line", source: "fc-water", paint: { "line-color": water, "line-width": 14, "line-opacity": 0.55 } },
        { id: "fc-roads-minor", type: "line", source: "fc-roads", filter: ["==", ["get", "major"], 0], paint: { "line-color": isDark ? "#141A26" : "#EAE8E0", "line-width": 2, "line-opacity": 0.4 } },
        { id: "fc-roads-major", type: "line", source: "fc-roads", filter: ["==", ["get", "major"], 1], paint: { "line-color": isDark ? "#1B2332" : "#F5F4EE", "line-width": 4.5, "line-opacity": 0.45 } },
        { id: "ofm-landuse-res", type: "fill", source: "openmaptiles", "source-layer": "landuse", filter: ["in", "class", "residential", "suburb", "neighbourhood"], paint: { "fill-color": res } },
        { id: "ofm-landcover-grass", type: "fill", source: "openmaptiles", "source-layer": "landcover", filter: ["in", "class", "grass", "wood", "park", "forest"], paint: { "fill-color": park } },
        { id: "ofm-landuse-park", type: "fill", source: "openmaptiles", "source-layer": "landuse", filter: ["in", "class", "park", "cemetery", "pitch", "playground", "recreation_ground"], paint: { "fill-color": park } },
        { id: "ofm-water", type: "fill", source: "openmaptiles", "source-layer": "water", paint: { "fill-color": water } },
        { id: "ofm-waterway", type: "line", source: "openmaptiles", "source-layer": "waterway", paint: { "line-color": waterway, "line-width": 1.6 } },
        { id: "ofm-building", type: "fill", source: "openmaptiles", "source-layer": "building", minzoom: 13.5, paint: { "fill-color": bld, "fill-outline-color": bldOut } },
        { id: "ofm-road-minor-casing", type: "line", source: "openmaptiles", "source-layer": "transportation", filter: ["in", "class", "minor", "service", "track"], minzoom: 13, layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": roadCasing, "line-width": ["interpolate", ["linear"], ["zoom"], 13, 2.0, 16, 6.0] } },
        { id: "ofm-road-minor", type: "line", source: "openmaptiles", "source-layer": "transportation", filter: ["in", "class", "minor", "service", "track"], minzoom: 12, layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": roadMinor, "line-width": ["interpolate", ["linear"], ["zoom"], 12, 1.0, 16, 4.2] } },
        { id: "ofm-road-sec-casing", type: "line", source: "openmaptiles", "source-layer": "transportation", filter: ["in", "class", "secondary", "tertiary"], layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": roadCasing, "line-width": ["interpolate", ["linear"], ["zoom"], 10, 2.0, 16, 8.0] } },
        { id: "ofm-road-sec", type: "line", source: "openmaptiles", "source-layer": "transportation", filter: ["in", "class", "secondary", "tertiary"], layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": roadMajor, "line-width": ["interpolate", ["linear"], ["zoom"], 10, 1.2, 16, 6.0] } },
        { id: "ofm-road-pri-casing", type: "line", source: "openmaptiles", "source-layer": "transportation", filter: ["in", "class", "primary", "trunk", "motorway"], layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": roadCasing, "line-width": ["interpolate", ["linear"], ["zoom"], 6, 2.0, 16, 10.5] } },
        { id: "ofm-road-pri", type: "line", source: "openmaptiles", "source-layer": "transportation", filter: ["in", "class", "primary", "trunk", "motorway"], layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": roadMajor, "line-width": ["interpolate", ["linear"], ["zoom"], 6, 1.2, 16, 8.0] } },
        { id: "ofm-place-city", type: "symbol", source: "openmaptiles", "source-layer": "place", filter: ["in", "class", "city", "town", "village", "suburb", "neighbourhood"], layout: { "text-field": "{name}", "text-font": ["Noto Sans Regular"], "text-size": ["interpolate", ["linear"], ["zoom"], 8, 11, 14, 15], "text-max-width": 8 }, paint: { "text-color": textCol, "text-halo-color": textHalo, "text-halo-width": 1.6 } },
        { id: "ofm-road-label", type: "symbol", source: "openmaptiles", "source-layer": "transportation_name", minzoom: 14, layout: { "symbol-placement": "line", "text-field": "{name}", "text-font": ["Noto Sans Regular"], "text-size": 11 }, paint: { "text-color": textCol, "text-halo-color": textHalo, "text-halo-width": 1.3 } }
      ]
    };
  }

  function initOrUpdateMap() {
    const container = document.getElementById("ours-map");
    if (!container || typeof maplibregl === "undefined") return;
    const themeKey = resolveMapThemeKey();
    if (mapInstance && mapInstance.getContainer() === container && mapCurrentThemeKey === themeKey) {
      updateMapMarkers();
      updateRouteLayer();
      updatePlacesLayer();
      updateCheckInPins();
      return;
    }
    if (mapInstance) {
      try {
        savedMapCamera = { center: mapInstance.getCenter(), zoom: mapInstance.getZoom() };
      } catch (_) {}
      try { mapInstance.remove(); } catch (_) {}
      mapInstance = null;
      meMarker = null;
      partnerMarker = null;
      placeMarkers = [];
      checkinMapMarkers = [];
    }
    mapCurrentThemeKey = themeKey;

    const center = savedMapCamera?.center
      ? [savedMapCamera.center.lng, savedMapCamera.center.lat]
      : state.partnerLocation?.lat != null && !state.partnerLocation?.paused
      ? [state.partnerLocation.lon, state.partnerLocation.lat]
      : state.meLocation?.lat != null
      ? [state.meLocation.lon, state.meLocation.lat]
      : [106.8272, -6.1754];
    const zoom = savedMapCamera?.zoom ?? 14;

    mapInstance = new maplibregl.Map({
      container,
      style: buildMapStyle(themeKey, center),
      center,
      zoom,
      attributionControl: false
    });

    mapInstance.on("error", () => {});
    const markUserMove = (e) => { if (e && e.originalEvent) mapUserMoved = true; };
    mapInstance.on("dragstart", markUserMove);
    mapInstance.on("zoomstart", markUserMove);
    mapInstance.on("load", () => {
      if (!savedMapCamera && window.innerWidth < 1024) {
        const vh = window.innerHeight || 844;
        mapInstance.jumpTo({ center, zoom, padding: { top: 105, left: 40, right: 64, bottom: Math.min(Math.round(vh * 0.52), 430) } });
      }
      updateMapMarkers();
      updateRouteLayer();
      updatePlacesLayer();
      updateCheckInPins();
      checkPartnerEdgePin();
    });
    mapInstance.on("moveend", () => {
      try {
        savedMapCamera = { center: mapInstance.getCenter(), zoom: mapInstance.getZoom() };
      } catch (_) {}
      checkPartnerEdgePin();
    });
  }

  function makeCircleRing(lon, lat, radiusM) {
    const pts = [];
    const latRad = (lat * Math.PI) / 180;
    const dLat = radiusM / 111320;
    const dLon = radiusM / (111320 * Math.max(0.2, Math.cos(latRad)));
    for (let i = 0; i <= 48; i++) {
      const th = (i / 48) * Math.PI * 2;
      pts.push([lon + Math.cos(th) * dLon, lat + Math.sin(th) * dLat]);
    }
    return [pts];
  }

  function updatePlacesLayer() {
    if (!mapInstance) return;
    if (!mapInstance.isStyleLoaded()) {
      mapInstance.once("styledata", updatePlacesLayer);
      return;
    }
    placeMarkers.forEach((m) => m.remove());
    placeMarkers = [];
    const features = state.showPlacesLayer
      ? (state.places || []).map((p) => ({
          type: "Feature",
          geometry: { type: "Polygon", coordinates: makeCircleRing(p.lon, p.lat, Number(p.radius_m) || 150) }
        }))
      : [];
    const geojson = { type: "FeatureCollection", features };
    const existing = mapInstance.getSource("ours-places");
    if (existing) {
      existing.setData(geojson);
    } else if (features.length > 0) {
      mapInstance.addSource("ours-places", { type: "geojson", data: geojson });
      mapInstance.addLayer({
        id: "ours-places-fill",
        type: "fill",
        source: "ours-places",
        paint: { "fill-color": resolveMapDark() ? "#4D8BFF" : "#0055FF", "fill-opacity": 0.08 }
      });
      mapInstance.addLayer({
        id: "ours-places-ring",
        type: "line",
        source: "ours-places",
        paint: {
          "line-color": resolveMapDark() ? "#4D8BFF" : "#0055FF",
          "line-width": 1.6,
          "line-dasharray": [2, 2],
          "line-opacity": 0.65
        }
      });
    }
    if (state.showPlacesLayer) {
      (state.places || []).forEach((p) => {
        const el = document.createElement("div");
        el.className = "map-place-pill";
        el.innerHTML = `<span class="map-place-dot"></span><span>${escapeHtml(p.name)}</span>`;
        const nearMe = state.meLocation?.lat != null && haversineM(p.lat, p.lon, state.meLocation.lat, state.meLocation.lon) < 120;
        const nearPartner = state.partnerLocation?.lat != null && !state.partnerPaused && haversineM(p.lat, p.lon, state.partnerLocation.lat, state.partnerLocation.lon) < 120;
        const offset = (nearMe || nearPartner) ? [0, -52] : [0, 0];
        const mk = new maplibregl.Marker({ element: el, offset }).setLngLat([p.lon, p.lat]).addTo(mapInstance);
        placeMarkers.push(mk);
      });
    }
  }

  function updateCheckInPins() {
    if (!mapInstance || typeof maplibregl === "undefined") return;
    checkinMapMarkers.forEach((m) => m.remove());
    checkinMapMarkers = [];
    if (!state.showCheckinPins) return;
    (state.checkins || []).filter((c) => c.lat != null && c.lon != null).slice(0, 6).forEach((c) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "map-checkin-pin";
      btn.setAttribute("aria-label", c.place_name || t("checkIn"));
      const img = document.createElement("img");
      img.setAttribute("data-photo-id", String(c.photo_id));
      img.setAttribute("data-photo-size", "thumb");
      img.alt = "";
      btn.appendChild(img);
      btn.onclick = () => openLightbox(c.photo_id);
      const nearMe = state.meLocation?.lat != null && haversineM(c.lat, c.lon, state.meLocation.lat, state.meLocation.lon) < 150;
      const nearPartner = state.partnerLocation?.lat != null && !state.partnerPaused && haversineM(c.lat, c.lon, state.partnerLocation.lat, state.partnerLocation.lon) < 150;
      const offset = (nearMe || nearPartner) ? [60, -28] : [0, 0];
      const mk = new maplibregl.Marker({ element: btn, offset }).setLngLat([c.lon, c.lat]).addTo(mapInstance);
      checkinMapMarkers.push(mk);
    });
    hydratePhotos();
  }

  function updateRouteLayer() {
    if (!mapInstance) return;
    if (!mapInstance.isStyleLoaded()) {
      mapInstance.once("styledata", updateRouteLayer);
      return;
    }
    const coords = (state.showRoute24h ? state.routePoints : []).map((p) => [p.lon, p.lat]);
    const geojson = {
      type: "Feature",
      geometry: { type: "LineString", coordinates: coords }
    };
    const existing = mapInstance.getSource("ours-route");
    if (existing) {
      existing.setData(geojson);
    } else if (coords.length > 1) {
      mapInstance.addSource("ours-route", { type: "geojson", data: geojson });
      mapInstance.addLayer({
        id: "ours-route-line",
        type: "line",
        source: "ours-route",
        paint: {
          "line-color": resolveMapDark() ? "#4D8BFF" : "#0055FF",
          "line-width": 4,
          "line-opacity": 0.85
        }
      });
    }
  }

  function updateMapMarkers() {
    if (!mapInstance || typeof maplibregl === "undefined") return;
    if (state.meLocation?.lat != null) {
      const mInit = (state.me?.name || "M").slice(0, 1).toUpperCase();
      const html = buildPinHtml(mInit, state.meLocation.battery_pct, state.sharingPaused ? t("pausedLabel") : t("mePin"), "", true, state.sharingPaused);
      if (!meMarker) {
        const el = document.createElement("div");
        el.className = "map-pin";
        el.innerHTML = html;
        meMarker = new maplibregl.Marker({ element: el }).setLngLat([state.meLocation.lon, state.meLocation.lat]).addTo(mapInstance);
      } else {
        meMarker.getElement().innerHTML = html;
        meMarker.setLngLat([state.meLocation.lon, state.meLocation.lat]);
      }
    }

    const pLoc = state.partnerLocation;
    if (pLoc?.lat != null && !pLoc.paused && !state.partnerPaused) {
      const pInit = (state.partner?.name || "P").slice(0, 1).toUpperCase();
      const spdKmh = pLoc.speed_mps > 0.6 ? Math.round(pLoc.speed_mps * 3.6) : 0;
      const spdTxt = spdKmh ? t("movingSpeed", { k: spdKmh }) : "";
      const dwellTxt = !spdKmh && state.partnerDwellSince ? t("hereFor", { m: formatDwell(Date.now() - state.partnerDwellSince) }) : "";
      const html = buildPinHtml(pInit, pLoc.battery_pct, spdTxt, dwellTxt, false, false, pLoc.bearing);
      if (!partnerMarker) {
        const el = document.createElement("div");
        el.className = "map-pin";
        el.style.cursor = "pointer";
        el.onclick = () => {
          state.partnerFocused = true;
          render();
          recenterMapSmart(false);
        };
        el.innerHTML = html;
        partnerMarker = new maplibregl.Marker({ element: el }).setLngLat([pLoc.lon, pLoc.lat]).addTo(mapInstance);
      } else {
        partnerMarker.getElement().innerHTML = html;
        partnerMarker.setLngLat([pLoc.lon, pLoc.lat]);
      }
    } else if (partnerMarker) {
      partnerMarker.remove();
      partnerMarker = null;
    }
    if (!mapHasAutoFitPair && !mapUserMoved && pLoc?.lat != null && !pLoc.paused && !state.partnerPaused && state.meLocation?.lat != null) {
      mapHasAutoFitPair = true;
      recenterMapSmart(true);
    }
  }

  /* FigCraft 6.1 & 6.2 Memories Screen */
  function renderMemoriesScreen() {
    const q = state.memoriesQuery.trim().toLowerCase();
    const baseItems = state.memoriesFilter === "all" || state.memoriesFilter === "highlights"
      ? (state.memories.length ? state.memories : state.checkins)
      : state.memories.length ? state.memories : state.checkins;
    const seenIds = new Set();
    const pool = [...(state.memories || []), ...(state.checkins || [])].filter((c) => {
      if (seenIds.has(c.id)) return false;
      seenIds.add(c.id);
      return true;
    });
    const items = (state.memoriesFilter === "highlights" ? state.memories : pool).filter((c) => {
      if (state.memoriesFilter === "places" && !c.place_name) return false;
      if (q) {
        const hay = `${c.note || ""} ${c.place_name || ""}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
    const selectedPin = items.find((m) => m.id === state.selectedMemoryId) || items.find((m) => m.lat != null) || items[0] || null;

    return `
      <header class="top-bar">
        <h1 class="top-title" style="margin:0;">${escapeHtml(t("tabMemories"))}</h1>
        <div class="top-actions">
          <div class="segmented" style="width:168px;height:36px;border-radius:18px;">
            <button class="seg-btn ${state.memoriesMode === "timeline" ? "active" : ""}" id="seg-mem-timeline" style="min-height:28px;border-radius:14px;font-size:12px;">${escapeHtml(t("timeline"))}</button>
            <button class="seg-btn ${state.memoriesMode === "map" ? "active" : ""}" id="seg-mem-map" style="min-height:28px;border-radius:14px;font-size:12px;">${escapeHtml(t("mapPins"))}</button>
          </div>
        </div>
      </header>
      <main class="screen">
        <div class="page-pad">
          <label class="sr-only" for="mem-search-input">${escapeHtml(t("searchMemoriesPlaceholder"))}</label>
          <input class="input glass-search" id="mem-search-input" placeholder="${escapeHtml(t("searchMemoriesPlaceholder"))}" value="${escapeHtml(state.memoriesQuery)}" />
          <div class="filter-row">
            <button class="filter-pill ${state.memoriesFilter === "all" ? "active" : ""}" data-mem-filter="all">${escapeHtml(t("all"))}</button>
            <button class="filter-pill ${state.memoriesFilter === "photos" ? "active" : ""}" data-mem-filter="photos">${escapeHtml(t("photos"))}</button>
            <button class="filter-pill ${state.memoriesFilter === "places" ? "active" : ""}" data-mem-filter="places">${escapeHtml(t("places"))}</button>
            <button class="filter-pill ${state.memoriesFilter === "highlights" ? "active" : ""}" data-mem-filter="highlights">${escapeHtml(t("highlights"))}</button>
          </div>

          <div style="font-size:13px;font-weight:700;color:var(--ink-secondary);padding:2px 4px;">
            ${escapeHtml(new Date().toLocaleDateString(state.lang === "id" ? "id-ID" : "en-US", { month: "long", year: "numeric" }))} · ${items.length} ${escapeHtml(t("saved").toLowerCase())}
          </div>

          ${
            state.memoriesMode === "map"
              ? `<div style="position:relative;border-radius:24px;overflow:hidden;border:1px solid var(--border-glass);box-shadow:var(--shadow-md);height:480px;">
                   <div id="memories-map" style="width:100%;height:100%;"></div>
                   ${
                     selectedPin
                       ? `<div class="card" style="position:absolute;left:12px;right:12px;bottom:12px;z-index:5;display:flex;align-items:center;gap:12px;padding:12px;border-radius:22px;">
                            <img data-photo-id="${escapeHtml(selectedPin.photo_id)}" data-photo-size="thumb" data-lightbox="${escapeHtml(selectedPin.photo_id)}" style="width:82px;height:82px;border-radius:12px;object-fit:cover;flex-shrink:0;background:var(--surface-muted);cursor:pointer;" alt="" />
                            <div style="min-width:0;flex:1;">
                              <div style="font-weight:700;font-size:15px;">${escapeHtml(selectedPin.note || t("checkIn"))}</div>
                              ${selectedPin.place_name ? `<div style="font-size:13px;font-weight:600;color:var(--action);margin-top:2px;">${escapeHtml(selectedPin.place_name)}</div>` : ""}
                              <div class="row-sub tnum" style="font-size:12px;margin-top:4px;">${escapeHtml(formatDateShort(selectedPin.created_at))} · ${escapeHtml(formatTime(selectedPin.created_at))}</div>
                            </div>
                          </div>`
                       : ""
                   }
                 </div>`
              : items.length === 0
              ? `<div class="card" style="text-align:center;padding:24px 16px;">
                   ${figcraftEmptyArt("memories")}
                   <div class="row-sub">${escapeHtml(q ? t("noResults") : t("noMemories"))}</div>
                   ${q ? `<button class="btn btn-soft" style="margin-top:12px;" id="btn-mem-clear">${escapeHtml(t("clearFilters"))}</button>` : ""}
                 </div>`
              : items.map((c) => renderCheckInCard(c, false)).join("")
          }
        </div>
      </main>
    `;
  }

  function renderCheckInCard(c, inChat = false) {
    const isMine = c.author_id === state.me?.id;
    const authorName = isMine ? (state.me?.name || "Me") : (state.partner?.name || "Partner");
    if (inChat) {
      return `
        <div class="bubble-row ${isMine ? "mine" : "theirs"}">
          <div class="bubble-photo-card">
            <div class="memory-media-wrap">
              <img class="memory-photo" data-photo-id="${escapeHtml(c.photo_id)}" data-photo-size="full" data-lightbox="${escapeHtml(c.photo_id)}" tabindex="0" role="button" aria-label="View photo" alt="" />
              ${c.place_name ? `<div class="memory-place-badge">${escapeHtml(c.place_name)}</div>` : `<div class="memory-place-badge tnum">${escapeHtml(t("hoursLeftLabel"))}</div>`}
              <button type="button" class="memory-status-badge ${c.highlighted ? "saved" : ""}" data-toggle-hl="${c.id}" data-hl="${c.highlighted}">
                ${escapeHtml(c.highlighted ? t("saved") : t("save"))}
              </button>
            </div>
            <div class="bubble-photo-cap">
              ${c.note ? `<div style="font-weight:600;font-size:14px;">${escapeHtml(c.note)}</div>` : ""}
              <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                <span class="row-sub tnum" style="font-size:11px;">${escapeHtml(formatTime(c.created_at))}</span>
                <button type="button" class="pill-action" style="min-height:24px;padding:0 10px;font-size:11px;" data-reply-checkin="${c.id}">${escapeHtml(t("reply"))}</button>
              </div>
              ${
                c.replies && c.replies.length
                  ? `<div class="chat-quoted-reply">
                      ${c.replies.map((r) => `<div><strong style="color:var(--action);">${escapeHtml(r.author_id === state.me?.id ? (state.me?.name || "Me") : (state.partner?.name || "Partner"))}:</strong> ${escapeHtml(r.text || "[Photo]")}</div>`).join("")}
                    </div>`
                  : ""
              }
            </div>
          </div>
        </div>
      `;
    }

    return `
      <div class="memory-card">
        <div class="memory-media-wrap">
          <img class="memory-photo" data-photo-id="${escapeHtml(c.photo_id)}" data-photo-size="full" data-lightbox="${escapeHtml(c.photo_id)}" tabindex="0" role="button" aria-label="View photo" alt="" />
          ${c.place_name ? `<div class="memory-place-badge">${escapeHtml(c.place_name)}</div>` : ""}
          <button type="button" class="memory-status-badge ${c.highlighted ? "saved" : ""}" data-toggle-hl="${c.id}" data-hl="${c.highlighted}">
            ${escapeHtml(c.highlighted ? t("saved") : t("save"))}
          </button>
        </div>
        <div class="memory-body">
          <div style="font-weight:700;font-size:16px;">${escapeHtml(c.note || c.place_name || t("checkIn"))}</div>
          <div class="row-sub tnum" style="font-size:13px;">${escapeHtml(formatDateShort(c.created_at))} · ${escapeHtml(formatTime(c.created_at))} · ${escapeHtml(t("saved"))} by ${escapeHtml(authorName)}</div>
          ${
            c.replies && c.replies.length
              ? `<div class="chat-quoted-reply">
                  ${c.replies.map((r) => `<div><strong>${escapeHtml(r.author_id === state.me?.id ? (state.me?.name || "Me") : (state.partner?.name || "Partner"))}:</strong> ${escapeHtml(r.text || "[Photo]")}</div>`).join("")}
                </div>`
              : ""
          }
          <div class="memory-actions">
            <button class="pill-action" data-reply-checkin="${c.id}">${escapeHtml(t("reply"))}</button>
            ${isMine ? `<button class="pill-action" style="color:var(--danger);" data-del-checkin="${c.id}">${escapeHtml(t("del"))}</button>` : ""}
          </div>
        </div>
      </div>
    `;
  }

  /* FigCraft 5.1 Liquid Glass Messenger */
  function renderChatScreen() {
    const q = state.chatQuery.trim().toLowerCase();
    const st = partnerStatusSummary();
    const pName = state.partner?.name || t("tabChat");
    const pInit = pName.slice(0, 1).toUpperCase();
    const matchedPlace = partnerCurrentPlace();
    const liveSubtitle = matchedPlace
      ? `${t("liveNow")} · ${matchedPlace.name}`
      : st.label;

    const combined = [
      ...state.messages.map((m) => ({ kind: "msg", created_at: m.created_at, item: m })),
      ...state.checkins.map((c) => ({ kind: "checkin", created_at: c.created_at, item: c }))
    ]
      .filter((entry) => {
        if (state.chatFilter === "photos" && entry.kind === "msg" && !entry.item.photo_id) return false;
        if (state.chatFilter === "saved" && (entry.kind !== "checkin" || !entry.item.highlighted)) return false;
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
        return `${divider}${renderCheckInCard(entry.item, true)}`;
      }
      const m = entry.item;
      const mine = m.author_id === state.me?.id;
      return `
        ${divider}
        <div class="bubble-row ${mine ? "mine" : "theirs"}">
          <div class="bubble ${mine ? "mine" : "theirs"}">
            ${m.photo_id ? `<img data-photo-id="${escapeHtml(m.photo_id)}" data-photo-size="thumb" data-lightbox="${escapeHtml(m.photo_id)}" tabindex="0" role="button" aria-label="View photo" width="220" height="165" style="max-width:220px;width:100%;height:auto;aspect-ratio:4/3;object-fit:cover;border-radius:12px;display:block;margin-bottom:${m.text ? "6px" : "0"};cursor:pointer;" alt="" />` : ""}
            ${m.text ? `<span>${escapeHtml(m.text)}</span>` : ""}
            <span class="bubble-inline-time tnum">${escapeHtml(formatTime(m.created_at))}${mine ? " ✓✓" : ""}</span>
          </div>
        </div>
      `;
    }).join("");

    const activeReplyCheckin = state.replyToCheckInId
      ? state.checkins.find((c) => c.id === state.replyToCheckInId)
      : null;

    return `
      <header class="top-bar chat-bar">
        <div style="display:flex;align-items:center;gap:12px;min-width:0;">
          <div style="position:relative;">
            <div class="avatar-circle">
              ${state.partner?.avatar_photo_id
                ? `<img data-photo-id="${escapeHtml(state.partner.avatar_photo_id)}" data-photo-size="thumb" width="40" height="40" alt="" />`
                : escapeHtml(pInit)}
            </div>
            <span class="live-dot ${st.dotClass}" style="position:absolute;right:0;bottom:0;width:10px;height:10px;border:2px solid var(--surface);"></span>
          </div>
          <div style="min-width:0;">
            <h1 class="top-title" style="margin:0;font-size:18px;line-height:1.15;">${escapeHtml(pName)}</h1>
            <div class="tnum" style="font-size:12px;font-weight:600;color:${st.live ? "var(--success)" : "var(--ink-muted)"};">${escapeHtml(liveSubtitle)}</div>
          </div>
        </div>
        <button class="icon-btn" id="btn-chat-alerts" aria-label="${escapeHtml(t("activityInbox"))}">${svgIcon("bell")}</button>
      </header>
      <main class="screen chat-screen chat-with-header" id="chat-scroll">
        <div class="page-pad" style="padding-bottom:4px;gap:10px;">
          <div style="display:flex;align-items:center;gap:8px;">
            <label class="sr-only" for="chat-search-input">${escapeHtml(t("searchChatPlaceholder"))}</label>
            <input class="input glass-search" style="flex:1;min-width:0;" id="chat-search-input" placeholder="${escapeHtml(t("searchChatPlaceholder"))}" value="${escapeHtml(state.chatQuery)}" />
            <button class="filter-pill ${state.chatFilter === "all" ? "active" : ""}" data-chat-filter="all">${escapeHtml(t("all"))}</button>
            <button class="filter-pill ${state.chatFilter === "photos" ? "active" : ""}" data-chat-filter="photos">${escapeHtml(t("photos"))}</button>
            <button class="filter-pill ${state.chatFilter === "saved" ? "active" : ""}" data-chat-filter="saved">${escapeHtml(t("saved"))}</button>
          </div>
        </div>
        <div class="chat-list">
          ${rowsHtml || `<div class="card" style="text-align:center;margin-top:16px;">${figcraftEmptyArt("chat")}<div class="row-sub">${escapeHtml(q ? t("noResults") : t("sayHello"))}</div></div>`}
        </div>
      </main>
      <div class="chat-composer">
        ${
          state.replyToCheckInId
            ? `<div class="reply-banner"><span>${escapeHtml(t("replyingTo"))}: ${escapeHtml(activeReplyCheckin?.note || activeReplyCheckin?.place_name || `#${state.replyToCheckInId}`)}</span><button class="icon-btn" id="btn-cancel-reply" style="width:28px;height:28px;min-width:28px;min-height:28px;">×</button></div>`
            : ""
        }
        <div class="composer-row">
          <button class="composer-cam-btn" id="btn-chat-camera" aria-label="${escapeHtml(t("checkIn"))}">${svgIcon("camera")}</button>
          <label class="sr-only" for="chat-input">${escapeHtml(t("typeMessage"))}</label>
          <input class="composer-input" id="chat-input" placeholder="${escapeHtml(t("typeMessage"))}" />
          <button class="composer-send-btn" id="btn-chat-send" aria-label="${escapeHtml(t("send"))}">${svgIcon("sendArrow")}</button>
        </div>
      </div>
    `;
  }

  /* FigCraft 6.3 Saved Places Body (reused in Desktop tab + Mobile modal) */
  function buildSavedPlacesContentHtml(inModal = false) {
    const listHtml = state.places.map((p) => {
      const bellOn = state.placeBells[String(p.id)] !== false;
      return `
        <div class="place-bell-row">
          <div class="place-icon-badge">${placeCategoryIcon(p.name)}</div>
          <div style="min-width:0;flex:1;">
            <div style="font-weight:700;font-size:15px;">${escapeHtml(p.name)}</div>
            <div class="row-sub tnum" style="font-size:12px;">${p.lat.toFixed(4)}, ${p.lon.toFixed(4)}</div>
          </div>
          <span class="place-rad-pill tnum">${Math.round(p.radius_m)}m</span>
          <button class="place-bell-btn ${bellOn ? "on" : ""}" data-modal-bell="${p.id}" aria-label="Toggle alerts">
            ${bellOn ? svgIcon("bell") : svgIcon("bellOff")}
          </button>
          <button class="icon-btn" style="width:34px;height:34px;min-width:34px;min-height:34px;color:var(--danger);" data-del-place="${p.id}" aria-label="${escapeHtml(t("del"))}">×</button>
        </div>
      `;
    }).join("");

    return `
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("savedPlaces"))}</div>
        <button class="btn btn-primary" style="min-height:34px;padding:0 14px;font-size:13px;" id="pl-add">${escapeHtml(t("addPlaceBtn"))}</button>
      </div>
      <div style="display:flex;gap:8px;">
        <label class="sr-only" for="pl-name">${escapeHtml(t("placeName"))}</label>
        <input class="input glass-search" style="flex:1;" id="pl-name" placeholder="${escapeHtml(t("searchPlacesPlaceholder"))}" />
        <label class="sr-only" for="pl-radius">${escapeHtml(t("radiusM"))}</label>
        <input class="input glass-search tnum" style="width:86px;text-align:center;" id="pl-radius" type="number" value="150" placeholder="${escapeHtml(t("radiusM"))}" />
      </div>
      <div class="section-label" style="padding:0 2px;">${escapeHtml(t("nearbySuggestions"))}</div>
      <div class="filter-row">
        <button type="button" class="filter-pill" style="background:var(--action-soft);color:var(--action);" data-pl-suggest="Home">+ Home</button>
        <button type="button" class="filter-pill" style="background:var(--action-soft);color:var(--action);" data-pl-suggest="Studio">+ Studio</button>
        <button type="button" class="filter-pill" style="background:var(--action-soft);color:var(--action);" data-pl-suggest="Kopi Kemang">+ Coffee</button>
      </div>
      <div class="section-label" style="padding:2px 2px 0;">${escapeHtml(t("pairGeofences"))}</div>
      <div style="display:flex;flex-direction:column;gap:8px;">${listHtml || `<div class="card"><span class="row-sub">${escapeHtml(t("noSavedPlaces"))}</span></div>`}</div>
      <div style="padding:12px 14px;border-radius:18px;background:var(--action-soft-grad);border:1px solid var(--border-glass);">
        <div style="font-weight:700;font-size:13px;color:var(--action);">${escapeHtml(t("placesTipTitle"))}</div>
        <div class="row-sub" style="font-size:12px;margin-top:3px;">${escapeHtml(t("placesTipSub"))}</div>
      </div>
      ${inModal ? `<button class="btn btn-secondary" id="pl-close">${escapeHtml(t("close"))}</button>` : ""}
    `;
  }

  function renderPlacesTabScreen() {
    return `
      <main class="screen" style="padding-top:18px;">
        <div class="page-pad">
          ${buildSavedPlacesContentHtml(false)}
        </div>
      </main>
    `;
  }

  /* FigCraft 6.5 Settings Screen */
  function renderSettingsScreen() {
    const placeZonesTxt = t("zonesCount", { n: state.places.length });
    return `
      <header class="top-bar">
        <h1 class="top-title" style="margin:0;">${escapeHtml(t("settings"))}</h1>
        <span class="tnum" style="font-size:12px;font-weight:700;color:var(--success);display:inline-flex;align-items:center;gap:5px;"><span class="live-dot"></span>${escapeHtml(t("liveNow"))}</span>
      </header>
      <main class="screen">
        <div class="page-pad">
          <div class="card" style="display:flex;align-items:center;gap:12px;padding:12px 14px;">
            <label style="cursor:pointer;flex-shrink:0;" title="Update avatar">
              <input type="file" id="avatar-file-input" accept="image/*" class="hidden" />
              ${
                state.me?.avatar_photo_id
                  ? `<div class="avatar-circle" style="width:44px;height:44px;"><img data-photo-id="${escapeHtml(state.me.avatar_photo_id)}" data-photo-size="thumb" width="44" height="44" alt="" /></div>`
                  : `<div class="onb-squircle" style="width:44px;height:44px;border-radius:11px;">${oursRingsLogo(34)}</div>`
              }
            </label>
            <div style="min-width:0;flex:1;">
              <div style="font-size:16px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(state.spaceName || `${state.me?.name || ""} & ${state.partner?.name || "..."}`)}</div>
              <div class="row-sub" style="font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(state.me?.email || (state.spaceName ? `${state.me?.name || ""} & ${state.partner?.name || "..."}` : ""))}</div>
            </div>
            ${state.partner ? `<span class="chip chip-success" style="font-size:11px;"><span class="live-dot"></span>${escapeHtml(t("pairedBadge"))}</span>` : ""}
          </div>

          <div class="split-hero">
            <div class="split-hero-body">
              <div class="chip chip-success" style="align-self:flex-start;font-size:11px;">
                <span class="live-dot"></span>
                <span>${escapeHtml(t("securityHeroBadge"))}</span>
              </div>
              <div class="split-hero-title">${escapeHtml(t("securityHeroTitle"))}</div>
              <div class="row-sub">${escapeHtml(t("securityHeroSub"))}</div>
            </div>
            <button class="split-hero-foot" id="btn-security-audit">
              <span>${escapeHtml(t("securityHeroCta"))}</span>
              <span>›</span>
            </button>
          </div>

          <div class="grouped-list">
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("themeLabel"))}</div>
              <div class="segmented" style="width:198px;">
                <button class="seg-btn ${state.theme === "system" ? "active-blue" : ""}" style="min-height:26px;font-size:12px;padding:0;" data-theme-set="system">${escapeHtml(t("themeSystem"))}</button>
                <button class="seg-btn ${state.theme === "light" ? "active-blue" : ""}" style="min-height:26px;font-size:12px;padding:0;" data-theme-set="light">${escapeHtml(t("themeLight"))}</button>
                <button class="seg-btn ${state.theme === "dark" ? "active-blue" : ""}" style="min-height:26px;font-size:12px;padding:0;" data-theme-set="dark">${escapeHtml(t("themeDark"))}</button>
              </div>
            </div>
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("language"))}</div>
              <div class="segmented" style="width:82px;">
                <button class="seg-btn ${state.lang === "en" ? "active" : ""}" style="min-height:26px;font-size:12px;padding:0;" data-lang-set="en">EN</button>
                <button class="seg-btn ${state.lang === "id" ? "active" : ""}" style="min-height:26px;font-size:12px;padding:0;" data-lang-set="id">ID</button>
              </div>
            </div>
          </div>

          <div class="grouped-list">
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("pauseSharing"))}</div>
              <button class="switch ${state.sharingPaused ? "on" : ""}" id="sw-pause" role="switch" aria-checked="${state.sharingPaused}" aria-label="${escapeHtml(t("pauseSharing"))}"></button>
            </div>
            <button type="button" class="grouped-row row-btn" id="row-places">
              <div style="font-weight:600;">${escapeHtml(t("savedPlacesBells"))}</div>
              <div style="display:flex;align-items:center;gap:8px;">
                <span class="chip chip-action tnum" style="min-height:24px;font-size:12px;">${escapeHtml(placeZonesTxt)}</span>
                <span class="row-sub">›</span>
              </div>
            </button>
            <button type="button" class="grouped-row row-btn" id="row-bg-guide">
              <div style="font-weight:600;">${escapeHtml(t("bgReliability"))}</div>
              <span class="row-sub">Xiaomi / HyperOS ›</span>
            </button>
          </div>

          <div class="grouped-list">
            <div class="grouped-row">
              <div style="font-weight:600;">${escapeHtml(t("crashDetection"))}</div>
              <div style="display:flex;align-items:center;gap:8px;">
                <span class="chip chip-success" style="min-height:24px;font-size:11px;">${escapeHtml(t("activeStatus"))}</span>
                <button class="switch ${state.crashDetectionEnabled ? "on" : ""}" id="sw-crash" role="switch" aria-checked="${state.crashDetectionEnabled}" aria-label="${escapeHtml(t("crashDetection"))}"></button>
              </div>
            </div>
            <div class="grouped-row" style="gap:8px;">
              <label for="input-wa" style="font-weight:600;white-space:nowrap;">${escapeHtml(t("emergencyWhatsapp"))}</label>
              <input class="input" style="min-height:34px;padding:4px 10px;font-size:13px;" id="input-wa" placeholder="+62812..." value="${escapeHtml(state.emergencyContact)}" />
            </div>
            <div class="grouped-row">
              <button class="btn btn-danger" style="width:100%;min-height:40px;" id="btn-open-sos">${escapeHtml(t("triggerSos"))}</button>
            </div>
          </div>

          <div class="grouped-list">
            <div class="grouped-row" style="flex-direction:column;align-items:stretch;gap:8px;">
              <label class="sr-only" for="input-server-url">${escapeHtml(t("serverSection"))}</label>
              <input class="input" style="min-height:38px;font-size:13px;" id="input-server-url" value="${escapeHtml(state.serverUrl)}" />
              <div style="display:flex;gap:8px;">
                <button class="btn btn-secondary" style="flex:1;min-height:38px;font-size:13px;" id="btn-test-conn">${escapeHtml(t("testConnection"))}</button>
                <button class="btn btn-soft" style="flex:1;min-height:38px;font-size:13px;" id="btn-reload-ui">${escapeHtml(t("reloadUi"))}</button>
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
      </main>
    `;
  }

  function openModal(html, centered = false, extraClass = "") {
    const root = document.getElementById("modal-root");
    const nextClass = `modal-root ${centered ? "center" : ""} ${extraClass}`.trim();
    const existingSheet = !root.classList.contains("hidden") && root.className === nextClass
      ? root.querySelector(".modal-sheet")
      : null;
    root.className = nextClass;
    root.setAttribute("role", "dialog");
    root.setAttribute("aria-modal", "true");
    root.onclick = (e) => {
      if (e.target === root) closeModal();
    };
    if (existingSheet) {
      existingSheet.innerHTML = html;
      return;
    }
    const sheetClass = extraClass.includes("lightbox-dark") ? "modal-sheet lightbox-sheet" : "modal-sheet";
    root.innerHTML = `<div class="${sheetClass}">${html}</div>`;
    const firstFocusable = root.querySelector("input, textarea, button");
    if (firstFocusable) firstFocusable.focus();
  }

  function closeModal() {
    const root = document.getElementById("modal-root");
    root.className = "modal-root hidden";
    root.removeAttribute("role");
    root.removeAttribute("aria-modal");
    root.innerHTML = "";
  }

  /* FigCraft 4.3 Privacy Bubble Sheet */
  function openPrivacyBubbleSheet() {
    const kmLabel = `${(state.bubbleRadiusM / 1000).toFixed(1)} km`;
    openModal(`
      <div class="sheet-grabber-bar" style="align-self:center;"></div>
      <div>
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("privacyBubble"))}</div>
        <div class="row-sub">${escapeHtml(t("privacyBubbleSub"))}</div>
      </div>
      <div class="segmented" style="height:42px;border-radius:21px;">
        <button class="seg-btn ${!state.sharingPaused ? "active-blue" : ""}" style="border-radius:17px;" id="bub-exact">${escapeHtml(t("exactGps"))}</button>
        <button class="seg-btn ${state.sharingPaused ? "active-blue" : ""}" style="border-radius:17px;" id="bub-on">${escapeHtml(t("privacyBubble"))}</button>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
          <span style="font-weight:700;font-size:14px;">${escapeHtml(t("bubbleRadius"))}</span>
          <span class="tnum" id="bub-rad-val" style="font-weight:800;color:var(--action);">${escapeHtml(kmLabel)}</span>
        </div>
        <input type="range" class="bubble-range" id="bub-slider" min="500" max="3000" step="100" value="${state.bubbleRadiusM}" />
        <div style="display:flex;justify-content:space-between;font-size:12px;color:var(--ink-muted);" class="tnum">
          <span>500 m</span><span>3 km</span>
        </div>
      </div>
      <div>
        <div style="font-weight:700;font-size:14px;margin-bottom:8px;">${escapeHtml(t("autoExpireDur"))}</div>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;">
          ${[1, 3, 6, 24].map((h) => `
            <button type="button" class="seg-btn ${state.bubbleDurationH === h ? "active-blue" : ""}" style="min-height:36px;border-radius:16px;border:1px solid var(--line);" data-bub-dur="${h}">${h}h</button>
          `).join("")}
        </div>
      </div>
      <div class="row-sub" style="font-size:12px;">${escapeHtml(t("bubbleSosNote"))}</div>
      <button class="btn btn-primary" id="bub-cta">
        ${escapeHtml(state.sharingPaused ? t("resumeLiveBtn") : t("activateBubbleCta", { r: kmLabel }))}
      </button>
    `);

    const slider = document.getElementById("bub-slider");
    const valEl = document.getElementById("bub-rad-val");
    const ctaBtn = document.getElementById("bub-cta");
    slider.oninput = () => {
      state.bubbleRadiusM = Number(slider.value) || 1200;
      const lbl = `${(state.bubbleRadiusM / 1000).toFixed(1)} km`;
      valEl.textContent = lbl;
      if (!state.sharingPaused) ctaBtn.textContent = t("activateBubbleCta", { r: lbl });
    };
    document.querySelectorAll("[data-bub-dur]").forEach((btn) => {
      btn.onclick = () => {
        state.bubbleDurationH = Number(btn.getAttribute("data-bub-dur")) || 3;
        openPrivacyBubbleSheet();
      };
    });
    document.getElementById("bub-exact").onclick = async () => {
      if (state.sharingPaused) await togglePrivacyBubble();
      closeModal();
    };
    document.getElementById("bub-on").onclick = async () => {
      if (!state.sharingPaused) await togglePrivacyBubble();
      closeModal();
    };
    ctaBtn.onclick = async () => {
      await togglePrivacyBubble();
      closeModal();
    };
  }

  /* FigCraft 4.4 Quick Photo Check-In Modal */
  function openCheckInModal() {
    const placeChips = (state.places || []).slice(0, 4).map((p) =>
      `<button type="button" class="filter-pill" data-ci-suggest="${escapeHtml(p.name)}">${escapeHtml(p.name)}</button>`
    ).join("");
    openModal(`
      <div class="sheet-grabber-bar" style="align-self:center;"></div>
      <div>
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("quickCheckInTitle"))}</div>
        <div class="row-sub" style="font-size:13px;">${escapeHtml(t("quickCheckInSub"))}</div>
      </div>
      <div class="ci-photo-drop" id="ci-drop-frame">
        ${figcraftEmptyArt("checkinPreview")}
        <span id="ci-retake" class="ci-retake-pill">${escapeHtml(t("pickPhotoPrompt"))}</span>
        <img id="ci-preview" class="hidden" alt="" />
        <input type="file" id="ci-file" accept="image/*" capture="environment" class="ci-file-overlay" aria-label="${escapeHtml(t("pickPhotoPrompt"))}" />
      </div>
      <input type="text" id="ci-note" class="input" placeholder="${escapeHtml(t("noteOptional"))}" aria-label="${escapeHtml(t("noteOptional"))}" />
      ${placeChips ? `<div class="filter-row">${placeChips}</div>` : ""}
      <input type="text" id="ci-place" class="input" placeholder="${escapeHtml(t("placeOptional"))}" aria-label="${escapeHtml(t("placeOptional"))}" />
      <div style="font-size:12px;font-weight:600;color:var(--action);">${escapeHtml(t("exifNote"))}</div>
      <button class="btn btn-primary" id="ci-submit">${escapeHtml(t("sendCheckIn24h"))}</button>
      <button class="btn btn-secondary" id="ci-cancel" style="min-height:40px;">${escapeHtml(t("cancel"))}</button>
    `);
    let previewUrl = "";
    const fileInp = document.getElementById("ci-file");
    const prevImg = document.getElementById("ci-preview");
    const retakePill = document.getElementById("ci-retake");
    fileInp.onchange = () => {
      const f = fileInp.files[0];
      if (!f) return;
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = URL.createObjectURL(f);
      prevImg.src = previewUrl;
      prevImg.classList.remove("hidden");
      retakePill.textContent = t("retakePhoto");
    };
    document.querySelectorAll("[data-ci-suggest]").forEach((chip) => {
      chip.onclick = () => {
        const inp = document.getElementById("ci-place");
        if (inp) inp.value = chip.getAttribute("data-ci-suggest") || "";
      };
    });
    document.getElementById("ci-cancel").onclick = () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      closeModal();
    };
    document.getElementById("ci-submit").onclick = async () => {
      const file = document.getElementById("ci-file").files[0];
      if (!file) return showToast(t("photoRequired"));
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
        if (previewUrl) URL.revokeObjectURL(previewUrl);
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

  function bindPlacesHandlers(inModal) {
    if (inModal) {
      document.getElementById("pl-close").onclick = closeModal;
    }
    document.querySelectorAll("[data-pl-suggest]").forEach((btn) => {
      btn.onclick = () => {
        const inp = document.getElementById("pl-name");
        if (inp) inp.value = btn.getAttribute("data-pl-suggest") || "";
      };
    });
    document.querySelectorAll("[data-modal-bell]").forEach((btn) => {
      btn.onclick = () => {
        const id = btn.getAttribute("data-modal-bell");
        const next = !(state.placeBells[id] !== false);
        state.placeBells[id] = next;
        persistState();
        showToast(next ? t("bellOnToast") : t("bellOffToast"));
        if (inModal) openPlacesModal();
        else render();
      };
    });
    document.querySelectorAll("[data-del-place]").forEach((btn) => {
      btn.onclick = async () => {
        const id = Number(btn.getAttribute("data-del-place"));
        btn.disabled = true;
        try {
          await apiFetch(`/api/places/${id}`, { method: "DELETE" });
          state.places = state.places.filter((p) => p.id !== id);
          if (inModal) openPlacesModal();
          render();
        } catch (err) {
          btn.disabled = false;
          showToast(err.message);
        }
      };
    });
    document.getElementById("pl-add").onclick = async () => {
      const addBtn = document.getElementById("pl-add");
      const name = document.getElementById("pl-name").value.trim();
      const radius_m = Number(document.getElementById("pl-radius").value) || 150;
      if (!name) return showToast(t("nameLocationRequired"));
      addBtn.disabled = true;
      try {
        if (state.meLocation?.lat == null) {
          const ps = await apiFetch("/api/pair/state").catch(() => null);
          if (ps?.me_location) state.meLocation = ps.me_location;
        }
        const lat = state.meLocation?.lat ?? mapInstance?.getCenter()?.lat ?? state.partnerLocation?.lat;
        const lon = state.meLocation?.lon ?? mapInstance?.getCenter()?.lng ?? state.partnerLocation?.lon;
        if (lat == null) {
          addBtn.disabled = false;
          return showToast(t("nameLocationRequired"));
        }
        const created = await apiFetch("/api/places", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name, lat, lon, radius_m })
        });
        state.places.push(created);
        if (inModal) openPlacesModal();
        render();
      } catch (err) {
        addBtn.disabled = false;
        showToast(err.message);
      }
    };
  }

  function openPlacesModal() {
    openModal(buildSavedPlacesContentHtml(true));
    bindPlacesHandlers(true);
  }

  /* FigCraft 6.4 History / 24h Route Modal */
  function openRoute24hModal() {
    const pts = state.routePoints || [];
    const stopsHtml = pts.slice(-8).reverse().map((pt, idx) => `
      <div class="route-stop-card">
        <span class="route-spine-dot ${idx === 0 ? "current" : ""}"></span>
        <div style="min-width:0;flex:1;">
          <div style="font-weight:700;font-size:14px;">${escapeHtml(idx === 0 ? (partnerCurrentPlace()?.name || t("liveNow")) : `${pt.lat.toFixed(4)}, ${pt.lon.toFixed(4)}`)}</div>
          <div class="row-sub tnum" style="font-size:12px;">${escapeHtml(pt.speed_mps > 0.6 ? t("movingSpeed", { k: Math.round(pt.speed_mps * 3.6) }) : t("livePin"))} · ${escapeHtml(formatTime(pt.recorded_at))}</div>
        </div>
        <span class="tnum" style="font-size:12px;font-weight:700;color:${idx === 0 ? "var(--action)" : "var(--ink-muted)"};">${escapeHtml(idx === 0 ? t("updatedNow") : formatTime(pt.recorded_at))}</span>
      </div>
    `).join("");

    openModal(`
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("route24hTitle"))}</div>
        <span class="chip chip-action tnum">${pts.length} pts</span>
      </div>
      <div class="section-label" style="padding:0;">${escapeHtml(t("dwellTripsLbl"))}</div>
      <div style="display:flex;flex-direction:column;gap:8px;">
        ${stopsHtml || `<div class="card"><div class="row-sub">${escapeHtml(t("noStopsYet"))}</div></div>`}
      </div>
      <div class="row-sub" style="font-size:12px;">${escapeHtml(t("dwellAutoPruneNote"))}</div>
      <button class="btn btn-primary" id="route-close">${escapeHtml(t("close"))}</button>
    `);
    document.getElementById("route-close").onclick = closeModal;
  }

  /* FigCraft 4.5 Map Layers & Style Modal */
  function openSwitcherModal() {
    const activeKey = resolveMapThemeKey();
    openModal(`
      <div class="sheet-grabber-bar" style="align-self:center;"></div>
      <div>
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("switcherTitle"))}</div>
        <div class="row-sub">${escapeHtml(t("switcherSub"))}</div>
      </div>
      <div class="layer-swatches">
        <button type="button" class="layer-swatch ${state.mapTileStyle === "light" || (state.mapTileStyle === "auto" && activeKey === "light") ? "active" : ""}" style="background:#F4F3EE;" data-map-style="light">
          <span class="layer-swatch-water" style="background:#CFE2F5;"></span>
          <span class="layer-swatch-bar" style="background:rgba(255,255,252,0.92);color:#0055FF;">${escapeHtml(t("mapTileLight"))}</span>
        </button>
        <button type="button" class="layer-swatch ${state.mapTileStyle === "dark" || (state.mapTileStyle === "auto" && activeKey === "dark") ? "active" : ""}" style="background:#0C1018;" data-map-style="dark">
          <span class="layer-swatch-water" style="background:#112238;"></span>
          <span class="layer-swatch-bar" style="background:rgba(9,11,16,0.86);color:#FAF9F5;">${escapeHtml(t("mapTileDark"))}</span>
        </button>
        <button type="button" class="layer-swatch ${state.mapTileStyle === "terrain" ? "active" : ""}" style="background:#E9F0E6;" data-map-style="terrain">
          <span class="layer-swatch-water" style="background:#BFD6EC;"></span>
          <span class="layer-swatch-bar" style="background:rgba(255,255,252,0.92);color:#111318;">${escapeHtml(t("mapTileTerrain"))}</span>
        </button>
      </div>
      <div class="grouped-list">
        <div class="grouped-row">
          <div>
            <div style="font-weight:600;">${escapeHtml(t("geofenceLayerTitle"))}</div>
            <div class="row-sub tnum" style="font-size:12px;">${escapeHtml(t("geofenceLayerSub", { n: state.places.length }))}</div>
          </div>
          <button class="switch ${state.showPlacesLayer ? "on" : ""}" id="sw-modal-places" role="switch" aria-checked="${state.showPlacesLayer}" aria-label="${escapeHtml(t("geofenceLayerTitle"))}"></button>
        </div>
        <div class="grouped-row">
          <div>
            <div style="font-weight:600;">${escapeHtml(t("routeLayerTitle"))}</div>
            <div class="row-sub" style="font-size:12px;">${escapeHtml(t("routeLayerSub"))}</div>
          </div>
          <button class="switch ${state.showRoute24h ? "on" : ""}" id="sw-modal-route" role="switch" aria-checked="${state.showRoute24h}" aria-label="${escapeHtml(t("route24h"))}"></button>
        </div>
        <div class="grouped-row">
          <div>
            <div style="font-weight:600;">${escapeHtml(t("checkinPinsToggle"))}</div>
            <div class="row-sub" style="font-size:12px;">${escapeHtml(t("checkinPinsSub"))}</div>
          </div>
          <button class="switch ${state.showCheckinPins ? "on" : ""}" id="sw-modal-pins" role="switch" aria-checked="${state.showCheckinPins}" aria-label="${escapeHtml(t("checkinPinsToggle"))}"></button>
        </div>
        <div class="grouped-row">
          <div>
            <div style="font-weight:600;">${escapeHtml(t("privacyBubble"))}</div>
            <div class="row-sub" style="font-size:12px;">${escapeHtml(t("pauseSharing"))}</div>
          </div>
          <button class="switch ${state.sharingPaused ? "on" : ""}" id="sw-modal-bubble" role="switch" aria-checked="${state.sharingPaused}" aria-label="${escapeHtml(t("privacyBubble"))}"></button>
        </div>
      </div>
      <div style="display:flex;gap:8px;">
        <button class="btn btn-secondary" style="flex:1;" id="sw-modal-auto">${escapeHtml(t("mapTileAuto"))}</button>
        <button class="btn btn-primary" style="flex:1;" id="sw-modal-close">${escapeHtml(t("close"))}</button>
      </div>
    `);
    document.getElementById("sw-modal-close").onclick = closeModal;
    document.getElementById("sw-modal-auto").onclick = () => {
      state.mapTileStyle = "auto";
      persistState();
      closeModal();
      render();
    };
    document.getElementById("sw-modal-places").onclick = () => {
      state.showPlacesLayer = !state.showPlacesLayer;
      closeModal();
      render();
    };
    document.getElementById("sw-modal-bubble").onclick = async () => {
      await togglePrivacyBubble();
      closeModal();
    };
    document.getElementById("sw-modal-route").onclick = async () => {
      state.showRoute24h = !state.showRoute24h;
      if (state.showRoute24h) await fetchRoute24h(false);
      closeModal();
      render();
    };
    document.getElementById("sw-modal-pins").onclick = () => {
      state.showCheckinPins = !state.showCheckinPins;
      localStorage.setItem("ours_checkin_pins", state.showCheckinPins ? "1" : "0");
      closeModal();
      render();
    };
    document.querySelectorAll("[data-map-style]").forEach((b) => {
      b.onclick = () => {
        state.mapTileStyle = b.getAttribute("data-map-style");
        persistState();
        closeModal();
        render();
      };
    });
  }

  /* FigCraft 5.4 Alerts / Pair Activity Inbox */
  function openAlertsModal() {
    state.unreadAlerts = 0;
    const pName = state.partner?.name || "Partner";
    const filteredEvents = state.events.filter((ev) => {
      const f = state.alertsFilter;
      if (f === "arrivals") return ev.type === "place" || ev.type === "place_enter" || ev.type === "place_exit";
      if (f === "checkins") return ev.type === "checkin" || ev.type === "highlight";
      if (f === "reactions") return ev.type === "nudge" || ev.type === "chat_message" || ev.type === "reply";
      return true;
    });
    const rows = filteredEvents.slice(0, 20).map((ev) => {
      const isPlace = ev.type === "place" || ev.type === "place_enter" || ev.type === "place_exit";
      const isExit = ev.type === "place_exit" || (ev.type === "place" && ev.data?.transition === "exit");
      let msg = ev.type;
      let cta = t("badgePrivacy");
      if (ev.type === "nudge") {
        msg = t("partnerNudged");
        cta = t("badgeNudge");
      } else if (ev.type === "sos") {
        msg = t("sosTriggered");
        cta = t("badgeSos");
      } else if (isPlace) {
        msg = t(isExit ? "leftPlace" : "arrivedAt", { name: pName, place: ev.data?.place_name || t("places") });
        cta = t("badgePlace");
      } else if (ev.type === "checkin") {
        const placeSuffix = ev.data?.checkin?.place_name ? ` · ${ev.data.checkin.place_name}` : "";
        msg = t("eventCheckin", { name: pName, place: placeSuffix });
        cta = t("badgeCheckin");
      } else if (ev.type === "chat_message") {
        msg = t("eventMessage", { name: pName });
        cta = t("badgeChat");
      } else if (ev.type === "reply") {
        msg = t("eventReply", { name: pName });
        cta = t("badgeChat");
      } else if (ev.type === "highlight") {
        msg = t(ev.data?.highlighted ? "eventHighlight" : "eventUnhighlight", { name: pName });
        cta = t("badgeCheckin");
      } else if (ev.type === "sharing") {
        msg = t(ev.data?.paused ? "eventPaused" : "eventResumed", { name: pName });
        cta = t("badgePrivacy");
      } else if (ev.type === "avatar") {
        msg = t("eventAvatar", { name: pName });
      }
      return `
        <div class="card" style="padding:12px 14px;border-radius:20px;">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:10px;">
            <div style="display:flex;align-items:flex-start;gap:10px;min-width:0;">
              <span class="live-dot ${ev.type === "sos" ? "danger" : "action"}" style="width:10px;height:10px;margin-top:5px;"></span>
              <div style="min-width:0;">
                <div style="font-weight:700;font-size:15px;">${escapeHtml(msg)}</div>
                <div class="row-sub tnum" style="font-size:13px;">${escapeHtml(formatRelative(ev.created_at))} · ${escapeHtml(formatTime(ev.created_at))}</div>
              </div>
            </div>
            <div style="display:flex;flex-direction:column;align-items:flex-end;gap:8px;flex-shrink:0;">
              <span class="tnum" style="font-size:12px;font-weight:700;color:${ev.type === "sos" ? "var(--danger)" : "var(--action)"};">${escapeHtml(formatTime(ev.created_at))}</span>
              <span class="chip ${ev.type === "sos" ? "chip-danger" : "chip-action"}" style="min-height:26px;">${escapeHtml(cta)}</span>
            </div>
          </div>
        </div>
      `;
    }).join("");

    openModal(`
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
        <div style="font-size:20px;font-weight:800;letter-spacing:-0.02em;">${escapeHtml(t("activityInbox"))}</div>
        <button class="chip chip-action" style="border:none;" id="alerts-mark-read">${escapeHtml(t("markAllRead"))}</button>
      </div>
      <div class="filter-row">
        <button class="filter-pill ${state.alertsFilter === "all" ? "active" : ""}" data-alert-filter="all">${escapeHtml(t("all"))}</button>
        <button class="filter-pill ${state.alertsFilter === "arrivals" ? "active" : ""}" data-alert-filter="arrivals">${escapeHtml(t("arrivalsTab"))}</button>
        <button class="filter-pill ${state.alertsFilter === "checkins" ? "active" : ""}" data-alert-filter="checkins">${escapeHtml(t("checkinsTab"))}</button>
        <button class="filter-pill ${state.alertsFilter === "reactions" ? "active" : ""}" data-alert-filter="reactions">${escapeHtml(t("reactionsTab"))}</button>
      </div>
      <div class="section-label" style="padding:0 2px;">${escapeHtml(t("newToday"))}</div>
      <div style="display:flex;flex-direction:column;gap:8px;">${rows || `<div class="card"><span class="row-sub">${escapeHtml(t("noEventsYet"))}</span></div>`}</div>
      <button class="btn btn-secondary" id="alerts-close">${escapeHtml(t("close"))}</button>
    `);
    document.querySelectorAll("[data-alert-filter]").forEach((btn) => {
      btn.onclick = () => {
        state.alertsFilter = btn.getAttribute("data-alert-filter");
        openAlertsModal();
      };
    });
    document.getElementById("alerts-mark-read").onclick = () => {
      state.unreadAlerts = 0;
      showToast("Marked all read");
      closeModal();
      render();
    };
    document.getElementById("alerts-close").onclick = () => { closeModal(); render(); };
  }

  /* FigCraft 6.6 Settings / Security Shield Modal (#S1-#S9) */
  function openSecurityAuditModal() {
    const items = SECURITY_CONTROLS.map(([code, title, desc]) => `
      <div class="sec-row-card">
        <span class="sec-badge tnum">${escapeHtml(code)}</span>
        <div style="min-width:0;flex:1;">
          <div style="font-weight:700;font-size:14px;">${escapeHtml(title)}</div>
          <div class="row-sub" style="font-size:12px;">${escapeHtml(desc)}</div>
        </div>
      </div>
    `).join("");
    openModal(`
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
        <div style="display:flex;align-items:center;gap:8px;font-size:20px;font-weight:800;color:var(--ink);">
          ${svgIcon("shield")}
          <span>${escapeHtml(t("securityModalTitle"))}</span>
        </div>
        <button class="icon-btn" id="sec-x" aria-label="${escapeHtml(t("close"))}">×</button>
      </div>
      <div class="waiting-pulse-row" style="justify-content:flex-start;">
        <span class="waiting-pulse-dot" style="animation:none;"></span>
        <span>${escapeHtml(t("hardeningActive"))}</span>
      </div>
      <div style="display:flex;flex-direction:column;gap:8px;">${items}</div>
      <button class="btn btn-primary" id="sec-close">${escapeHtml(t("close"))}</button>
    `);
    document.getElementById("sec-close").onclick = closeModal;
    document.getElementById("sec-x").onclick = closeModal;
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
      await executeSosSend();
      closeModal();
    };
  }

  async function executeSosSend() {
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
      showToast(t("sosSent"));
    } catch (err) {
      showToast(err.message);
    }
  }

  async function togglePrivacyBubble() {
    const next = !state.sharingPaused;
    try {
      await apiFetch("/api/sharing", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ paused: next })
      });
      state.sharingPaused = next;
      persistAndSyncNative();
      haptic();
      showToast(next ? t("privacyOnToast") : t("privacyOffToast"));
      render();
    } catch (err) {
      showToast(err.message);
    }
  }

  /* FigCraft 5.3 Full Dark Photo Lightbox */
  function openLightbox(photoId) {
    const item = [...state.checkins, ...state.memories].find((c) => String(c.photo_id) === String(photoId));
    const cap = item?.note || item?.place_name || t("checkIn");
    const who = item ? (item.author_id === state.me?.id ? (state.me?.name || "Me") : (state.partner?.name || "Partner")) : "";
    const meta = item ? `${who} · ${formatTime(item.created_at)}` : "";
    openModal(`
      <div style="display:flex;justify-content:flex-end;">
        <button class="icon-btn" id="lb-close" style="background:rgba(21,27,40,0.85);color:#FAF9F5;border-color:rgba(255,255,255,0.22);" aria-label="${escapeHtml(t("close"))}">×</button>
      </div>
      <img class="lightbox-img" data-photo-id="${escapeHtml(photoId)}" data-photo-size="full" alt="" />
      ${
        item
          ? `<div class="lightbox-caption-bar">
              <div style="min-width:0;">
                <div style="font-weight:700;font-size:15px;">${escapeHtml(cap)}</div>
                <div style="font-size:12px;color:#C5CAD6;">${escapeHtml(meta)}</div>
              </div>
              <button class="btn btn-primary" style="min-height:36px;padding:0 18px;" id="lb-save" data-hl="${item.highlighted}">${escapeHtml(item.highlighted ? t("saved") : t("save"))}</button>
            </div>`
          : ""
      }
    `, true, "lightbox-dark");
    hydratePhotos();
    document.getElementById("lb-close").onclick = closeModal;
    const lbSave = document.getElementById("lb-save");
    if (lbSave && item) {
      lbSave.onclick = async () => {
        const isHl = lbSave.getAttribute("data-hl") === "true";
        try {
          await apiFetch(`/api/checkins/${item.id}/highlight`, { method: isHl ? "DELETE" : "POST" });
          await syncAllData();
          closeModal();
        } catch (err) {
          showToast(err.message);
        }
      };
    }
  }

  function wireHoldSosButton(btn, fill) {
    if (!btn || !fill) return;
    let holdStart = 0;
    let triggered = false;
    const startHold = (e) => {
      if (e?.preventDefault) e.preventDefault();
      clearInterval(sosHoldTimer);
      holdStart = Date.now();
      triggered = false;
      sosHolding = true;
      sosHoldTimer = setInterval(() => {
        const progress = Math.min(1, (Date.now() - holdStart) / 2000);
        fill.style.transform = `scaleX(${progress})`;
        if (progress >= 1 && !triggered) {
          triggered = true;
          sosHolding = false;
          clearInterval(sosHoldTimer);
          fill.style.transform = "scaleX(0)";
          executeSosSend();
        }
      }, 50);
    };
    const endHold = () => {
      sosHolding = false;
      clearInterval(sosHoldTimer);
      fill.style.transform = "scaleX(0)";
      if (!triggered && Date.now() - holdStart < 1800 && holdStart > 0) {
        showToast(t("holdSosHint"));
      }
      holdStart = 0;
      if (deferredRenderAfterSos) {
        deferredRenderAfterSos = false;
        render();
      }
    };
    btn.oncontextmenu = (e) => e.preventDefault();
    btn.onpointerdown = startHold;
    btn.onpointerup = endHold;
    btn.onpointerleave = endHold;
    btn.onpointercancel = endHold;
  }

  function recenterMapSmart(immediate = false) {
    if (immediate !== true) haptic();
    if (!mapInstance) return;
    const p = state.partnerLocation;
    const m = state.meLocation;
    const vh = window.innerHeight || 820;
    const dur = immediate === true ? 0 : 650;
    if (state.partnerFocused && p?.lat != null && !p.paused && !state.partnerPaused) {
      const focusPad = window.innerWidth < 1024
        ? { top: 96, left: 40, right: 40, bottom: Math.min(Math.round(vh * 0.54), 460) }
        : { top: 155, left: 330, right: 90, bottom: 245 };
      mapInstance.flyTo({ center: [p.lon, p.lat], zoom: 14.5, padding: focusPad, duration: dur });
      return;
    }
    const pad = window.innerWidth < 1024
      ? { top: 105, left: 48, right: 68, bottom: Math.min(Math.round(vh * 0.54), 455) }
      : { top: 155, left: 330, right: 90, bottom: 245 };
    if (p?.lat != null && !p.paused && !state.partnerPaused && m?.lat != null && haversineM(m.lat, m.lon, p.lat, p.lon) <= 100000) {
      const bounds = new maplibregl.LngLatBounds([m.lon, m.lat], [m.lon, m.lat]).extend([p.lon, p.lat]);
      mapInstance.fitBounds(bounds, { padding: pad, maxZoom: 14.5, duration: dur });
    } else if (p?.lat != null && !p.paused && !state.partnerPaused) {
      mapInstance.flyTo({ center: [p.lon, p.lat], zoom: 14.5, padding: pad, duration: dur });
    } else if (m?.lat != null) {
      mapInstance.flyTo({ center: [m.lon, m.lat], zoom: 14.5, padding: pad, duration: dur });
    }
  }

  function bindMapOverlayHandlers() {
    document.getElementById("btn-map-settings")?.addEventListener("click", () => { haptic(); state.tab = "settings"; render(); });
    document.getElementById("btn-map-switcher")?.addEventListener("click", openSwitcherModal);
    document.getElementById("btn-map-alerts")?.addEventListener("click", openAlertsModal);
    document.getElementById("btn-map-checkin")?.addEventListener("click", openCheckInModal);
    document.getElementById("btn-peek-checkin")?.addEventListener("click", openCheckInModal);
    document.getElementById("btn-peek-manage-places")?.addEventListener("click", openPlacesModal);
    document.getElementById("btn-focus-back")?.addEventListener("click", () => { haptic(); state.partnerFocused = false; render(); });
    document.getElementById("btn-focus-route")?.addEventListener("click", openRoute24hModal);
    document.getElementById("btn-focus-bubble")?.addEventListener("click", openPrivacyBubbleSheet);
    document.getElementById("btn-focus-alerts")?.addEventListener("click", openAlertsModal);

    document.getElementById("btn-map-layers")?.addEventListener("click", openSwitcherModal);

    document.getElementById("btn-map-route")?.addEventListener("click", async () => {
      haptic();
      state.showRoute24h = !state.showRoute24h;
      if (state.showRoute24h) await fetchRoute24h(false);
      render();
    });

    document.getElementById("btn-map-bubble")?.addEventListener("click", openPrivacyBubbleSheet);

    const edgeBtn = document.getElementById("btn-edge-partner");
    if (edgeBtn) {
      edgeBtn.onclick = () => {
        if (mapInstance && state.partnerLocation?.lat != null) {
          const pad = window.innerWidth < 1024 ? { top: 80, left: 32, right: 32, bottom: 260 } : { top: 80, left: 320, right: 80, bottom: 140 };
          mapInstance.flyTo({ center: [state.partnerLocation.lon, state.partnerLocation.lat], zoom: 15, padding: pad });
        }
      };
    }

    document.getElementById("btn-map-center")?.addEventListener("click", recenterMapSmart);

    document.getElementById("btn-sheet-cycle")?.addEventListener("click", () => {
      haptic();
      if (state.partnerFocused) {
        state.partnerFocused = false;
        render();
        return;
      }
      const cycle = { collapsed: "peek", peek: "expanded", expanded: "collapsed" };
      state.sheetDetent = cycle[state.sheetDetent] || "peek";
      render();
    });

    document.getElementById("peek-header-tap")?.addEventListener("click", (e) => {
      if (e.target.closest("#btn-peek-nudge")) return;
      haptic();
      state.partnerFocused = true;
      render();
      recenterMapSmart(false);
    });

    document.querySelectorAll("[data-sheet-tab]").forEach((btn) => {
      btn.onclick = () => {
        haptic();
        state.sheetTab = btn.getAttribute("data-sheet-tab") || "all";
        render();
      };
    });

    document.getElementById("btn-peek-resume")?.addEventListener("click", togglePrivacyBubble);
    document.getElementById("btn-peek-trail-toggle")?.addEventListener("click", async () => {
      haptic();
      state.showRoute24h = !state.showRoute24h;
      if (state.showRoute24h) await fetchRoute24h(false);
      render();
    });
    document.getElementById("btn-peek-route-modal")?.addEventListener("click", openRoute24hModal);

    const doNudge = async () => {
      haptic();
      triggerNudgeWave();
      try {
        await apiFetch("/api/nudge", { method: "POST" });
        showToast(t("nudgeSent"));
      } catch (err) {
        showToast(err.message);
      }
    };
    document.getElementById("btn-peek-nudge")?.addEventListener("click", doNudge);
    document.getElementById("dsk-dock-nudge")?.addEventListener("click", doNudge);
    document.getElementById("btn-peek-chat")?.addEventListener("click", () => { haptic(); state.tab = "chat"; render(); });

    // Desktop Map Util & Dock bindings
    document.getElementById("dsk-switcher-open")?.addEventListener("click", openSwitcherModal);
    document.getElementById("dsk-btn-route")?.addEventListener("click", async () => {
      state.showRoute24h = !state.showRoute24h;
      if (state.showRoute24h) await fetchRoute24h(false);
      render();
    });
    document.getElementById("dsk-btn-layers")?.addEventListener("click", openSwitcherModal);
    document.getElementById("dsk-btn-bubble")?.addEventListener("click", openPrivacyBubbleSheet);
    document.getElementById("dsk-btn-pause")?.addEventListener("click", togglePrivacyBubble);
    document.getElementById("dsk-btn-alerts")?.addEventListener("click", openAlertsModal);
    document.getElementById("dsk-dock-checkin")?.addEventListener("click", openCheckInModal);
    document.getElementById("dsk-dock-center")?.addEventListener("click", recenterMapSmart);
    document.getElementById("dsk-dock-places")?.addEventListener("click", openPlacesModal);

    document.querySelectorAll("[data-quick-react]").forEach((chip) => {
      chip.onclick = async () => {
        const msgTxt = chip.getAttribute("data-quick-react");
        haptic();
        try {
          const sent = await apiFetch("/api/messages", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: msgTxt })
          });
          state.messages = [sent, ...state.messages];
          showToast(t("sentReaction", { msg: msgTxt }));
        } catch (err) {
          showToast(err.message);
        }
      };
    });

    document.querySelectorAll("[data-toggle-bell]").forEach((btn) => {
      btn.onclick = () => {
        const id = btn.getAttribute("data-toggle-bell");
        const next = !(state.placeBells[id] !== false);
        state.placeBells[id] = next;
        persistState();
        showToast(next ? t("bellOnToast") : t("bellOffToast"));
        render();
      };
    });

    wireHoldSosButton(document.getElementById("btn-hold-sos"), document.getElementById("sos-hold-fill"));
    wireHoldSosButton(document.getElementById("dsk-btn-sos"), document.getElementById("dsk-sos-fill"));
  }

  function bindNavAndRailHandlers() {
    document.querySelectorAll("[data-tab]").forEach((btn) => {
      btn.onclick = () => {
        haptic();
        state.tab = btn.getAttribute("data-tab");
        state.partnerFocused = false;
        render();
      };
    });
    document.querySelectorAll("[data-rail-bell]").forEach((btn) => {
      btn.onclick = () => {
        const id = btn.getAttribute("data-rail-bell");
        const next = !(state.placeBells[id] !== false);
        state.placeBells[id] = next;
        persistState();
        showToast(next ? t("bellOnToast") : t("bellOffToast"));
        render();
      };
    });
    document.getElementById("btn-rail-heart")?.addEventListener("click", async () => {
      haptic();
      triggerNudgeWave();
      try {
        await apiFetch("/api/nudge", { method: "POST" });
        showToast(t("nudgeSent"));
      } catch (err) {
        showToast(err.message);
      }
    });
  }

  function bindHandlers() {
    bindNavAndRailHandlers();

    if (state.tab === "map") {
      initOrUpdateMap();
      bindMapOverlayHandlers();
    }

    if (state.tab === "places") {
      bindPlacesHandlers(false);
    }

    if (state.tab === "memories") {
      document.getElementById("seg-mem-timeline").onclick = () => { state.memoriesMode = "timeline"; render(); };
      document.getElementById("seg-mem-map").onclick = () => { state.memoriesMode = "map"; render(); };
      const si = document.getElementById("mem-search-input");
      if (si) si.oninput = (e) => {
        state.memoriesQuery = e.target.value;
        render();
        const nextSi = document.getElementById("mem-search-input");
        if (nextSi) {
          nextSi.focus();
          nextSi.setSelectionRange(nextSi.value.length, nextSi.value.length);
        }
      };
      const cb = document.getElementById("btn-mem-clear");
      if (cb) cb.onclick = () => { state.memoriesQuery = ""; state.memoriesFilter = "all"; render(); };
      document.querySelectorAll("[data-mem-filter]").forEach((b) => {
        b.onclick = () => { state.memoriesFilter = b.getAttribute("data-mem-filter"); render(); };
      });
      if (state.memoriesMode === "map" && typeof maplibregl !== "undefined") {
        const c = document.getElementById("memories-map");
        if (c) {
          const center = [state.partnerLocation?.lon ?? 106.8272, state.partnerLocation?.lat ?? -6.1754];
          const mm = new maplibregl.Map({
            container: c,
            style: buildMapStyle(resolveMapThemeKey(), center),
            center,
            zoom: 12,
            attributionControl: false
          });
          mm.on("error", () => {});
          state.memories.filter((m) => m.lat != null).forEach((m) => {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "map-checkin-pin";
            btn.setAttribute("aria-label", m.place_name || "Memory");
            const img = document.createElement("img");
            img.setAttribute("data-photo-id", String(m.photo_id));
            img.setAttribute("data-photo-size", "thumb");
            img.alt = "";
            btn.appendChild(img);
            btn.onclick = () => {
              state.selectedMemoryId = m.id;
              openLightbox(m.photo_id);
            };
            new maplibregl.Marker({ element: btn }).setLngLat([m.lon, m.lat]).addTo(mm);
          });
          hydratePhotos();
        }
      }
    }

    if (state.tab === "chat") {
      const sc = document.getElementById("chat-scroll");
      if (sc) sc.scrollTop = sc.scrollHeight;
      document.getElementById("btn-chat-alerts")?.addEventListener("click", openAlertsModal);
      document.querySelectorAll("[data-chat-filter]").forEach((b) => {
        b.onclick = () => {
          state.chatFilter = b.getAttribute("data-chat-filter") || "all";
          render();
        };
      });
      const csi = document.getElementById("chat-search-input");
      if (csi) csi.oninput = (e) => {
        state.chatQuery = e.target.value;
        render();
        const nextCsi = document.getElementById("chat-search-input");
        if (nextCsi) {
          nextCsi.focus();
          nextCsi.setSelectionRange(nextCsi.value.length, nextCsi.value.length);
        }
      };
      const cr = document.getElementById("btn-cancel-reply");
      if (cr) cr.onclick = () => { state.replyToCheckInId = null; render(); };
      document.getElementById("btn-chat-camera").onclick = openCheckInModal;
      const sendFn = async () => {
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
      document.getElementById("btn-chat-send").onclick = sendFn;
      document.getElementById("chat-input").onkeydown = (e) => {
        if (e.key === "Enter") sendFn();
      };
    }

    if (state.tab === "settings") {
      document.getElementById("btn-security-audit").onclick = openSecurityAuditModal;
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
      document.getElementById("sw-pause").onclick = togglePrivacyBubble;
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
          <div class="row-sub">${escapeHtml(t("bgGuideText"))}</div>
          <button class="btn btn-primary" id="bg-ok">${escapeHtml(t("close"))}</button>
        `, true);
        document.getElementById("bg-ok").onclick = closeModal;
      };
      document.getElementById("btn-open-sos").onclick = openSosModal;
      document.getElementById("btn-test-conn").onclick = async () => {
        const nextUrl = sanitizeServerUrl(document.getElementById("input-server-url").value) || PRODUCTION_URL;
        state.serverUrl = nextUrl;
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
        const nextUrl = sanitizeServerUrl(document.getElementById("input-server-url").value) || PRODUCTION_URL;
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
        resetClientSession();
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
      const openFn = () => openLightbox(img.getAttribute("data-lightbox"));
      img.onclick = openFn;
      img.onkeydown = (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          openFn();
        }
      };
    });

    hydratePhotos();
  }

  /* FigCraft 03 Desktop Rail & Context Inspector */
  function renderDesktopRail() {
    const pName = state.partner?.name || "Partner";
    const pInit = pName.slice(0, 1).toUpperCase();
    const mName = state.me?.name || "User";
    const mInit = mName.slice(0, 1).toUpperCase();
    const st = partnerStatusSummary();
    const loc = state.partnerLocation;
    const batTxt = loc?.battery_pct != null ? `${loc.battery_pct}%` : "100%";
    const spdKmh = loc?.speed_mps > 0.6 ? `${Math.round(loc.speed_mps * 3.6)} km/h` : "";
    const distTxt = state.meLocation?.lat != null && loc?.lat != null && !loc?.paused && !state.partnerPaused
      ? formatDist(haversineM(state.meLocation.lat, state.meLocation.lon, loc.lat, loc.lon))
      : "";
    const placesRows = (state.places || []).slice(0, 3).map((pl) => {
      const bellOn = state.placeBells[String(pl.id)] !== false;
      return `
        <div class="rail-place-row">
          <div style="display:flex;align-items:center;gap:7px;min-width:0;">
            <span class="live-dot ${bellOn ? "action" : "paused"}"></span>
            <div style="min-width:0;">
              <div style="font-size:12px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(pl.name)}</div>
              <div class="row-sub tnum" style="font-size:10px;">${Math.round(pl.radius_m)}m</div>
            </div>
          </div>
          <button type="button" class="rail-bell-pill ${bellOn ? "on" : ""}" data-rail-bell="${pl.id}">
            ${escapeHtml(bellOn ? t("bellOnBadge") : t("bellMutedBadge"))}
          </button>
        </div>
      `;
    }).join("");

    return `
      <aside class="desktop-rail" aria-label="Workspace Navigation">
        <div style="display:flex;flex-direction:column;gap:11px;">
          <div class="desktop-rail-brand">
            <div class="onb-squircle" style="width:42px;height:42px;border-radius:11px;">${oursRingsLogo(32)}</div>
            <div style="min-width:0;">
              <div style="font-weight:800;font-size:17px;letter-spacing:-0.02em;line-height:1.1;">OURS</div>
              <div class="row-sub" style="font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(state.spaceName || `${mName} & ${pName}`)}</div>
            </div>
          </div>

          <div class="rail-partner-tile">
            <div style="display:flex;align-items:center;gap:10px;">
              <div class="avatar-circle" style="width:38px;height:38px;position:relative;">${escapeHtml(pInit)}</div>
              <div style="min-width:0;flex:1;">
                <div style="font-weight:700;font-size:14px;">${escapeHtml(pName)}</div>
                <div class="tnum" style="font-size:12px;font-weight:600;color:${st.live ? "var(--success)" : "var(--ink-muted)"};">${escapeHtml(st.label)}</div>
              </div>
              <button type="button" class="peek-heart-btn" style="width:32px;height:32px;background:var(--cta-grad);color:var(--on-action);" id="btn-rail-heart" aria-label="${escapeHtml(t("nudge"))}">${svgIcon("heart")}</button>
            </div>
            <div class="row-sub tnum" style="font-size:12px;">
              ${escapeHtml([distTxt, spdKmh, batTxt].filter(Boolean).join(" · "))}
            </div>
          </div>

          <nav class="desktop-rail-nav">
            <button class="rail-item ${state.tab === "map" ? "active" : ""}" data-tab="map">${svgIcon("map")}<span>${escapeHtml(t("tabMap"))}</span></button>
            <button class="rail-item ${state.tab === "chat" ? "active" : ""}" data-tab="chat">${svgIcon("chat")}<span>${escapeHtml(t("tabChat"))}</span>${state.messages.length ? `<span class="rail-item-badge tnum">${state.messages.length}</span>` : ""}</button>
            <button class="rail-item ${state.tab === "memories" ? "active" : ""}" data-tab="memories">${svgIcon("memories")}<span>${escapeHtml(t("tabMemories"))}</span><span class="rail-item-badge tnum">${state.memories.length}</span></button>
            <button class="rail-item ${state.tab === "places" ? "active" : ""}" data-tab="places">${svgIcon("pin")}<span>${escapeHtml(t("tabPlaces"))}</span><span class="rail-item-badge tnum">${state.places.length}</span></button>
            <button class="rail-item ${state.tab === "settings" ? "active" : ""}" data-tab="settings">${svgIcon("settings")}<span>${escapeHtml(t("settings"))}</span></button>
          </nav>

          ${
            placesRows
              ? `<div class="rail-places-card">
                  <div class="section-label" style="padding:0 2px;">SAVED PLACES · 1-TAP BELLS</div>
                  ${placesRows}
                </div>`
              : ""
          }
        </div>

        <div style="display:flex;align-items:center;gap:10px;padding:10px;border-radius:18px;background:var(--surface-muted);border:1px solid var(--line);">
          <span class="peek-me-av" style="width:34px;height:34px;font-size:13px;">${escapeHtml(mInit)}</span>
          <div style="min-width:0;">
            <div style="font-size:12px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(state.me?.email || "")}</div>
            <div class="tnum" style="font-size:11px;font-weight:600;color:var(--success);">${escapeHtml(t("googleVerified"))} · 9/9 Active</div>
          </div>
        </div>
      </aside>
    `;
  }

  function renderDesktopInspector() {
    if (state.tab !== "chat" && state.tab !== "memories") return "";
    const topMem = state.memories[0] || state.checkins[0] || null;
    return `
      <aside class="desktop-inspector" aria-label="Context Panel">
        <div style="font-size:16px;font-weight:700;">${escapeHtml(t("desktopInspTitle"))}</div>
        <div class="section-label" style="padding:0;">${escapeHtml(t("desktopRecentMem", { n: state.memories.length }))}</div>
        ${
          topMem
            ? `<div class="memory-card" style="padding:10px;border-radius:20px;">
                 <div class="memory-media-wrap">
                   <img class="memory-photo" style="height:132px;" data-photo-id="${escapeHtml(topMem.photo_id)}" data-photo-size="thumb" data-lightbox="${escapeHtml(topMem.photo_id)}" alt="" />
                   ${topMem.place_name ? `<div class="memory-place-badge" style="font-size:10px;padding:3px 9px;">${escapeHtml(topMem.place_name)}</div>` : ""}
                 </div>
                 <div style="font-weight:700;font-size:13px;">${escapeHtml(topMem.note || topMem.place_name || t("checkIn"))}</div>
                 <div class="row-sub tnum" style="font-size:11px;">${escapeHtml(formatDateShort(topMem.created_at))} · ${escapeHtml(formatTime(topMem.created_at))}</div>
               </div>`
            : `<div class="card"><div class="row-sub">${escapeHtml(t("noMemories"))}</div></div>`
        }
        <div class="card" style="background:var(--action-soft-grad);border-color:var(--border-glass);padding:14px;">
          <div style="font-weight:700;font-size:14px;color:var(--action);margin-bottom:6px;">${escapeHtml(t("desktopLiveSyncTitle"))}</div>
          <div class="row-sub" style="font-size:12px;line-height:1.55;">
            • ${escapeHtml(t("desktopLiveLine1"))}<br/>
            • ${escapeHtml(t("desktopLiveLine2"))}<br/>
            • ${escapeHtml(t("desktopLiveLine3"))}<br/>
            • ${escapeHtml(t("desktopLiveLine4"))}
          </div>
        </div>
      </aside>
    `;
  }

  function render() {
    const app = document.getElementById("app");
    if (!state.accessToken || (state.inviteCode && !state.partner)) {
      if (mapInstance) {
        try { mapInstance.remove(); } catch (_) {}
        mapInstance = null;
        meMarker = null;
        partnerMarker = null;
        placeMarkers = [];
        checkinMapMarkers = [];
      }
      renderOnboarding();
      return;
    }

    const existingMap = document.getElementById("ours-map");
    const existingOverlay = document.getElementById("map-overlay-root");
    if (state.tab === "map" && existingMap && existingOverlay && app.classList.contains("with-nav")) {
      if (sosHolding) {
        deferredRenderAfterSos = true;
        return;
      }
      document.querySelectorAll("[data-tab]").forEach((btn) => {
        btn.classList.toggle("active", btn.getAttribute("data-tab") === "map");
      });
      const existingRail = app.querySelector(".desktop-rail");
      if (existingRail) {
        const nextRailHtml = renderDesktopRail();
        if (existingRail.__lastHtml !== nextRailHtml) {
          const tmp = document.createElement("div");
          tmp.innerHTML = nextRailHtml.trim();
          const newRail = tmp.firstElementChild;
          if (newRail) {
            newRail.__lastHtml = nextRailHtml;
            existingRail.replaceWith(newRail);
            bindNavAndRailHandlers();
          }
        }
      }
      const nextOverlayHtml = renderMapOverlayHtml();
      if (existingOverlay.__lastHtml !== nextOverlayHtml) {
        existingOverlay.__lastHtml = nextOverlayHtml;
        existingOverlay.classList.add("map-overlay-settled");
        existingOverlay.innerHTML = nextOverlayHtml;
        bindMapOverlayHandlers();
        hydratePhotos();
      }
      initOrUpdateMap();
      return;
    }

    if (mapInstance) {
      try {
        savedMapCamera = { center: mapInstance.getCenter(), zoom: mapInstance.getZoom() };
      } catch (_) {}
      try { mapInstance.remove(); } catch (_) {}
      mapInstance = null;
      meMarker = null;
      partnerMarker = null;
      placeMarkers = [];
      checkinMapMarkers = [];
    }

    let bodyHtml = "";
    if (state.tab === "map") bodyHtml = renderMapScreen();
    else if (state.tab === "memories") bodyHtml = renderMemoriesScreen();
    else if (state.tab === "chat") bodyHtml = renderChatScreen();
    else if (state.tab === "places") bodyHtml = renderPlacesTabScreen();
    else bodyHtml = renderSettingsScreen();

    const inspectorHtml = renderDesktopInspector();

    app.className = `app-shell with-nav${state.tab === "map" ? " desktop-map-mode" : ""}`;
    app.innerHTML = `
      ${
        state.tab !== "map"
          ? `<div class="ambient-orbs" aria-hidden="true"><span class="ambient-orb orb-top"></span><span class="ambient-orb orb-mid"></span><span class="ambient-orb orb-bot"></span></div>`
          : ""
      }
      ${renderDesktopRail()}
      <div class="screen-stage">
        <div class="desktop-context-grid">
          <div class="desktop-main-col">${bodyHtml}</div>
          ${inspectorHtml}
        </div>
      </div>
      <nav class="bottom-nav" aria-label="Main navigation">
        <button class="nav-item ${state.tab === "map" ? "active" : ""}" data-tab="map">${svgIcon("map")}<span>${escapeHtml(t("tabMap"))}</span></button>
        <button class="nav-item ${state.tab === "chat" ? "active" : ""}" data-tab="chat">${svgIcon("chat")}${state.unreadAlerts > 0 ? `<span class="nav-unread-dot"></span>` : ""}<span>${escapeHtml(t("tabChat"))}</span></button>
        <button class="nav-item ${state.tab === "memories" ? "active" : ""}" data-tab="memories">${svgIcon("memories")}<span>${escapeHtml(t("tabMemories"))}</span></button>
        <button class="nav-item ${state.tab === "settings" ? "active" : ""}" data-tab="settings">${svgIcon("settings")}<span>${escapeHtml(t("tabSettings"))}</span></button>
      </nav>
    `;
    bindHandlers();
  }

  function boot() {
    inheritNativeState();
    applyTheme();

    const splashSub = document.getElementById("splash-sub");
    if (splashSub) splashSub.textContent = t("appTagline");

    if ("serviceWorker" in navigator && !native) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeModal();
    });

    const reducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches;
    setTimeout(() => {
      const splash = document.getElementById("splash");
      const app = document.getElementById("app");
      if (splash) splash.classList.add("fade-out");
      if (app) app.classList.remove("hidden");
      setTimeout(() => splash?.remove(), reducedMotion ? 80 : 340);
    }, reducedMotion ? 120 : 620);

    loadServerConfig();
    render();
    if (state.accessToken) {
      syncAllData();
      connectWebSocket();
      startBrowserLocationWatch();
    }
  }

  boot();
})();
