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
pub struct AppState {
    pub config: Config,
    pub db: Db,
    pub ws_senders: WsSenders,
    pub fcm: FcmClient,
    next_ws_id: Arc<AtomicU64>,
    join_failures: SlidingLimiter,
    create_failures: SlidingLimiter,
    refresh_failures: SlidingLimiter,
}

impl AppState {
    pub fn new(config: Config, db: Db, fcm: FcmClient) -> Self {
        Self {
            config,
            db,
            ws_senders: Arc::new(RwLock::new(HashMap::new())),
            fcm,
            next_ws_id: Arc::new(AtomicU64::new(1)),
            join_failures: SlidingLimiter::default(),
            create_failures: SlidingLimiter::default(),
            refresh_failures: SlidingLimiter::default(),
        }
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
