use std::time::Duration;

use axum::{
    Json,
    extract::{Path, State},
    http::StatusCode,
};
use rusqlite::{OptionalExtension, params};

use crate::auth::AuthUser;
use crate::db::{get_partner_id, now_ms};
use crate::error::{AppError, AppResult};
use crate::models::{CreatePlaceRequest, Event, Place, PlacesResponse};
use crate::state::AppState;

const PLACE_MAX_PER_DAY: usize = 50;

pub async fn list_places(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<Json<PlacesResponse>> {
    let db = state.db.clone();
    let places = tokio::task::spawn_blocking(move || -> AppResult<Vec<Place>> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let mut stmt = conn.prepare(
            "SELECT id, name, lat, lon, radius_m, created_by, created_at
             FROM places ORDER BY id ASC",
        )?;
        let rows = stmt.query_map([], |r| {
            Ok(Place {
                id: r.get(0)?,
                name: r.get(1)?,
                lat: r.get(2)?,
                lon: r.get(3)?,
                radius_m: r.get(4)?,
                created_by: r.get(5)?,
                created_at: r.get(6)?,
            })
        })?;
        let mut list = Vec::new();
        for r in rows {
            list.push(r?);
        }
        Ok(list)
    })
    .await??;

    Ok(Json(PlacesResponse { places }))
}

pub async fn create_place(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Json(req): Json<CreatePlaceRequest>,
) -> AppResult<(StatusCode, Json<Place>)> {
    let name = req.name.trim().to_string();
    if name.is_empty() || name.chars().count() > 40 {
        return Err(AppError::bad_request("name must be 1-40 characters"));
    }
    if !(-90.0..=90.0).contains(&req.lat) || !req.lat.is_finite() {
        return Err(AppError::bad_request("lat must be in [-90, 90]"));
    }
    if !(-180.0..=180.0).contains(&req.lon) || !req.lon.is_finite() {
        return Err(AppError::bad_request("lon must be in [-180, 180]"));
    }
    if !(20.0..=5000.0).contains(&req.radius_m) || !req.radius_m.is_finite() {
        return Err(AppError::bad_request("radius_m must be in [20, 5000]"));
    }

    state.check_user_action(user_id, "place", PLACE_MAX_PER_DAY, Duration::from_secs(86400))?;
    state.record_user_action(user_id, "place");

    let db = state.db.clone();
    let (place, partner_id) = tokio::task::spawn_blocking(move || -> AppResult<(Place, Option<i64>)> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let now = now_ms();
        conn.execute(
            "INSERT INTO places (name, lat, lon, radius_m, created_by, created_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            params![name, req.lat, req.lon, req.radius_m, user_id, now],
        )?;
        let id = conn.last_insert_rowid();
        let partner_id = get_partner_id(&conn, user_id)?;
        Ok((
            Place {
                id,
                name,
                lat: req.lat,
                lon: req.lon,
                radius_m: req.radius_m,
                created_by: user_id,
                created_at: now,
            },
            partner_id,
        ))
    })
    .await??;

    if let Some(pid) = partner_id {
        let ev = Event {
            id: 0,
            event_type: "places_changed".to_string(),
            created_at: place.created_at,
            actor_id: user_id,
            data: serde_json::json!({}),
        };
        state.push_ws_event(pid, &ev).await;
    }

    Ok((StatusCode::CREATED, Json(place)))
}

pub async fn delete_place(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Path(place_id): Path<i64>,
) -> AppResult<StatusCode> {
    let db = state.db.clone();
    let (deleted, partner_id) = tokio::task::spawn_blocking(move || -> AppResult<(usize, Option<i64>)> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;

        // Only the creator or their paired partner may delete a place.
        let created_by: Option<i64> = conn
            .query_row(
                "SELECT created_by FROM places WHERE id = ?1",
                params![place_id],
                |r| r.get(0),
            )
            .optional()?;
        let Some(owner_id) = created_by else {
            return Ok((0, None));
        };
        let partner_of_owner = get_partner_id(&conn, owner_id)?;
        let is_creator = owner_id == user_id;
        let is_partner = partner_of_owner == Some(user_id);
        if !is_creator && !is_partner {
            return Err(AppError::forbidden("only the pair that owns this place can delete it"));
        }

        let n = conn.execute("DELETE FROM places WHERE id = ?1", params![place_id])?;
        let partner_id = if n > 0 {
            get_partner_id(&conn, user_id)?
        } else {
            None
        };
        Ok((n, partner_id))
    })
    .await??;

    if deleted == 0 {
        return Err(AppError::not_found("place not found"));
    }

    if let Some(pid) = partner_id {
        let ev = Event {
            id: 0,
            event_type: "places_changed".to_string(),
            created_at: now_ms(),
            actor_id: user_id,
            data: serde_json::json!({}),
        };
        state.push_ws_event(pid, &ev).await;
    }

    Ok(StatusCode::NO_CONTENT)
}
