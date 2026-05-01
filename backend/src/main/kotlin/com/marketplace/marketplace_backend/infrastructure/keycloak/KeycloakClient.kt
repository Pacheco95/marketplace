package com.marketplace.marketplace_backend.infrastructure.keycloak

interface KeycloakClient {
    fun exchangeAuthCode(
        code: String,
        redirectUri: String,
    ): KeycloakTokenResponse

    fun refreshToken(refreshToken: String): KeycloakTokenResponse

    fun revokeSession(accessToken: String)

    fun exchangeGoogleToken(googleIdToken: String): KeycloakTokenResponse
}

data class KeycloakTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val refreshExpiresIn: Long,
)
