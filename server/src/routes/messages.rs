use std::collections::HashMap;

use axum::{
    Json,
    extract::{FromRequest, Multipart, Query, Request, State},
    http::{StatusCode, header},
};
use rusqlite::{OptionalExtension, params};
use serde::Deserialize;

use crate::auth::AuthUser;
use crate::db::{
    get_partner_id, get_user_fcm_token, get_user_name, insert_partner_event, now_ms,
};
use crate::error::{AppError, AppResult};
use crate::models::{ChatMessage, Event, MessagesResponse};
use crate::photos::{MAX_PHOTO_BYTES, delete_photo_files, save_jpeg_and_thumb};
use crate::state::AppState;

#[derive(Deserialize)]
struct CreateMessageJson {
    text: Option<String>,
    reply_to_message_id: Option<i64>,
    reply_to_checkin_id: Option<i64>,
}

pub async fn post_message(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    req: Request,
) -> AppResult<(StatusCode, Json<ChatMessage>)> {
    let content_type = req
        .headers()
        .get(header::CONTENT_TYPE)
        .and_then(|v| v.to_str().ok())
        .unwrap_or("")
        .to_ascii_lowercase();

    let mut text: Option<String> = None;
    let mut photo_bytes: Option<Vec<u8>> = None;
    let mut reply_to_message_id: Option<i64> = None;
    let mut reply_to_checkin_id: Option<i64> = None;

    if content_type.starts_with("multipart/form-data") {
        let mut multipart = Multipart::from_request(req, &state)
            .await
            .map_err(|_| AppError::bad_request("invalid multipart request"))?;

        while let Some(field) = multipart
            .next_field()
            .await
            .map_err(|_| AppError::bad_request("malformed multipart body"))?
        {
            let name = field.name().unwrap_or("").to_string();
            match name.as_str() {
                "text" => {
                    let raw = field
                        .text()
                        .await
                        .map_err(|_| AppError::bad_request("invalid text field"))?;
                    let trimmed = raw.trim().to_string();
                    if !trimmed.is_empty() {
                        text = Some(trimmed);
                    }
                }
                "photo" => {
                    let bytes = field
                        .bytes()
                        .await
                        .map_err(|_| AppError::bad_request("failed to read photo field"))?;
                    if bytes.len() > MAX_PHOTO_BYTES {
                        return Err(AppError::too_large("photo exceeds 10 MB limit"));
                    }
                    photo_bytes = Some(bytes.to_vec());
                }
                "reply_to_message_id" => {
                    let raw = field
                        .text()
                        .await
                        .map_err(|_| AppError::bad_request("invalid reply_to_message_id"))?;
                    let trimmed = raw.trim();
                    if !trimmed.is_empty() {
                        reply_to_message_id = Some(
                            trimmed
                                .parse::<i64>()
                                .map_err(|_| AppError::bad_request("invalid reply_to_message_id"))?,
                        );
                    }
                }
                "reply_to_checkin_id" => {
                    let raw = field
                        .text()
                        .await
                        .map_err(|_| AppError::bad_request("invalid reply_to_checkin_id"))?;
                    let trimmed = raw.trim();
                    if !trimmed.is_empty() {
                        reply_to_checkin_id = Some(
                            trimmed
                                .parse::<i64>()
                                .map_err(|_| AppError::bad_request("invalid reply_to_checkin_id"))?,
                        );
                    }
                }
                _ => {}
            }
        }
    } else {
        let Json(payload) = Json::<CreateMessageJson>::from_request(req, &state)
            .await
            .map_err(|_| AppError::bad_request("invalid JSON body"))?;
        if let Some(t) = payload.text {
            let trimmed = t.trim().to_string();
            if !trimmed.is_empty() {
                text = Some(trimmed);
            }
        }
        reply_to_message_id = payload.reply_to_message_id;
        reply_to_checkin_id = payload.reply_to_checkin_id;
    }

    if let Some(ref t) = text
        && t.chars().count() > 2000
    {
        return Err(AppError::bad_request("text exceeds 2000 characters"));
    }

    if text.is_none() && photo_bytes.is_none() {
        return Err(AppError::bad_request("text or photo is required"));
    }

    let data_dir = state.config.data_dir.clone();
    let db = state.db.clone();

    let (msg, ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(ChatMessage, Option<(i64, Event)>, Option<(String, HashMap<String, String>)>)> {
            let photo_id = if let Some(bytes) = photo_bytes {
                Some(save_jpeg_and_thumb(&data_dir, &bytes)?)
            } else {
                None
            };

            let mut conn = match db.lock() {
                Ok(c) => c,
                Err(_) => {
                    if let Some(ref pid) = photo_id {
                        delete_photo_files(&data_dir, pid);
                    }
                    return Err(AppError::internal("database lock poisoned"));
                }
            };
            let tx = conn.transaction()?;

            if let Some(mid) = reply_to_message_id {
                let exists: Option<i64> = tx
                    .query_row("SELECT id FROM messages WHERE id = ?1", params![mid], |r| r.get(0))
                    .optional()?;
                if exists.is_none() {
                    if let Some(ref pid) = photo_id {
                        delete_photo_files(&data_dir, pid);
                    }
                    return Err(AppError::not_found("reply_to_message_id not found"));
                }
            }

            if let Some(cid) = reply_to_checkin_id {
                let exists: Option<i64> = tx
                    .query_row("SELECT id FROM checkins WHERE id = ?1", params![cid], |r| r.get(0))
                    .optional()?;
                if exists.is_none() {
                    if let Some(ref pid) = photo_id {
                        delete_photo_files(&data_dir, pid);
                    }
                    return Err(AppError::not_found("reply_to_checkin_id not found"));
                }
            }

            let now = now_ms();
            tx.execute(
                "INSERT INTO messages (author_id, text, photo_id, reply_to_message_id, reply_to_checkin_id, created_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                params![
                    user_id,
                    text,
                    photo_id,
                    reply_to_message_id,
                    reply_to_checkin_id,
                    now
                ],
            )?;
            let id = tx.last_insert_rowid();
            let msg = ChatMessage {
                id,
                author_id: user_id,
                text,
                photo_id,
                reply_to_message_id,
                reply_to_checkin_id,
                created_at: now,
            };

            let ws_push = insert_partner_event(
                &tx,
                user_id,
                "chat_message",
                serde_json::json!({ "message": &msg }),
                now,
            )?;

            let fcm_push = if let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let body = msg
                            .text
                            .clone()
                            .unwrap_or_else(|| "Sent a photo".to_string());
                        let mut data = HashMap::new();
                        data.insert("type".into(), "chat_message".into());
                        data.insert("event_id".into(), ev.id.to_string());
                        data.insert("title".into(), actor_name);
                        data.insert("body".into(), body);
                        data.insert("message_id".into(), msg.id.to_string());
                        Some((token, data))
                    } else {
                        None
                    }
                } else {
                    None
                }
            } else {
                None
            };

            tx.commit()?;
            Ok((msg, ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok((StatusCode::CREATED, Json(msg)))
}

#[derive(Deserialize)]
pub struct MessagesQuery {
    pub before: Option<i64>,
    pub limit: Option<i64>,
}

pub async fn list_messages(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
    Query(query): Query<MessagesQuery>,
) -> AppResult<Json<MessagesResponse>> {
    let before = query.before.unwrap_or_else(|| now_ms() + 1);
    let limit = query.limit.unwrap_or(50).clamp(1, 200);

    let db = state.db.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<MessagesResponse> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let mut stmt = conn.prepare(
            "SELECT id, author_id, text, photo_id, reply_to_message_id, reply_to_checkin_id, created_at
             FROM messages
             WHERE created_at < ?1
             ORDER BY created_at DESC, id DESC
             LIMIT ?2",
        )?;
        let rows = stmt.query_map(params![before, limit + 1], |r| {
            Ok(ChatMessage {
                id: r.get(0)?,
                author_id: r.get(1)?,
                text: r.get(2)?,
                photo_id: r.get(3)?,
                reply_to_message_id: r.get(4)?,
                reply_to_checkin_id: r.get(5)?,
                created_at: r.get(6)?,
            })
        })?;

        let mut items = Vec::new();
        for r in rows {
            items.push(r?);
        }
        let has_more = items.len() as i64 > limit;
        if has_more {
            items.pop();
        }
        let next_before = if has_more {
            items.last().map(|m| m.created_at)
        } else {
            None
        };

        Ok(MessagesResponse { items, next_before })
    })
    .await??;

    Ok(Json(resp))
}
