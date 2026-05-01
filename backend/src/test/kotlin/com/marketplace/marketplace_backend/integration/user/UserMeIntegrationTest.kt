package com.marketplace.marketplace_backend.integration.user

import com.marketplace.marketplace_backend.TestcontainersConfiguration
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class UserMeIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET users-me without cookie returns 401`() {
        val result = mockMvc.get("/api/v1/users/me").andReturn()
        assert(result.response.status == HttpStatus.UNAUTHORIZED.value()) {
            "Expected 401, got ${result.response.status}"
        }
    }
}
