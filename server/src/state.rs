use std::collections::HashMap;
use std::sync::Arc;

use tokio::sync::{RwLock, mpsc};

use crate::config::Config;
use crate::db::Db;
use crate::fcm::FcmClient;
use crate::models::Event;

pub type WsSenders = Arc<RwLock<HashMap<i64, Vec<mpsc::UnboundedSender<String>>>>>;

#[derive(Clone)]
pub struct AppState {
    pub config: Config,
    pub db: Db,
    pub ws_senders: WsSenders,
    pub fcm: FcmClient,
}

impl AppState {
    pub fn new(config: Config, db: Db, fcm: FcmClient) -> Self {
        Self {
            config,
            db,
            ws_senders: Arc::new(RwLock::new(HashMap::new())),
            fcm,
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
