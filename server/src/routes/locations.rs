use std::collections::HashMap;

use axum::{
    Json,
    extract::{Query, State},
};
use rusqlite::{OptionalExtension, params};
use serde::Deserialize;

use crate::auth::AuthUser;
use crate::db::{
    get_partner_id, get_partner_location_for, get_user_fcm_token, get_user_name,
    insert_partner_event, now_ms, query_events_after,
};
use crate::error::{AppError, AppResult};
use crate::models::{
    Event, HistoryResponse, LocationPoint, LocationsResponse, PartnerLocation, SharingRequest,
    SharingResponse,
};
use crate::state::AppState;

#[derive(Deserialize)]
pub struct LocationsQuery {
    pub after_event_id: Option<i64>,
}

#[derive(Deserialize)]
pub struct HistoryQuery {
    pub since: Option<i64>,
    pub until: Option<i64>,
}

fn validate_point(p: &LocationPoint) -> AppResult<()> {
    if !(-90.0..=90.0).contains(&p.lat) || !p.lat.is_finite() {
        return Err(AppError::bad_request("lat must be in [-90, 90]"));
    }
    if !(-180.0..=180.0).contains(&p.lon) || !p.lon.is_finite() {
        return Err(AppError::bad_request("lon must be in [-180, 180]"));
    }
    if p.accuracy_m < 0.0 || !p.accuracy_m.is_finite() {
        return Err(AppError::bad_request("accuracy_m must be >= 0"));
    }
    if p.speed_mps < 0.0 || !p.speed_mps.is_finite() {
        return Err(AppError::bad_request("speed_mps must be >= 0"));
    }
    if let Some(b) = p.bearing
        && (!(0.0..360.0).contains(&b) || !b.is_finite())
    {
        return Err(AppError::bad_request("bearing must be in [0, 360)"));
    }
    if let Some(ref act) = p.activity
        && !matches!(
            act.as_str(),
            "still" | "walking" | "running" | "cycling" | "driving" | "stairs"
        )
    {
        return Err(AppError::bad_request(
            "activity must be one of: still, walking, running, cycling, driving, stairs",
        ));
    }
    if let Some(alt) = p.altitude_m
        && (!alt.is_finite() || !(-500.0..=20000.0).contains(&alt))
    {
        return Err(AppError::bad_request("altitude_m out of range"));
    }
    if let Some(vs) = p.vertical_speed_mps
        && (!vs.is_finite() || !(-200.0..=200.0).contains(&vs))
    {
        return Err(AppError::bad_request("vertical_speed_mps out of range"));
    }
    if !(0..=100).contains(&p.battery_pct) {
        return Err(AppError::bad_request("battery_pct must be in [0, 100]"));
    }
    Ok(())
}

pub fn haversine_m(lat1: f64, lon1: f64, lat2: f64, lon2: f64) -> f64 {
    const EARTH_RADIUS_M: f64 = 6_371_000.0;
    let d_lat = (lat2 - lat1).to_radians();
    let d_lon = (lon2 - lon1).to_radians();
    let a = (d_lat / 2.0).sin().powi(2)
        + lat1.to_radians().cos() * lat2.to_radians().cos() * (d_lon / 2.0).sin().powi(2);
    let c = 2.0 * a.sqrt().atan2((1.0 - a).sqrt());
    EARTH_RADIUS_M * c
}

struct PendingFcm {
    token: String,
    data: HashMap<String, String>,
}

pub async fn post_locations(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Query(query): Query<LocationsQuery>,
    Json(mut points): Json<Vec<LocationPoint>>,
) -> AppResult<Json<LocationsResponse>> {
    if points.is_empty() || points.len() > 500 {
        return Err(AppError::bad_request("locations batch must contain 1-500 points"));
    }
    for p in &points {
        validate_point(p)?;
    }
    points.sort_by_key(|p| p.recorded_at);

    let db = state.db.clone();
    let history_days = state.config.history_days;
    let after_event_id = query.after_event_id;

    let (resp, ws_pushes, fcm_pushes) = tokio::task::spawn_blocking(
        move || -> AppResult<(LocationsResponse, Vec<(i64, Event)>, Vec<PendingFcm>)> {
            let mut conn = db
                .lock()
                .map_err(|_| AppError::internal("database lock poisoned"))?;
            let tx = conn.transaction()?;

            let now = now_ms();
            let paused_int: i64 = tx.query_row(
                "SELECT sharing_paused FROM users WHERE id = ?1",
                params![user_id],
                |r| r.get(0),
            )?;
            let paused = paused_int != 0;

            let inbox_events = if let Some(after_id) = after_event_id {
                query_events_after(&tx, user_id, after_id, 100)?
            } else {
                Vec::new()
            };

            if paused {
                tx.commit()?;
                return Ok((
                    LocationsResponse {
                        accepted: 0,
                        paused: true,
                        events: inbox_events,
                    },
                    Vec::new(),
                    Vec::new(),
                ));
            }

            let mut ws_pushes: Vec<(i64, Event)> = Vec::new();
            let mut fcm_pushes: Vec<PendingFcm> = Vec::new();

            if history_days > 0 {
                let mut stmt = tx.prepare(
                    "INSERT INTO location_history
                     (user_id, lat, lon, accuracy_m, speed_mps, bearing, activity, altitude_m, vertical_speed_mps, battery_pct, charging, recorded_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)",
                )?;
                for p in &points {
                    stmt.execute(params![
                        user_id,
                        p.lat,
                        p.lon,
                        p.accuracy_m,
                        p.speed_mps,
                        p.bearing,
                        p.activity,
                        p.altitude_m,
                        p.vertical_speed_mps,
                        p.battery_pct,
                        i64::from(p.charging),
                        p.recorded_at
                    ])?;
                }
            }

            // Update latest_locations if newest point is newer than stored
            let newest = points.last().expect("non-empty batch");
            let existing_recorded_at: Option<i64> = tx
                .query_row(
                    "SELECT recorded_at FROM latest_locations WHERE user_id = ?1",
                    params![user_id],
                    |r| r.get(0),
                )
                .optional()?;

            let updated_latest = existing_recorded_at.is_none_or(|prev| newest.recorded_at > prev);
            if updated_latest {
                tx.execute(
                    "INSERT INTO latest_locations
                     (user_id, lat, lon, accuracy_m, speed_mps, bearing, activity, altitude_m, vertical_speed_mps, battery_pct, charging, recorded_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)
                     ON CONFLICT(user_id) DO UPDATE SET
                       lat = excluded.lat,
                       lon = excluded.lon,
                       accuracy_m = excluded.accuracy_m,
                       speed_mps = excluded.speed_mps,
                       bearing = excluded.bearing,
                       activity = excluded.activity,
                       altitude_m = excluded.altitude_m,
                       vertical_speed_mps = excluded.vertical_speed_mps,
                       battery_pct = excluded.battery_pct,
                       charging = excluded.charging,
                       recorded_at = excluded.recorded_at",
                    params![
                        user_id,
                        newest.lat,
                        newest.lon,
                        newest.accuracy_m,
                        newest.speed_mps,
                        newest.bearing,
                        newest.activity,
                        newest.altitude_m,
                        newest.vertical_speed_mps,
                        newest.battery_pct,
                        i64::from(newest.charging),
                        newest.recorded_at
                    ],
                )?;
            }

            // Place enter/exit detection
            let places: Vec<(i64, String, f64, f64, f64)> = {
                let mut stmt =
                    tx.prepare("SELECT id, name, lat, lon, radius_m FROM places ORDER BY id")?;
                let rows = stmt.query_map([], |r| {
                    Ok((r.get(0)?, r.get(1)?, r.get(2)?, r.get(3)?, r.get(4)?))
                })?;
                let mut v = Vec::new();
                for r in rows {
                    v.push(r?);
                }
                v
            };

            if !places.is_empty() {
                let actor_name = get_user_name(&tx, user_id)?;
                let partner_fcm = if let Some(pid) = get_partner_id(&tx, user_id)? {
                    get_user_fcm_token(&tx, pid)?
                } else {
                    None
                };

                for p in &points {
                    if p.accuracy_m > 200.0 {
                        continue;
                    }
                    for (place_id, place_name, p_lat, p_lon, radius_m) in &places {
                        let dist = haversine_m(p.lat, p.lon, *p_lat, *p_lon);
                        let prev_inside: Option<i64> = tx
                            .query_row(
                                "SELECT inside FROM place_states WHERE user_id = ?1 AND place_id = ?2",
                                params![user_id, place_id],
                                |r| r.get(0),
                            )
                            .optional()?;

                        match prev_inside {
                            None => {
                                let inside = i64::from(dist <= *radius_m);
                                tx.execute(
                                    "INSERT INTO place_states (user_id, place_id, inside, updated_at)
                                     VALUES (?1, ?2, ?3, ?4)",
                                    params![user_id, place_id, inside, p.recorded_at],
                                )?;
                            }
                            Some(was_inside_int) => {
                                let was_inside = was_inside_int != 0;
                                let transition = if !was_inside && dist <= *radius_m {
                                    Some((1_i64, "enter"))
                                } else if was_inside && dist > *radius_m + 50.0 {
                                    Some((0_i64, "exit"))
                                } else {
                                    None
                                };

                                if let Some((new_inside, trans_str)) = transition {
                                    tx.execute(
                                        "UPDATE place_states SET inside = ?1, updated_at = ?2
                                         WHERE user_id = ?3 AND place_id = ?4",
                                        params![new_inside, p.recorded_at, user_id, place_id],
                                    )?;

                                    let data = serde_json::json!({
                                        "place_id": place_id,
                                        "place_name": place_name,
                                        "transition": trans_str,
                                        "lat": p.lat,
                                        "lon": p.lon,
                                    });
                                    if let Some((partner_id, ev)) =
                                        insert_partner_event(&tx, user_id, "place", data, now)?
                                    {
                                        if let Some(ref token) = partner_fcm {
                                            let body = if trans_str == "enter" {
                                                format!("{actor_name} arrived at {place_name}")
                                            } else {
                                                format!("{actor_name} left {place_name}")
                                            };
                                            let mut fcm_data = HashMap::new();
                                            fcm_data.insert("type".into(), "place".into());
                                            fcm_data.insert("event_id".into(), ev.id.to_string());
                                            fcm_data.insert("title".into(), place_name.clone());
                                            fcm_data.insert("body".into(), body);
                                            fcm_data
                                                .insert("place_id".into(), place_id.to_string());
                                            fcm_data
                                                .insert("transition".into(), trans_str.to_string());
                                            fcm_pushes.push(PendingFcm {
                                                token: token.clone(),
                                                data: fcm_data,
                                            });
                                        }
                                        ws_pushes.push((partner_id, ev));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if updated_latest && let Some(partner_id) = get_partner_id(&tx, user_id)? {
                let partner_loc = get_partner_location_for(&tx, user_id)?;
                let loc_event = Event {
                    id: 0,
                    event_type: "location".to_string(),
                    created_at: now,
                    actor_id: user_id,
                    data: serde_json::to_value(partner_loc).unwrap_or(serde_json::Value::Null),
                };
                ws_pushes.push((partner_id, loc_event));
            }

            let accepted = points.len();
            tx.commit()?;

            Ok((
                LocationsResponse {
                    accepted,
                    paused: false,
                    events: inbox_events,
                },
                ws_pushes,
                fcm_pushes,
            ))
        },
    )
    .await??;

    for (recipient_id, ev) in ws_pushes {
        state.push_ws_event(recipient_id, &ev).await;
    }
    for push in fcm_pushes {
        state.fcm.send_async(Some(push.token), push.data);
    }

    Ok(Json(resp))
}

pub async fn get_partner_location(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
) -> AppResult<Json<PartnerLocation>> {
    let db = state.db.clone();
    let loc = tokio::task::spawn_blocking(move || -> AppResult<PartnerLocation> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let partner_id = get_partner_id(&conn, user_id)?.ok_or_else(AppError::no_partner)?;
        get_partner_location_for(&conn, partner_id)
    })
    .await??;

    Ok(Json(loc))
}

pub async fn get_partner_history(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Query(query): Query<HistoryQuery>,
) -> AppResult<Json<HistoryResponse>> {
    let since = query
        .since
        .ok_or_else(|| AppError::bad_request("since query parameter is required"))?;
    let until = query.until.unwrap_or_else(now_ms);

    let db = state.db.clone();
    let points = tokio::task::spawn_blocking(move || -> AppResult<Vec<LocationPoint>> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let partner_id = get_partner_id(&conn, user_id)?.ok_or_else(AppError::no_partner)?;
        let paused_int: i64 = conn.query_row(
            "SELECT sharing_paused FROM users WHERE id = ?1",
            params![partner_id],
            |r| r.get(0),
        )?;
        if paused_int != 0 {
            return Ok(Vec::new());
        }

        let mut stmt = conn.prepare(
            "SELECT lat, lon, accuracy_m, speed_mps, bearing, activity, altitude_m, vertical_speed_mps,
                    battery_pct, charging, recorded_at
             FROM location_history
             WHERE user_id = ?1 AND recorded_at >= ?2 AND recorded_at <= ?3
             ORDER BY recorded_at ASC
             LIMIT 5000",
        )?;
        let rows = stmt.query_map(params![partner_id, since, until], |r| {
            let charging_int: i64 = r.get(9)?;
            Ok(LocationPoint {
                lat: r.get(0)?,
                lon: r.get(1)?,
                accuracy_m: r.get(2)?,
                speed_mps: r.get(3)?,
                bearing: r.get(4)?,
                activity: r.get(5)?,
                altitude_m: r.get(6)?,
                vertical_speed_mps: r.get(7)?,
                battery_pct: r.get(8)?,
                charging: charging_int != 0,
                recorded_at: r.get(10)?,
            })
        })?;
        let mut pts = Vec::new();
        for r in rows {
            pts.push(r?);
        }
        Ok(pts)
    })
    .await??;

    Ok(Json(HistoryResponse { points }))
}

pub async fn set_sharing(
    AuthUser(user_id): AuthUser,
    State(state): State<AppState>,
    Json(req): Json<SharingRequest>,
) -> AppResult<Json<SharingResponse>> {
    let paused = req.paused;
    let db = state.db.clone();

    let ws_pushes = tokio::task::spawn_blocking(move || -> AppResult<Vec<(i64, Event)>> {
        let mut conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        let tx = conn.transaction()?;

        let prev_int: i64 = tx.query_row(
            "SELECT sharing_paused FROM users WHERE id = ?1",
            params![user_id],
            |r| r.get(0),
        )?;
        let prev = prev_int != 0;

        tx.execute(
            "UPDATE users SET sharing_paused = ?1 WHERE id = ?2",
            params![i64::from(paused), user_id],
        )?;

        if paused {
            tx.execute(
                "DELETE FROM latest_locations WHERE user_id = ?1",
                params![user_id],
            )?;
        }

        let mut pushes = Vec::new();
        if prev != paused {
            let now = now_ms();
            if let Some((partner_id, ev)) = insert_partner_event(
                &tx,
                user_id,
                "sharing",
                serde_json::json!({ "paused": paused }),
                now,
            )? {
                pushes.push((partner_id, ev));
            }
            if let Some(partner_id) = get_partner_id(&tx, user_id)? {
                let partner_loc = get_partner_location_for(&tx, user_id)?;
                pushes.push((
                    partner_id,
                    Event {
                        id: 0,
                        event_type: "location".to_string(),
                        created_at: now,
                        actor_id: user_id,
                        data: serde_json::to_value(partner_loc).unwrap_or(serde_json::Value::Null),
                    },
                ));
            }
        }

        tx.commit()?;
        Ok(pushes)
    })
    .await??;

    for (recipient_id, ev) in ws_pushes {
        state.push_ws_event(recipient_id, &ev).await;
    }

    Ok(Json(SharingResponse { paused }))
}
