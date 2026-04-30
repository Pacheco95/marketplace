package com.marketplace.marketplace_backend.web.controller

import com.marketplace.marketplace_backend.service.AuthService
import com.marketplace.marketplace_backend.web.api.AuthApi
import com.marketplace.marketplace_backend.web.dto.request.OneTapRequestDto
import com.marketplace.marketplace_backend.web.dto.response.UserResponseDto
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
class AuthController(
    private val authService: AuthService,
) : AuthApi {
    override fun login(response: HttpServletResponse): ResponseEntity<Void> {
        val state = UUID.randomUUID().toString()
        val stateCookie =
            jakarta.servlet.http.Cookie("oauth_state", state).apply {
                isHttpOnly = true
                secure = true
                path = "/"
                maxAge = 300
                setAttribute("SameSite", "Lax")
            }
        response.addCookie(stateCookie)

        val url = authService.buildAuthorizationRedirectUrl(state)
        return ResponseEntity.status(302).location(URI(url)).build()
    }

    override fun callback(
        code: String,
        state: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val stateCookie = request.cookies?.firstOrNull { it.name == "oauth_state" }?.value
        val redirectUrl = authService.handleCallback(code, state, stateCookie, response)
        return ResponseEntity.status(302).location(URI(redirectUrl)).build()
    }

    override fun oneTap(
        body: OneTapRequestDto,
        response: HttpServletResponse,
    ): UserResponseDto = authService.handleOneTap(body.credential, response)

    override fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        authService.refresh(request, response)
        return ResponseEntity.noContent().build()
    }

    override fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val accessToken = request.cookies?.firstOrNull { it.name == "marketplace_access_token" }?.value
        authService.logout(accessToken, response)
        return ResponseEntity.noContent().build()
    }
}
