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
use crate::models::{Event, EventsResponse};
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
