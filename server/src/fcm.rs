use std::collections::HashMap;
use std::path::Path;
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};

use jsonwebtoken::{Algorithm, EncodingKey, Header};
use serde::{Deserialize, Serialize};
use tokio::sync::Mutex;

#[derive(Deserialize)]
struct ServiceAccount {
    project_id: String,
    client_email: String,
    private_key: String,
}

#[derive(Serialize)]
struct JwtClaims<'a> {
    iss: &'a str,
    scope: &'static str,
    aud: &'static str,
    iat: i64,
    exp: i64,
}

#[derive(Deserialize)]
struct TokenResponse {
    access_token: String,
    expires_in: i64,
}

struct CachedToken {
    token: String,
    expires_at_sec: i64,
}

struct FcmInner {
    project_id: String,
    client_email: String,
    encoding_key: EncodingKey,
    http: reqwest::Client,
    cached: Mutex<Option<CachedToken>>,
}

#[derive(Clone)]
pub struct FcmClient {
    inner: Option<Arc<FcmInner>>,
}

impl FcmClient {
    pub fn disabled() -> Self {
        Self { inner: None }
    }

    pub fn from_path(path: Option<&Path>) -> Result<Self, String> {
        let Some(path) = path else {
            tracing::info!("FCM disabled (OURS_FCM_SERVICE_ACCOUNT not set)");
            return Ok(Self::disabled());
        };
        let raw = std::fs::read_to_string(path)
            .map_err(|e| format!("failed to read FCM service account {}: {e}", path.display()))?;
        let sa: ServiceAccount = serde_json::from_str(&raw)
            .map_err(|e| format!("invalid FCM service account JSON: {e}"))?;
        let encoding_key = EncodingKey::from_rsa_pem(sa.private_key.as_bytes())
            .map_err(|e| format!("invalid FCM private_key PEM: {e}"))?;
        let http = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(10))
            .build()
            .map_err(|e| format!("failed to build reqwest client: {e}"))?;

        tracing::info!(project_id = %sa.project_id, "FCM enabled");
        Ok(Self {
            inner: Some(Arc::new(FcmInner {
                project_id: sa.project_id,
                client_email: sa.client_email,
                encoding_key,
                http,
                cached: Mutex::new(None),
            })),
        })
    }

    pub fn is_enabled(&self) -> bool {
        self.inner.is_some()
    }

    pub fn send_async(&self, fcm_token: Option<String>, data: HashMap<String, String>) {
        let Some(inner) = self.inner.clone() else {
            return;
        };
        let Some(token) = fcm_token else {
            return;
        };
        tokio::spawn(async move {
            if let Err(err) = send_message(&inner, &token, &data).await {
                tracing::warn!(error = %err, "FCM send failed");
            }
        });
    }

    pub async fn send_blocking(&self, fcm_token: &str, data: &HashMap<String, String>) -> Result<(), String> {
        let Some(inner) = self.inner.as_ref() else {
            return Ok(());
        };
        send_message(inner, fcm_token, data).await
    }
}

async fn get_oauth_token(inner: &FcmInner) -> Result<String, String> {
    let now_sec = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs() as i64;

    let mut guard = inner.cached.lock().await;
    if let Some(ref c) = *guard
        && c.expires_at_sec > now_sec + 60
    {
        return Ok(c.token.clone());
    }

    let claims = JwtClaims {
        iss: &inner.client_email,
        scope: "https://www.googleapis.com/auth/firebase.messaging",
        aud: "https://oauth2.googleapis.com/token",
        iat: now_sec,
        exp: now_sec + 3600,
    };
    let jwt = jsonwebtoken::encode(&Header::new(Algorithm::RS256), &claims, &inner.encoding_key)
        .map_err(|e| format!("jwt encode error: {e}"))?;

    let resp = inner
        .http
        .post("https://oauth2.googleapis.com/token")
        .form(&[
            ("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"),
            ("assertion", jwt.as_str()),
        ])
        .send()
        .await
        .map_err(|e| format!("oauth request error: {e}"))?;

    if !resp.status().is_success() {
        let status = resp.status();
        let text = resp.text().await.unwrap_or_default();
        return Err(format!("oauth error {status}: {text}"));
    }

    let parsed: TokenResponse = resp
        .json()
        .await
        .map_err(|e| format!("oauth json error: {e}"))?;
    let expires_at_sec = now_sec + parsed.expires_in;
    *guard = Some(CachedToken {
        token: parsed.access_token.clone(),
        expires_at_sec,
    });
    Ok(parsed.access_token)
}

async fn send_message(
    inner: &FcmInner,
    device_token: &str,
    data: &HashMap<String, String>,
) -> Result<(), String> {
    let access_token = get_oauth_token(inner).await?;
    let url = format!(
        "https://fcm.googleapis.com/v1/projects/{}/messages:send",
        inner.project_id
    );
    let body = serde_json::json!({
        "message": {
            "token": device_token,
            "data": data,
            "android": {
                "priority": "high"
            }
        }
    });

    let resp = inner
        .http
        .post(&url)
        .bearer_auth(&access_token)
        .json(&body)
        .send()
        .await
        .map_err(|e| format!("fcm request error: {e}"))?;

    if !resp.status().is_success() {
        let status = resp.status();
        let text = resp.text().await.unwrap_or_default();
        return Err(format!("fcm error {status}: {text}"));
    }
    Ok(())
}
