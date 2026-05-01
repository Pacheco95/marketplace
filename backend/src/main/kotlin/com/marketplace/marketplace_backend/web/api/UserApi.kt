package com.marketplace.marketplace_backend.web.api

import com.marketplace.marketplace_backend.web.dto.response.UserResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Users")
@RequestMapping("/api/v1/users")
interface UserApi {
    @GetMapping("/me")
    @Operation(summary = "Get authenticated user profile")
    @ApiResponse(responseCode = "200", description = "Authenticated user profile")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    fun getMe(): UserResponseDto
}
