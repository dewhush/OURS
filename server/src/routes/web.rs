use axum::{
    extract::State,
    http::{StatusCode, header},
    response::{IntoResponse, Response},
};

use crate::state::AppState;

const EMBED_INDEX: &str = include_str!("../../../web/index.html");
const EMBED_STYLES: &str = include_str!("../../../web/styles.css");
const EMBED_APP: &str = include_str!("../../../web/app.js");
const EMBED_MANIFEST: &str = include_str!("../../../web/manifest.webmanifest");
const EMBED_SW: &str = include_str!("../../../web/sw.js");

async fn serve_web_asset(
    state: &AppState,
    filename: &str,
    content_type: &'static str,
    fallback: &'static str,
) -> Response {
    let bytes = if let Some(ref dir) = state.config.web_dir {
        tokio::fs::read(dir.join(filename))
            .await
            .unwrap_or_else(|_| fallback.as_bytes().to_vec())
    } else {
        fallback.as_bytes().to_vec()
    };

    (
        StatusCode::OK,
        [
            (header::CONTENT_TYPE, content_type),
            (header::CACHE_CONTROL, "no-cache, no-store, must-revalidate"),
            (header::PRAGMA, "no-cache"),
        ],
        bytes,
    )
        .into_response()
}

pub async fn index_html(State(state): State<AppState>) -> Response {
    serve_web_asset(&state, "index.html", "text/html; charset=utf-8", EMBED_INDEX).await
}

pub async fn styles_css(State(state): State<AppState>) -> Response {
    serve_web_asset(&state, "styles.css", "text/css; charset=utf-8", EMBED_STYLES).await
}

pub async fn app_js(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "app.js",
        "application/javascript; charset=utf-8",
        EMBED_APP,
    )
    .await
}

pub async fn manifest_webmanifest(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "manifest.webmanifest",
        "application/manifest+json; charset=utf-8",
        EMBED_MANIFEST,
    )
    .await
}

pub async fn sw_js(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "sw.js",
        "application/javascript; charset=utf-8",
        EMBED_SW,
    )
    .await
}
