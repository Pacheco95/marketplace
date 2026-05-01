package com.marketplace.marketplace_backend.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val typeUriSuffix: String,
    val titleKey: String,
    val detailKey: String,
) {
    AUTHENTICATION_FAILED(
        HttpStatus.UNAUTHORIZED,
        "authentication-failed",
        "error.authentication-failed.title",
        "error.authentication-failed.detail",
    ),
    SESSION_EXPIRED(
        HttpStatus.UNAUTHORIZED,
        "session-expired",
        "error.session-expired.title",
        "error.session-expired.detail",
    ),
    GOOGLE_CREDENTIAL_INVALID(
        HttpStatus.UNAUTHORIZED,
        "google-credential-invalid",
        "error.google-credential-invalid.title",
        "error.google-credential-invalid.detail",
    ),
    TOKEN_EXCHANGE_FAILED(
        HttpStatus.BAD_GATEWAY,
        "token-exchange-failed",
        "error.token-exchange-failed.title",
        "error.token-exchange-failed.detail",
    ),
    ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "access-denied",
        "error.access-denied.title",
        "error.access-denied.detail",
    ),
    VALIDATION_ERROR(
        HttpStatus.BAD_REQUEST,
        "validation-error",
        "error.validation-error.title",
        "error.validation-error.detail",
    ),
    INTERNAL_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal-error",
        "error.internal-error.title",
        "error.internal-error.detail",
    ),
}
