use axum::{
    extract::FromRequestParts,
    http::{header, request::Parts},
};
use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use hmac::{Hmac, Mac};
use rand::RngCore;
use rusqlite::{Connection, OptionalExtension, params};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

use crate::db::now_ms;
use crate::error::{AppError, AppResult};
use crate::models::Tokens;
use crate::state::AppState;

type HmacSha256 = Hmac<Sha256>;

pub const ACCESS_TOKEN_TTL_MS: i64 = 60 * 60 * 1000;
pub const REFRESH_TOKEN_TTL_MS: i64 = 180 * 24 * 60 * 60 * 1000;
const INVITE_ALPHABET: &[u8] = b"ABCDEFGHJKMNPQRSTUVWXYZ23456789";

#[derive(Serialize, Deserialize)]
struct AccessPayload {
    uid: i64,
    exp: i64,
}

pub fn sign_access_token(secret: &[u8], user_id: i64, now: i64) -> AppResult<(String, i64)> {
    let exp = now + ACCESS_TOKEN_TTL_MS;
    let payload = AccessPayload { uid: user_id, exp };
    let payload_json =
        serde_json::to_vec(&payload).map_err(|_| AppError::internal("token serialize error"))?;
    let payload_b64 = URL_SAFE_NO_PAD.encode(payload_json);

    let mut mac =
        HmacSha256::new_from_slice(secret).map_err(|_| AppError::internal("hmac key error"))?;
    mac.update(payload_b64.as_bytes());
    let sig_b64 = URL_SAFE_NO_PAD.encode(mac.finalize().into_bytes());

    Ok((format!("{payload_b64}.{sig_b64}"), exp))
}

pub fn verify_access_token(secret: &[u8], token: &str, now: i64) -> AppResult<i64> {
    let Some((payload_b64, sig_b64)) = token.split_once('.') else {
        return Err(AppError::unauthorized("malformed access token"));
    };
    let sig = URL_SAFE_NO_PAD
        .decode(sig_b64)
        .map_err(|_| AppError::unauthorized("invalid token signature"))?;

    let mut mac =
        HmacSha256::new_from_slice(secret).map_err(|_| AppError::internal("hmac key error"))?;
    mac.update(payload_b64.as_bytes());
    mac.verify_slice(&sig)
        .map_err(|_| AppError::unauthorized("invalid token signature"))?;

    let payload_bytes = URL_SAFE_NO_PAD
        .decode(payload_b64)
        .map_err(|_| AppError::unauthorized("invalid token payload"))?;
    let payload: AccessPayload = serde_json::from_slice(&payload_bytes)
        .map_err(|_| AppError::unauthorized("invalid token payload"))?;

    if payload.exp <= now {
        return Err(AppError::unauthorized("access token expired"));
    }

    Ok(payload.uid)
}

pub fn hash_refresh_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex::encode(hasher.finalize())
}

pub fn issue_tokens(conn: &Connection, secret: &[u8], user_id: i64, now: i64) -> AppResult<Tokens> {
    let (access_token, access_expires_at) = sign_access_token(secret, user_id, now)?;

    let mut raw_refresh = [0u8; 32];
    rand::rng().fill_bytes(&mut raw_refresh);
    let refresh_token = URL_SAFE_NO_PAD.encode(raw_refresh);
    let refresh_hash = hash_refresh_token(&refresh_token);
    let refresh_expires_at = now + REFRESH_TOKEN_TTL_MS;

    conn.execute(
        "INSERT INTO refresh_tokens (token_hash, user_id, expires_at, created_at)
         VALUES (?1, ?2, ?3, ?4)",
        params![refresh_hash, user_id, refresh_expires_at, now],
    )?;

    Ok(Tokens {
        access_token,
        access_expires_at,
        refresh_token,
        refresh_expires_at,
    })
}

pub fn generate_invite_code() -> String {
    let mut rng = rand::rng();
    let mut buf = [0u8; 6];
    rng.fill_bytes(&mut buf);
    buf.iter()
        .map(|b| INVITE_ALPHABET[(*b as usize) % INVITE_ALPHABET.len()] as char)
        .collect()
}

pub struct AuthUser(pub i64);

impl FromRequestParts<AppState> for AuthUser {
    type Rejection = AppError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let auth_header = parts
            .headers
            .get(header::AUTHORIZATION)
            .and_then(|v| v.to_str().ok())
            .ok_or_else(|| AppError::unauthorized("missing authorization header"))?;

        let token = auth_header
            .strip_prefix("Bearer ")
            .or_else(|| auth_header.strip_prefix("bearer "))
            .map(str::trim)
            .filter(|t| !t.is_empty())
            .ok_or_else(|| AppError::unauthorized("invalid authorization header"))?;

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
                    |row| row.get(0),
                )
                .optional()?;
            if found.is_some() {
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

        Ok(AuthUser(user_id))
    }
}
