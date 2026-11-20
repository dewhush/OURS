use std::time::Duration;

use axum::{
    extract::{
        Query, State, WebSocketUpgrade,
        ws::{Message, WebSocket},
    },
    http::{HeaderMap, header},
    response::Response,
};
use futures_util::{SinkExt, StreamExt};
use rusqlite::{OptionalExtension, params};
use serde::Deserialize;
use tokio::sync::mpsc;

use crate::auth::verify_access_claims;
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

fn extract_authority_host(raw: &str) -> String {
    let without_scheme = raw
        .trim()
        .strip_prefix("https://")
        .or_else(|| raw.trim().strip_prefix("http://"))
        .unwrap_or(raw.trim());
    let authority = without_scheme.split('/').next().unwrap_or("");
    if let Some(bracketed) = authority.strip_prefix('[') {
        bracketed.split(']').next().unwrap_or("").to_ascii_lowercase()
    } else {
        authority
            .split(':')
            .next()
            .unwrap_or("")
            .to_ascii_lowercase()
    }
}

fn is_loopback_host(host: &str) -> bool {
    matches!(host, "localhost" | "127.0.0.1" | "::1" | "10.0.2.2")
}

fn validate_ws_origin(headers: &HeaderMap) -> AppResult<()> {
    let Some(origin) = headers.get(header::ORIGIN).and_then(|v| v.to_str().ok()) else {
        return Ok(());
    };
    let trimmed = origin.trim();
    if trimmed.is_empty() {
        return Ok(());
    }
    if trimmed == "file://" || trimmed == "null" {
        // Offline Android WebView asset fallback emits file:// or null Origin on local upgrades.
        let host = headers
            .get(header::HOST)
            .and_then(|v| v.to_str().ok())
            .map(extract_authority_host)
            .unwrap_or_default();
        if trimmed == "file://" || is_loopback_host(&host) {
            return Ok(());
        }
        return Err(AppError::forbidden("untrusted websocket origin"));
    }

    let origin_host = extract_authority_host(trimmed);
    let req_host = headers
        .get("X-Forwarded-Host")
        .or_else(|| headers.get(header::HOST))
        .and_then(|v| v.to_str().ok())
        .map(extract_authority_host)
        .unwrap_or_default();

    if !origin_host.is_empty()
        && (origin_host == req_host || (is_loopback_host(&origin_host) && is_loopback_host(&req_host)))
    {
        return Ok(());
    }

    Err(AppError::forbidden("untrusted websocket origin"))
}

fn extract_ws_subprotocol_token(headers: &HeaderMap) -> (Option<&str>, bool) {
    let Some(raw) = headers.get("Sec-WebSocket-Protocol").and_then(|v| v.to_str().ok()) else {
        return (None, false);
    };
    let mut token = None;
    let mut has_ours_proto = false;
    for part in raw.split(',').map(str::trim).filter(|s| !s.is_empty()) {
        if part == "ours.v1" {
            has_ours_proto = true;
        } else if let Some(rest) = part.strip_prefix("bearer.")
            && !rest.is_empty()
        {
            token = Some(rest);
        }
    }
    (token, has_ours_proto)
}

pub async fn ws_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
    Query(query): Query<WsQuery>,
    ws: WebSocketUpgrade,
) -> AppResult<Response> {
    validate_ws_origin(&headers)?;

    let header_token = headers
        .get(header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer ").or_else(|| v.strip_prefix("bearer ")))
        .map(str::trim)
        .filter(|t| !t.is_empty());

    let (proto_token, has_ours_proto) = extract_ws_subprotocol_token(&headers);

    let token = header_token
        .or(proto_token)
        .or_else(|| {
            query
                .token
                .as_deref()
                .map(str::trim)
                .filter(|t| !t.is_empty())
        })
        .ok_or_else(|| AppError::unauthorized("missing token"))?;

    if header_token.is_none() && proto_token.is_none() {
        tracing::warn!(
            "websocket authenticated via ?token= query string; use Authorization header \
             or Sec-WebSocket-Protocol bearer.<token> to avoid token leakage in logs"
        );
    }

    let now = now_ms();
    let claims = verify_access_claims(&state.config.token_secret, token, now)?;
    let user_id = claims.user_id;

    let db = state.db.clone();
    let exists = tokio::task::spawn_blocking(move || -> AppResult<bool> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let found: Option<i64> = conn
            .query_row(
                "SELECT COALESCE(tokens_revoked_before, 0) FROM users WHERE id = ?1",
                params![user_id],
                |r| r.get(0),
            )
            .optional()?;
        if let Some(revoked_before) = found {
            if revoked_before > 0 && claims.iat_ms <= revoked_before {
                return Err(AppError::unauthorized("access token revoked"));
            }
            conn.execute(
                "UPDATE users SET last_seen_at = ?1 WHERE id = ?2",
                params![now, user_id],
            )?;
            Ok(true)
        } else {
            Ok(false)
        }
    })
    .await??;

    if !exists {
        return Err(AppError::unauthorized("user no longer exists"));
    }

    let ws = if has_ours_proto {
        ws.protocols(["ours.v1"])
    } else {
        ws
    };

    Ok(ws
        .max_message_size(64 * 1024)
        .max_frame_size(64 * 1024)
        .on_upgrade(move |socket| handle_socket(state, user_id, socket)))
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
        let list = guard.entry(user_id).or_default();
        if list.len() >= 8 {
            list.remove(0);
        }
        list.push((conn_id, tx));
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
                        let db = state.db.clone();
                        tokio::task::spawn_blocking(move || {
                            if let Ok(conn) = db.lock() {
                                let now = now_ms();
                                let stale_cutoff = now - 60_000;
                                let _ = conn.execute(
                                    "UPDATE users SET last_seen_at = ?1
                                     WHERE id = ?2 AND (last_seen_at IS NULL OR last_seen_at <= ?3)",
                                    params![now, user_id, stale_cutoff],
                                );
                            }
                        });
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
