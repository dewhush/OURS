use axum::{
    extract::{Path as AxumPath, State},
    http::{StatusCode, header},
    response::{IntoResponse, Response},
};

use crate::state::AppState;

const EMBED_INDEX: &str = include_str!("../../../web/index.html");
const EMBED_STYLES: &str = include_str!("../../../web/styles.css");
const EMBED_APP: &str = include_str!("../../../web/app.js");
const EMBED_MANIFEST: &str = include_str!("../../../web/manifest.webmanifest");
const EMBED_SW: &str = include_str!("../../../web/sw.js");
const EMBED_TOKENS_HTML: &str = include_str!("../../../web/tokens-preview.html");
const EMBED_TOKENS_JS: &str = include_str!("../../../web/tokens-preview.js");
const EMBED_ASSETS_HTML: &str = include_str!("../../../web/assets-preview.html");
const EMBED_ASSETS_JS: &str = include_str!("../../../web/assets-preview.js");
const EMBED_COMPONENTS_CSS: &str = include_str!("../../../web/components.css");
const EMBED_COMPONENTS_JS: &str = include_str!("../../../web/components.js");
const EMBED_COMPONENTS_HTML: &str = include_str!("../../../web/components-preview.html");
const EMBED_COMPONENTS_PREVIEW_JS: &str = include_str!("../../../web/components-preview.js");

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

pub async fn tokens_preview_html(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "tokens-preview.html",
        "text/html; charset=utf-8",
        EMBED_TOKENS_HTML,
    )
    .await
}

pub async fn tokens_preview_js(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "tokens-preview.js",
        "application/javascript; charset=utf-8",
        EMBED_TOKENS_JS,
    )
    .await
}

pub async fn assets_preview_html(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "assets-preview.html",
        "text/html; charset=utf-8",
        EMBED_ASSETS_HTML,
    )
    .await
}

pub async fn assets_preview_js(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "assets-preview.js",
        "application/javascript; charset=utf-8",
        EMBED_ASSETS_JS,
    )
    .await
}

pub async fn components_css(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "components.css",
        "text/css; charset=utf-8",
        EMBED_COMPONENTS_CSS,
    )
    .await
}

pub async fn components_js(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "components.js",
        "application/javascript; charset=utf-8",
        EMBED_COMPONENTS_JS,
    )
    .await
}

pub async fn components_preview_html(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "components-preview.html",
        "text/html; charset=utf-8",
        EMBED_COMPONENTS_HTML,
    )
    .await
}

pub async fn components_preview_js(State(state): State<AppState>) -> Response {
    serve_web_asset(
        &state,
        "components-preview.js",
        "application/javascript; charset=utf-8",
        EMBED_COMPONENTS_PREVIEW_JS,
    )
    .await
}

fn is_safe_asset_filename(name: &str) -> bool {
    !name.is_empty()
        && !name.starts_with('.')
        && !name.contains("..")
        && !name.contains('/')
        && !name.contains('\\')
        && name
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_' || c == '.')
}

async fn serve_binary_asset(
    state: &AppState,
    rel_path: &str,
    content_type: &'static str,
    fallback: Option<&'static [u8]>,
) -> Response {
    let disk_bytes = if let Some(ref dir) = state.config.web_dir {
        tokio::fs::read(dir.join(rel_path)).await.ok()
    } else {
        None
    };

    let Some(bytes) = disk_bytes.or_else(|| fallback.map(|b| b.to_vec())) else {
        return StatusCode::NOT_FOUND.into_response();
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

pub async fn serve_font(State(state): State<AppState>, AxumPath(name): AxumPath<String>) -> Response {
    if !is_safe_asset_filename(&name) {
        return StatusCode::NOT_FOUND.into_response();
    }
    let fallback: Option<&'static [u8]> = match name.as_str() {
        "Inter-VF.woff2" => Some(include_bytes!("../../../web/fonts/Inter-VF.woff2")),
        "GeistMono-VF.woff2" => Some(include_bytes!("../../../web/fonts/GeistMono-VF.woff2")),
        _ => None,
    };
    let ct = if name.ends_with(".woff2") {
        "font/woff2"
    } else {
        "application/octet-stream"
    };
    serve_binary_asset(&state, &format!("fonts/{name}"), ct, fallback).await
}

pub async fn serve_asset_icon(
    State(state): State<AppState>,
    AxumPath(name): AxumPath<String>,
) -> Response {
    if !is_safe_asset_filename(&name) || !name.ends_with(".svg") {
        return StatusCode::NOT_FOUND.into_response();
    }
    let fallback: Option<&'static [u8]> = match name.as_str() {
        "check-in-frame.svg" => Some(include_bytes!("../../../web/assets/icons/check-in-frame.svg")),
        "compass-rose.svg" => Some(include_bytes!("../../../web/assets/icons/compass-rose.svg")),
        "empty-chat-art.svg" => Some(include_bytes!("../../../web/assets/icons/empty-chat-art.svg")),
        "empty-memories-art.svg" => Some(include_bytes!("../../../web/assets/icons/empty-memories-art.svg")),
        "fab-alerts.svg" => Some(include_bytes!("../../../web/assets/icons/fab-alerts.svg")),
        "fab-bubble.svg" => Some(include_bytes!("../../../web/assets/icons/fab-bubble.svg")),
        "fab-camera.svg" => Some(include_bytes!("../../../web/assets/icons/fab-camera.svg")),
        "fab-layers.svg" => Some(include_bytes!("../../../web/assets/icons/fab-layers.svg")),
        "fab-recenter.svg" => Some(include_bytes!("../../../web/assets/icons/fab-recenter.svg")),
        "fab-settings.svg" => Some(include_bytes!("../../../web/assets/icons/fab-settings.svg")),
        "nav-chat.svg" => Some(include_bytes!("../../../web/assets/icons/nav-chat.svg")),
        "nav-map.svg" => Some(include_bytes!("../../../web/assets/icons/nav-map.svg")),
        "nav-memories.svg" => Some(include_bytes!("../../../web/assets/icons/nav-memories.svg")),
        "nav-settings.svg" => Some(include_bytes!("../../../web/assets/icons/nav-settings.svg")),
        "nudge-wave.svg" => Some(include_bytes!("../../../web/assets/icons/nudge-wave.svg")),
        "ours-icon-dark.svg" => Some(include_bytes!("../../../web/assets/icons/ours-icon-dark.svg")),
        "ours-icon-light.svg" => Some(include_bytes!("../../../web/assets/icons/ours-icon-light.svg")),
        "pair-invite-art.svg" => Some(include_bytes!("../../../web/assets/icons/pair-invite-art.svg")),
        "safety-shield.svg" => Some(include_bytes!("../../../web/assets/icons/safety-shield.svg")),
        _ => None,
    };
    serve_binary_asset(
        &state,
        &format!("assets/icons/{name}"),
        "image/svg+xml; charset=utf-8",
        fallback,
    )
    .await
}

pub async fn serve_asset_file(
    State(state): State<AppState>,
    AxumPath(name): AxumPath<String>,
) -> Response {
    if !is_safe_asset_filename(&name) {
        return StatusCode::NOT_FOUND.into_response();
    }
    let fallback: Option<&'static [u8]> = match name.as_str() {
        "map-style-light.json" => Some(include_bytes!("../../../web/assets/map-style-light.json")),
        "map-style-dark.json" => Some(include_bytes!("../../../web/assets/map-style-dark.json")),
        _ => None,
    };
    let ct = if name.ends_with(".json") {
        "application/json; charset=utf-8"
    } else if name.ends_with(".svg") {
        "image/svg+xml; charset=utf-8"
    } else {
        "application/octet-stream"
    };
    serve_binary_asset(&state, &format!("assets/{name}"), ct, fallback).await
}
