use std::fs;
use std::path::{Path, PathBuf};

use rand::RngCore;

#[derive(Clone, Debug)]
pub struct Config {
    pub bind: String,
    pub data_dir: PathBuf,
    pub setup_key: Option<String>,
    pub google_client_id: Option<String>,
    pub google_client_secret: Option<String>,
    pub google_redirect_uri: Option<String>,
    pub oidc_issuer: Option<String>,
    pub oidc_client_ids: Vec<String>,
    pub oidc_android_client_id: Option<String>,
    pub oidc_web_client_id: Option<String>,
    pub oidc_jwks_url: Option<String>,
    pub firebase_project_id: Option<String>,
    pub token_secret: Vec<u8>,
    pub history_days: i64,
    pub stale_minutes: i64,
    pub fcm_service_account: Option<PathBuf>,
    pub web_dir: Option<PathBuf>,
}

impl Config {
    pub fn from_env() -> Result<Self, String> {
        let bind = std::env::var("OURS_BIND").unwrap_or_else(|_| "0.0.0.0:8080".to_string());
        let data_dir = PathBuf::from(
            std::env::var("OURS_DATA_DIR").unwrap_or_else(|_| "./data".to_string()),
        );
        let setup_key = std::env::var("OURS_SETUP_KEY")
            .ok()
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty());

        fs::create_dir_all(&data_dir)
            .map_err(|e| format!("failed to create data dir {}: {e}", data_dir.display()))?;
        fs::create_dir_all(data_dir.join("photos"))
            .map_err(|e| format!("failed to create photos dir: {e}"))?;

        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let _ = fs::set_permissions(&data_dir, fs::Permissions::from_mode(0o700));
            let _ = fs::set_permissions(data_dir.join("photos"), fs::Permissions::from_mode(0o700));
        }

        let token_secret = match std::env::var("OURS_TOKEN_SECRET") {
            Ok(s) if !s.trim().is_empty() => s.into_bytes(),
            _ => load_or_generate_secret(&data_dir)?,
        };

        let history_days = match std::env::var("OURS_HISTORY_DAYS") {
            Ok(v) => v
                .parse::<i64>()
                .map_err(|_| format!("invalid OURS_HISTORY_DAYS: {v}"))?,
            Err(_) => 30,
        };

        let stale_minutes = match std::env::var("OURS_STALE_MINUTES") {
            Ok(v) => v
                .parse::<i64>()
                .map_err(|_| format!("invalid OURS_STALE_MINUTES: {v}"))?,
            Err(_) => 15,
        };

        let fcm_service_account = std::env::var("OURS_FCM_SERVICE_ACCOUNT")
            .ok()
            .filter(|s| !s.trim().is_empty())
            .map(PathBuf::from);

        // OIDC (Pocket ID) identity provider. If OURS_OIDC_ISSUER is set, the
        // server verifies incoming id_tokens against this provider's JWKS and
        // issuer instead of Google's. Audience is accepted if it matches ANY of
        // OURS_OIDC_CLIENT_IDS (comma-separated) so web + native clients both work.
        let oidc_issuer = std::env::var("OURS_OIDC_ISSUER")
            .ok()
            .map(|s| s.trim().trim_end_matches('/').to_string())
            .filter(|s| !s.is_empty());
        let oidc_client_ids: Vec<String> = std::env::var("OURS_OIDC_CLIENT_IDS")
            .ok()
            .map(|s| {
                s.split(',')
                    .map(|c| c.trim().to_string())
                    .filter(|c| !c.is_empty())
                    .collect()
            })
            .unwrap_or_default();
        // Per-client audiences (also served via /api/config so clients pick their own).
        let oidc_android_client_id = std::env::var("OURS_OIDC_ANDROID_CLIENT_ID")
            .ok()
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty());
        let oidc_web_client_id = std::env::var("OURS_OIDC_WEB_CLIENT_ID")
            .ok()
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty());
        // Ensure the per-client IDs are part of the accepted-audience set.
        let mut oidc_client_ids = oidc_client_ids;
        for id in [&oidc_android_client_id, &oidc_web_client_id] {
            let Some(id) = id else { continue };
            if !oidc_client_ids.contains(id) {
                oidc_client_ids.push(id.clone());
            }
        }
        // JWKS URL defaults to <issuer>/.well-known/jwks.json when not set.
        let oidc_jwks_url = std::env::var("OURS_OIDC_JWKS_URL")
            .ok()
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
            .or_else(|| oidc_issuer.as_ref().map(|i| format!("{i}/.well-known/jwks.json")));
        // Firebase Auth (Google Sign-In) uses its own issuer + JWKS. If set, the
        // server ALSO accepts Firebase id_tokens alongside the OIDC provider.
        let firebase_project_id = std::env::var("OURS_FIREBASE_PROJECT_ID")
            .ok()
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty());

        let web_dir = std::env::var("OURS_WEB_DIR")
            .ok()
            .filter(|s| !s.trim().is_empty())
            .map(PathBuf::from)
            .or_else(|| {
                let cwd_web = PathBuf::from("./web");
                if cwd_web.is_dir() {
                    Some(cwd_web)
                } else {
                    let parent_web = PathBuf::from("../web");
                    if parent_web.is_dir() {
                        Some(parent_web)
                    } else {
                        None
                    }
                }
            });

        let config = Self {
            bind,
            data_dir,
            setup_key,
            google_client_id: std::env::var("OURS_GOOGLE_CLIENT_ID")
                .ok()
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty()),
            // Google's Web-application client requires client_secret at the token
            // endpoint even when PKCE is used, so the server holds it and performs
            // the code exchange. Never exposed to the frontend.
            google_client_secret: std::env::var("OURS_GOOGLE_CLIENT_SECRET")
                .ok()
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty()),
            google_redirect_uri: std::env::var("OURS_GOOGLE_REDIRECT_URI")
                .ok()
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty()),
            oidc_issuer,
            oidc_client_ids,
            oidc_android_client_id,
            oidc_web_client_id,
            oidc_jwks_url,
            firebase_project_id,
            token_secret,
            history_days,
            stale_minutes,
            fcm_service_account,
            web_dir,
        };

        // First-run claim race guard: a deployment reachable beyond loopback MUST
        // set OURS_SETUP_KEY, or anyone on the network can claim the owner slot
        // before the real owner does (and get the second-slot invite code).
        let bind_loopback_only = config
            .bind
            .split(':')
            .next()
            .is_some_and(|host| host == "127.0.0.1" || host == "::1" || host == "localhost");
        if !bind_loopback_only && config.setup_key.is_none() {
            return Err(
                "OURS_SETUP_KEY must be set when the server binds beyond loopback \
                 (first-run claim protection). Set a long random value and restart."
                    .to_string(),
            );
        }

        Ok(config)
    }
}

fn load_or_generate_secret(data_dir: &Path) -> Result<Vec<u8>, String> {
    let secret_path = data_dir.join("token_secret");
    if secret_path.exists() {
        let content = fs::read_to_string(&secret_path)
            .map_err(|e| format!("failed to read {}: {e}", secret_path.display()))?;
        let trimmed = content.trim();
        if !trimmed.is_empty() {
            return hex::decode(trimmed)
                .map_err(|e| format!("invalid hex in {}: {e}", secret_path.display()));
        }
    }

    let mut bytes = [0u8; 32];
    rand::rng().fill_bytes(&mut bytes);
    let encoded = hex::encode(bytes);
    fs::write(&secret_path, format!("{encoded}\n"))
        .map_err(|e| format!("failed to write {}: {e}", secret_path.display()))?;

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let _ = fs::set_permissions(&secret_path, fs::Permissions::from_mode(0o600));
    }

    Ok(bytes.to_vec())
}
