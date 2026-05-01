package com.marketplace.marketplace_backend.service

import com.marketplace.marketplace_backend.web.dto.response.UserResponseDto
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

interface AuthService {
    fun buildAuthorizationRedirectUrl(state: String): String

    fun handleCallback(
        code: String,
        state: String,
        stateCookie: String?,
        response: HttpServletResponse,
    ): String

    fun handleOneTap(
        credential: String,
        response: HttpServletResponse,
    ): UserResponseDto

    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    )

    fun logout(
        accessToken: String?,
        response: HttpServletResponse,
    )
}
