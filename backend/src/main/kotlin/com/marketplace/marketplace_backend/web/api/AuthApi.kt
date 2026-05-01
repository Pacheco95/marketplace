package com.marketplace.marketplace_backend.web.api

import com.marketplace.marketplace_backend.web.dto.request.OneTapRequestDto
import com.marketplace.marketplace_backend.web.dto.response.UserResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Auth")
@RequestMapping("/api/v1/auth")
interface AuthApi {
    @GetMapping("/login")
    @Operation(summary = "Initiate Google OAuth login")
    @ApiResponse(responseCode = "302", description = "Redirect to Keycloak authorization URL")
    fun login(response: HttpServletResponse): ResponseEntity<Void>

    @GetMapping("/callback")
    @Operation(summary = "OAuth callback — exchange code for tokens")
    @ApiResponse(
        responseCode = "302",
        description = "Redirect to /profile on success, /login?error=auth_failed on failure",
    )
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void>

    @PostMapping("/one-tap")
    @Operation(summary = "Validate Google One Tap credential")
    @ApiResponse(responseCode = "200", description = "Authenticated user profile")
    @ApiResponse(responseCode = "401", description = "Invalid credential")
    @ApiResponse(responseCode = "502", description = "Keycloak exchange failed")
    fun oneTap(
        @RequestBody @Valid body: OneTapRequestDto,
        response: HttpServletResponse,
    ): UserResponseDto

    @PostMapping("/refresh")
    @Operation(summary = "Rotate access and refresh tokens")
    @ApiResponse(responseCode = "204", description = "New cookies set")
    @ApiResponse(responseCode = "401", description = "Token expired or revoked")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void>

    @PostMapping("/logout")
    @Operation(summary = "End user session")
    @ApiResponse(responseCode = "204", description = "Session ended")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void>
}
