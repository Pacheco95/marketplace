package com.marketplace.marketplace_backend.service

import com.marketplace.marketplace_backend.exception.AuthException
import com.marketplace.marketplace_backend.exception.GoogleCredentialInvalidException
import com.marketplace.marketplace_backend.exception.SessionExpiredException
import com.marketplace.marketplace_backend.exception.TokenExchangeFailedException
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakClient
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakTokenResponse
import com.marketplace.marketplace_backend.web.dto.response.UserResponseDto
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

@Service
open class AuthServiceImpl(
    private val keycloakClient: KeycloakClient,
    private val userService: UserService,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
    @Value("\${keycloak.client-id:marketplace-backend}")
    private val clientId: String,
    @Value("\${app.frontend.base-url:http://localhost:3000}")
    private val frontendBaseUrl: String,
    @Value("\${app.backend.base-url:http://localhost:8080}")
    private val backendBaseUrl: String,
    @Value("\${app.cookie.secure:true}")
    private val cookieSecure: Boolean,
    @Value("\${google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}")
    private val googleJwksUri: String,
) : AuthService {
    private val callbackUri get() = "$backendBaseUrl/api/v1/auth/callback"

    override fun buildAuthorizationRedirectUrl(state: String): String =
        "$issuerUri/protocol/openid-connect/auth" +
            "?response_type=code" +
            "&client_id=$clientId" +
            "&redirect_uri=${java.net.URLEncoder.encode(callbackUri, "UTF-8")}" +
            "&scope=openid+email+profile" +
            "&state=$state"

    override fun handleCallback(
        code: String,
        state: String,
        stateCookie: String?,
        response: HttpServletResponse,
    ): String {
        if (stateCookie == null || stateCookie != state) {
            throw AuthException()
        }

        val tokens =
            try {
                keycloakClient.exchangeAuthCode(code, callbackUri)
            } catch (ex: TokenExchangeFailedException) {
                throw AuthException()
            }

        val claims = decodeJwtClaims(tokens.accessToken)
        userService.upsertFromTokenClaims(claims)
        setAuthCookies(response, tokens)

        return "$frontendBaseUrl/profile"
    }

    override fun handleOneTap(
        credential: String,
        response: HttpServletResponse,
    ): UserResponseDto {
        validateGoogleCredential(credential)

        val tokens =
            try {
                keycloakClient.exchangeGoogleToken(credential)
            } catch (ex: Exception) {
                throw TokenExchangeFailedException()
            }

        val claims = decodeJwtClaims(tokens.accessToken)
        val user = userService.upsertFromTokenClaims(claims)
        setAuthCookies(response, tokens)
        return UserResponseDto.from(user)
    }

    override fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val refreshToken =
            request.cookies
                ?.firstOrNull { it.name == "marketplace_refresh_token" }
                ?.value
                ?: throw SessionExpiredException()

        val tokens =
            try {
                keycloakClient.refreshToken(refreshToken)
            } catch (ex: Exception) {
                clearAuthCookies(response)
                throw SessionExpiredException()
            }

        setAuthCookies(response, tokens)
    }

    override fun logout(
        accessToken: String?,
        response: HttpServletResponse,
    ) {
        if (accessToken != null) {
            keycloakClient.revokeSession(accessToken)
        }
        clearAuthCookies(response)
    }

    private fun validateGoogleCredential(credential: String) {
        try {
            val parts = credential.split(".")
            if (parts.size != 3) throw GoogleCredentialInvalidException()

            val payloadJson = String(Base64.getUrlDecoder().decode(addPadding(parts[1])))
            val expMatch = Regex("\"exp\"\\s*:\\s*(\\d+)").find(payloadJson)
            val exp = expMatch?.groupValues?.get(1)?.toLong()
            if (exp != null && exp < System.currentTimeMillis() / 1000) {
                throw GoogleCredentialInvalidException()
            }

            val audMatch = Regex("\"aud\"\\s*:\\s*\"([^\"]+)\"").find(payloadJson)
            if (audMatch == null) throw GoogleCredentialInvalidException()
        } catch (ex: GoogleCredentialInvalidException) {
            throw ex
        } catch (ex: Exception) {
            throw GoogleCredentialInvalidException()
        }
    }

    private fun addPadding(base64: String): String {
        val pad = (4 - base64.length % 4) % 4
        return base64 + "=".repeat(pad)
    }

    private fun setAuthCookies(
        response: HttpServletResponse,
        tokens: KeycloakTokenResponse,
    ) {
        response.addCookie(buildCookie("marketplace_access_token", tokens.accessToken, 900))
        response.addCookie(buildCookie("marketplace_refresh_token", tokens.refreshToken, 2592000))
    }

    private fun clearAuthCookies(response: HttpServletResponse) {
        response.addCookie(buildCookie("marketplace_access_token", "", 0))
        response.addCookie(buildCookie("marketplace_refresh_token", "", 0))
    }

    private fun buildCookie(
        name: String,
        value: String,
        maxAge: Int,
    ): Cookie {
        val cookie = Cookie(name, value)
        cookie.isHttpOnly = true
        cookie.secure = cookieSecure
        cookie.path = "/"
        cookie.maxAge = maxAge
        cookie.setAttribute("SameSite", "Lax")
        return cookie
    }

    private fun decodeJwtClaims(token: String): Map<String, Any> {
        val parts = token.split(".")
        if (parts.size != 3) return emptyMap()
        return try {
            val payloadJson = String(Base64.getUrlDecoder().decode(addPadding(parts[1])))
            parseSimpleJson(payloadJson)
        } catch (ex: Exception) {
            emptyMap()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSimpleJson(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val stringPattern = Regex("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"")
        val numberPattern = Regex("\"(\\w+)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)")
        stringPattern.findAll(json).forEach { result[it.groupValues[1]] = it.groupValues[2] }
        numberPattern.findAll(json).forEach {
            val key = it.groupValues[1]
            if (!result.containsKey(key)) {
                result[key] =
                    it.groupValues[2].toLongOrNull() ?: it.groupValues[2].toDoubleOrNull() ?: it.groupValues[2]
            }
        }
        return result
    }
}
