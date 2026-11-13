use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct User {
    pub id: i64,
    pub name: String,
    pub created_at: i64,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Tokens {
    pub access_token: String,
    pub access_expires_at: i64,
    pub refresh_token: String,
    pub refresh_expires_at: i64,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct LocationPoint {
    pub lat: f64,
    pub lon: f64,
    pub accuracy_m: f64,
    pub speed_mps: f64,
    pub bearing: Option<f64>,
    pub battery_pct: i64,
    pub charging: bool,
    pub recorded_at: i64,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct PartnerLocation {
    pub user_id: i64,
    pub paused: bool,
    pub last_seen_at: Option<i64>,
    pub lat: Option<f64>,
    pub lon: Option<f64>,
    pub accuracy_m: Option<f64>,
    pub speed_mps: Option<f64>,
    pub bearing: Option<f64>,
    pub battery_pct: Option<i64>,
    pub charging: Option<bool>,
    pub recorded_at: Option<i64>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Place {
    pub id: i64,
    pub name: String,
    pub lat: f64,
    pub lon: f64,
    pub radius_m: f64,
    pub created_by: i64,
    pub created_at: i64,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Reply {
    pub id: i64,
    pub checkin_id: i64,
    pub author_id: i64,
    pub text: Option<String>,
    pub photo_id: Option<String>,
    pub created_at: i64,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct CheckIn {
    pub id: i64,
    pub author_id: i64,
    pub photo_id: String,
    pub note: Option<String>,
    pub lat: Option<f64>,
    pub lon: Option<f64>,
    pub place_name: Option<String>,
    pub created_at: i64,
    pub expires_at: i64,
    pub highlighted: bool,
    pub highlighted_by: Option<i64>,
    pub highlighted_at: Option<i64>,
    pub replies: Vec<Reply>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Event {
    pub id: i64,
    #[serde(rename = "type")]
    pub event_type: String,
    pub created_at: i64,
    pub actor_id: i64,
    pub data: serde_json::Value,
}

#[derive(Deserialize)]
pub struct CreatePairRequest {
    pub name: String,
}

#[derive(Serialize)]
pub struct CreatePairResponse {
    pub user: User,
    pub tokens: Tokens,
    pub invite_code: String,
    pub invite_expires_at: i64,
}

#[derive(Serialize)]
pub struct InviteResponse {
    pub invite_code: String,
    pub invite_expires_at: i64,
}

#[derive(Deserialize)]
pub struct JoinPairRequest {
    pub code: String,
    pub name: String,
}

#[derive(Serialize)]
pub struct JoinPairResponse {
    pub user: User,
    pub tokens: Tokens,
}

#[derive(Deserialize)]
pub struct RefreshRequest {
    pub refresh_token: String,
}

#[derive(Serialize)]
pub struct MeResponse {
    pub me: User,
    pub partner: Option<User>,
    pub sharing_paused: bool,
    pub partner_paused: bool,
    pub server_time: i64,
}

#[derive(Deserialize)]
pub struct PushTokenRequest {
    pub token: String,
}

#[derive(Serialize)]
pub struct LocationsResponse {
    pub accepted: usize,
    pub paused: bool,
    pub events: Vec<Event>,
}

#[derive(Serialize)]
pub struct HistoryResponse {
    pub points: Vec<LocationPoint>,
}

#[derive(Deserialize)]
pub struct SharingRequest {
    pub paused: bool,
}

#[derive(Serialize)]
pub struct SharingResponse {
    pub paused: bool,
}

#[derive(Serialize)]
pub struct PlacesResponse {
    pub places: Vec<Place>,
}

#[derive(Deserialize)]
pub struct CreatePlaceRequest {
    pub name: String,
    pub lat: f64,
    pub lon: f64,
    pub radius_m: f64,
}

#[derive(Serialize)]
pub struct CheckInsResponse {
    pub checkins: Vec<CheckIn>,
}

#[derive(Serialize)]
pub struct MemoriesResponse {
    pub items: Vec<CheckIn>,
    pub next_before: Option<i64>,
}

#[derive(Serialize)]
pub struct EventsResponse {
    pub events: Vec<Event>,
}
