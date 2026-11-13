use axum::{
    Json,
    extract::State,
    http::{HeaderMap, StatusCode},
};
use rusqlite::{OptionalExtension, params};

use crate::auth::{AuthUser, generate_invite_code, hash_refresh_token, issue_tokens};
use crate::db::{get_partner, get_user, now_ms};
use crate::error::{AppError, AppResult};
use crate::models::{
    CreatePairRequest, CreatePairResponse, InviteResponse, JoinPairRequest, JoinPairResponse,
    MeResponse, PushTokenRequest, RefreshRequest, Tokens,
};
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

pub async fn create_pair(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<CreatePairRequest>,
) -> AppResult<(StatusCode, Json<CreatePairResponse>)> {
    let provided_key = headers
        .get("X-Setup-Key")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");
    if provided_key != state.config.setup_key {
        return Err(AppError::forbidden("invalid or missing X-Setup-Key"));
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
    let code = req.code.trim().to_ascii_uppercase();
    if code.is_empty() {
        return Err(AppError::invite_not_found());
    }
    let name = req.name.trim().to_string();
    if name.is_empty() || name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    let email = normalize_email(req.email)?;

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();
    let resp = tokio::task::spawn_blocking(move || -> AppResult<JoinPairResponse> {
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
    .await??;

    Ok((StatusCode::CREATED, Json(resp)))
}

pub async fn refresh_auth(
    State(state): State<AppState>,
    Json(req): Json<RefreshRequest>,
) -> AppResult<Json<Tokens>> {
    let raw = req.refresh_token.trim().to_string();
    if raw.is_empty() {
        return Err(AppError::invalid_refresh_token("missing refresh token"));
    }
    let token_hash = hash_refresh_token(&raw);

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();
    let tokens = tokio::task::spawn_blocking(move || -> AppResult<Tokens> {
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
    .await??;

    Ok(Json(tokens))
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
