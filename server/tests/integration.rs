use std::io::Cursor;
use std::time::Duration;

use futures_util::{SinkExt, StreamExt};
use image::{ImageBuffer, ImageFormat, Rgb};
use ours_server::build_router;
use ours_server::config::Config;
use ours_server::db::{now_ms, open_db};
use ours_server::fcm::FcmClient;
use ours_server::jobs::run_hourly_cleanup;
use ours_server::state::AppState;
use reqwest::StatusCode;
use serde_json::{Value, json};
use tempfile::TempDir;
use tokio::net::TcpListener;
use tokio_tungstenite::tungstenite::Message as WsMessage;

struct TestApp {
    base_url: String,
    ws_base_url: String,
    state: AppState,
    _tmp: TempDir,
    client: reqwest::Client,
}

impl TestApp {
    async fn start() -> Self {
        Self::start_with_setup_key(Some("test-setup-key")).await
    }

    async fn start_with_setup_key(setup_key: Option<&str>) -> Self {
        let tmp = TempDir::new().expect("tempdir");
        let data_dir = tmp.path().to_path_buf();
        std::fs::create_dir_all(data_dir.join("photos")).unwrap();

        let config = Config {
            bind: "127.0.0.1:0".to_string(),
            data_dir: data_dir.clone(),
            setup_key: setup_key.map(str::to_string),
            token_secret: b"0123456789abcdef0123456789abcdef".to_vec(),
            history_days: 30,
            stale_minutes: 15,
            fcm_service_account: None,
        };

        let db = open_db(&data_dir.join("ours.db")).expect("open_db");
        let state = AppState::new(config, db, FcmClient::disabled());
        let app = build_router(state.clone());

        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();

        tokio::spawn(async move {
            axum::serve(listener, app).await.unwrap();
        });

        Self {
            base_url: format!("http://{addr}"),
            ws_base_url: format!("ws://{addr}"),
            state,
            _tmp: tmp,
            client: reqwest::Client::new(),
        }
    }

    async fn pair_both(&self) -> (String, String, i64, i64) {
        let r1: Value = self
            .client
            .post(format!("{}/api/pair/create", self.base_url))
            .header("X-Setup-Key", "test-setup-key")
            .json(&json!({"name": "Dewantoro"}))
            .send()
            .await
            .unwrap()
            .json()
            .await
            .unwrap();

        let t1 = r1["tokens"]["access_token"].as_str().unwrap().to_string();
        let id1 = r1["user"]["id"].as_i64().unwrap();
        let code = r1["invite_code"].as_str().unwrap().to_string();

        let r2: Value = self
            .client
            .post(format!("{}/api/pair/join", self.base_url))
            .json(&json!({"code": code, "name": "Marchanda"}))
            .send()
            .await
            .unwrap()
            .json()
            .await
            .unwrap();

        let t2 = r2["tokens"]["access_token"].as_str().unwrap().to_string();
        let id2 = r2["user"]["id"].as_i64().unwrap();
        (t1, t2, id1, id2)
    }
}

fn make_test_jpeg(w: u32, h: u32) -> Vec<u8> {
    let img: ImageBuffer<Rgb<u8>, Vec<u8>> = ImageBuffer::from_fn(w, h, |x, y| {
        Rgb([(x % 255) as u8, (y % 255) as u8, 128])
    });
    let mut buf = Cursor::new(Vec::new());
    image::DynamicImage::ImageRgb8(img)
        .write_to(&mut buf, ImageFormat::Jpeg)
        .unwrap();
    buf.into_inner()
}

#[tokio::test]
async fn pairing_and_refresh_flow() {
    let app = TestApp::start().await;

    let health = app
        .client
        .get(format!("{}/health", app.base_url))
        .send()
        .await
        .unwrap();
    assert_eq!(health.status(), StatusCode::OK);

    // Wrong setup key -> 403
    let bad = app
        .client
        .post(format!("{}/api/pair/create", app.base_url))
        .header("X-Setup-Key", "wrong")
        .json(&json!({"name": "Dewantoro"}))
        .send()
        .await
        .unwrap();
    assert_eq!(bad.status(), StatusCode::FORBIDDEN);

    // Create first user with Gmail email
    let res = app
        .client
        .post(format!("{}/api/pair/create", app.base_url))
        .header("X-Setup-Key", "test-setup-key")
        .json(&json!({"name": "Dewantoro", "email": "Dewantoro@gmail.com"}))
        .send()
        .await
        .unwrap();
    assert_eq!(res.status(), StatusCode::CREATED);
    let body1: Value = res.json().await.unwrap();
    assert_eq!(body1["user"]["email"], "dewantoro@gmail.com");
    let access1 = body1["tokens"]["access_token"].as_str().unwrap().to_string();
    let refresh1 = body1["tokens"]["refresh_token"].as_str().unwrap().to_string();
    let old_code = body1["invite_code"].as_str().unwrap().to_string();
    assert_eq!(old_code.len(), 6);

    // Second create -> 409 already_set_up
    let dup = app
        .client
        .post(format!("{}/api/pair/create", app.base_url))
        .header("X-Setup-Key", "test-setup-key")
        .json(&json!({"name": "Other"}))
        .send()
        .await
        .unwrap();
    assert_eq!(dup.status(), StatusCode::CONFLICT);

    // Reissue invite -> invalidates old code
    let inv_res = app
        .client
        .get(format!("{}/api/pair/invite", app.base_url))
        .bearer_auth(&access1)
        .send()
        .await
        .unwrap();
    assert_eq!(inv_res.status(), StatusCode::OK);
    let inv_body: Value = inv_res.json().await.unwrap();
    let new_code = inv_body["invite_code"].as_str().unwrap().to_string();

    if new_code != old_code {
        let old_join = app
            .client
            .post(format!("{}/api/pair/join", app.base_url))
            .json(&json!({"code": old_code, "name": "Marchanda"}))
            .send()
            .await
            .unwrap();
        assert_eq!(old_join.status(), StatusCode::NOT_FOUND);
    }

    // Join with lowercase + spaces and Gmail email
    let join_res = app
        .client
        .post(format!("{}/api/pair/join", app.base_url))
        .json(&json!({
            "code": format!("  {} ", new_code.to_lowercase()),
            "name": "Marchanda",
            "email": "marchanda@gmail.com"
        }))
        .send()
        .await
        .unwrap();
    assert_eq!(join_res.status(), StatusCode::CREATED);
    let join_body: Value = join_res.json().await.unwrap();
    assert_eq!(join_body["user"]["email"], "marchanda@gmail.com");

    // Third user -> 409 pair_full
    let third = app
        .client
        .post(format!("{}/api/pair/join", app.base_url))
        .json(&json!({"code": new_code, "name": "Intruder"}))
        .send()
        .await
        .unwrap();
    assert_eq!(third.status(), StatusCode::CONFLICT);

    // GET /api/me
    let me: Value = app
        .client
        .get(format!("{}/api/me", app.base_url))
        .bearer_auth(&access1)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(me["me"]["name"], "Dewantoro");
    assert_eq!(me["me"]["email"], "dewantoro@gmail.com");
    assert_eq!(me["partner"]["name"], "Marchanda");
    assert_eq!(me["partner"]["email"], "marchanda@gmail.com");

    // Refresh token rotation
    let ref_res = app
        .client
        .post(format!("{}/api/auth/refresh", app.base_url))
        .json(&json!({"refresh_token": refresh1}))
        .send()
        .await
        .unwrap();
    assert_eq!(ref_res.status(), StatusCode::OK);
    let new_tokens: Value = ref_res.json().await.unwrap();
    assert_ne!(new_tokens["refresh_token"].as_str().unwrap(), refresh1);

    // Reusing old refresh token -> 401 invalid_refresh_token
    let reuse = app
        .client
        .post(format!("{}/api/auth/refresh", app.base_url))
        .json(&json!({"refresh_token": refresh1}))
        .send()
        .await
        .unwrap();
    assert_eq!(reuse.status(), StatusCode::UNAUTHORIZED);
    let reuse_body: Value = reuse.json().await.unwrap();
    assert_eq!(reuse_body["error"], "invalid_refresh_token");
}

#[tokio::test]
async fn locations_piggyback_and_pause() {
    let app = TestApp::start().await;
    let (t1, t2, _id1, _id2) = app.pair_both().await;
    let now = now_ms();

    // User 2 sends nudge -> creates event for User 1
    let nudge = app
        .client
        .post(format!("{}/api/nudge", app.base_url))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap();
    assert_eq!(nudge.status(), StatusCode::NO_CONTENT);

    // User 1 posts locations with after_event_id=0 -> receives nudge in piggyback events
    let loc_res = app
        .client
        .post(format!("{}/api/locations?after_event_id=0", app.base_url))
        .bearer_auth(&t1)
        .json(&json!([
            {
                "lat": -6.2000, "lon": 106.8000,
                "accuracy_m": 10.0, "speed_mps": 1.2, "bearing": 90.0,
                "battery_pct": 85, "charging": false, "recorded_at": now - 5000
            },
            {
                "lat": -6.2010, "lon": 106.8010,
                "accuracy_m": 8.0, "speed_mps": 2.5, "bearing": 180.0,
                "battery_pct": 84, "charging": false, "recorded_at": now
            }
        ]))
        .send()
        .await
        .unwrap();
    assert_eq!(loc_res.status(), StatusCode::OK);
    let loc_body: Value = loc_res.json().await.unwrap();
    assert_eq!(loc_body["accepted"], 2);
    assert_eq!(loc_body["paused"], false);
    assert_eq!(loc_body["events"].as_array().unwrap().len(), 1);
    assert_eq!(loc_body["events"][0]["type"], "nudge");

    // User 2 reads User 1's latest location and history
    let partner_loc: Value = app
        .client
        .get(format!("{}/api/partner/location", app.base_url))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(partner_loc["paused"], false);
    assert_eq!(partner_loc["lat"], -6.2010);

    let hist: Value = app
        .client
        .get(format!(
            "{}/api/partner/history?since={}",
            app.base_url,
            now - 10_000
        ))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(hist["points"].as_array().unwrap().len(), 2);

    // User 1 pauses sharing
    let pause = app
        .client
        .post(format!("{}/api/sharing", app.base_url))
        .bearer_auth(&t1)
        .json(&json!({"paused": true}))
        .send()
        .await
        .unwrap();
    assert_eq!(pause.status(), StatusCode::OK);

    // Partner sees paused=true and null coords, empty history
    let partner_loc_paused: Value = app
        .client
        .get(format!("{}/api/partner/location", app.base_url))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(partner_loc_paused["paused"], true);
    assert!(partner_loc_paused["lat"].is_null());

    let hist_paused: Value = app
        .client
        .get(format!(
            "{}/api/partner/history?since={}",
            app.base_url,
            now - 10_000
        ))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(hist_paused["points"].as_array().unwrap().len(), 0);

    // Uploading locations while paused drops them
    let paused_upload: Value = app
        .client
        .post(format!("{}/api/locations", app.base_url))
        .bearer_auth(&t1)
        .json(&json!([{
            "lat": -6.2, "lon": 106.8, "accuracy_m": 5.0, "speed_mps": 0.0,
            "bearing": null, "battery_pct": 80, "charging": false, "recorded_at": now + 1000
        }]))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(paused_upload["accepted"], 0);
    assert_eq!(paused_upload["paused"], true);
}

#[tokio::test]
async fn places_enter_exit_hysteresis() {
    let app = TestApp::start().await;
    let (t1, t2, _id1, _id2) = app.pair_both().await;
    let now = now_ms();

    // Create place at (-6.2, 106.8) with radius 100m
    let p_res = app
        .client
        .post(format!("{}/api/places", app.base_url))
        .bearer_auth(&t1)
        .json(&json!({"name": "Home", "lat": -6.2000, "lon": 106.8000, "radius_m": 100.0}))
        .send()
        .await
        .unwrap();
    assert_eq!(p_res.status(), StatusCode::CREATED);

    // 1st point outside (approx 500m north): initializes state, no event
    // 2nd point inside (center): emits enter
    // 3rd point at ~120m north (inside radius+50m=150m hysteresis): NO exit event
    // 4th point at ~200m north (beyond 150m): emits exit
    app.client
        .post(format!("{}/api/locations", app.base_url))
        .bearer_auth(&t2)
        .json(&json!([
            {"lat": -6.1955, "lon": 106.8000, "accuracy_m": 10.0, "speed_mps": 1.0, "bearing": null, "battery_pct": 90, "charging": false, "recorded_at": now - 4000},
            {"lat": -6.2000, "lon": 106.8000, "accuracy_m": 10.0, "speed_mps": 1.0, "bearing": null, "battery_pct": 90, "charging": false, "recorded_at": now - 3000},
            {"lat": -6.1989, "lon": 106.8000, "accuracy_m": 10.0, "speed_mps": 1.0, "bearing": null, "battery_pct": 90, "charging": false, "recorded_at": now - 2000},
            {"lat": -6.1982, "lon": 106.8000, "accuracy_m": 10.0, "speed_mps": 1.0, "bearing": null, "battery_pct": 90, "charging": false, "recorded_at": now - 1000}
        ]))
        .send()
        .await
        .unwrap();

    let events: Value = app
        .client
        .get(format!("{}/api/events?after=0", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let list = events["events"].as_array().unwrap();
    assert_eq!(list.len(), 2);
    assert_eq!(list[0]["type"], "place");
    assert_eq!(list[0]["data"]["transition"], "enter");
    assert_eq!(list[0]["data"]["place_name"], "Home");
    assert_eq!(list[1]["type"], "place");
    assert_eq!(list[1]["data"]["transition"], "exit");
}

#[tokio::test]
async fn checkins_replies_highlights_expiry_and_photo_auth() {
    let app = TestApp::start().await;
    let (t1, t2, _id1, _id2) = app.pair_both().await;
    let jpeg = make_test_jpeg(800, 600);

    // Upload check-in 1 (will be highlighted)
    let form1 = reqwest::multipart::Form::new()
        .part(
            "photo",
            reqwest::multipart::Part::bytes(jpeg.clone())
                .file_name("p1.jpg")
                .mime_str("image/jpeg")
                .unwrap(),
        )
        .text("note", "coffee")
        .text("lat", "-6.2")
        .text("lon", "106.8")
        .text("place_name", "Cafe");

    let c1: Value = app
        .client
        .post(format!("{}/api/checkins", app.base_url))
        .bearer_auth(&t1)
        .multipart(form1)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let c1_id = c1["id"].as_i64().unwrap();
    let c1_photo = c1["photo_id"].as_str().unwrap().to_string();

    // Photo requires auth
    let unauth_photo = app
        .client
        .get(format!("{}/api/photos/{c1_photo}?size=thumb", app.base_url))
        .send()
        .await
        .unwrap();
    assert_eq!(unauth_photo.status(), StatusCode::UNAUTHORIZED);

    let auth_thumb = app
        .client
        .get(format!("{}/api/photos/{c1_photo}?size=thumb", app.base_url))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap();
    assert_eq!(auth_thumb.status(), StatusCode::OK);
    let thumb_bytes = auth_thumb.bytes().await.unwrap();
    let thumb_img = image::load_from_memory(&thumb_bytes).unwrap();
    assert!(thumb_img.width() <= 400 && thumb_img.height() <= 400);

    // User 2 replies with text and photo
    let reply_form = reqwest::multipart::Form::new()
        .text("text", "looks good!")
        .part(
            "photo",
            reqwest::multipart::Part::bytes(jpeg.clone())
                .file_name("r.jpg")
                .mime_str("image/jpeg")
                .unwrap(),
        );
    let reply_res = app
        .client
        .post(format!("{}/api/checkins/{c1_id}/replies", app.base_url))
        .bearer_auth(&t2)
        .multipart(reply_form)
        .send()
        .await
        .unwrap();
    assert_eq!(reply_res.status(), StatusCode::CREATED);

    // Highlight check-in 1
    let hl = app
        .client
        .post(format!("{}/api/checkins/{c1_id}/highlight", app.base_url))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap();
    assert_eq!(hl.status(), StatusCode::OK);
    let hl_body: Value = hl.json().await.unwrap();
    assert_eq!(hl_body["highlighted"], true);
    assert_eq!(hl_body["replies"].as_array().unwrap().len(), 1);

    // Upload check-in 2 (not highlighted, will expire)
    let form2 = reqwest::multipart::Form::new().part(
        "photo",
        reqwest::multipart::Part::bytes(jpeg)
            .file_name("p2.jpg")
            .mime_str("image/jpeg")
            .unwrap(),
    );
    let c2: Value = app
        .client
        .post(format!("{}/api/checkins", app.base_url))
        .bearer_auth(&t1)
        .multipart(form2)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let c2_id = c2["id"].as_i64().unwrap();
    let c2_photo = c2["photo_id"].as_str().unwrap().to_string();

    // Backdate expires_at of both check-ins in SQLite
    {
        let conn = app.state.db.lock().unwrap();
        let past = now_ms() - 1000;
        conn.execute(
            "UPDATE checkins SET expires_at = ?1",
            rusqlite::params![past],
        )
        .unwrap();
    }

    // Check-in 2 (unhighlighted) is immediately 404 on GET and 410 on highlight/reply
    let get_exp = app
        .client
        .get(format!("{}/api/checkins/{c2_id}", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap();
    assert_eq!(get_exp.status(), StatusCode::NOT_FOUND);

    let hl_exp = app
        .client
        .post(format!("{}/api/checkins/{c2_id}/highlight", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap();
    assert_eq!(hl_exp.status(), StatusCode::GONE);

    // Run hourly cleanup -> deletes c2 + its photo, keeps highlighted c1
    run_hourly_cleanup(&app.state).await;

    let c2_photo_res = app
        .client
        .get(format!("{}/api/photos/{c2_photo}?size=full", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap();
    assert_eq!(c2_photo_res.status(), StatusCode::NOT_FOUND);

    let memories: Value = app
        .client
        .get(format!("{}/api/memories?limit=10", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(memories["items"].as_array().unwrap().len(), 1);
    assert_eq!(memories["items"][0]["id"], c1_id);
}

#[tokio::test]
async fn websocket_live_events_and_ping() {
    let app = TestApp::start().await;
    let (t1, t2, _id1, _id2) = app.pair_both().await;

    // Invalid token rejected before upgrade
    let bad_ws = tokio_tungstenite::connect_async(format!("{}/api/ws?token=bad", app.ws_base_url))
        .await;
    assert!(bad_ws.is_err());

    // User 1 connects WS
    let (mut ws, _) =
        tokio_tungstenite::connect_async(format!("{}/api/ws?token={t1}", app.ws_base_url))
            .await
            .expect("ws connect");

    // Ping/pong
    ws.send(WsMessage::Text(r#"{"type":"ping"}"#.into()))
        .await
        .unwrap();
    let pong = tokio::time::timeout(Duration::from_secs(2), ws.next())
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    assert_eq!(pong.into_text().unwrap(), r#"{"type":"pong"}"#);

    // User 2 nudges -> User 1 receives nudge event live
    app.client
        .post(format!("{}/api/nudge", app.base_url))
        .bearer_auth(&t2)
        .send()
        .await
        .unwrap();

    let msg = tokio::time::timeout(Duration::from_secs(2), ws.next())
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    let ev: Value = serde_json::from_str(&msg.into_text().unwrap()).unwrap();
    assert_eq!(ev["type"], "nudge");
    assert!(ev["id"].as_i64().unwrap() > 0);

    // User 2 uploads location -> User 1 receives location event with id 0
    app.client
        .post(format!("{}/api/locations", app.base_url))
        .bearer_auth(&t2)
        .json(&json!([{
            "lat": -6.25, "lon": 106.85, "accuracy_m": 5.0, "speed_mps": 0.5,
            "bearing": null, "battery_pct": 77, "charging": true, "recorded_at": now_ms()
        }]))
        .send()
        .await
        .unwrap();

    let loc_msg = tokio::time::timeout(Duration::from_secs(2), ws.next())
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    let loc_ev: Value = serde_json::from_str(&loc_msg.into_text().unwrap()).unwrap();
    assert_eq!(loc_ev["type"], "location");
    assert_eq!(loc_ev["id"], 0);
    assert_eq!(loc_ev["data"]["lat"], -6.25);
}

#[tokio::test]
async fn chat_messages_json_multipart_and_ws() {
    let app = TestApp::start().await;
    let (t1, t2, id1, id2) = app.pair_both().await;

    // User 2 connects WS to receive live chat_message events
    let (mut ws, _) =
        tokio_tungstenite::connect_async(format!("{}/api/ws?token={t2}", app.ws_base_url))
            .await
            .expect("ws connect");

    // 1. User 1 sends plain text JSON message
    let m1_res = app
        .client
        .post(format!("{}/api/messages", app.base_url))
        .bearer_auth(&t1)
        .json(&json!({"text": "on my way!"}))
        .send()
        .await
        .unwrap();
    assert_eq!(m1_res.status(), StatusCode::CREATED);
    let m1: Value = m1_res.json().await.unwrap();
    let m1_id = m1["id"].as_i64().unwrap();
    assert_eq!(m1["author_id"], id1);
    assert_eq!(m1["text"], "on my way!");
    assert!(m1["photo_id"].is_null());

    // User 2 receives WS chat_message event
    let ws_frame = tokio::time::timeout(Duration::from_secs(2), ws.next())
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    let ws_ev: Value = serde_json::from_str(&ws_frame.into_text().unwrap()).unwrap();
    assert_eq!(ws_ev["type"], "chat_message");
    assert!(ws_ev["id"].as_i64().unwrap() > 0);
    assert_eq!(ws_ev["data"]["message"]["id"], m1_id);
    assert_eq!(ws_ev["data"]["message"]["text"], "on my way!");

    // Small delay so created_at differs cleanly for pagination
    tokio::time::sleep(Duration::from_millis(5)).await;

    // 2. User 2 sends multipart photo + quoted reply to m1
    let jpeg = make_test_jpeg(600, 400);
    let form = reqwest::multipart::Form::new()
        .text("text", "see you soon")
        .text("reply_to_message_id", m1_id.to_string())
        .part(
            "photo",
            reqwest::multipart::Part::bytes(jpeg)
                .file_name("msg.jpg")
                .mime_str("image/jpeg")
                .unwrap(),
        );
    let m2_res = app
        .client
        .post(format!("{}/api/messages", app.base_url))
        .bearer_auth(&t2)
        .multipart(form)
        .send()
        .await
        .unwrap();
    assert_eq!(m2_res.status(), StatusCode::CREATED);
    let m2: Value = m2_res.json().await.unwrap();
    assert_eq!(m2["author_id"], id2);
    assert_eq!(m2["reply_to_message_id"], m1_id);
    let m2_photo = m2["photo_id"].as_str().unwrap();

    // Photo thumbnail is reachable with auth
    let thumb_res = app
        .client
        .get(format!("{}/api/photos/{m2_photo}?size=thumb", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap();
    assert_eq!(thumb_res.status(), StatusCode::OK);

    // 3. GET /api/messages pagination (newest first)
    let page1: Value = app
        .client
        .get(format!("{}/api/messages?limit=1", app.base_url))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(page1["items"].as_array().unwrap().len(), 1);
    assert_eq!(page1["items"][0]["id"], m2["id"]);
    let next_before = page1["next_before"].as_i64().unwrap();

    let page2: Value = app
        .client
        .get(format!(
            "{}/api/messages?before={next_before}&limit=10",
            app.base_url
        ))
        .bearer_auth(&t1)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert_eq!(page2["items"].as_array().unwrap().len(), 1);
    assert_eq!(page2["items"][0]["id"], m1_id);
    assert!(page2["next_before"].is_null());
}

#[tokio::test]
async fn pairing_without_setup_key_when_unset() {
    let app = TestApp::start_with_setup_key(None).await;

    // Create first user without X-Setup-Key header
    let res = app
        .client
        .post(format!("{}/api/pair/create", app.base_url))
        .json(&json!({"name": "Dewantoro", "email": "dewantoro@gmail.com"}))
        .send()
        .await
        .unwrap();
    assert_eq!(res.status(), StatusCode::CREATED);
    let body1: Value = res.json().await.unwrap();
    let code = body1["invite_code"].as_str().unwrap().to_string();

    // Second create still refused (409 already_set_up)
    let dup = app
        .client
        .post(format!("{}/api/pair/create", app.base_url))
        .json(&json!({"name": "Other"}))
        .send()
        .await
        .unwrap();
    assert_eq!(dup.status(), StatusCode::CONFLICT);

    // Join works and caps at 2 users
    let join = app
        .client
        .post(format!("{}/api/pair/join", app.base_url))
        .json(&json!({"code": code, "name": "Marchanda", "email": "marchanda@gmail.com"}))
        .send()
        .await
        .unwrap();
    assert_eq!(join.status(), StatusCode::CREATED);

    let third = app
        .client
        .post(format!("{}/api/pair/join", app.base_url))
        .json(&json!({"code": code, "name": "Intruder"}))
        .send()
        .await
        .unwrap();
    assert_eq!(third.status(), StatusCode::CONFLICT);
}
