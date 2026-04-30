package com.marketplace.marketplace_backend.integration.auth

import com.marketplace.marketplace_backend.TestcontainersConfiguration
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
import org.springframework.test.web.servlet.get
import java.util.Base64

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class AuthFlowIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var keycloakClient: KeycloakClient

    private fun fakeJwt(
        sub: String,
        email: String,
    ): String {
        val header = Base64.getUrlEncoder().encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload =
            Base64.getUrlEncoder().encodeToString(
                """{"sub":"$sub","email":"$email","name":"Test User"}""".toByteArray(),
            )
        return "$header.$payload.fakesig"
    }

    @Test
    fun `callback with matching state sets cookies and redirects to profile`() {
        whenever(keycloakClient.exchangeAuthCode(any(), any())).thenReturn(
            KeycloakTokenResponse(
                accessToken = fakeJwt("sub-001", "user@test.com"),
                refreshToken = "refresh-001",
                expiresIn = 900L,
                refreshExpiresIn = 2592000L,
            ),
        )

        val result =
            mockMvc
                .get("/api/v1/auth/callback?code=auth-code&state=matching-state") {
                    cookie(Cookie("oauth_state", "matching-state"))
                }.andReturn()

        val response = result.response
        assert(response.status == HttpStatus.FOUND.value()) {
            "Expected 302, got ${response.status}"
        }
        val location = response.getHeader("Location") ?: ""
        assert(location.contains("/profile")) { "Expected redirect to /profile, got: $location" }

        val cookies = response.cookies
        assert(cookies.any { it.name == "marketplace_access_token" }) { "Missing access token cookie" }
        assert(cookies.any { it.name == "marketplace_refresh_token" }) { "Missing refresh token cookie" }
    }

    @Test
    fun `callback with mismatched state returns 401`() {
        val result =
            mockMvc
                .get("/api/v1/auth/callback?code=auth-code&state=wrong-state") {
                    cookie(Cookie("oauth_state", "correct-state"))
                }.andReturn()

        assert(result.response.status == HttpStatus.UNAUTHORIZED.value()) {
            "Expected 401 for mismatched state, got ${result.response.status}"
        }
    }
}
