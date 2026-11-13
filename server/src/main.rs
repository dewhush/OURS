use std::process::ExitCode;

use tokio::net::TcpListener;
use tracing_subscriber::EnvFilter;

use ours_server::build_router;
use ours_server::config::Config;
use ours_server::db::open_db;
use ours_server::fcm::FcmClient;
use ours_server::jobs::spawn_background_jobs;
use ours_server::state::AppState;

#[tokio::main]
async fn main() -> ExitCode {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let config = match Config::from_env() {
        Ok(c) => c,
        Err(err) => {
            tracing::error!("{err}");
            return ExitCode::FAILURE;
        }
    };

    let db_path = config.data_dir.join("ours.db");
    let db = match open_db(&db_path) {
        Ok(d) => d,
        Err(err) => {
            tracing::error!("{err}");
            return ExitCode::FAILURE;
        }
    };

    let fcm = match FcmClient::from_path(config.fcm_service_account.as_deref()) {
        Ok(f) => f,
        Err(err) => {
            tracing::error!("{err}");
            return ExitCode::FAILURE;
        }
    };

    let bind = config.bind.clone();
    let state = AppState::new(config, db, fcm);
    spawn_background_jobs(state.clone());

    let listener = match TcpListener::bind(&bind).await {
        Ok(l) => l,
        Err(err) => {
            tracing::error!(bind = %bind, error = %err, "failed to bind");
            return ExitCode::FAILURE;
        }
    };

    tracing::info!(bind = %bind, "ours-server listening");
    let app = build_router(state);

    if let Err(err) = axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal())
        .await
    {
        tracing::error!(error = %err, "server error");
        return ExitCode::FAILURE;
    }

    ExitCode::SUCCESS
}

async fn shutdown_signal() {
    let ctrl_c = async {
        let _ = tokio::signal::ctrl_c().await;
    };

    #[cfg(unix)]
    let terminate = async {
        if let Ok(mut sig) =
            tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
        {
            sig.recv().await;
        }
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
    tracing::info!("shutting down");
}
