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

#[derive(Clone)]
pub struct AppState {
    pub config: Config,
    pub db: Db,
    pub ws_senders: WsSenders,
    pub fcm: FcmClient,
    join_failures: Arc<Mutex<Vec<Instant>>>,
}

impl AppState {
    pub fn new(config: Config, db: Db, fcm: FcmClient) -> Self {
        Self {
            config,
            db,
            ws_senders: Arc::new(RwLock::new(HashMap::new())),
            fcm,
            join_failures: Arc::new(Mutex::new(Vec::new())),
        }
    }

    pub fn check_join_rate_limit(&self) -> AppResult<()> {
        let now = Instant::now();
        let window = Duration::from_secs(60);
        let mut guard = self
            .join_failures
            .lock()
            .map_err(|_| AppError::internal("rate limit lock poisoned"))?;
        guard.retain(|t| now.duration_since(*t) < window);
        if guard.len() >= 10 {
            return Err(AppError::rate_limited(
                "too many failed invite attempts; try again in a minute",
            ));
        }
        Ok(())
    }

    pub fn record_join_failure(&self) {
        if let Ok(mut guard) = self.join_failures.lock() {
            guard.push(Instant::now());
        }
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
