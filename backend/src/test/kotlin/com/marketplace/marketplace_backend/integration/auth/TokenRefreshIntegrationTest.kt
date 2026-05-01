package com.marketplace.marketplace_backend.integration.auth

import com.marketplace.marketplace_backend.TestcontainersConfiguration
import com.marketplace.marketplace_backend.exception.SessionExpiredException
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakClient
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakTokenResponse
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.Base64

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class TokenRefreshIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var keycloakClient: KeycloakClient

    private fun fakeJwt(sub: String): String {
        val header = Base64.getUrlEncoder().encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload = Base64.getUrlEncoder().encodeToString("""{"sub":"$sub","email":"u@test.com"}""".toByteArray())
        return "$header.$payload.sig"
    }

    @Test
    fun `POST refresh with valid refresh token returns 204 and sets new cookies`() {
        whenever(keycloakClient.refreshToken(any())).thenReturn(
            KeycloakTokenResponse(
                accessToken = fakeJwt("sub-001"),
                refreshToken = "new-refresh",
                expiresIn = 900L,
                refreshExpiresIn = 2592000L,
            ),
        )

        val result =
            mockMvc
                .post("/api/v1/auth/refresh") {
                    cookie(Cookie("marketplace_refresh_token", "valid-refresh-token"))
                }.andReturn()

        assert(result.response.status == HttpStatus.NO_CONTENT.value()) {
            "Expected 204, got ${result.response.status}"
        }
        val cookies = result.response.cookies
        assert(cookies.any { it.name == "marketplace_access_token" }) { "Missing new access token cookie" }
        assert(cookies.any { it.name == "marketplace_refresh_token" }) { "Missing new refresh token cookie" }
    }

    @Test
    fun `POST refresh with revoked token returns 401 with session-expired type`() {
        whenever(keycloakClient.refreshToken(any())).thenThrow(SessionExpiredException())

        val result =
            mockMvc
                .post("/api/v1/auth/refresh") {
                    cookie(Cookie("marketplace_refresh_token", "revoked-token"))
                }.andReturn()

        assert(result.response.status == HttpStatus.UNAUTHORIZED.value()) {
            "Expected 401, got ${result.response.status}"
        }
        val body = result.response.contentAsString
        assert(body.contains("session-expired")) { "Expected session-expired in body, got: $body" }
    }

    @Test
    fun `POST refresh without cookie returns 401 with RFC 7807 instance`() {
        val result = mockMvc.post("/api/v1/auth/refresh").andReturn()

        assert(result.response.status == HttpStatus.UNAUTHORIZED.value()) {
            "Expected 401, got ${result.response.status}"
        }
        val body = result.response.contentAsString
        assert(body.contains("urn:uuid:")) { "Expected RFC 7807 instance urn:uuid:, got: $body" }
    }
}
