use std::path::Path;
use std::sync::{Arc, Mutex};
use std::time::{SystemTime, UNIX_EPOCH};

use rusqlite::{Connection, OptionalExtension, params};

use crate::error::AppResult;
use crate::models::{CheckIn, Event, PartnerLocation, Reply, User};

pub type Db = Arc<Mutex<Connection>>;

pub fn now_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as i64
}

pub fn open_db(path: &Path) -> Result<Db, String> {
    let conn = Connection::open(path).map_err(|e| format!("failed to open sqlite db: {e}"))?;
    conn.execute_batch(
        "
        PRAGMA journal_mode = WAL;
        PRAGMA foreign_keys = ON;
        PRAGMA busy_timeout = 5000;
        PRAGMA synchronous = NORMAL;

        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT,
            created_at INTEGER NOT NULL,
            sharing_paused INTEGER NOT NULL DEFAULT 0,
            last_seen_at INTEGER,
            last_wake_sent_at INTEGER,
            fcm_token TEXT
        );

        CREATE TABLE IF NOT EXISTS invites (
            code TEXT PRIMARY KEY,
            created_by INTEGER NOT NULL,
            created_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            used_at INTEGER,
            FOREIGN KEY(created_by) REFERENCES users(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS refresh_tokens (
            token_hash TEXT PRIMARY KEY,
            user_id INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS latest_locations (
            user_id INTEGER PRIMARY KEY,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            accuracy_m REAL NOT NULL,
            speed_mps REAL NOT NULL,
            bearing REAL,
            activity TEXT,
            altitude_m REAL,
            vertical_speed_mps REAL,
            battery_pct INTEGER NOT NULL,
            charging INTEGER NOT NULL,
            recorded_at INTEGER NOT NULL,
            FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS location_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            accuracy_m REAL NOT NULL,
            speed_mps REAL NOT NULL,
            bearing REAL,
            activity TEXT,
            altitude_m REAL,
            vertical_speed_mps REAL,
            battery_pct INTEGER NOT NULL,
            charging INTEGER NOT NULL,
            recorded_at INTEGER NOT NULL,
            FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
        );
        CREATE INDEX IF NOT EXISTS idx_loc_hist_user_time ON location_history(user_id, recorded_at);

        CREATE TABLE IF NOT EXISTS places (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            radius_m REAL NOT NULL,
            created_by INTEGER NOT NULL,
            created_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS place_states (
            user_id INTEGER NOT NULL,
            place_id INTEGER NOT NULL,
            inside INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            PRIMARY KEY(user_id, place_id),
            FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS checkins (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            author_id INTEGER NOT NULL,
            photo_id TEXT NOT NULL,
            note TEXT,
            lat REAL,
            lon REAL,
            place_name TEXT,
            created_at INTEGER NOT NULL,
            expires_at INTEGER NOT NULL,
            highlighted INTEGER NOT NULL DEFAULT 0,
            highlighted_by INTEGER,
            highlighted_at INTEGER
        );
        CREATE INDEX IF NOT EXISTS idx_checkins_created ON checkins(created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_checkins_highlighted ON checkins(highlighted, created_at DESC);

        CREATE TABLE IF NOT EXISTS replies (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            checkin_id INTEGER NOT NULL,
            author_id INTEGER NOT NULL,
            text TEXT,
            photo_id TEXT,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(checkin_id) REFERENCES checkins(id) ON DELETE CASCADE
        );
        CREATE INDEX IF NOT EXISTS idx_replies_checkin ON replies(checkin_id, created_at ASC);

        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            recipient_id INTEGER NOT NULL,
            actor_id INTEGER NOT NULL,
            event_type TEXT NOT NULL,
            data_json TEXT NOT NULL,
            created_at INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_events_recipient ON events(recipient_id, id);

        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            author_id INTEGER NOT NULL,
            text TEXT,
            photo_id TEXT,
            reply_to_message_id INTEGER,
            reply_to_checkin_id INTEGER,
            created_at INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at DESC, id DESC);
        ",
    )
    .map_err(|e| format!("failed to initialize schema: {e}"))?;

    // Migrate existing DBs that predate the email column
    let has_email: bool = {
        let mut stmt = conn
            .prepare("PRAGMA table_info(users)")
            .map_err(|e| format!("failed to inspect users table: {e}"))?;
        let cols = stmt
            .query_map([], |row| row.get::<_, String>(1))
            .map_err(|e| format!("failed to query table_info: {e}"))?;
        let mut found = false;
        for c in cols.flatten() {
            if c == "email" {
                found = true;
                break;
            }
        }
        found
    };
    if !has_email {
        conn.execute("ALTER TABLE users ADD COLUMN email TEXT", [])
            .map_err(|e| format!("failed to migrate users.email: {e}"))?;
    }

    ensure_column(&conn, "latest_locations", "activity", "TEXT")?;
    ensure_column(&conn, "latest_locations", "altitude_m", "REAL")?;
    ensure_column(&conn, "latest_locations", "vertical_speed_mps", "REAL")?;
    ensure_column(&conn, "location_history", "activity", "TEXT")?;
    ensure_column(&conn, "location_history", "altitude_m", "REAL")?;
    ensure_column(&conn, "location_history", "vertical_speed_mps", "REAL")?;

    Ok(Arc::new(Mutex::new(conn)))
}

fn ensure_column(conn: &Connection, table: &str, column: &str, col_type: &str) -> Result<(), String> {
    let mut stmt = conn
        .prepare(&format!("PRAGMA table_info({table})"))
        .map_err(|e| format!("failed to inspect {table}: {e}"))?;
    let cols = stmt
        .query_map([], |row| row.get::<_, String>(1))
        .map_err(|e| format!("failed to query table_info({table}): {e}"))?;
    let exists = cols.flatten().any(|c| c == column);
    if !exists {
        conn.execute(&format!("ALTER TABLE {table} ADD COLUMN {column} {col_type}"), [])
            .map_err(|e| format!("failed to migrate {table}.{column}: {e}"))?;
    }
    Ok(())
}

pub fn get_user(conn: &Connection, user_id: i64) -> AppResult<Option<User>> {
    Ok(conn
        .query_row(
            "SELECT id, name, email, created_at FROM users WHERE id = ?1",
            params![user_id],
            |row| {
                Ok(User {
                    id: row.get(0)?,
                    name: row.get(1)?,
                    email: row.get(2)?,
                    created_at: row.get(3)?,
                })
            },
        )
        .optional()?)
}

pub fn get_partner(conn: &Connection, user_id: i64) -> AppResult<Option<User>> {
    Ok(conn
        .query_row(
            "SELECT id, name, email, created_at FROM users WHERE id != ?1 ORDER BY id LIMIT 1",
            params![user_id],
            |row| {
                Ok(User {
                    id: row.get(0)?,
                    name: row.get(1)?,
                    email: row.get(2)?,
                    created_at: row.get(3)?,
                })
            },
        )
        .optional()?)
}

pub fn get_partner_id(conn: &Connection, user_id: i64) -> AppResult<Option<i64>> {
    Ok(conn
        .query_row(
            "SELECT id FROM users WHERE id != ?1 ORDER BY id LIMIT 1",
            params![user_id],
            |row| row.get(0),
        )
        .optional()?)
}

pub fn get_user_name(conn: &Connection, user_id: i64) -> AppResult<String> {
    let name: String = conn.query_row(
        "SELECT name FROM users WHERE id = ?1",
        params![user_id],
        |row| row.get(0),
    )?;
    Ok(name)
}

pub fn get_user_fcm_token(conn: &Connection, user_id: i64) -> AppResult<Option<String>> {
    Ok(conn
        .query_row(
            "SELECT fcm_token FROM users WHERE id = ?1",
            params![user_id],
            |row| row.get(0),
        )
        .optional()?
        .flatten())
}

pub fn insert_partner_event(
    conn: &Connection,
    actor_id: i64,
    event_type: &str,
    data: serde_json::Value,
    created_at: i64,
) -> AppResult<Option<(i64, Event)>> {
    let Some(partner_id) = get_partner_id(conn, actor_id)? else {
        return Ok(None);
    };
    let data_json = serde_json::to_string(&data).unwrap_or_else(|_| "{}".to_string());
    conn.execute(
        "INSERT INTO events (recipient_id, actor_id, event_type, data_json, created_at)
         VALUES (?1, ?2, ?3, ?4, ?5)",
        params![partner_id, actor_id, event_type, data_json, created_at],
    )?;
    let id = conn.last_insert_rowid();
    Ok(Some((
        partner_id,
        Event {
            id,
            event_type: event_type.to_string(),
            created_at,
            actor_id,
            data,
        },
    )))
}

pub fn query_events_after(
    conn: &Connection,
    recipient_id: i64,
    after_id: i64,
    limit: i64,
) -> AppResult<Vec<Event>> {
    let mut stmt = conn.prepare(
        "SELECT id, event_type, created_at, actor_id, data_json
         FROM events
         WHERE recipient_id = ?1 AND id > ?2
         ORDER BY id ASC
         LIMIT ?3",
    )?;
    let rows = stmt.query_map(params![recipient_id, after_id, limit], |row| {
        let data_json: String = row.get(4)?;
        let data = serde_json::from_str(&data_json).unwrap_or(serde_json::Value::Null);
        Ok(Event {
            id: row.get(0)?,
            event_type: row.get(1)?,
            created_at: row.get(2)?,
            actor_id: row.get(3)?,
            data,
        })
    })?;
    let mut events = Vec::new();
    for r in rows {
        events.push(r?);
    }
    Ok(events)
}

pub fn get_partner_location_for(
    conn: &Connection,
    partner_id: i64,
) -> AppResult<PartnerLocation> {
    let (paused_int, last_seen_at): (i64, Option<i64>) = conn.query_row(
        "SELECT sharing_paused, last_seen_at FROM users WHERE id = ?1",
        params![partner_id],
        |row| Ok((row.get(0)?, row.get(1)?)),
    )?;
    let paused = paused_int != 0;
    if paused {
        return Ok(PartnerLocation {
            user_id: partner_id,
            paused: true,
            last_seen_at,
            lat: None,
            lon: None,
            accuracy_m: None,
            speed_mps: None,
            bearing: None,
            activity: None,
            altitude_m: None,
            vertical_speed_mps: None,
            battery_pct: None,
            charging: None,
            recorded_at: None,
        });
    }

    let loc = conn
        .query_row(
            "SELECT lat, lon, accuracy_m, speed_mps, bearing, activity, altitude_m, vertical_speed_mps,
                    battery_pct, charging, recorded_at
             FROM latest_locations WHERE user_id = ?1",
            params![partner_id],
            |row| {
                let charging_int: i64 = row.get(9)?;
                Ok((
                    row.get::<_, f64>(0)?,
                    row.get::<_, f64>(1)?,
                    row.get::<_, f64>(2)?,
                    row.get::<_, f64>(3)?,
                    row.get::<_, Option<f64>>(4)?,
                    row.get::<_, Option<String>>(5)?,
                    row.get::<_, Option<f64>>(6)?,
                    row.get::<_, Option<f64>>(7)?,
                    row.get::<_, i64>(8)?,
                    charging_int != 0,
                    row.get::<_, i64>(10)?,
                ))
            },
        )
        .optional()?;

    if let Some((
        lat,
        lon,
        accuracy_m,
        speed_mps,
        bearing,
        activity,
        altitude_m,
        vertical_speed_mps,
        battery_pct,
        charging,
        recorded_at,
    )) = loc
    {
        Ok(PartnerLocation {
            user_id: partner_id,
            paused: false,
            last_seen_at,
            lat: Some(lat),
            lon: Some(lon),
            accuracy_m: Some(accuracy_m),
            speed_mps: Some(speed_mps),
            bearing,
            activity,
            altitude_m,
            vertical_speed_mps,
            battery_pct: Some(battery_pct),
            charging: Some(charging),
            recorded_at: Some(recorded_at),
        })
    } else {
        Ok(PartnerLocation {
            user_id: partner_id,
            paused: false,
            last_seen_at,
            lat: None,
            lon: None,
            accuracy_m: None,
            speed_mps: None,
            bearing: None,
            activity: None,
            altitude_m: None,
            vertical_speed_mps: None,
            battery_pct: None,
            charging: None,
            recorded_at: None,
        })
    }
}

pub fn load_replies_for_checkin(conn: &Connection, checkin_id: i64) -> AppResult<Vec<Reply>> {
    let mut stmt = conn.prepare(
        "SELECT id, checkin_id, author_id, text, photo_id, created_at
         FROM replies
         WHERE checkin_id = ?1
         ORDER BY created_at ASC, id ASC",
    )?;
    let rows = stmt.query_map(params![checkin_id], |row| {
        Ok(Reply {
            id: row.get(0)?,
            checkin_id: row.get(1)?,
            author_id: row.get(2)?,
            text: row.get(3)?,
            photo_id: row.get(4)?,
            created_at: row.get(5)?,
        })
    })?;
    let mut replies = Vec::new();
    for r in rows {
        replies.push(r?);
    }
    Ok(replies)
}

pub fn get_checkin_row(conn: &Connection, checkin_id: i64) -> AppResult<Option<CheckIn>> {
    let row = conn
        .query_row(
            "SELECT id, author_id, photo_id, note, lat, lon, place_name,
                    created_at, expires_at, highlighted, highlighted_by, highlighted_at
             FROM checkins WHERE id = ?1",
            params![checkin_id],
            |row| {
                let highlighted_int: i64 = row.get(9)?;
                Ok(CheckIn {
                    id: row.get(0)?,
                    author_id: row.get(1)?,
                    photo_id: row.get(2)?,
                    note: row.get(3)?,
                    lat: row.get(4)?,
                    lon: row.get(5)?,
                    place_name: row.get(6)?,
                    created_at: row.get(7)?,
                    expires_at: row.get(8)?,
                    highlighted: highlighted_int != 0,
                    highlighted_by: row.get(10)?,
                    highlighted_at: row.get(11)?,
                    replies: Vec::new(),
                })
            },
        )
        .optional()?;
    match row {
        Some(mut c) => {
            c.replies = load_replies_for_checkin(conn, c.id)?;
            Ok(Some(c))
        }
        None => Ok(None),
    }
}
