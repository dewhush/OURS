use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use tokio::sync::{RwLock, mpsc};

use crate::config::Config;
use crate::db::Db;
use crate::error::{AppError, AppResult};
use crate::fcm::FcmClient;
use crate::models::Event;

pub type WsSenders = Arc<RwLock<HashMap<i64, Vec<(u64, mpsc::Sender<String>)>>>>;

#[derive(Clone, Default)]
pub struct SlidingLimiter {
    attempts: Arc<Mutex<HashMap<String, Vec<Instant>>>>,
}

impl SlidingLimiter {
    pub fn check(&self, key: &str, max_attempts: usize, window: Duration, msg: &str) -> AppResult<()> {
        let now = Instant::now();
        let mut guard = self
            .attempts
            .lock()
            .map_err(|_| AppError::internal("rate limit lock poisoned"))?;
        guard.retain(|_, timestamps| {
            timestamps.retain(|t| now.duration_since(*t) < window);
            !timestamps.is_empty()
        });
        if guard.get(key).is_some_and(|v| v.len() >= max_attempts) {
            return Err(AppError::rate_limited(msg));
        }
        Ok(())
    }

    pub fn record(&self, key: &str) {
        if let Ok(mut guard) = self.attempts.lock() {
            guard.entry(key.to_string()).or_default().push(Instant::now());
        }
    }

    pub fn clear(&self, key: &str) {
        if let Ok(mut guard) = self.attempts.lock() {
            guard.remove(key);
        }
    }
}

#[derive(Clone)]
pub struct CachedJwks {
    pub keys: HashMap<String, (String, String)>,
    pub fetched_at: Instant,
}

#[derive(Clone)]
pub struct AppState {
    pub config: Config,
    pub db: Db,
    pub ws_senders: WsSenders,
    pub fcm: FcmClient,
    pub http_client: reqwest::Client,
    google_jwks: Arc<RwLock<Option<CachedJwks>>>,
    // Dedicated JWKS cache for Google's OAuth certs, separate from the Pocket ID
    // OIDC JWKS (which google_jwks backs when oidc_jwks_url is set). Legacy-Google
    // id_tokens (iss=accounts.google.com) must verify against Google's own keys,
    // not the self-hosted IdP's.
    google_oauth_jwks: Arc<RwLock<Option<CachedJwks>>>,
    firebase_jwks: Arc<RwLock<Option<CachedJwks>>>,
    next_ws_id: Arc<AtomicU64>,
    join_failures: SlidingLimiter,
    create_failures: SlidingLimiter,
    refresh_failures: SlidingLimiter,
    google_auth_failures: SlidingLimiter,
    user_action_limiter: SlidingLimiter,
}

impl AppState {
    pub fn new(config: Config, db: Db, fcm: FcmClient) -> Self {
        let http_client = reqwest::Client::builder()
            .timeout(Duration::from_secs(6))
            .build()
            .unwrap_or_else(|_| reqwest::Client::new());
        Self {
            config,
            db,
            ws_senders: Arc::new(RwLock::new(HashMap::new())),
            fcm,
            http_client,
            google_jwks: Arc::new(RwLock::new(None)),
            google_oauth_jwks: Arc::new(RwLock::new(None)),
            firebase_jwks: Arc::new(RwLock::new(None)),
            next_ws_id: Arc::new(AtomicU64::new(1)),
            join_failures: SlidingLimiter::default(),
            create_failures: SlidingLimiter::default(),
            refresh_failures: SlidingLimiter::default(),
            google_auth_failures: SlidingLimiter::default(),
            user_action_limiter: SlidingLimiter::default(),
        }
    }

    /// Per-user sliding-window limiter for authenticated mutation endpoints
    /// (nudge, SOS, messages, check-ins, places). Keyed on `"{user_id}:{action}"`.
    pub fn check_user_action(&self, user_id: i64, action: &str, max: usize, window: Duration) -> AppResult<()> {
        let key = format!("{user_id}:{action}");
        self.user_action_limiter.check(&key, max, window, "too many requests; slow down")
    }

    pub fn record_user_action(&self, user_id: i64, action: &str) {
        self.user_action_limiter.record(&format!("{user_id}:{action}"))
    }

    pub fn next_ws_conn_id(&self) -> u64 {
        self.next_ws_id.fetch_add(1, Ordering::Relaxed)
    }

    pub fn check_join_rate_limit(&self, key: &str) -> AppResult<()> {
        self.join_failures.check(
            key,
            10,
            Duration::from_secs(60),
            "too many failed invite attempts; try again in a minute",
        )
    }

    pub fn record_join_failure(&self, key: &str) {
        self.join_failures.record(key);
    }

    pub fn check_create_rate_limit(&self, key: &str) -> AppResult<()> {
        self.create_failures.check(
            key,
            10,
            Duration::from_secs(60),
            "too many failed setup key attempts; try again in a minute",
        )
    }

    pub fn record_create_failure(&self, key: &str) {
        self.create_failures.record(key);
    }

    pub fn check_refresh_rate_limit(&self, key: &str) -> AppResult<()> {
        self.refresh_failures.check(
            key,
            20,
            Duration::from_secs(60),
            "too many failed refresh attempts; try again in a minute",
        )
    }

    pub fn record_refresh_failure(&self, key: &str) {
        self.refresh_failures.record(key);
    }

    pub fn clear_join_rate_limit(&self, key: &str) {
        self.join_failures.clear(key);
    }

    pub fn clear_create_rate_limit(&self, key: &str) {
        self.create_failures.clear(key);
    }

    pub fn clear_refresh_rate_limit(&self, key: &str) {
        self.refresh_failures.clear(key);
    }

    pub fn check_google_auth_rate_limit(&self, key: &str) -> AppResult<()> {
        self.google_auth_failures.check(
            key,
            12,
            Duration::from_secs(60),
            "too many sign-in attempts; try again in a minute",
        )
    }

    pub fn record_google_auth_failure(&self, key: &str) {
        self.google_auth_failures.record(key);
    }

    /// Raise the pre-check ceiling for a key instead of fully clearing it.
    /// Clearing the entire window on a single success creates a legitimate-user
    /// DoS: an attacker can exhaust the pre-check by sending garbage tokens for
    /// the victim's IP right before the victim's real login, and the victim's
    /// request would then land on a fresh-but-quota-exhausted window. By raising
    /// (not clearing), a successful auth does not reset an attacker's progress
    /// toward the ceiling.
    pub fn raise_google_auth_rate_limit(&self, key: &str) {
        let _ = self.google_auth_failures.check(key, 50, Duration::from_secs(600), "");
    }

    /// Fetch a signing key by `kid`. Uses the configured OIDC provider's JWKS
    /// endpoint when `OURS_OIDC_JWKS_URL`/`OURS_OIDC_ISSUER` is set, otherwise
    /// falls back to Google's certs endpoint. Returns (modulus_n, exponent_e)
    /// for RSA verification.
    pub async fn get_google_jwk(&self, kid: &str, force_refresh: bool) -> AppResult<Option<(String, String)>> {
        const JWKS_TTL: Duration = Duration::from_secs(3600);
        if !force_refresh {
            let guard = self.google_jwks.read().await;
            if let Some(cached) = guard.as_ref()
                && cached.fetched_at.elapsed() < JWKS_TTL
                && let Some(pair) = cached.keys.get(kid)
            {
                return Ok(Some(pair.clone()));
            }
        }

        let jwks_url = self
            .config
            .oidc_jwks_url
            .clone()
            .unwrap_or_else(|| "https://www.googleapis.com/oauth2/v3/certs".to_string());

        let resp = self
            .http_client
            .get(&jwks_url)
            .send()
            .await
            .map_err(|e| AppError::internal(format!("failed to fetch jwks from {jwks_url}: {e}")))?;
        if !resp.status().is_success() {
            return Err(AppError::internal("jwks endpoint error"));
        }
        #[derive(serde::Deserialize)]
        struct JwkEntry {
            kid: Option<String>,
            n: Option<String>,
            e: Option<String>,
        }
        #[derive(serde::Deserialize)]
        struct JwksDoc {
            keys: Vec<JwkEntry>,
        }
        let doc: JwksDoc = resp
            .json()
            .await
            .map_err(|e| AppError::internal(format!("invalid google certs json: {e}")))?;
        let mut keys = HashMap::new();
        for k in doc.keys {
            if let (Some(id), Some(n), Some(e)) = (k.kid, k.n, k.e) {
                keys.insert(id, (n, e));
            }
        }
        let found = keys.get(kid).cloned();
        let mut guard = self.google_jwks.write().await;
        *guard = Some(CachedJwks {
            keys,
            fetched_at: Instant::now(),
        });
        Ok(found)
    }

    /// Fetch a signing key by `kid` from Google's OAuth certs endpoint
    /// (iss=accounts.google.com). Always pinned to Google's certs URL regardless
    /// of the Pocket ID OIDC configuration — the legacy-Google id_token path must
    /// verify against Google's own keys, never the self-hosted IdP's.
    pub async fn get_google_oauth_jwk(&self, kid: &str, force_refresh: bool) -> AppResult<Option<(String, String)>> {
        const JWKS_TTL: Duration = Duration::from_secs(3600);
        if !force_refresh {
            let guard = self.google_oauth_jwks.read().await;
            if let Some(cached) = guard.as_ref()
                && cached.fetched_at.elapsed() < JWKS_TTL
                && let Some(pair) = cached.keys.get(kid)
            {
                return Ok(Some(pair.clone()));
            }
        }
        const GOOGLE_OAUTH_JWKS_URL: &str = "https://www.googleapis.com/oauth2/v3/certs";
        let resp = self
            .http_client
            .get(GOOGLE_OAUTH_JWKS_URL)
            .send()
            .await
            .map_err(|e| AppError::internal(format!("failed to fetch google oauth jwks: {e}")))?;
        if !resp.status().is_success() {
            return Err(AppError::internal("google oauth jwks endpoint error"));
        }
        #[derive(serde::Deserialize)]
        struct JwkEntry {
            kid: Option<String>,
            n: Option<String>,
            e: Option<String>,
        }
        #[derive(serde::Deserialize)]
        struct JwksDoc {
            keys: Vec<JwkEntry>,
        }
        let doc: JwksDoc = resp
            .json()
            .await
            .map_err(|e| AppError::internal(format!("invalid google oauth certs json: {e}")))?;
        let mut keys = HashMap::new();
        for k in doc.keys {
            if let (Some(id), Some(n), Some(e)) = (k.kid, k.n, k.e) {
                keys.insert(id, (n, e));
            }
        }
        let found = keys.get(kid).cloned();
        let mut guard = self.google_oauth_jwks.write().await;
        *guard = Some(CachedJwks {
            keys,
            fetched_at: Instant::now(),
        });
        Ok(found)
    }

    /// Fetch a Firebase Auth signing key by `kid` from Google's securetoken JWKS
    /// endpoint. Returns (modulus_n, exponent_e) for RSA verification.
    pub async fn get_firebase_jwk(&self, kid: &str, force_refresh: bool) -> AppResult<Option<(String, String)>> {
        const JWKS_TTL: Duration = Duration::from_secs(3600);
        if !force_refresh {
            let guard = self.firebase_jwks.read().await;
            if let Some(cached) = guard.as_ref()
                && cached.fetched_at.elapsed() < JWKS_TTL
                && let Some(pair) = cached.keys.get(kid)
            {
                return Ok(Some(pair.clone()));
            }
        }

        const FIREBASE_JWKS_URL: &str =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

        let resp = self
            .http_client
            .get(FIREBASE_JWKS_URL)
            .send()
            .await
            .map_err(|e| AppError::internal(format!("failed to fetch firebase jwks: {e}")))?;
        if !resp.status().is_success() {
            return Err(AppError::internal("firebase jwks endpoint error"));
        }
        #[derive(serde::Deserialize)]
        struct JwkEntry {
            kid: Option<String>,
            n: Option<String>,
            e: Option<String>,
        }
        #[derive(serde::Deserialize)]
        struct JwksDoc {
            keys: Vec<JwkEntry>,
        }
        let doc: JwksDoc = resp
            .json()
            .await
            .map_err(|e| AppError::internal(format!("invalid firebase jwks json: {e}")))?;
        let mut keys = HashMap::new();
        for k in doc.keys {
            if let (Some(id), Some(n), Some(e)) = (k.kid, k.n, k.e) {
                keys.insert(id, (n, e));
            }
        }
        let found = keys.get(kid).cloned();
        let mut guard = self.firebase_jwks.write().await;
        *guard = Some(CachedJwks {
            keys,
            fetched_at: Instant::now(),
        });
        Ok(found)
    }

    pub async fn push_ws_event(&self, recipient_id: i64, event: &Event) {
        let Ok(json) = serde_json::to_string(event) else {
            return;
        };
        let mut guard = self.ws_senders.write().await;
        if let Some(senders) = guard.get_mut(&recipient_id) {
            senders.retain(|(_, tx)| tx.try_send(json.clone()).is_ok());
            if senders.is_empty() {
                guard.remove(&recipient_id);
            }
        }
    }

    pub async fn disconnect_ws(&self, user_id: i64) {
        self.ws_senders.write().await.remove(&user_id);
    }
}
