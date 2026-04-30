package com.marketplace.marketplace_backend.web.controller

import com.marketplace.marketplace_backend.service.UserService
import com.marketplace.marketplace_backend.web.api.UserApi
import com.marketplace.marketplace_backend.web.dto.response.UserResponseDto
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
) : UserApi {
    override fun getMe(): UserResponseDto {
        val auth =
            SecurityContextHolder.getContext().authentication
                ?: throw com.marketplace.marketplace_backend.exception
                    .AuthException()
        val jwt = auth.principal as Jwt
        val subject = jwt.subject
        val user = userService.findByKeycloakSubject(subject)
        return UserResponseDto.from(user)
    }
}
