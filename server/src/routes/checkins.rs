use std::collections::HashMap;

use axum::{
    Json,
    extract::{Multipart, Path, Query, State},
    http::StatusCode,
};
use rusqlite::{OptionalExtension, params};
use serde::Deserialize;

use crate::auth::AuthUser;
use crate::db::{
    get_checkin_row, get_partner_id, get_user_fcm_token, get_user_name, insert_partner_event,
    load_replies_for_checkin, now_ms,
};
use crate::error::{AppError, AppResult};
use crate::models::{CheckIn, CheckInsResponse, MemoriesResponse, Reply};
use crate::photos::{delete_photo_files, save_jpeg_and_thumb};
use crate::state::AppState;

const CHECKIN_TTL_MS: i64 = 24 * 60 * 60 * 1000;

async fn read_field_bytes_limited(field: axum::extract::multipart::Field<'_>, max_bytes: usize) -> AppResult<Vec<u8>> {
    let bytes = field
        .bytes()
        .await
        .map_err(|_| AppError::bad_request("failed to read multipart field"))?;
    if bytes.len() > max_bytes {
        return Err(AppError::too_large("upload exceeds size limit"));
    }
    Ok(bytes.to_vec())
}

pub async fn create_checkin(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> AppResult<(StatusCode, Json<CheckIn>)> {
    let mut photo_bytes: Option<Vec<u8>> = None;
    let mut note: Option<String> = None;
    let mut lat_str: Option<String> = None;
    let mut lon_str: Option<String> = None;
    let mut place_name: Option<String> = None;

    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|_| AppError::bad_request("malformed multipart body"))?
    {
        let name = field.name().unwrap_or("").to_string();
        match name.as_str() {
            "photo" => {
                let bytes = read_field_bytes_limited(field, crate::photos::MAX_PHOTO_BYTES).await?;
                photo_bytes = Some(bytes);
            }
            "note" => {
                let text = field
                    .text()
                    .await
                    .map_err(|_| AppError::bad_request("invalid note field"))?;
                let trimmed = text.trim().to_string();
                if !trimmed.is_empty() {
                    if trimmed.chars().count() > 500 {
                        return Err(AppError::bad_request("note exceeds 500 characters"));
                    }
                    note = Some(trimmed);
                }
            }
            "lat" => {
                let text = field
                    .text()
                    .await
                    .map_err(|_| AppError::bad_request("invalid lat field"))?;
                let trimmed = text.trim().to_string();
                if !trimmed.is_empty() {
                    lat_str = Some(trimmed);
                }
            }
            "lon" => {
                let text = field
                    .text()
                    .await
                    .map_err(|_| AppError::bad_request("invalid lon field"))?;
                let trimmed = text.trim().to_string();
                if !trimmed.is_empty() {
                    lon_str = Some(trimmed);
                }
            }
            "place_name" => {
                let text = field
                    .text()
                    .await
                    .map_err(|_| AppError::bad_request("invalid place_name field"))?;
                let trimmed = text.trim().to_string();
                if !trimmed.is_empty() {
                    if trimmed.chars().count() > 100 {
                        return Err(AppError::bad_request("place_name exceeds 100 characters"));
                    }
                    place_name = Some(trimmed);
                }
            }
            _ => {}
        }
    }

    let photo_bytes = photo_bytes.ok_or_else(|| AppError::bad_request("photo is required"))?;
    let (lat, lon) = match (lat_str, lon_str) {
        (Some(la), Some(lo)) => {
            let la_f: f64 = la
                .parse()
                .map_err(|_| AppError::bad_request("invalid lat"))?;
            let lo_f: f64 = lo
                .parse()
                .map_err(|_| AppError::bad_request("invalid lon"))?;
            if !(-90.0..=90.0).contains(&la_f) || !(-180.0..=180.0).contains(&lo_f) {
                return Err(AppError::bad_request("lat/lon out of range"));
            }
            (Some(la_f), Some(lo_f))
        }
        (None, None) => (None, None),
        _ => return Err(AppError::bad_request("lat and lon must both be provided")),
    };

    let data_dir = state.config.data_dir.clone();
    let db = state.db.clone();

    let (checkin, ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(CheckIn, Option<(i64, crate::models::Event)>, Option<(String, HashMap<String, String>)>)> {
            let photo_id = save_jpeg_and_thumb(&data_dir, &photo_bytes)?;

            let mut conn = match db.lock() {
                Ok(c) => c,
                Err(_) => {
                    delete_photo_files(&data_dir, &photo_id);
                    return Err(AppError::internal("database lock poisoned"));
                }
            };
            let tx = conn.transaction()?;

            let now = now_ms();
            let expires_at = now + CHECKIN_TTL_MS;
            tx.execute(
                "INSERT INTO checkins (author_id, photo_id, note, lat, lon, place_name, created_at, expires_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
                params![user_id, photo_id, note, lat, lon, place_name, now, expires_at],
            )?;
            let checkin_id = tx.last_insert_rowid();
            let checkin = get_checkin_row(&tx, checkin_id)?
                .ok_or_else(|| AppError::internal("checkin missing"))?;

            let ws_push = insert_partner_event(
                &tx,
                user_id,
                "checkin",
                serde_json::json!({ "checkin": &checkin }),
                now,
            )?;

            let fcm_push = if let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let body = checkin
                            .note
                            .clone()
                            .unwrap_or_else(|| "Posted a check-in".to_string());
                        let mut data = HashMap::new();
                        data.insert("type".into(), "checkin".into());
                        data.insert("event_id".into(), ev.id.to_string());
                        data.insert("title".into(), format!("{actor_name} checked in"));
                        data.insert("body".into(), body);
                        data.insert("checkin_id".into(), checkin.id.to_string());
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
            Ok((checkin, ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok((StatusCode::CREATED, Json(checkin)))
}

pub async fn list_checkins(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<Json<CheckInsResponse>> {
    let db = state.db.clone();
    let checkins = tokio::task::spawn_blocking(move || -> AppResult<Vec<CheckIn>> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let now = now_ms();
        let mut stmt = conn.prepare(
            "SELECT id, author_id, photo_id, note, lat, lon, place_name,
                    created_at, expires_at, highlighted, highlighted_by, highlighted_at
             FROM checkins
             WHERE expires_at > ?1
             ORDER BY created_at DESC, id DESC",
        )?;
        let rows = stmt.query_map(params![now], |row| {
            let highlighted_int: i64 = row.get(9)?;
            Ok(CheckIn {
                id: row.get(0)?,
                author_id: row.get(1)?,
                photo_id: row.get(2)?,
                note: row.get(3)?,
                lat: row.get(4)?,
                lon: row.get(5)?,
                place_name: row.get(6)?,
                created_at: row.get(7)?,
                expires_at: row.get(8)?,
                highlighted: highlighted_int != 0,
                highlighted_by: row.get(10)?,
                highlighted_at: row.get(11)?,
                replies: Vec::new(),
            })
        })?;
        let mut list = Vec::new();
        for r in rows {
            let mut c = r?;
            c.replies = load_replies_for_checkin(&conn, c.id)?;
            list.push(c);
        }
        Ok(list)
    })
    .await??;

    Ok(Json(CheckInsResponse { checkins }))
}

pub async fn get_checkin(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
    Path(checkin_id): Path<i64>,
) -> AppResult<Json<CheckIn>> {
    let db = state.db.clone();
    let checkin = tokio::task::spawn_blocking(move || -> AppResult<CheckIn> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let now = now_ms();
        let c = get_checkin_row(&conn, checkin_id)?
            .ok_or_else(|| AppError::not_found("check-in not found"))?;
        if !c.highlighted && c.expires_at <= now {
            return Err(AppError::not_found("check-in not found"));
        }
        Ok(c)
    })
    .await??;

    Ok(Json(checkin))
}

pub async fn delete_checkin(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Path(checkin_id): Path<i64>,
) -> AppResult<StatusCode> {
    let db = state.db.clone();
    let data_dir = state.config.data_dir.clone();

    tokio::task::spawn_blocking(move || -> AppResult<()> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let row: Option<(i64, String)> = tx
            .query_row(
                "SELECT author_id, photo_id FROM checkins WHERE id = ?1",
                params![checkin_id],
                |r| Ok((r.get(0)?, r.get(1)?)),
            )
            .optional()?;

        let Some((author_id, checkin_photo_id)) = row else {
            return Err(AppError::not_found("check-in not found"));
        };
        if author_id != user_id {
            return Err(AppError::forbidden("only the author can delete this check-in"));
        }

        let mut photo_ids = vec![checkin_photo_id];
        {
            let mut stmt = tx.prepare(
                "SELECT photo_id FROM replies WHERE checkin_id = ?1 AND photo_id IS NOT NULL",
            )?;
            let rows = stmt.query_map(params![checkin_id], |r| r.get::<_, String>(0))?;
            for r in rows {
                photo_ids.push(r?);
            }
        }

        tx.execute("DELETE FROM checkins WHERE id = ?1", params![checkin_id])?;
        tx.commit()?;

        for pid in photo_ids {
            delete_photo_files(&data_dir, &pid);
        }
        Ok(())
    })
    .await??;

    Ok(StatusCode::NO_CONTENT)
}

pub async fn set_highlight(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Path(checkin_id): Path<i64>,
) -> AppResult<Json<CheckIn>> {
    toggle_highlight(user_id, state, checkin_id, true).await
}

pub async fn unset_highlight(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Path(checkin_id): Path<i64>,
) -> AppResult<Json<CheckIn>> {
    toggle_highlight(user_id, state, checkin_id, false).await
}

async fn toggle_highlight(
    user_id: i64,
    state: AppState,
    checkin_id: i64,
    highlight: bool,
) -> AppResult<Json<CheckIn>> {
    let db = state.db.clone();
    let (checkin, ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(CheckIn, Option<(i64, crate::models::Event)>, Option<(String, HashMap<String, String>)>)> {
            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
            let tx = conn.transaction()?;

            let now = now_ms();
            let existing = get_checkin_row(&tx, checkin_id)?
                .ok_or_else(|| AppError::not_found("check-in not found"))?;

            if highlight && !existing.highlighted && existing.expires_at <= now {
                return Err(AppError::expired("check-in has already expired"));
            }

            let changed = existing.highlighted != highlight;
            if changed {
                if highlight {
                    tx.execute(
                        "UPDATE checkins SET highlighted = 1, highlighted_by = ?1, highlighted_at = ?2 WHERE id = ?3",
                        params![user_id, now, checkin_id],
                    )?;
                } else {
                    tx.execute(
                        "UPDATE checkins SET highlighted = 0, highlighted_by = NULL, highlighted_at = NULL WHERE id = ?1",
                        params![checkin_id],
                    )?;
                }
            }

            let updated = get_checkin_row(&tx, checkin_id)?
                .ok_or_else(|| AppError::internal("check-in missing"))?;

            let ws_push = if changed {
                insert_partner_event(
                    &tx,
                    user_id,
                    "highlight",
                    serde_json::json!({ "checkin_id": checkin_id, "highlighted": highlight }),
                    now,
                )?
            } else {
                None
            };

            let fcm_push = if highlight && let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let mut data = HashMap::new();
                        data.insert("type".into(), "highlight".into());
                        data.insert("event_id".into(), ev.id.to_string());
                        data.insert("title".into(), format!("{actor_name} saved a highlight"));
                        data.insert("body".into(), "Saved to your shared memories".into());
                        data.insert("checkin_id".into(), checkin_id.to_string());
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
            Ok((updated, ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok(Json(checkin))
}

pub async fn create_reply(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Path(checkin_id): Path<i64>,
    mut multipart: Multipart,
) -> AppResult<(StatusCode, Json<Reply>)> {
    let mut text: Option<String> = None;
    let mut photo_bytes: Option<Vec<u8>> = None;

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
                    if trimmed.chars().count() > 1000 {
                        return Err(AppError::bad_request("text exceeds 1000 characters"));
                    }
                    text = Some(trimmed);
                }
            }
            "photo" => {
                let bytes = read_field_bytes_limited(field, crate::photos::MAX_PHOTO_BYTES).await?;
                photo_bytes = Some(bytes);
            }
            _ => {}
        }
    }

    if text.is_none() && photo_bytes.is_none() {
        return Err(AppError::bad_request("text or photo is required"));
    }

    let data_dir = state.config.data_dir.clone();
    let db = state.db.clone();

    let (reply, ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(Reply, Option<(i64, crate::models::Event)>, Option<(String, HashMap<String, String>)>)> {
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

            let now = now_ms();
            let row: Option<(i64, i64)> = tx
                .query_row(
                    "SELECT expires_at, highlighted FROM checkins WHERE id = ?1",
                    params![checkin_id],
                    |r| Ok((r.get(0)?, r.get(1)?)),
                )
                .optional()?;

            let Some((expires_at, highlighted_int)) = row else {
                if let Some(ref pid) = photo_id {
                    delete_photo_files(&data_dir, pid);
                }
                return Err(AppError::not_found("check-in not found"));
            };
            if highlighted_int == 0 && expires_at <= now {
                if let Some(ref pid) = photo_id {
                    delete_photo_files(&data_dir, pid);
                }
                return Err(AppError::expired("check-in has expired"));
            }

            tx.execute(
                "INSERT INTO replies (checkin_id, author_id, text, photo_id, created_at)
                 VALUES (?1, ?2, ?3, ?4, ?5)",
                params![checkin_id, user_id, text, photo_id, now],
            )?;
            let reply_id = tx.last_insert_rowid();
            let reply = Reply {
                id: reply_id,
                checkin_id,
                author_id: user_id,
                text,
                photo_id,
                created_at: now,
            };

            let ws_push = insert_partner_event(
                &tx,
                user_id,
                "reply",
                serde_json::json!({ "reply": &reply }),
                now,
            )?;

            let fcm_push = if let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let body = reply
                            .text
                            .clone()
                            .unwrap_or_else(|| "Sent a photo reply".to_string());
                        let mut data = HashMap::new();
                        data.insert("type".into(), "reply".into());
                        data.insert("event_id".into(), ev.id.to_string());
                        data.insert("title".into(), format!("{actor_name} replied"));
                        data.insert("body".into(), body);
                        data.insert("checkin_id".into(), checkin_id.to_string());
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
            Ok((reply, ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok((StatusCode::CREATED, Json(reply)))
}

#[derive(Deserialize)]
pub struct MemoriesQuery {
    pub before: Option<i64>,
    pub limit: Option<i64>,
}

pub async fn list_memories(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
    Query(query): Query<MemoriesQuery>,
) -> AppResult<Json<MemoriesResponse>> {
    let before = query.before.unwrap_or_else(|| now_ms() + 1);
    let limit = query.limit.unwrap_or(50).clamp(1, 200);

    let db = state.db.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<MemoriesResponse> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let mut stmt = conn.prepare(
            "SELECT id, author_id, photo_id, note, lat, lon, place_name,
                    created_at, expires_at, highlighted, highlighted_by, highlighted_at
             FROM checkins
             WHERE highlighted = 1 AND created_at < ?1
             ORDER BY created_at DESC, id DESC
             LIMIT ?2",
        )?;
        let rows = stmt.query_map(params![before, limit + 1], |row| {
            let highlighted_int: i64 = row.get(9)?;
            Ok(CheckIn {
                id: row.get(0)?,
                author_id: row.get(1)?,
                photo_id: row.get(2)?,
                note: row.get(3)?,
                lat: row.get(4)?,
                lon: row.get(5)?,
                place_name: row.get(6)?,
                created_at: row.get(7)?,
                expires_at: row.get(8)?,
                highlighted: highlighted_int != 0,
                highlighted_by: row.get(10)?,
                highlighted_at: row.get(11)?,
                replies: Vec::new(),
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
        for item in &mut items {
            item.replies = load_replies_for_checkin(&conn, item.id)?;
        }
        let next_before = if has_more {
            items.last().map(|c| c.created_at)
        } else {
            None
        };

        Ok(MemoriesResponse { items, next_before })
    })
    .await??;

    Ok(Json(resp))
}
