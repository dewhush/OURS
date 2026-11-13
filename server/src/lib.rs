pub mod auth;
pub mod config;
pub mod db;
pub mod error;
pub mod fcm;
pub mod jobs;
pub mod models;
pub mod photos;
pub mod routes;
pub mod state;
pub mod ws;

use axum::{
    Json, Router,
    extract::DefaultBodyLimit,
    routing::{delete, get, post},
};
use serde_json::json;

use crate::state::AppState;

async fn health() -> Json<serde_json::Value> {
    Json(json!({
        "status": "ok",
        "version": env!("CARGO_PKG_VERSION"),
    }))
}

pub fn build_router(state: AppState) -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/api/pair/create", post(routes::pair::create_pair))
        .route("/api/pair/invite", get(routes::pair::reissue_invite))
        .route("/api/pair/join", post(routes::pair::join_pair))
        .route("/api/auth/refresh", post(routes::pair::refresh_auth))
        .route("/api/auth/logout", post(routes::pair::logout))
        .route("/api/me", get(routes::pair::get_me))
        .route("/api/push-token", post(routes::pair::set_push_token))
        .route("/api/locations", post(routes::locations::post_locations))
        .route(
            "/api/partner/location",
            get(routes::locations::get_partner_location),
        )
        .route(
            "/api/partner/history",
            get(routes::locations::get_partner_history),
        )
        .route("/api/sharing", post(routes::locations::set_sharing))
        .route(
            "/api/places",
            get(routes::places::list_places).post(routes::places::create_place),
        )
        .route("/api/places/{id}", delete(routes::places::delete_place))
        .route(
            "/api/checkins",
            get(routes::checkins::list_checkins).post(routes::checkins::create_checkin),
        )
        .route(
            "/api/checkins/{id}",
            get(routes::checkins::get_checkin).delete(routes::checkins::delete_checkin),
        )
        .route(
            "/api/checkins/{id}/highlight",
            post(routes::checkins::set_highlight).delete(routes::checkins::unset_highlight),
        )
        .route(
            "/api/checkins/{id}/replies",
            post(routes::checkins::create_reply),
        )
        .route("/api/memories", get(routes::checkins::list_memories))
        .route(
            "/api/messages",
            get(routes::messages::list_messages).post(routes::messages::post_message),
        )
        .route("/api/photos/{photo_id}", get(photos::get_photo))
        .route("/api/nudge", post(routes::events::nudge))
        .route("/api/events", get(routes::events::get_events))
        .route("/api/ws", get(ws::ws_handler))
        .layer(DefaultBodyLimit::max(12 * 1024 * 1024))
        .with_state(state)
}
