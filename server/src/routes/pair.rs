use std::net::IpAddr;

use axum::{
    Json,
    extract::{Multipart, State},
    http::{HeaderMap, StatusCode},
};
use rusqlite::{OptionalExtension, params};

use crate::auth::{AuthUser, OptionalPeerIp, generate_invite_code, hash_device_id, hash_refresh_token, issue_tokens};
use crate::db::{
    get_partner, get_partner_id, get_partner_location_for, get_space_name, get_user,
    get_user_fcm_token, get_user_name, insert_partner_event, now_ms, set_space_name,
};
use crate::error::{AppError, AppResult};
use crate::models::{
    CreatePairRequest, CreatePairResponse, GoogleAuthRequest, GoogleAuthResponse,
    GoogleCallbackRequest, InviteResponse, JoinPairRequest, JoinPairResponse, MeResponse,
    PairStateResponse, PushTokenRequest, RefreshRequest, Tokens, UpdateNameRequest, User,
};
use crate::photos::{MAX_PHOTO_BYTES, PhotoGuard, delete_photo_files, save_jpeg_and_thumb};
use crate::routes::{FcmPush, WsPush};
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

pub fn client_rate_key(
    peer_ip: Option<IpAddr>,
    headers: &HeaderMap,
    fallback_subkey: &str,
) -> String {
    // Honor proxy headers only when the TCP peer itself is loopback (local Nginx/Caddy/tunnel),
    // or in direct in-process unit tests without ConnectInfo wiring.
    // Only the Cloudflare edge header (CF-Connecting-IP) is trusted: it is set by
    // Cloudflare's edge (which also blocks client-forged copies), so it is
    // authoritative. X-Real-IP / X-Forwarded-For pass through the edge untouched
    // and are client-forgeable — never use them for rate-limit identity.
    let trust_forwarded = peer_ip.is_none_or(|ip| ip.is_loopback());
    if trust_forwarded {
        let forwarded = headers
            .get("CF-Connecting-IP")
            .and_then(|v| v.to_str().ok())
            .map(str::trim)
            .filter(|s| !s.is_empty())
            .and_then(|s| s.parse::<IpAddr>().ok());

        if let Some(ip) = forwarded {
            return format!("ip:{ip}");
        }
    }

    match peer_ip {
        Some(ip) if !ip.is_loopback() => format!("ip:{ip}"),
        _ => format!("key:{fallback_subkey}"),
    }
}

pub async fn create_pair(
    OptionalPeerIp(peer_ip): OptionalPeerIp,
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<CreatePairRequest>,
) -> AppResult<(StatusCode, Json<CreatePairResponse>)> {
    let mut create_rate_key: Option<String> = None;
    if let Some(ref expected_key) = state.config.setup_key {
        let provided_key = headers
            .get("X-Setup-Key")
            .and_then(|v| v.to_str().ok())
            .unwrap_or("");
        let rk = client_rate_key(peer_ip, &headers, "setup_key");
        state.check_create_rate_limit(&rk)?;
        if !constant_time_eq(provided_key.as_bytes(), expected_key.as_bytes()) {
            state.record_create_failure(&rk);
            return Err(AppError::forbidden("invalid or missing X-Setup-Key"));
        }
        create_rate_key = Some(rk);
    }

    let name = req.name.trim().to_string();
    if name.is_empty() || name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    let email = normalize_email(req.email)?;
    let raw_space_name = req.space_name;
    let creator_device_hash = req.device_id.as_deref().and_then(|d| hash_device_id(d, &state.config.token_secret));

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
        let space_name = set_space_name(&tx, raw_space_name.as_deref())?;
        tx.execute(
            "INSERT INTO users (name, email, device_hash, created_at, last_seen_at) VALUES (?1, ?2, ?3, ?4, ?4)",
            params![name, email, creator_device_hash, now],
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
            space_name,
        })
    })
    .await??;

    if let Some(ref rk) = create_rate_key {
        state.clear_create_rate_limit(rk);
    }
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
    OptionalPeerIp(peer_ip): OptionalPeerIp,
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<JoinPairRequest>,
) -> AppResult<(StatusCode, Json<JoinPairResponse>)> {
    let code = req.code.trim().to_ascii_uppercase();
    let rate_key = client_rate_key(peer_ip, &headers, "invite_attempt");
    state.check_join_rate_limit(&rate_key)?;

    if code.is_empty() {
        state.record_join_failure(&rate_key);
        return Err(AppError::invite_not_found());
    }
    let name = req.name.trim().to_string();
    if name.is_empty() || name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    let email = normalize_email(req.email)?;
    let joiner_device_hash = req.device_id.as_deref().and_then(|d| hash_device_id(d, &state.config.token_secret));

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

        // Reject the join if the email already belongs to an account. Otherwise a
        // second user row could carry the same email (identity confusion later in
        // the Google login path, which resolves by newest row).
        let existing_email: Option<i64> = tx
            .query_row(
                "SELECT id FROM users WHERE lower(email) = lower(?1) LIMIT 1",
                params![email],
                |r| r.get(0),
            )
            .optional()?;
        if existing_email.is_some() {
            return Err(AppError::bad_request("email is already associated with an account"));
        }

        tx.execute(
            "UPDATE invites SET used_at = ?1 WHERE code = ?2",
            params![now, code],
        )?;
        tx.execute(
            "INSERT INTO users (name, email, device_hash, created_at, last_seen_at) VALUES (?1, ?2, ?3, ?4, ?4)",
            params![name, email, joiner_device_hash, now],
        )?;
        let user_id = tx.last_insert_rowid();
        let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
        let tokens = issue_tokens(&tx, &secret, user_id, now)?;
        let space_name = get_space_name(&tx)?;

        tx.commit()?;
        Ok(JoinPairResponse { user, tokens, space_name })
    })
    .await?;

    match res {
        Ok(resp) => {
            state.clear_join_rate_limit(&rate_key);
            Ok((StatusCode::CREATED, Json(resp)))
        }
        Err(err) => {
            if err.code == "invite_not_found" {
                state.record_join_failure(&rate_key);
            }
            Err(err)
        }
    }
}

pub async fn refresh_auth(
    OptionalPeerIp(peer_ip): OptionalPeerIp,
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<RefreshRequest>,
) -> AppResult<Json<Tokens>> {
    let raw = req.refresh_token.trim().to_string();
    let token_hash = hash_refresh_token(&raw);
    // Key the limiter on the FULL token hash — a 64-bit prefix can collide
    // across distinct tokens and lets an attacker spread attempts across buckets.
    let rate_key = client_rate_key(peer_ip, &headers, &token_hash);
    state.check_refresh_rate_limit(&rate_key)?;

    if raw.is_empty() {
        state.record_refresh_failure(&rate_key);
        return Err(AppError::invalid_refresh_token("missing refresh token"));
    }

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();
    let res = tokio::task::spawn_blocking(move || -> AppResult<Tokens> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let now = now_ms();
        let row: Option<(i64, i64, String)> = tx
            .query_row(
                "SELECT user_id, expires_at, family_id FROM refresh_tokens WHERE token_hash = ?1",
                params![token_hash],
                |r| Ok((r.get(0)?, r.get(1)?, r.get(2)?)),
            )
            .optional()?;

        let Some((user_id, expires_at, family_id)) = row else {
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

        // Revoke the ENTIRE refresh-token family, not just the presented token.
        // A stolen sibling — even one issued beside a legitimate client's token —
        // dies the moment any member of the family is used to rotate.
        tx.execute(
            "DELETE FROM refresh_tokens WHERE user_id = ?1 AND family_id = ?2",
            params![user_id, family_id],
        )?;
        // issue_tokens starts a fresh family, so the new token shares nothing with
        // the revoked one.
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
        Ok(tokens) => {
            state.clear_refresh_rate_limit(&rate_key);
            Ok(Json(tokens))
        }
        Err(err) => {
            if err.code == "invalid_refresh_token" {
                state.record_refresh_failure(&rate_key);
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
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;
        let now = now_ms();
        tx.execute(
            "UPDATE users SET tokens_revoked_before = MAX(COALESCE(tokens_revoked_before, 0), ?1) WHERE id = ?2",
            params![now, user_id],
        )?;
        if let Some(token_hash) = specific_hash {
            tx.execute(
                "DELETE FROM refresh_tokens WHERE user_id = ?1 AND token_hash = ?2",
                params![user_id, token_hash],
            )?;
        } else {
            tx.execute(
                "DELETE FROM refresh_tokens WHERE user_id = ?1",
                params![user_id],
            )?;
        }
        tx.commit()?;
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

        let space_name = get_space_name(&conn)?;

        Ok(PairStateResponse {
            me,
            partner,
            sharing_paused,
            partner_paused,
            me_location,
            partner_location,
            server_time: now_ms(),
            space_name,
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
        move || -> AppResult<(User, WsPush, FcmPush)> {
            let photo_id = save_jpeg_and_thumb(&data_dir, &photo_bytes)?;
            let mut photo_guard = PhotoGuard::new(&data_dir, photo_id.clone());

            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
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
            photo_guard.disarm();
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

pub async fn update_name(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Json(req): Json<UpdateNameRequest>,
) -> AppResult<Json<User>> {
    let raw_name = req.name.trim();
    if raw_name.is_empty() || raw_name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    let new_name: String = raw_name.chars().take(40).collect();
    let db = state.db.clone();

    let (user, ws_push) = tokio::task::spawn_blocking(move || -> AppResult<(User, WsPush)> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        tx.execute(
            "UPDATE users SET name = ?1 WHERE id = ?2",
            params![new_name, user_id],
        )?;
        let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
        let ws_push = insert_partner_event(
            &tx,
            user_id,
            "name",
            serde_json::json!({ "user_id": user_id, "name": &new_name }),
            now_ms(),
        )?;
        tx.commit()?;
        Ok((user, ws_push))
    })
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
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

        let space_name = get_space_name(&conn)?;

        Ok(MeResponse {
            me,
            partner,
            sharing_paused: sharing_paused != 0,
            partner_paused,
            server_time: now_ms(),
            space_name,
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
    // FCM registration tokens are ~163 chars in practice; 4096 gives generous headroom
    // while blocking DB-abuse via arbitrarily long blobs.
    if token.len() > 4096 {
        return Err(AppError::bad_request("token exceeds 4096 characters"));
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

pub async fn public_config(State(state): State<AppState>) -> Json<serde_json::Value> {
    // Structured OIDC block. client_ids is an object so native clients can pick
    // their own audience; endpoints are explicit so clients need no URL guessing.
    let issuer = state.config.oidc_issuer.clone();
    Json(serde_json::json!({
        // Legacy Google field — retained for backward compatibility only.
        "google_client_id": state.config.google_client_id,
        "oidc": {
            "issuer": issuer,
            "client_ids": {
                "android": state.config.oidc_android_client_id,
                "web": state.config.oidc_web_client_id,
            },
            "endpoints": {
                "authorization": issuer.as_ref().map(|i| format!("{i}/authorize")),
                "token": issuer.as_ref().map(|i| format!("{i}/api/oidc/token")),
            },
            "jwks_url": state.config.oidc_jwks_url,
        },
        "setup_key_required": state.config.setup_key.is_some(),
        "version": env!("CARGO_PKG_VERSION"),
    }))
}

#[derive(serde::Deserialize)]
struct IdTokenClaims {
    #[allow(dead_code)]
    iss: Option<String>,
    // OIDC `aud` can be a single string OR an array of strings depending on the
    // provider (Pocket ID emits an array). Accept both.
    #[allow(dead_code)]
    aud: Option<serde_json::Value>,
    email: Option<String>,
    email_verified: Option<bool>,
    name: Option<String>,
    picture: Option<String>,
    // Pocket ID (and generic OIDC) subject claim — stable per-user identifier.
    #[allow(dead_code)]
    sub: Option<String>,
}

pub fn verify_jwt_with_rsa_components(
    id_token: &str,
    n: &str,
    e: &str,
    issuer: Option<&str>,
    audience: Option<&[&str]>,
    email_verified_required: bool,
) -> AppResult<(String, Option<String>, Option<String>)> {
    let key = jsonwebtoken::DecodingKey::from_rsa_components(n, e)
        .map_err(|_| AppError::unauthorized("invalid id token public key"))?;
    let mut validation = jsonwebtoken::Validation::new(jsonwebtoken::Algorithm::RS256);
    if let Some(iss) = issuer {
        validation.set_issuer(&[iss]);
    } else {
        validation.set_issuer(&["https://accounts.google.com", "accounts.google.com"]);
    }
    if let Some(auds) = audience {
        if auds.is_empty() {
            validation.validate_aud = false;
        } else {
            validation.set_audience(auds);
        }
    } else {
        validation.validate_aud = false;
    }
    let decoded = jsonwebtoken::decode::<IdTokenClaims>(id_token, &key, &validation)
        .map_err(|e| {
            tracing::warn!(
                "ID token verification failed: {e} (iss={:?} aud={:?} kid_present={})",
                issuer,
                audience.map(|a| a.to_vec()),
                id_token.split('.').count()
            );
            AppError::unauthorized("invalid or expired ID token")
        })?;
    // email_verified enforcement: providers that assert the claim (Firebase,
    // legacy Google) require it; the self-hosted OIDC (Pocket ID) path is a
    // documented exception — passkey auth at the IdP substitutes for email
    // verification, and accept_id_token explicitly passes `false` for it.
    let email_verified_ok = if email_verified_required {
        decoded.claims.email_verified == Some(true)
    } else {
        true
    };
    if !email_verified_ok {
        return Err(AppError::unauthorized("email is not verified"));
    }
    let email = decoded
        .claims
        .email
        .and_then(|s| {
            let t = s.trim().to_ascii_lowercase();
            if t.is_empty() || !t.contains('@') || t.len() > 254 {
                None
            } else {
                Some(t)
            }
        })
        .ok_or_else(|| AppError::unauthorized("id token missing verified email"))?;
    let name = decoded
        .claims
        .name
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty());
    let picture = decoded
        .claims
        .picture
        .map(|s| s.trim().to_string())
        .filter(|s| s.starts_with("https://"));
    Ok((email, name, picture))
}

async fn verify_id_token(state: &AppState, id_token: &str) -> AppResult<(String, Option<String>, Option<String>)> {
    // Try each configured provider and let the SIGNATURE decide which (if any)
    // validates. We never route on the token's own iss/aud before verification —
    // that would let a forger pick the validator.
    let header = jsonwebtoken::decode_header(id_token)
        .map_err(|_| AppError::unauthorized("malformed ID token header"))?;
    let kid = header
        .kid
        .as_deref()
        .ok_or_else(|| AppError::unauthorized("ID token missing key id"))?
        .to_string();

    // 1) Firebase Auth (Google Sign-In): iss=securetoken.google.com/<proj>, aud=<proj>.
    if let Some(proj) = state.config.firebase_project_id.clone() {
        let issuer = format!("https://securetoken.google.com/{proj}");
        let aud_refs = vec![proj.as_str()];
        // A missing kid here is NOT fatal — the token may belong to another
        // provider (e.g. a Google OAuth id_token whose kid is in Google's JWKS).
        // Fall through to the next provider instead of returning early.
        let pair = match state.get_firebase_jwk(&kid, false).await? {
            Some(p) => p,
            None => match state.get_firebase_jwk(&kid, true).await? {
                Some(p) => p,
                // kid genuinely unknown to Firebase — try the other providers.
                None => {
                    tracing::warn!("id_token kid not in Firebase JWKS, trying next provider: kid={kid}");
                    return verify_id_token_non_firebase(state, id_token, &kid).await;
                }
            },
        };
        if let Ok(claims) = verify_jwt_with_rsa_components(
            id_token, &pair.0, &pair.1, Some(&issuer), Some(&aud_refs), true,
        ) {
            return Ok(claims);
        }
    }

    // Firebase not configured, or its check didn't validate the token — try the
    // remaining providers so neither short-circuits the others.
    verify_id_token_non_firebase(state, id_token, &kid).await
}

/// Try Pocket ID + legacy Google for an id_token whose kid was NOT found in
/// Firebase's JWKS (or when Firebase isn't configured). Keeps the provider
/// attempts independent so a missing kid in one never hides another.
async fn verify_id_token_non_firebase(
    state: &AppState,
    id_token: &str,
    kid: &str,
) -> AppResult<(String, Option<String>, Option<String>)> {
    // 2) Pocket ID OIDC: iss=OURS_OIDC_ISSUER, aud=one of OURS_OIDC_CLIENT_IDS.
    if let Some(oidc_issuer) = state.config.oidc_issuer.clone() {
        let audience = state.config.oidc_client_ids.clone();
        if !audience.is_empty() {
            let aud_refs: Vec<&str> = audience.iter().map(|s| s.as_str()).collect();
            let pair = match state.get_google_jwk(kid, false).await? {
                Some(p) => p,
                None => match state.get_google_jwk(kid, true).await? {
                    Some(p) => p,
                    None => {
                        tracing::warn!("id_token kid not in OIDC JWKS: kid={kid}");
                        // fall through to legacy Google
                        return verify_id_token_legacy_google(state, id_token, kid).await;
                    }
                },
            };
            if let Ok(claims) = verify_jwt_with_rsa_components(
                id_token, &pair.0, &pair.1, Some(&oidc_issuer), Some(&aud_refs), false,
            ) {
                return Ok(claims);
            }
        }
    }

    // 3) Legacy Google: iss=accounts.google.com, aud=google_client_id.
    verify_id_token_legacy_google(state, id_token, kid).await
}

async fn verify_id_token_legacy_google(
    state: &AppState,
    id_token: &str,
    kid: &str,
) -> AppResult<(String, Option<String>, Option<String>)> {
    if let Some(gcid) = state.config.google_client_id.clone() {
        let aud_refs = vec![gcid.as_str()];
        // Use Google's OWN OAuth certs (iss=accounts.google.com) — NOT get_google_jwk,
        // which resolves to the Pocket ID OIDC JWKS when that provider is configured.
        let pair = match state.get_google_oauth_jwk(kid, false).await? {
            Some(p) => p,
            None => match state.get_google_oauth_jwk(kid, true).await? {
                Some(p) => p,
                None => {
                    tracing::warn!("id_token kid not in Google OAuth JWKS: kid={kid}");
                    return Err(AppError::unauthorized("unrecognized signing key"));
                }
            },
        };
        if let Ok(claims) = verify_jwt_with_rsa_components(
            id_token,
            &pair.0,
            &pair.1,
            Some("https://accounts.google.com"),
            Some(&aud_refs),
            true,
        ) {
            return Ok(claims);
        }
        tracing::warn!("id_token matched Google JWKS but failed iss/aud check: kid={kid} aud={gcid}");
    }

    Err(AppError::unauthorized("invalid or expired ID token"))
}

/// Request-derived fields shared by every Google sign-in entrypoint. Both the
/// legacy id_token handler and the PKCE callback handler resolve these from
/// their own request and then feed them through the same join-or-create core.
struct GoogleAuthIntent {
    email: String,
    name: Option<String>,
    space_name: Option<String>,
    device_id: Option<String>,
    invite_code: Option<String>,
    setup_key: Option<String>,
    auto_create: bool,
}

/// Identity claims a Google token was verified to carry. `verified` is true only
/// when a signature check succeeded (never from client-supplied fields alone).
struct VerifiedGoogleIdentity {
    verified: bool,
    email: Option<String>,
    name: Option<String>,
    picture: Option<String>,
}

pub async fn google_auth(
    OptionalPeerIp(peer_ip): OptionalPeerIp,
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<GoogleAuthRequest>,
) -> AppResult<Json<GoogleAuthResponse>> {
    // Pre-verification rate limit keyed by client IP. This is the FIRST thing that
    // runs, before any token verification — otherwise an attacker can spam garbage
    // id_tokens and force a JWKS network fetch per request (each unknown `kid`
    // bypasses the cache) with no limit applied. Verification failures are also
    // recorded below (record_google_auth_failure) to tighten the limit further.
    let ip_rate_key = client_rate_key(peer_ip, &headers, "google_auth_pre");
    state.check_google_auth_rate_limit(&ip_rate_key)?;

    let mut identity = VerifiedGoogleIdentity {
        verified: false,
        email: None,
        name: None,
        picture: None,
    };

    if let Some(raw_tok) = req.id_token.as_deref().map(str::trim).filter(|s| !s.is_empty()) {
        let verification = verify_id_token(&state, raw_tok).await;
        match verification {
            Ok((v_email, v_name, v_picture)) => {
                identity.verified = true;
                identity.email = Some(v_email);
                identity.name = v_name;
                identity.picture = v_picture.or_else(|| req.picture.clone());
                // Success: clear the IP pre-check so legit retries aren't throttled.
                state.raise_google_auth_rate_limit(&ip_rate_key);
            }
            Err(_) => {
                // Failure: record against the IP pre-check so repeated garbage
                // tokens (incl. forged `kid`s that force JWKS fetches) trip it.
                state.record_google_auth_failure(&ip_rate_key);
                return Err(AppError::unauthorized("invalid or expired ID token"));
            }
        }
    }

    let intent = GoogleAuthIntent {
        email: req.email,
        name: req.name,
        space_name: req.space_name,
        device_id: req.device_id,
        invite_code: req.invite_code,
        setup_key: req.setup_key,
        auto_create: req.auto_create == Some(true),
    };
    finish_google_auth(state, peer_ip, headers, identity, intent).await.map(Json)
}

/// Shared join-or-create core for Google sign-in. Resolves the effective email
/// (verified token email wins over any client-supplied one), applies the same
/// per-IP and per-email rate limits, then logs into an existing account or
/// creates a new one (via invite code, or auto-create for first-run). Returns
/// the identical JSON shape for every entrypoint.
async fn finish_google_auth(
    state: AppState,
    peer_ip: Option<IpAddr>,
    headers: HeaderMap,
    identity: VerifiedGoogleIdentity,
    req: GoogleAuthIntent,
) -> AppResult<GoogleAuthResponse> {
    let verified_by_google = identity.verified;
    let jwt_email = identity.email;
    let jwt_name = identity.name;
    let jwt_picture = identity.picture;

    let raw_email = if let Some(ref verified_email) = jwt_email {
        if !req.email.trim().is_empty() && req.email.trim().to_ascii_lowercase() != *verified_email {
            return Err(AppError::unauthorized("email does not match verified Google token"));
        }
        verified_email.clone()
    } else {
        req.email.clone()
    };
    let email = normalize_email(Some(raw_email))?
        .ok_or_else(|| AppError::bad_request("valid email is required"))?;

    let email_rate_key = client_rate_key(peer_ip, &headers, &email);
    let scoped_email_key = format!("email:{email}");
    state.check_join_rate_limit(&email_rate_key)?;
    state.check_google_auth_rate_limit(&email_rate_key)?;
    state.check_google_auth_rate_limit(&scoped_email_key)?;
    // (The IP pre-check key is handled entirely by the caller: both entrypoints
    // clear it once a signature-verified identity is in hand and record it on
    // verification failure, before reaching this core.)
    let req_device_hash = req.device_id.as_deref().and_then(|d| hash_device_id(d, &state.config.token_secret));

    let raw_name = req
        .name
        .as_deref()
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(str::to_string)
        .or_else(|| {
            jwt_name
                .as_deref()
                .map(str::trim)
                .filter(|s| !s.is_empty())
                .map(str::to_string)
        })
        .unwrap_or_else(|| email.split('@').next().unwrap_or("User").to_string());
    let name: String = raw_name.chars().take(40).collect();

    let invite_code = req
        .invite_code
        .as_deref()
        .map(|s| s.trim().to_ascii_uppercase())
        .filter(|s| !s.is_empty());

    let setup_key_ok = state.config.setup_key.as_deref().is_some_and(|expected| {
        req.setup_key
            .as_deref()
            .or_else(|| headers.get("X-Setup-Key").and_then(|v| v.to_str().ok()))
            .map(str::trim)
            .is_some_and(|provided| constant_time_eq(provided.as_bytes(), expected.as_bytes()))
    });

    let db = state.db.clone();
    let secret = state.config.token_secret.clone();

    // Case A: Existing user login by email (requires verified Google token, matching device_hash, valid setup_key, or valid invite_code)
    let existing = {
        let db = db.clone();
        let secret = secret.clone();
        let email_q = email.clone();
        let req_dev_hash = req_device_hash.clone();
        let inv_code = invite_code.clone();
        let picture_in = jwt_picture.clone();
        tokio::task::spawn_blocking(move || -> AppResult<Option<GoogleAuthResponse>> {
            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
            let tx = conn.transaction()?;
            let found: Option<(i64, Option<String>)> = tx
                .query_row(
                    "SELECT id, device_hash FROM users WHERE lower(email) = lower(?1) ORDER BY id DESC LIMIT 1",
                    params![email_q],
                    |r| Ok((r.get(0)?, r.get(1)?)),
                )
                .optional()?;
            let Some((user_id, stored_device_hash)) = found else {
                return Ok(None);
            };

            let now = now_ms();
            let device_ok = stored_device_hash.as_deref().is_some_and(|expected_hash| {
                req_dev_hash
                    .as_deref()
                    .is_some_and(|h| constant_time_eq(h.as_bytes(), expected_hash.as_bytes()))
            });
            let invite_ok = if !verified_by_google && !device_ok && !setup_key_ok {
                if let Some(ref code) = inv_code {
                    tx.query_row(
                        "SELECT code FROM invites WHERE code = ?1 AND used_at IS NULL AND expires_at > ?2",
                        params![code, now],
                        |r| r.get::<_, String>(0),
                    )
                    .optional()?
                    .is_some()
                } else {
                    false
                }
            } else {
                false
            };

            if !verified_by_google && !device_ok && !invite_ok && !setup_key_ok {
                return Err(AppError::unauthorized(
                    "device verification, verified Google sign-in, setup key, or valid invite code required for existing account",
                ));
            }

            if let Some(ref h) = req_dev_hash {
                let needs_update = stored_device_hash
                    .as_deref()
                    .map(|prev| !constant_time_eq(prev.as_bytes(), h.as_bytes()))
                    .unwrap_or(true);
                if needs_update && (stored_device_hash.is_none() || verified_by_google || invite_ok || setup_key_ok) {
                    tx.execute(
                        "UPDATE users SET device_hash = ?1 WHERE id = ?2",
                        params![h, user_id],
                    )?;
                }
            }

            tx.execute(
                "UPDATE users SET last_seen_at = ?1 WHERE id = ?2",
                params![now, user_id],
            )?;
            // Refresh the Gmail profile photo URL on verified sign-in, but never
            // overwrite a photo the user has explicitly uploaded (avatar_photo_id wins).
            if let Some(ref pic) = picture_in {
                tx.execute(
                    "UPDATE users SET google_picture_url = ?1 WHERE id = ?2 AND (google_picture_url IS NULL OR google_picture_url != ?1)",
                    params![pic, user_id],
                )?;
            }
            let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
            let partner = get_partner(&tx, user_id)?;
            let active_invite = if partner.is_none() {
                tx.query_row(
                    "SELECT code FROM invites WHERE created_by = ?1 AND used_at IS NULL AND expires_at > ?2 ORDER BY created_at DESC LIMIT 1",
                    params![user_id, now],
                    |r| r.get(0),
                )
                .optional()?
            } else {
                None
            };
            let tokens = issue_tokens(&tx, &secret, user_id, now)?;
            let space_name = get_space_name(&tx)?;
            tx.commit()?;
            Ok(Some(GoogleAuthResponse {
                status: "authenticated".to_string(),
                user: Some(user),
                partner,
                invite_code: active_invite,
                tokens: Some(tokens),
                email: None,
                name: None,
                space_name,
            }))
        })
        .await?
    };

    match existing {
        Ok(Some(resp)) => {
            state.clear_join_rate_limit(&email_rate_key);
            state.raise_google_auth_rate_limit(&email_rate_key);
            state.raise_google_auth_rate_limit(&scoped_email_key);
            return Ok(resp);
        }
        Ok(None) => {}
        Err(err) => {
            if err.code == "unauthorized" {
                state.record_join_failure(&email_rate_key);
                state.record_google_auth_failure(&email_rate_key);
                state.record_google_auth_failure(&scoped_email_key);
            }
            return Err(err);
        }
    }

    // Case B: New user joining with invite_code
    if let Some(code) = invite_code {
        let rate_key = client_rate_key(peer_ip, &headers, "invite_attempt");
        state.check_join_rate_limit(&rate_key)?;
        let dev_hash = req_device_hash.clone();
        let picture_in = jwt_picture.clone();
        let res = tokio::task::spawn_blocking(move || -> AppResult<GoogleAuthResponse> {
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
                "INSERT INTO users (name, email, device_hash, google_picture_url, created_at, last_seen_at) VALUES (?1, ?2, ?3, ?4, ?5, ?5)",
                params![name, Some(email), dev_hash, picture_in, now],
            )?;
            let user_id = tx.last_insert_rowid();
            let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
            let partner = get_partner(&tx, user_id)?;
            let tokens = issue_tokens(&tx, &secret, user_id, now)?;
            let space_name = get_space_name(&tx)?;
            tx.commit()?;
            Ok(GoogleAuthResponse {
                status: "authenticated".to_string(),
                user: Some(user),
                partner,
                invite_code: None,
                tokens: Some(tokens),
                email: None,
                name: None,
                space_name,
            })
        })
        .await?;

        return match res {
            Ok(resp) => {
                state.clear_join_rate_limit(&rate_key);
                Ok(resp)
            }
            Err(err) => {
                if err.code == "invite_not_found" {
                    state.record_join_failure(&rate_key);
                }
                Err(err)
            }
        };
    }

    // Case C: New user without invite_code
    if req.auto_create {
        let mut create_rate_key: Option<String> = None;
        if let Some(ref expected_key) = state.config.setup_key {
            let provided_key = req
                .setup_key
                .as_deref()
                .or_else(|| headers.get("X-Setup-Key").and_then(|v| v.to_str().ok()))
                .unwrap_or("");
            let rk = client_rate_key(peer_ip, &headers, "setup_key");
            state.check_create_rate_limit(&rk)?;
            if !constant_time_eq(provided_key.as_bytes(), expected_key.as_bytes()) {
                state.record_create_failure(&rk);
                return Err(AppError::forbidden("invalid or missing X-Setup-Key"));
            }
            create_rate_key = Some(rk);
        }

        let dev_hash = req_device_hash;
        let raw_space_name = req.space_name.clone();
        let picture_in = jwt_picture;
        let resp = tokio::task::spawn_blocking(move || -> AppResult<GoogleAuthResponse> {
            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
            let tx = conn.transaction()?;

            let count: i64 = tx.query_row("SELECT COUNT(*) FROM users", [], |r| r.get(0))?;
            if count > 0 {
                return Err(AppError::already_set_up());
            }

            let now = now_ms();
            let space_name = set_space_name(&tx, raw_space_name.as_deref())?;
            tx.execute(
                "INSERT INTO users (name, email, device_hash, google_picture_url, created_at, last_seen_at) VALUES (?1, ?2, ?3, ?4, ?5, ?5)",
                params![name, Some(email), dev_hash, picture_in, now],
            )?;
            let user_id = tx.last_insert_rowid();
            let user = get_user(&tx, user_id)?.ok_or_else(|| AppError::internal("user missing"))?;
            let tokens = issue_tokens(&tx, &secret, user_id, now)?;
            let code = generate_invite_code();
            let invite_expires_at = now + INVITE_TTL_MS;
            tx.execute(
                "INSERT INTO invites (code, created_by, created_at, expires_at) VALUES (?1, ?2, ?3, ?4)",
                params![code, user_id, now, invite_expires_at],
            )?;
            tx.commit()?;
            Ok(GoogleAuthResponse {
                status: "authenticated".to_string(),
                user: Some(user),
                partner: None,
                invite_code: Some(code),
                tokens: Some(tokens),
                email: None,
                name: None,
                space_name,
            })
        })
        .await??;

        if let Some(ref rk) = create_rate_key {
            state.clear_create_rate_limit(rk);
        }
        return Ok(resp);
    }

    Ok(GoogleAuthResponse {
        status: "needs_pair".to_string(),
        user: None,
        partner: None,
        invite_code: None,
        tokens: None,
        email: Some(email),
        name: Some(name),
        space_name: None,
    })
}

/// Authorization Code + PKCE callback. The SPA redirected the top-level window
/// to Google and landed back on our origin with `?code=...&state=...`, validated
/// `state` client-side, and now POSTs the code plus the PKCE verifier it kept in
/// sessionStorage. We hold the client_secret (required by Google for a Web
/// application client even with PKCE) and perform the token exchange here so the
/// secret never reaches the browser, then hand the resulting id_token to the
/// SAME join-or-create core the legacy id_token handler uses.
pub async fn google_callback(
    OptionalPeerIp(peer_ip): OptionalPeerIp,
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<GoogleCallbackRequest>,
) -> AppResult<Json<GoogleAuthResponse>> {
    // Rate limit BEFORE any outbound call so a flood of bogus codes can't drive
    // unlimited token-endpoint requests. Uses the same limiter as the legacy path.
    let ip_rate_key = client_rate_key(peer_ip, &headers, "google_auth_pre");
    state.check_google_auth_rate_limit(&ip_rate_key)?;

    let code = req.code.trim().to_string();
    let code_verifier = req.code_verifier.trim().to_string();
    if code.is_empty() || code_verifier.is_empty() {
        state.record_google_auth_failure(&ip_rate_key);
        return Err(AppError::bad_request("code and code_verifier are required"));
    }

    let Some(google_client_id) = state.config.google_client_id.clone() else {
        state.record_google_auth_failure(&ip_rate_key);
        return Err(AppError::bad_request("Google sign-in is not configured"));
    };
    let Some(google_client_secret) = state.config.google_client_secret.clone() else {
        state.record_google_auth_failure(&ip_rate_key);
        return Err(AppError::bad_request(
            "Google sign-in is not fully configured (missing client secret)",
        ));
    };

    // The redirect_uri must be a server-side configuration, NOT derived from the
    // Origin header. The browser's Origin is trustworthy for CSRF but must not
    // determine the OAuth redirect URI — that's a server-side decision, pinned
    // to the authorized redirect URI registered in Google Cloud Console.
    let redirect_uri = state
        .config
        .google_redirect_uri
        .clone()
        .unwrap_or_else(|| "https://ours.mnemonic.my.id/".to_string());

    // Exchange the authorization code (+ PKCE verifier + client_secret) for tokens.
    // Reuse the shared HTTP client (used for JWKS / FCM fetches).
    let token_resp = state
        .http_client
        .post("https://oauth2.googleapis.com/token")
        .form(&[
            ("code", code.as_str()),
            ("code_verifier", code_verifier.as_str()),
            ("client_id", google_client_id.as_str()),
            ("client_secret", google_client_secret.as_str()),
            ("redirect_uri", redirect_uri.as_str()),
            ("grant_type", "authorization_code"),
        ])
        .send()
        .await;
    let token_resp = match token_resp {
        Ok(r) => r,
        Err(_) => {
            state.record_google_auth_failure(&ip_rate_key);
            return Err(AppError::unauthorized("Google token exchange failed"));
        }
    };

    #[derive(serde::Deserialize)]
    struct GoogleTokenResponse {
        id_token: Option<String>,
        #[allow(dead_code)]
        access_token: Option<String>,
        error: Option<String>,
        error_description: Option<String>,
    }

    let status = token_resp.status();
    let body: GoogleTokenResponse = token_resp.json().await.unwrap_or(GoogleTokenResponse {
        id_token: None,
        access_token: None,
        error: None,
        error_description: None,
    });

    if !status.is_success() || body.id_token.is_none() {
        state.record_google_auth_failure(&ip_rate_key);
        let detail = body
            .error_description
            .or(body.error)
            .unwrap_or_else(|| format!("token endpoint returned {status}"));
        tracing::warn!("Google PKCE token exchange failed: {detail}");
        return Err(AppError::unauthorized("invalid or expired authorization code"));
    }

    let id_token = body.id_token.unwrap();
    // Reuse the same signature-verified identity resolution as the legacy path.
    let verification = verify_id_token(&state, &id_token).await;
    let (v_email, v_name, v_picture) = match verification {
        Ok(claims) => {
            state.raise_google_auth_rate_limit(&ip_rate_key);
            claims
        }
        Err(_) => {
            state.record_google_auth_failure(&ip_rate_key);
            return Err(AppError::unauthorized("invalid or expired ID token"));
        }
    };

    let identity = VerifiedGoogleIdentity {
        verified: true,
        email: Some(v_email),
        name: v_name,
        picture: v_picture,
    };
    let intent = GoogleAuthIntent {
        email: String::new(),
        name: None,
        space_name: None,
        device_id: None,
        invite_code: None,
        setup_key: None,
        auto_create: false,
    };
    finish_google_auth(state, peer_ip, headers, identity, intent)
        .await
        .map(Json)
}

