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
    extract::{DefaultBodyLimit, Request},
    http::HeaderValue,
    middleware::{self, Next},
    response::Response,
    routing::{delete, get, post},
};
use serde_json::json;

use crate::state::AppState;

async fn security_headers(req: Request, next: Next) -> Response {
    let mut resp = next.run(req).await;
    let headers = resp.headers_mut();
    headers.insert(
        "x-content-type-options",
        HeaderValue::from_static("nosniff"),
    );
    headers.insert("x-frame-options", HeaderValue::from_static("DENY"));
    // HSTS: force HTTPS on every subsequent visit for a year (browser-level
    // downgrade protection; currently served through Cloudflare's HTTPS edge).
    headers.insert(
        "strict-transport-security",
        HeaderValue::from_static("max-age=31536000; includeSubDomains"),
    );
    // Required for Google Identity Services OAuth popup flow: without this,
    // Chrome blocks window.closed polling from the popup, causing instant
    // "sign in cancelled". same-origin-allow-popups explicitly permits
    // popup↔opener postMessage for OAuth callbacks.
    headers.insert(
        "cross-origin-opener-policy",
        HeaderValue::from_static("same-origin-allow-popups"),
    );
    headers.insert(
        "referrer-policy",
        HeaderValue::from_static("strict-origin-when-cross-origin"),
    );
    headers.insert(
        "permissions-policy",
        HeaderValue::from_static("geolocation=(self), camera=(self), microphone=()"),
    );
    headers.insert(
        "content-security-policy",
        HeaderValue::from_static(
            "default-src 'self'; font-src 'self'; script-src 'self' https://unpkg.com https://static.cloudflareinsights.com; style-src 'self' 'unsafe-inline' https://unpkg.com; img-src 'self' data: blob: https:; connect-src 'self' ws: wss: https://tiles.openfreemap.org https://*.basemaps.cartocdn.com https://unpkg.com https://cloudflareinsights.com; worker-src 'self' blob:; frame-ancestors 'none'; base-uri 'self'; object-src 'none'",
        ),
    );
    resp
}

async fn health() -> Json<serde_json::Value> {
    Json(json!({
        "status": "ok",
        "version": env!("CARGO_PKG_VERSION"),
    }))
}

pub fn build_router(state: AppState) -> Router {
    Router::new()
        .route("/", get(routes::web::index_html))
        .route("/index.html", get(routes::web::index_html))
        .route("/styles.css", get(routes::web::styles_css))
        .route("/web/styles.css", get(routes::web::styles_css))
        .route("/app.js", get(routes::web::app_js))
        .route("/web/app.js", get(routes::web::app_js))
        .route("/manifest.webmanifest", get(routes::web::manifest_webmanifest))
        .route("/web/manifest.webmanifest", get(routes::web::manifest_webmanifest))
        .route("/sw.js", get(routes::web::sw_js))
        .route("/web/sw.js", get(routes::web::sw_js))
        .route("/tokens-preview.html", get(routes::web::tokens_preview_html))
        .route("/web/tokens-preview.html", get(routes::web::tokens_preview_html))
        .route("/tokens-preview.js", get(routes::web::tokens_preview_js))
        .route("/web/tokens-preview.js", get(routes::web::tokens_preview_js))
        .route("/assets-preview.html", get(routes::web::assets_preview_html))
        .route("/web/assets-preview.html", get(routes::web::assets_preview_html))
        .route("/assets-preview.js", get(routes::web::assets_preview_js))
        .route("/web/assets-preview.js", get(routes::web::assets_preview_js))
        .route("/components.css", get(routes::web::components_css))
        .route("/web/components.css", get(routes::web::components_css))
        .route("/components.js", get(routes::web::components_js))
        .route("/web/components.js", get(routes::web::components_js))
        .route("/components-preview.html", get(routes::web::components_preview_html))
        .route("/web/components-preview.html", get(routes::web::components_preview_html))
        .route("/components-preview.js", get(routes::web::components_preview_js))
        .route("/web/components-preview.js", get(routes::web::components_preview_js))
        .route("/fonts/{name}", get(routes::web::serve_font))
        .route("/web/fonts/{name}", get(routes::web::serve_font))
        .route("/assets/icons/{name}", get(routes::web::serve_asset_icon))
        .route("/web/assets/icons/{name}", get(routes::web::serve_asset_icon))
        .route("/assets/{name}", get(routes::web::serve_asset_file))
        .route("/web/assets/{name}", get(routes::web::serve_asset_file))
        .route("/health", get(health))
        .route("/healthz", get(health))
        .route("/api/config", get(routes::pair::public_config))
        .route("/api/pair/create", post(routes::pair::create_pair))
        .route("/api/pair/invite", get(routes::pair::reissue_invite))
        .route("/api/pair/join", post(routes::pair::join_pair))
        .route("/api/pair/state", get(routes::pair::get_pair_state))
        .route("/api/auth/refresh", post(routes::pair::refresh_auth))
        .route("/api/auth/google", post(routes::pair::google_auth))
        .route("/api/auth/google/callback", post(routes::pair::google_callback))
        .route("/api/auth/logout", post(routes::pair::logout))
        .route("/api/me", get(routes::pair::get_me))
        .route("/api/me/avatar", post(routes::pair::upload_avatar))
        .route("/api/me/name", post(routes::pair::update_name))
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
        .route("/api/sos", post(routes::events::send_sos))
        .route("/api/events", get(routes::events::get_events))
        .route("/api/ws", get(ws::ws_handler))
        .layer(middleware::from_fn(security_headers))
        .layer(DefaultBodyLimit::max(12 * 1024 * 1024))
        .with_state(state)
}
