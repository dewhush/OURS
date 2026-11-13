use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use tokio::sync::{RwLock, mpsc};

use crate::config::Config;
use crate::db::Db;
use crate::error::{AppError, AppResult};
use crate::fcm::FcmClient;
use crate::models::Event;

pub type WsSenders = Arc<RwLock<HashMap<i64, Vec<mpsc::UnboundedSender<String>>>>>;

#[derive(Clone, Default)]
pub struct SlidingLimiter {
    attempts: Arc<Mutex<Vec<Instant>>>,
}

impl SlidingLimiter {
    pub fn check(&self, max_attempts: usize, window: Duration, msg: &str) -> AppResult<()> {
        let now = Instant::now();
        let mut guard = self
            .attempts
            .lock()
            .map_err(|_| AppError::internal("rate limit lock poisoned"))?;
        guard.retain(|t| now.duration_since(*t) < window);
        if guard.len() >= max_attempts {
            return Err(AppError::rate_limited(msg));
        }
        Ok(())
    }

    pub fn record(&self) {
        if let Ok(mut guard) = self.attempts.lock() {
            guard.push(Instant::now());
        }
    }
}

#[derive(Clone)]
pub struct AppState {
    pub config: Config,
    pub db: Db,
    pub ws_senders: WsSenders,
    pub fcm: FcmClient,
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
            join_failures: SlidingLimiter::default(),
            create_failures: SlidingLimiter::default(),
            refresh_failures: SlidingLimiter::default(),
        }
    }

    pub fn check_join_rate_limit(&self) -> AppResult<()> {
        self.join_failures.check(
            10,
            Duration::from_secs(60),
            "too many failed invite attempts; try again in a minute",
        )
    }

    pub fn record_join_failure(&self) {
        self.join_failures.record();
    }

    pub fn check_create_rate_limit(&self) -> AppResult<()> {
        self.create_failures.check(
            10,
            Duration::from_secs(60),
            "too many failed setup key attempts; try again in a minute",
        )
    }

    pub fn record_create_failure(&self) {
        self.create_failures.record();
    }

    pub fn check_refresh_rate_limit(&self) -> AppResult<()> {
        self.refresh_failures.check(
            20,
            Duration::from_secs(60),
            "too many failed refresh attempts; try again in a minute",
        )
    }

    pub fn record_refresh_failure(&self) {
        self.refresh_failures.record();
    }

    pub async fn push_ws_event(&self, recipient_id: i64, event: &Event) {
        let Ok(json) = serde_json::to_string(event) else {
            return;
        };
        let mut guard = self.ws_senders.write().await;
        if let Some(senders) = guard.get_mut(&recipient_id) {
            senders.retain(|tx| tx.send(json.clone()).is_ok());
            if senders.is_empty() {
                guard.remove(&recipient_id);
            }
        }
    }
}
