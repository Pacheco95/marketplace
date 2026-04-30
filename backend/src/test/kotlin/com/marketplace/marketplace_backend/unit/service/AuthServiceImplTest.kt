package com.marketplace.marketplace_backend.unit.service

import com.marketplace.marketplace_backend.domain.User
import com.marketplace.marketplace_backend.exception.AuthException
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakClient
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakTokenResponse
import com.marketplace.marketplace_backend.service.AuthServiceImpl
import com.marketplace.marketplace_backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID

class AuthServiceImplTest {
    private val keycloakClient: KeycloakClient = mock()
    private val userService: UserService = mock()
    private val response: HttpServletResponse = mock()
    private val request: HttpServletRequest = mock()

    private lateinit var service: AuthServiceImpl

    private val fakeTokens =
        KeycloakTokenResponse(
            accessToken = buildFakeJwt(mapOf("sub" to "sub123", "email" to "user@test.com", "name" to "Test User")),
            refreshToken = "refresh-token",
            expiresIn = 900L,
            refreshExpiresIn = 2592000L,
        )

    @BeforeEach
    fun setUp() {
        service =
            AuthServiceImpl(
                keycloakClient = keycloakClient,
                userService = userService,
                issuerUri = "http://localhost:8180/realms/marketplace",
                clientId = "marketplace-backend",
                frontendBaseUrl = "http://localhost:3000",
                backendBaseUrl = "http://localhost:8080",
                cookieSecure = false,
                googleJwksUri = "http://localhost:9999/certs",
            )
        val writer = PrintWriter(StringWriter())
        whenever(response.writer).thenReturn(writer)
    }

    @Test
    fun `buildAuthorizationRedirectUrl contains required params`() {
        val url = service.buildAuthorizationRedirectUrl("state-xyz")
        assert(url.contains("response_type=code"))
        assert(url.contains("client_id=marketplace-backend"))
        assert(url.contains("scope="))
        assert(url.contains("state=state-xyz"))
    }

    @Test
    fun `handleCallback rejects mismatched state`() {
        assertThrows<AuthException> {
            service.handleCallback("code", "state-a", "state-b", response)
        }
    }

    @Test
    fun `handleCallback rejects null state cookie`() {
        assertThrows<AuthException> {
            service.handleCallback("code", "state-a", null, response)
        }
    }

    @Test
    fun `handleCallback exchanges code, upserts user, sets cookies`() {
        whenever(keycloakClient.exchangeAuthCode(any(), any())).thenReturn(fakeTokens)
        whenever(userService.upsertFromTokenClaims(any())).thenReturn(fakeUser())

        service.handleCallback("auth-code", "same-state", "same-state", response)

        verify(keycloakClient).exchangeAuthCode("auth-code", "http://localhost:8080/api/v1/auth/callback")
        verify(userService).upsertFromTokenClaims(any())
        verify(response, atLeastOnce()).addCookie(any())
    }

    @Test
    fun `logout calls revokeSession and clears cookies`() {
        service.logout("access-token", response)

        verify(keycloakClient).revokeSession("access-token")
        verify(response, atLeastOnce()).addCookie(any())
    }

    // T050 — One Tap unit tests
    @Test
    fun `handleOneTap with valid credential calls exchangeGoogleToken and sets cookies`() {
        val credential =
            buildFakeJwt(
                mapOf(
                    "sub" to "google-sub",
                    "email" to "user@test.com",
                    "aud" to "test-client-id",
                    "exp" to (System.currentTimeMillis() / 1000 + 3600).toString(),
                ),
            )
        whenever(keycloakClient.exchangeGoogleToken(any())).thenReturn(fakeTokens)
        whenever(userService.upsertFromTokenClaims(any())).thenReturn(fakeUser())

        service.handleOneTap(credential, response)

        verify(keycloakClient).exchangeGoogleToken(credential)
        verify(userService).upsertFromTokenClaims(any())
        verify(response, atLeastOnce()).addCookie(any())
    }

    private fun fakeUser() =
        User(
            id = UUID.randomUUID(),
            keycloakSubject = "sub123",
            email = "user@test.com",
            displayName = "Test User",
        )

    private fun buildFakeJwt(claims: Map<String, String>): String {
        val header =
            java.util.Base64
                .getUrlEncoder()
                .encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload =
            java.util.Base64.getUrlEncoder().encodeToString(
                claims.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }.toByteArray(),
            )
        return "$header.$payload.fakesig"
    }
}
