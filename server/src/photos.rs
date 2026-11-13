use std::fs;
use std::path::Path;

use axum::{
    extract::{Path as AxumPath, Query, State},
    http::{StatusCode, header},
    response::{IntoResponse, Response},
};
use image::{ImageFormat, codecs::jpeg::JpegEncoder, imageops::FilterType};
use rand::RngCore;
use serde::Deserialize;

use crate::auth::AuthUser;
use crate::error::{AppError, AppResult};
use crate::state::AppState;

pub const MAX_PHOTO_BYTES: usize = 10 * 1024 * 1024;

pub fn save_jpeg_and_thumb(data_dir: &Path, bytes: &[u8]) -> AppResult<String> {
    if bytes.is_empty() {
        return Err(AppError::bad_request("empty photo"));
    }
    if bytes.len() > MAX_PHOTO_BYTES {
        return Err(AppError::too_large("photo exceeds 10 MB limit"));
    }

    let img = image::load_from_memory_with_format(bytes, ImageFormat::Jpeg)
        .map_err(|_| AppError::bad_request("photo must be a valid JPEG"))?;

    let thumb = if img.width().max(img.height()) > 400 {
        img.resize(400, 400, FilterType::Triangle)
    } else {
        img
    };

    let mut thumb_buf = Vec::new();
    {
        let mut encoder = JpegEncoder::new_with_quality(&mut thumb_buf, 80);
        encoder
            .encode_image(&thumb)
            .map_err(|_| AppError::internal("failed to encode thumbnail"))?;
    }

    let mut raw_id = [0u8; 16];
    rand::rng().fill_bytes(&mut raw_id);
    let photo_id = hex::encode(raw_id);

    let photos_dir = data_dir.join("photos");
    fs::create_dir_all(&photos_dir).map_err(|_| AppError::internal("failed to create photos dir"))?;

    let full_path = photos_dir.join(format!("{photo_id}_full.jpg"));
    let thumb_path = photos_dir.join(format!("{photo_id}_thumb.jpg"));

    fs::write(&full_path, bytes).map_err(|_| AppError::internal("failed to write photo"))?;
    if let Err(e) = fs::write(&thumb_path, &thumb_buf) {
        let _ = fs::remove_file(&full_path);
        tracing::error!(error = %e, "failed to write thumbnail");
        return Err(AppError::internal("failed to write thumbnail"));
    }

    Ok(photo_id)
}

pub fn delete_photo_files(data_dir: &Path, photo_id: &str) {
    let photos_dir = data_dir.join("photos");
    let _ = fs::remove_file(photos_dir.join(format!("{photo_id}_full.jpg")));
    let _ = fs::remove_file(photos_dir.join(format!("{photo_id}_thumb.jpg")));
}

pub fn is_valid_photo_id(photo_id: &str) -> bool {
    photo_id.len() == 32 && photo_id.bytes().all(|b| b.is_ascii_hexdigit())
}

#[derive(Deserialize)]
pub struct PhotoQuery {
    #[serde(default = "default_size")]
    pub size: String,
}

fn default_size() -> String {
    "full".to_string()
}

pub async fn get_photo(
    AuthUser(_user_id): AuthUser,
    State(state): State<AppState>,
    AxumPath(photo_id): AxumPath<String>,
    Query(query): Query<PhotoQuery>,
) -> AppResult<Response> {
    if !is_valid_photo_id(&photo_id) {
        return Err(AppError::not_found("photo not found"));
    }
    let suffix = match query.size.as_str() {
        "thumb" => "thumb",
        "full" => "full",
        _ => return Err(AppError::bad_request("size must be thumb or full")),
    };

    let path = state
        .config
        .data_dir
        .join("photos")
        .join(format!("{photo_id}_{suffix}.jpg"));

    let bytes = tokio::fs::read(&path)
        .await
        .map_err(|_| AppError::not_found("photo not found"))?;

    Ok((
        StatusCode::OK,
        [
            (header::CONTENT_TYPE, "image/jpeg"),
            (header::CACHE_CONTROL, "private, max-age=31536000, immutable"),
        ],
        bytes,
    )
        .into_response())
}
