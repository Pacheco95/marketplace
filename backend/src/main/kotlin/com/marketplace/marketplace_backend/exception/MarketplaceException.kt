package com.marketplace.marketplace_backend.exception

sealed class MarketplaceException(
    val errorCode: ErrorCode,
    vararg val args: Any,
) : RuntimeException(errorCode.titleKey)

class AuthException(
    errorCode: ErrorCode = ErrorCode.AUTHENTICATION_FAILED,
    vararg args: Any,
) : MarketplaceException(errorCode, *args)

class SessionExpiredException(
    vararg args: Any,
) : MarketplaceException(ErrorCode.SESSION_EXPIRED, *args)

class GoogleCredentialInvalidException(
    vararg args: Any,
) : MarketplaceException(ErrorCode.GOOGLE_CREDENTIAL_INVALID, *args)

class TokenExchangeFailedException(
    vararg args: Any,
) : MarketplaceException(ErrorCode.TOKEN_EXCHANGE_FAILED, *args)
