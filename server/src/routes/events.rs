use std::collections::HashMap;

use axum::{
    Json,
    extract::{Query, State},
    http::StatusCode,
};
use serde::Deserialize;

use crate::auth::AuthUser;
use crate::db::{
    get_partner_id, get_user_fcm_token, get_user_name, insert_partner_event, now_ms,
    query_events_after,
};
use crate::error::{AppError, AppResult};
use crate::models::{Event, EventsResponse, SosRequest, SosResponse};
use crate::state::AppState;

pub async fn nudge(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<StatusCode> {
    let db = state.db.clone();
    let (ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(Option<(i64, Event)>, Option<(String, HashMap<String, String>)>)> {
            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
            let tx = conn.transaction()?;
            let now = now_ms();

            let ws_push =
                insert_partner_event(&tx, user_id, "nudge", serde_json::json!({}), now)?;

            let fcm_push = if let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let mut data = HashMap::new();
                        data.insert("type".into(), "nudge".into());
                        data.insert("event_id".into(), ev.id.to_string());
                        data.insert("title".into(), actor_name.clone());
                        data.insert("body".into(), format!("{actor_name} is thinking of you"));
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
            Ok((ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok(StatusCode::NO_CONTENT)
}

#[derive(Deserialize)]
pub struct EventsQuery {
    #[serde(default)]
    pub after: i64,
}

pub async fn get_events(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Query(query): Query<EventsQuery>,
) -> AppResult<Json<EventsResponse>> {
    let db = state.db.clone();
    let after = query.after;
    let events = tokio::task::spawn_blocking(move || -> AppResult<Vec<Event>> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        query_events_after(&conn, user_id, after, 100)
    })
    .await??;

    Ok(Json(EventsResponse { events }))
}

pub async fn send_sos(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Json(req): Json<SosRequest>,
) -> AppResult<Json<SosResponse>> {
    let kind = req.kind.trim().to_ascii_lowercase();
    if kind != "manual" && kind != "crash" {
        return Err(AppError::bad_request("kind must be manual or crash"));
    }
    if let Some(lat) = req.lat
        && (!lat.is_finite() || !(-90.0..=90.0).contains(&lat))
    {
        return Err(AppError::bad_request("lat out of range"));
    }
    if let Some(lon) = req.lon
        && (!lon.is_finite() || !(-180.0..=180.0).contains(&lon))
    {
        return Err(AppError::bad_request("lon out of range"));
    }
    let message = req
        .message
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty());

    let db = state.db.clone();
    let (event_id, ws_push, fcm_push) = tokio::task::spawn_blocking(
        move || -> AppResult<(Option<i64>, Option<(i64, Event)>, Option<(String, HashMap<String, String>)>)> {
            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
            let tx = conn.transaction()?;
            let now = now_ms();

            let data = serde_json::json!({
                "kind": &kind,
                "lat": req.lat,
                "lon": req.lon,
                "message": &message,
            });
            let ws_push = insert_partner_event(&tx, user_id, "sos", data, now)?;
            let event_id = ws_push.as_ref().map(|(_, ev)| ev.id);

            let fcm_push = if let Some((_, ref ev)) = ws_push {
                if let Some(pid) = get_partner_id(&tx, user_id)? {
                    if let Some(token) = get_user_fcm_token(&tx, pid)? {
                        let actor_name = get_user_name(&tx, user_id)?;
                        let body = message.unwrap_or_else(|| {
                            if kind == "crash" {
                                format!("{actor_name} may have been in a crash")
                            } else {
                                format!("{actor_name} sent an SOS alert")
                            }
                        });
                        let mut fcm_data = HashMap::new();
                        fcm_data.insert("type".into(), "sos".into());
                        fcm_data.insert("event_id".into(), ev.id.to_string());
                        fcm_data.insert("title".into(), format!("SOS: {actor_name}"));
                        fcm_data.insert("body".into(), body);
                        fcm_data.insert("kind".into(), kind);
                        if let Some(lat) = req.lat {
                            fcm_data.insert("lat".into(), lat.to_string());
                        }
                        if let Some(lon) = req.lon {
                            fcm_data.insert("lon".into(), lon.to_string());
                        }
                        Some((token, fcm_data))
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
            Ok((event_id, ws_push, fcm_push))
        },
    )
    .await??;

    if let Some((recipient_id, ev)) = ws_push {
        state.push_ws_event(recipient_id, &ev).await;
    }
    if let Some((token, data)) = fcm_push {
        state.fcm.send_async(Some(token), data);
    }

    Ok(Json(SosResponse { ok: true, event_id }))
}
