package com.marketplace.marketplace_backend.infrastructure.keycloak

import com.marketplace.marketplace_backend.exception.TokenExchangeFailedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class KeycloakClientImpl(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
    @Value("\${keycloak.client-id:marketplace-backend}")
    private val clientId: String,
    @Value("\${keycloak.client-secret:change-me-in-production}")
    private val clientSecret: String,
) : KeycloakClient {
    private val restClient = RestClient.create()
    private val tokenEndpoint get() = "$issuerUri/protocol/openid-connect/token"
    private val logoutEndpoint get() = "$issuerUri/protocol/openid-connect/logout"

    override fun exchangeAuthCode(
        code: String,
        redirectUri: String,
    ): KeycloakTokenResponse {
        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "authorization_code")
        params.add("client_id", clientId)
        params.add("client_secret", clientSecret)
        params.add("code", code)
        params.add("redirect_uri", redirectUri)
        return postToTokenEndpoint(params)
    }

    override fun refreshToken(refreshToken: String): KeycloakTokenResponse {
        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "refresh_token")
        params.add("client_id", clientId)
        params.add("client_secret", clientSecret)
        params.add("refresh_token", refreshToken)
        return postToTokenEndpoint(params)
    }

    override fun revokeSession(accessToken: String) {
        try {
            val params = LinkedMultiValueMap<String, String>()
            params.add("client_id", clientId)
            params.add("client_secret", clientSecret)
            params.add("token", accessToken)
            restClient
                .post()
                .uri(logoutEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            // Best-effort logout — log but don't throw
        }
    }

    override fun exchangeGoogleToken(googleIdToken: String): KeycloakTokenResponse {
        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
        params.add("client_id", clientId)
        params.add("client_secret", clientSecret)
        params.add("subject_token", googleIdToken)
        params.add("subject_issuer", "google")
        params.add("subject_token_type", "urn:ietf:params:oauth:token-type:id_token")
        params.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
        return postToTokenEndpoint(params)
    }

    private fun postToTokenEndpoint(params: LinkedMultiValueMap<String, String>): KeycloakTokenResponse {
        try {
            val raw =
                restClient
                    .post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(Map::class.java) ?: throw TokenExchangeFailedException()

            return KeycloakTokenResponse(
                accessToken = raw["access_token"] as? String ?: throw TokenExchangeFailedException(),
                refreshToken = raw["refresh_token"] as? String ?: throw TokenExchangeFailedException(),
                expiresIn = (raw["expires_in"] as? Number)?.toLong() ?: 900L,
                refreshExpiresIn = (raw["refresh_expires_in"] as? Number)?.toLong() ?: 2592000L,
            )
        } catch (ex: RestClientResponseException) {
            throw TokenExchangeFailedException()
        }
    }
}
