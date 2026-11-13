use axum::{
    Json,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde::Serialize;

#[derive(Debug)]
pub struct AppError {
    pub status: StatusCode,
    pub code: &'static str,
    pub message: String,
}

#[derive(Serialize)]
struct ErrorBody<'a> {
    error: &'a str,
    message: &'a str,
}

impl AppError {
    pub fn bad_request(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::BAD_REQUEST,
            code: "bad_request",
            message: msg.into(),
        }
    }

    pub fn unauthorized(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::UNAUTHORIZED,
            code: "unauthorized",
            message: msg.into(),
        }
    }

    pub fn invalid_refresh_token(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::UNAUTHORIZED,
            code: "invalid_refresh_token",
            message: msg.into(),
        }
    }

    pub fn forbidden(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::FORBIDDEN,
            code: "forbidden",
            message: msg.into(),
        }
    }

    pub fn not_found(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::NOT_FOUND,
            code: "not_found",
            message: msg.into(),
        }
    }

    pub fn invite_not_found() -> Self {
        Self {
            status: StatusCode::NOT_FOUND,
            code: "invite_not_found",
            message: "invite code is unknown, expired, or already used".into(),
        }
    }

    pub fn already_set_up() -> Self {
        Self {
            status: StatusCode::CONFLICT,
            code: "already_set_up",
            message: "server is already set up".into(),
        }
    }

    pub fn pair_full() -> Self {
        Self {
            status: StatusCode::CONFLICT,
            code: "pair_full",
            message: "both user slots are already taken".into(),
        }
    }

    pub fn already_paired() -> Self {
        Self {
            status: StatusCode::CONFLICT,
            code: "already_paired",
            message: "partner has already joined".into(),
        }
    }

    pub fn no_partner() -> Self {
        Self {
            status: StatusCode::CONFLICT,
            code: "no_partner",
            message: "partner has not joined yet".into(),
        }
    }

    pub fn expired(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::GONE,
            code: "expired",
            message: msg.into(),
        }
    }

    pub fn too_large(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::PAYLOAD_TOO_LARGE,
            code: "too_large",
            message: msg.into(),
        }
    }

    pub fn rate_limited(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::TOO_MANY_REQUESTS,
            code: "rate_limited",
            message: msg.into(),
        }
    }

    pub fn internal(msg: impl Into<String>) -> Self {
        Self {
            status: StatusCode::INTERNAL_SERVER_ERROR,
            code: "internal",
            message: msg.into(),
        }
    }
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let body = ErrorBody {
            error: self.code,
            message: &self.message,
        };
        (self.status, Json(body)).into_response()
    }
}

impl From<rusqlite::Error> for AppError {
    fn from(err: rusqlite::Error) -> Self {
        tracing::error!(error = %err, "sqlite error");
        Self::internal("database error")
    }
}

impl From<tokio::task::JoinError> for AppError {
    fn from(err: tokio::task::JoinError) -> Self {
        tracing::error!(error = %err, "task join error");
        Self::internal("internal task error")
    }
}

pub type AppResult<T> = Result<T, AppError>;
