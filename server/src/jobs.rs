use std::collections::HashMap;
use std::time::Duration;

use rusqlite::params;

use crate::db::now_ms;
use crate::photos::delete_photo_files;
use crate::state::AppState;

const SEVEN_DAYS_MS: i64 = 7 * 24 * 60 * 60 * 1000;

pub fn spawn_background_jobs(state: AppState) {
    let wake_state = state.clone();
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_secs(60));
        interval.tick().await;
        loop {
            interval.tick().await;
            run_wake_check(&wake_state).await;
        }
    });

    let cleanup_state = state;
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_secs(3600));
        interval.tick().await;
        loop {
            interval.tick().await;
            run_hourly_cleanup(&cleanup_state).await;
        }
    });
}

pub async fn run_wake_check(state: &AppState) {
    if !state.fcm.is_enabled() || state.config.stale_minutes <= 0 {
        return;
    }
    let stale_ms = state.config.stale_minutes * 60 * 1000;
    let db = state.db.clone();

    let targets = tokio::task::spawn_blocking(move || -> Vec<(i64, String)> {
        let Ok(conn) = db.lock() else {
            return Vec::new();
        };
        let now = now_ms();
        let cutoff = now - stale_ms;

        let Ok(mut stmt) = conn.prepare(
            "SELECT id, fcm_token
             FROM users
             WHERE sharing_paused = 0
               AND fcm_token IS NOT NULL
               AND last_seen_at IS NOT NULL
               AND last_seen_at <= ?1
               AND (last_wake_sent_at IS NULL OR last_wake_sent_at <= ?1)",
        ) else {
            return Vec::new();
        };

        let mut list = Vec::new();
        if let Ok(rows) = stmt.query_map(params![cutoff], |r| Ok((r.get(0)?, r.get(1)?))) {
            for r in rows.flatten() {
                list.push(r);
            }
        }
        for (uid, _) in &list {
            let _ = conn.execute(
                "UPDATE users SET last_wake_sent_at = ?1 WHERE id = ?2",
                params![now, uid],
            );
        }
        list
    })
    .await
    .unwrap_or_default();

    for (uid, token) in targets {
        let mut data = HashMap::new();
        data.insert("type".into(), "wake".into());
        if let Err(err) = state.fcm.send_blocking(&token, &data).await {
            tracing::warn!(user_id = uid, error = %err, "wake FCM failed");
        }
    }
}

pub async fn run_hourly_cleanup(state: &AppState) {
    let db = state.db.clone();
    let data_dir = state.config.data_dir.clone();
    let history_days = state.config.history_days;

    let deleted_photos = tokio::task::spawn_blocking(move || -> Vec<String> {
        let Ok(mut conn) = db.lock() else {
            return Vec::new();
        };
        let Ok(tx) = conn.transaction() else {
            return Vec::new();
        };
        let now = now_ms();

        let mut photo_ids = Vec::new();
        if let Ok(mut stmt) = tx.prepare(
            "SELECT photo_id FROM checkins WHERE highlighted = 0 AND expires_at <= ?1",
        ) && let Ok(rows) = stmt.query_map(params![now], |r| r.get::<_, String>(0))
        {
            for pid in rows.flatten() {
                photo_ids.push(pid);
            }
        }

        if let Ok(mut stmt) = tx.prepare(
            "SELECT r.photo_id
             FROM replies r
             JOIN checkins c ON c.id = r.checkin_id
             WHERE c.highlighted = 0 AND c.expires_at <= ?1 AND r.photo_id IS NOT NULL",
        ) && let Ok(rows) = stmt.query_map(params![now], |r| r.get::<_, String>(0))
        {
            for pid in rows.flatten() {
                photo_ids.push(pid);
            }
        }

        let _ = tx.execute(
            "DELETE FROM checkins WHERE highlighted = 0 AND expires_at <= ?1",
            params![now],
        );

        if history_days > 0 {
            let hist_cutoff = now - history_days * 24 * 60 * 60 * 1000;
            let _ = tx.execute(
                "DELETE FROM location_history WHERE recorded_at <= ?1",
                params![hist_cutoff],
            );
        }

        let event_cutoff = now - SEVEN_DAYS_MS;
        let _ = tx.execute(
            "DELETE FROM events WHERE created_at <= ?1",
            params![event_cutoff],
        );
        let _ = tx.execute(
            "DELETE FROM refresh_tokens WHERE expires_at <= ?1",
            params![now],
        );

        let _ = tx.commit();
        photo_ids
    })
    .await
    .unwrap_or_default();

    for pid in deleted_photos {
        delete_photo_files(&data_dir, &pid);
    }
}
