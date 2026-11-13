use std::fs;
use std::path::{Path, PathBuf};

use rand::RngCore;

#[derive(Clone, Debug)]
pub struct Config {
    pub bind: String,
    pub data_dir: PathBuf,
    pub setup_key: Option<String>,
    pub token_secret: Vec<u8>,
    pub history_days: i64,
    pub stale_minutes: i64,
    pub fcm_service_account: Option<PathBuf>,
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

        Ok(Self {
            bind,
            data_dir,
            setup_key,
            token_secret,
            history_days,
            stale_minutes,
            fcm_service_account,
        })
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
