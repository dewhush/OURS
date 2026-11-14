use axum::{
    Json,
    extract::{Multipart, State},
    http::{HeaderMap, StatusCode},
};
use rusqlite::{OptionalExtension, params};

use crate::auth::{AuthUser, generate_invite_code, hash_refresh_token, issue_tokens};
use crate::db::{
    get_partner, get_partner_id, get_partner_location_for, get_user, get_user_fcm_token,
    get_user_name, insert_partner_event, now_ms,
};
use crate::error::{AppError, AppResult};
use crate::models::{
    CreatePairRequest, CreatePairResponse, InviteResponse, JoinPairRequest, JoinPairResponse,
    MeResponse, PairStateResponse, PushTokenRequest, RefreshRequest, Tokens, User,
};
use crate::photos::{MAX_PHOTO_BYTES, delete_photo_files, save_jpeg_and_thumb};
use crate::state::AppState;

const INVITE_TTL_MS: i64 = 24 * 60 * 60 * 1000;

fn normalize_email(raw: Option<String>) -> AppResult<Option<String>> {
    let Some(s) = raw else {
        return Ok(None);
    };
    let trimmed = s.trim().to_ascii_lowercase();
    if trimmed.is_empty() {
        return Ok(None);
    }
    if trimmed.len() > 254 || !trimmed.contains('@') {
        return Err(AppError::bad_request("invalid email address"));
    }
    Ok(Some(trimmed))
}

fn constant_time_eq(a: &[u8], b: &[u8]) -> bool {
    if a.len() != b.len() {
        return false;
    }
    let mut diff = 0u8;
    for (x, y) in a.iter().zip(b.iter()) {
        diff |= x ^ y;
    }
    diff == 0
}

pub async fn create_pair(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<CreatePairRequest>,
) -> AppResult<(StatusCode, Json<CreatePairResponse>)> {
    if let Some(ref expected_key) = state.config.setup_key {
        state.check_create_rate_limit()?;
        let provided_key = headers
            .get("X-Setup-Key")
            .and_then(|v| v.to_str().ok())
            .unwrap_or("");
        if !constant_time_eq(provided_key.as_bytes(), expected_key.as_bytes()) {
            state.record_create_failure();
            return Err(AppError::forbidden("invalid or missing X-Setup-Key"));
        }
    }

    let name = req.name.trim().to_string();
    if name.is_empty() || name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    let email = normalize_email(req.email)?;

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<CreatePairResponse> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let count: i64 = tx.query_row("SELECT COUNT(*) FROM users", [], |r| r.get(0))?;
        if count > 0 {
            return Err(AppError::already_set_up());
        }

        let now = now_ms();
        tx.execute(
            "INSERT INTO users (name, email, created_at, last_seen_at) VALUES (?1, ?2, ?3, ?3)",
            params![name, email, now],
        )?;
        let user_id = tx.last_insert_rowid();
        let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
        let tokens = issue_tokens(&tx, &secret, user_id, now)?;

        let invite_code = generate_invite_code();
        let invite_expires_at = now + INVITE_TTL_MS;
        tx.execute(
            "INSERT INTO invites (code, created_by, created_at, expires_at) VALUES (?1, ?2, ?3, ?4)",
            params![invite_code, user_id, now, invite_expires_at],
        )?;

        tx.commit()?;
        Ok(CreatePairResponse {
            user,
            tokens,
            invite_code,
            invite_expires_at,
        })
    })
    .await??;

    Ok((StatusCode::CREATED, Json(resp)))
}

pub async fn reissue_invite(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<Json<InviteResponse>> {
    let db = state.db.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<InviteResponse> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let count: i64 = tx.query_row("SELECT COUNT(*) FROM users", [], |r| r.get(0))?;
        if count >= 2 {
            return Err(AppError::already_paired());
        }

        let now = now_ms();
        tx.execute("DELETE FROM invites WHERE used_at IS NULL", [])?;

        let invite_code = generate_invite_code();
        let invite_expires_at = now + INVITE_TTL_MS;
        tx.execute(
            "INSERT INTO invites (code, created_by, created_at, expires_at) VALUES (?1, ?2, ?3, ?4)",
            params![invite_code, user_id, now, invite_expires_at],
        )?;

        tx.commit()?;
        Ok(InviteResponse {
            invite_code,
            invite_expires_at,
        })
    })
    .await??;

    Ok(Json(resp))
}

pub async fn join_pair(
    State(state): State<AppState>,
    Json(req): Json<JoinPairRequest>,
) -> AppResult<(StatusCode, Json<JoinPairResponse>)> {
    state.check_join_rate_limit()?;

    let code = req.code.trim().to_ascii_uppercase();
    if code.is_empty() {
        state.record_join_failure();
        return Err(AppError::invite_not_found());
    }
    let name = req.name.trim().to_string();
    if name.is_empty() || name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    let email = normalize_email(req.email)?;

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();
    let res = tokio::task::spawn_blocking(move || -> AppResult<JoinPairResponse> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let count: i64 = tx.query_row("SELECT COUNT(*) FROM users", [], |r| r.get(0))?;
        if count >= 2 {
            return Err(AppError::pair_full());
        }

        let now = now_ms();
        let valid_invite: Option<String> = tx
            .query_row(
                "SELECT code FROM invites WHERE code = ?1 AND used_at IS NULL AND expires_at > ?2",
                params![code, now],
                |r| r.get(0),
            )
            .optional()?;
        if valid_invite.is_none() {
            return Err(AppError::invite_not_found());
        }

        tx.execute(
            "UPDATE invites SET used_at = ?1 WHERE code = ?2",
            params![now, code],
        )?;
        tx.execute(
            "INSERT INTO users (name, email, created_at, last_seen_at) VALUES (?1, ?2, ?3, ?3)",
            params![name, email, now],
        )?;
        let user_id = tx.last_insert_rowid();
        let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
        let tokens = issue_tokens(&tx, &secret, user_id, now)?;

        tx.commit()?;
        Ok(JoinPairResponse { user, tokens })
    })
    .await?;

    match res {
        Ok(resp) => Ok((StatusCode::CREATED, Json(resp))),
        Err(err) => {
            if err.code == "invite_not_found" {
                state.record_join_failure();
            }
            Err(err)
        }
    }
}

pub async fn refresh_auth(
    State(state): State<AppState>,
    Json(req): Json<RefreshRequest>,
) -> AppResult<Json<Tokens>> {
    state.check_refresh_rate_limit()?;

    let raw = req.refresh_token.trim().to_string();
    if raw.is_empty() {
        state.record_refresh_failure();
        return Err(AppError::invalid_refresh_token("missing refresh token"));
    }
    let token_hash = hash_refresh_token(&raw);

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();
    let res = tokio::task::spawn_blocking(move || -> AppResult<Tokens> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let now = now_ms();
        let row: Option<(i64, i64)> = tx
            .query_row(
                "SELECT user_id, expires_at FROM refresh_tokens WHERE token_hash = ?1",
                params![token_hash],
                |r| Ok((r.get(0)?, r.get(1)?)),
            )
            .optional()?;

        let Some((user_id, expires_at)) = row else {
            return Err(AppError::invalid_refresh_token("unknown or already used refresh token"));
        };
        if expires_at <= now {
            tx.execute(
                "DELETE FROM refresh_tokens WHERE token_hash = ?1",
                params![token_hash],
            )?;
            tx.commit()?;
            return Err(AppError::invalid_refresh_token("refresh token expired"));
        }

        tx.execute(
            "DELETE FROM refresh_tokens WHERE token_hash = ?1",
            params![token_hash],
        )?;
        let tokens = issue_tokens(&tx, &secret, user_id, now)?;
        tx.execute(
            "UPDATE users SET last_seen_at = ?1 WHERE id = ?2",
            params![now, user_id],
        )?;
        tx.commit()?;
        Ok(tokens)
    })
    .await?;

    match res {
        Ok(tokens) => Ok(Json(tokens)),
        Err(err) => {
            if err.code == "invalid_refresh_token" {
                state.record_refresh_failure();
            }
            Err(err)
        }
    }
}

pub async fn logout(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    body: Option<Json<RefreshRequest>>,
) -> AppResult<StatusCode> {
    let db = state.db.clone();
    let specific_hash = body
        .map(|Json(r)| r.refresh_token.trim().to_string())
        .filter(|s| !s.is_empty())
        .map(|s| hash_refresh_token(&s));

    tokio::task::spawn_blocking(move || -> AppResult<()> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        if let Some(token_hash) = specific_hash {
            conn.execute(
                "DELETE FROM refresh_tokens WHERE user_id = ?1 AND token_hash = ?2",
                params![user_id, token_hash],
            )?;
        } else {
            conn.execute(
                "DELETE FROM refresh_tokens WHERE user_id = ?1",
                params![user_id],
            )?;
        }
        Ok(())
    })
    .await??;

    state.disconnect_ws(user_id).await;

    Ok(StatusCode::NO_CONTENT)
}

pub async fn get_pair_state(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<Json<PairStateResponse>> {
    let db = state.db.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<PairStateResponse> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let me = get_user(&conn, user_id)?.ok_or_else(|| AppError::unauthorized("user missing"))?;
        let partner = get_partner(&conn, user_id)?;
        let sharing_paused_int: i64 = conn.query_row(
            "SELECT sharing_paused FROM users WHERE id = ?1",
            params![user_id],
            |r| r.get(0),
        )?;
        let sharing_paused = sharing_paused_int != 0;
        let partner_paused = if let Some(ref p) = partner {
            let pp: i64 = conn.query_row(
                "SELECT sharing_paused FROM users WHERE id = ?1",
                params![p.id],
                |r| r.get(0),
            )?;
            pp != 0
        } else {
            false
        };
        let me_location = Some(get_partner_location_for(&conn, user_id)?);
        let partner_location = if let Some(ref p) = partner {
            Some(get_partner_location_for(&conn, p.id)?)
        } else {
            None
        };

        Ok(PairStateResponse {
            me,
            partner,
            sharing_paused,
            partner_paused,
            me_location,
            partner_location,
            server_time: now_ms(),
        })
    })
    .await??;

    Ok(Json(resp))
}

pub async fn upload_avatar(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> AppResult<Json<User>> {
    let mut photo_bytes: Option<Vec<u8>> = None;
    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|_| AppError::bad_request("malformed multipart body"))?
    {
        if field.name() == Some("photo") {
            let bytes = field
                .bytes()
                .await
                .map_err(|_| AppError::bad_request("failed to read photo field"))?;
            if bytes.len() > MAX_PHOTO_BYTES {
                return Err(AppError::too_large("photo exceeds 10 MB limit"));
            }
            photo_bytes = Some(bytes.to_vec());
        }
    }

    let photo_bytes = photo_bytes.ok_or_else(|| AppError::bad_request("photo is required"))?;
    let data_dir = state.config.data_dir.clone();
    let db = state.db.clone();

    let (user, ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(User, Option<(i64, crate::models::Event)>, Option<(String, std::collections::HashMap<String, String>)>)> {
            let photo_id = save_jpeg_and_thumb(&data_dir, &photo_bytes)?;

            let mut conn = match db.lock() {
                Ok(c) => c,
                Err(_) => {
                    delete_photo_files(&data_dir, &photo_id);
                    return Err(AppError::internal("database lock poisoned"));
                }
            };
            let tx = conn.transaction()?;

            let old_avatar: Option<String> = tx
                .query_row(
                    "SELECT avatar_photo_id FROM users WHERE id = ?1",
                    params![user_id],
                    |r| r.get(0),
                )
                .optional()?
                .flatten();

            tx.execute(
                "UPDATE users SET avatar_photo_id = ?1 WHERE id = ?2",
                params![photo_id, user_id],
            )?;
            let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
            let now = now_ms();
            let ws_push = insert_partner_event(
                &tx,
                user_id,
                "avatar",
                serde_json::json!({ "user_id": user_id, "avatar_photo_id": &photo_id }),
                now,
            )?;

            let fcm_push = if let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let mut data = std::collections::HashMap::new();
                        data.insert("type".into(), "avatar".into());
                        data.insert("event_id".into(), ev.id.to_string());
                        data.insert("title".into(), actor_name);
                        data.insert("body".into(), "Updated profile photo".into());
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
            if let Some(old_pid) = old_avatar {
                delete_photo_files(&data_dir, &old_pid);
            }
            Ok((user, ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok(Json(user))
}

pub async fn get_me(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<Json<MeResponse>> {
    let db = state.db.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<MeResponse> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let me = get_user(&conn, user_id)?.ok_or_else(|| AppError::unauthorized("user missing"))?;
        let partner = get_partner(&conn, user_id)?;
        let sharing_paused: i64 = conn.query_row(
            "SELECT sharing_paused FROM users WHERE id = ?1",
            params![user_id],
            |r| r.get(0),
        )?;
        let partner_paused = if let Some(ref p) = partner {
            let pp: i64 = conn.query_row(
                "SELECT sharing_paused FROM users WHERE id = ?1",
                params![p.id],
                |r| r.get(0),
            )?;
            pp != 0
        } else {
            false
        };

        Ok(MeResponse {
            me,
            partner,
            sharing_paused: sharing_paused != 0,
            partner_paused,
            server_time: now_ms(),
        })
    })
    .await??;

    Ok(Json(resp))
}

pub async fn set_push_token(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Json(req): Json<PushTokenRequest>,
) -> AppResult<StatusCode> {
    let token = req.token.trim().to_string();
    if token.is_empty() {
        return Err(AppError::bad_request("token must not be empty"));
    }

    let db = state.db.clone();
    tokio::task::spawn_blocking(move || -> AppResult<()> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        conn.execute(
            "UPDATE users SET fcm_token = ?1 WHERE id = ?2",
            params![token, user_id],
        )?;
        Ok(())
    })
    .await??;

    Ok(StatusCode::NO_CONTENT)
}

