use std::time::Duration;

use axum::{
    extract::{
        Query, State, WebSocketUpgrade,
        ws::{Message, WebSocket},
    },
    response::Response,
};
use futures_util::{SinkExt, StreamExt};
use rusqlite::{OptionalExtension, params};
use serde::Deserialize;
use tokio::sync::mpsc;

use crate::auth::verify_access_token;
use crate::db::now_ms;
use crate::error::{AppError, AppResult};
use crate::state::AppState;

#[derive(Deserialize)]
pub struct WsQuery {
    pub token: Option<String>,
}

#[derive(Deserialize)]
struct ClientMsg {
    #[serde(rename = "type")]
    msg_type: Option<String>,
}

pub async fn ws_handler(
    State(state): State<AppState>,
    Query(query): Query<WsQuery>,
    ws: WebSocketUpgrade,
) -> AppResult<Response> {
    let token = query
        .token
        .as_deref()
        .map(str::trim)
        .filter(|t| !t.is_empty())
        .ok_or_else(|| AppError::unauthorized("missing token"))?;

    let now = now_ms();
    let user_id = verify_access_token(&state.config.token_secret, token, now)?;

    let db = state.db.clone();
    let exists = tokio::task::spawn_blocking(move || -> AppResult<bool> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let found: Option<i64> = conn
            .query_row(
                "SELECT id FROM users WHERE id = ?1",
                params![user_id],
                |r| r.get(0),
            )
            .optional()?;
        Ok(found.is_some())
    })
    .await??;

    if !exists {
        return Err(AppError::unauthorized("user no longer exists"));
    }

    Ok(ws.on_upgrade(move |socket| handle_socket(state, user_id, socket)))
}

async fn handle_socket(state: AppState, user_id: i64, socket: WebSocket) {
    let (tx, mut rx) = mpsc::unbounded_channel::<String>();
    {
        let mut guard = state.ws_senders.write().await;
        guard.entry(user_id).or_default().push(tx.clone());
    }

    let (mut ws_tx, mut ws_rx) = socket.split();
    let (pong_tx, mut pong_rx) = mpsc::unbounded_channel::<Message>();

    let write_task = tokio::spawn(async move {
        let mut ping_interval = tokio::time::interval(Duration::from_secs(30));
        ping_interval.tick().await;
        loop {
            tokio::select! {
                _ = ping_interval.tick() => {
                    if ws_tx.send(Message::Ping(Vec::new().into())).await.is_err() {
                        break;
                    }
                }
                Some(msg) = pong_rx.recv() => {
                    if ws_tx.send(msg).await.is_err() {
                        break;
                    }
                }
                Some(text) = rx.recv() => {
                    if ws_tx.send(Message::Text(text.into())).await.is_err() {
                        break;
                    }
                }
                else => break,
            }
        }
    });

    while let Some(Ok(msg)) = ws_rx.next().await {
        match msg {
            Message::Text(txt) => {
                if let Ok(parsed) = serde_json::from_str::<ClientMsg>(&txt)
                    && parsed.msg_type.as_deref() == Some("ping")
                {
                    let _ = pong_tx.send(Message::Text(r#"{"type":"pong"}"#.into()));
                }
            }
            Message::Close(_) => break,
            _ => {}
        }
    }

    write_task.abort();
    let mut guard = state.ws_senders.write().await;
    if let Some(list) = guard.get_mut(&user_id) {
        list.retain(|s| !s.same_channel(&tx));
        if list.is_empty() {
            guard.remove(&user_id);
        }
    }
}
