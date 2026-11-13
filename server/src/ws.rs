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

async fn send_with_timeout<S>(ws_tx: &mut S, msg: Message) -> bool
where
    S: SinkExt<Message> + Unpin,
{
    matches!(
        tokio::time::timeout(Duration::from_secs(10), ws_tx.send(msg)).await,
        Ok(Ok(()))
    )
}

async fn handle_socket(state: AppState, user_id: i64, socket: WebSocket) {
    let conn_id = state.next_ws_conn_id();
    let (tx, mut rx) = mpsc::channel::<String>(128);
    {
        let mut guard = state.ws_senders.write().await;
        guard.entry(user_id).or_default().push((conn_id, tx));
    }

    let (mut ws_tx, mut ws_rx) = socket.split();
    let (pong_tx, mut pong_rx) = mpsc::channel::<Message>(16);

    let mut write_task = tokio::spawn(async move {
        let mut ping_interval = tokio::time::interval(Duration::from_secs(30));
        ping_interval.tick().await;
        loop {
            tokio::select! {
                _ = ping_interval.tick() => {
                    if !send_with_timeout(&mut ws_tx, Message::Ping(Vec::new().into())).await {
                        break;
                    }
                }
                msg = pong_rx.recv() => {
                    let Some(msg) = msg else { break; };
                    if !send_with_timeout(&mut ws_tx, msg).await {
                        break;
                    }
                }
                text = rx.recv() => {
                    let Some(text) = text else { break; };
                    if !send_with_timeout(&mut ws_tx, Message::Text(text.into())).await {
                        break;
                    }
                }
            }
        }
        let _ = tokio::time::timeout(Duration::from_secs(2), ws_tx.close()).await;
    });

    let read_task = async {
        while let Some(Ok(msg)) = ws_rx.next().await {
            match msg {
                Message::Text(txt) => {
                    if let Ok(parsed) = serde_json::from_str::<ClientMsg>(&txt)
                        && parsed.msg_type.as_deref() == Some("ping")
                    {
                        let _ = pong_tx.try_send(Message::Text(r#"{"type":"pong"}"#.into()));
                    }
                }
                Message::Close(_) => break,
                _ => {}
            }
        }
    };

    tokio::select! {
        _ = &mut write_task => {},
        _ = read_task => {
            write_task.abort();
        }
    }

    let mut guard = state.ws_senders.write().await;
    if let Some(list) = guard.get_mut(&user_id) {
        list.retain(|(id, _)| *id != conn_id);
        if list.is_empty() {
            guard.remove(&user_id);
        }
    }
}
