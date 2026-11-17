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
use crate::db::get_partner_id;
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

    let mut reader = image::ImageReader::with_format(std::io::Cursor::new(bytes), ImageFormat::Jpeg);
    let mut limits = image::Limits::default();
    limits.max_image_width = Some(8192);
    limits.max_image_height = Some(8192);
    limits.max_alloc = Some(128 * 1024 * 1024);
    reader.limits(limits);
    let img = reader
        .decode()
        .map_err(|_| AppError::bad_request("invalid or oversized JPEG dimensions"))?;

    // Re-encode full JPEG to strip any client EXIF/GPS/device metadata.
    let mut full_buf = Vec::new();
    {
        let mut encoder = JpegEncoder::new_with_quality(&mut full_buf, 90);
        encoder
            .encode_image(&img)
            .map_err(|_| AppError::internal("failed to encode photo"))?;
    }

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

    fs::write(&full_path, &full_buf).map_err(|_| AppError::internal("failed to write photo"))?;
    if let Err(e) = fs::write(&thumb_path, &thumb_buf) {
        let _ = fs::remove_file(&full_path);
        tracing::error!(error = %e, "failed to write thumbnail");
        return Err(AppError::internal("failed to write thumbnail"));
    }

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let _ = fs::set_permissions(&photos_dir, fs::Permissions::from_mode(0o700));
        let _ = fs::set_permissions(&full_path, fs::Permissions::from_mode(0o600));
        let _ = fs::set_permissions(&thumb_path, fs::Permissions::from_mode(0o600));
    }

    Ok(photo_id)
}

pub fn delete_photo_files(data_dir: &Path, photo_id: &str) {
    let photos_dir = data_dir.join("photos");
    let _ = fs::remove_file(photos_dir.join(format!("{photo_id}_full.jpg")));
    let _ = fs::remove_file(photos_dir.join(format!("{photo_id}_thumb.jpg")));
}

/// Removes written photo files on drop unless disarmed after DB commit.
pub struct PhotoGuard<'a> {
    data_dir: &'a Path,
    photo_id: Option<String>,
}

impl<'a> PhotoGuard<'a> {
    pub fn new(data_dir: &'a Path, photo_id: String) -> Self {
        Self {
            data_dir,
            photo_id: Some(photo_id),
        }
    }

    pub fn disarm(&mut self) {
        self.photo_id = None;
    }
}

impl Drop for PhotoGuard<'_> {
    fn drop(&mut self) {
        if let Some(ref pid) = self.photo_id {
            delete_photo_files(self.data_dir, pid);
        }
    }
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
    AuthUser(user_id): AuthUser,
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

    let db = state.db.clone();
    let pid_check = photo_id.clone();
    let owned = tokio::task::spawn_blocking(move || -> AppResult<bool> {
        let conn = db
            .lock()
            .map_err(|_| AppError::internal("database lock poisoned"))?;
        // Scope every photo lookup to the requesting user and their paired
        // partner so one authenticated user cannot fetch another pair's media.
        let allowed: Vec<i64> = {
            let mut ids = vec![user_id];
            if let Some(partner_id) = get_partner_id(&conn, user_id)? {
                ids.push(partner_id);
            }
            ids
        };
        let placeholders = allowed.iter().map(|_| "?").collect::<Vec<_>>().join(", ");
        let sql = format!(
            "SELECT 1 FROM checkins WHERE photo_id = ?1 AND author_id IN ({placeholders})
             UNION ALL
             SELECT 1 FROM replies WHERE photo_id = ?1 AND author_id IN ({placeholders})
             UNION ALL
             SELECT 1 FROM messages WHERE photo_id = ?1 AND author_id IN ({placeholders})
             UNION ALL
             SELECT 1 FROM users WHERE avatar_photo_id = ?1 AND id IN ({placeholders})
             LIMIT 1"
        );
        let mut stmt = conn.prepare(&sql)?;
        let mut params_vec: Vec<&dyn rusqlite::ToSql> = vec![&pid_check];
        for id in &allowed {
            params_vec.push(id);
        }
        for id in &allowed {
            params_vec.push(id);
        }
        for id in &allowed {
            params_vec.push(id);
        }
        for id in &allowed {
            params_vec.push(id);
        }
        let exists = stmt.exists(rusqlite::params_from_iter(params_vec))?;
        Ok(exists)
    })
    .await??;

    if !owned {
        return Err(AppError::not_found("photo not found"));
    }

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
