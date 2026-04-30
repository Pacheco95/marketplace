package com.marketplace.marketplace_backend.integration.auth

import com.marketplace.marketplace_backend.TestcontainersConfiguration
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakClient
import com.marketplace.marketplace_backend.infrastructure.keycloak.KeycloakTokenResponse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.Base64

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class OneTapIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var keycloakClient: KeycloakClient

    private fun buildJwt(claims: Map<String, String>): String {
        val header = Base64.getUrlEncoder().encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload =
            Base64.getUrlEncoder().encodeToString(
                claims.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }.toByteArray(),
            )
        return "$header.$payload.fakesig"
    }

    @Test
    fun `POST one-tap with expired credential returns 401`() {
        val expiredCredential =
            buildJwt(
                mapOf("sub" to "google-sub", "email" to "user@test.com", "aud" to "test-client-id", "exp" to "1000"),
            )

        val result =
            mockMvc
                .post("/api/v1/auth/one-tap") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"credential":"$expiredCredential"}"""
                }.andReturn()

        assert(result.response.status == HttpStatus.UNAUTHORIZED.value()) {
            "Expected 401 for expired credential, got ${result.response.status}"
        }
    }

    @Test
    fun `POST one-tap with valid credential returns 200 with cookies`() {
        val futureExp = (System.currentTimeMillis() / 1000 + 3600).toString()
        val validCredential =
            buildJwt(
                mapOf("sub" to "google-sub", "email" to "user@test.com", "aud" to "test-client-id", "exp" to futureExp),
            )
        val accessToken = buildJwt(mapOf("sub" to "kc-sub", "email" to "user@test.com", "name" to "Test User"))
        whenever(keycloakClient.exchangeGoogleToken(any())).thenReturn(
            KeycloakTokenResponse(
                accessToken = accessToken,
                refreshToken = "refresh-token",
                expiresIn = 900L,
                refreshExpiresIn = 2592000L,
            ),
        )

        val result =
            mockMvc
                .post("/api/v1/auth/one-tap") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"credential":"$validCredential"}"""
                }.andReturn()

        assert(result.response.status == HttpStatus.OK.value()) {
            "Expected 200, got ${result.response.status}: ${result.response.contentAsString}"
        }
        val cookies = result.response.cookies
        assert(cookies.any { it.name == "marketplace_access_token" }) { "Missing access token cookie" }
    }
}
