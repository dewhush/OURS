use axum::{
    Json,
    extract::{Path, State},
    http::StatusCode,
};
use rusqlite::params;

use crate::auth::AuthUser;
use crate::db::now_ms;
use crate::error::{AppError, AppResult};
use crate::models::{CreatePlaceRequest, Place, PlacesResponse};
use crate::state::AppState;

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

    let db = state.db.clone();
    let place = tokio::task::spawn_blocking(move || -> AppResult<Place> {
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
        Ok(Place {
            id,
            name,
            lat: req.lat,
            lon: req.lon,
            radius_m: req.radius_m,
            created_by: user_id,
            created_at: now,
        })
    })
    .await??;

    Ok((StatusCode::CREATED, Json(place)))
}

pub async fn delete_place(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
    Path(place_id): Path<i64>,
) -> AppResult<StatusCode> {
    let db = state.db.clone();
    let deleted = tokio::task::spawn_blocking(move || -> AppResult<usize> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let n = conn.execute("DELETE FROM places WHERE id = ?1", params![place_id])?;
        Ok(n)
    })
    .await??;

    if deleted == 0 {
        return Err(AppError::not_found("place not found"));
    }
    Ok(StatusCode::NO_CONTENT)
}
