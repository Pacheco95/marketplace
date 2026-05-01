package com.marketplace.marketplace_backend.web.dto.request

import jakarta.validation.constraints.NotBlank

data class OneTapRequestDto(
    @field:NotBlank
    val credential: String,
)
