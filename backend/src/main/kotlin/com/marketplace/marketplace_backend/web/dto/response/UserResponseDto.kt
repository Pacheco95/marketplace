package com.marketplace.marketplace_backend.web.dto.response

import com.marketplace.marketplace_backend.domain.User
import java.util.UUID

data class UserResponseDto(
    val id: UUID,
    val email: String,
    val displayName: String?,
    val profilePictureUrl: String?,
) {
    companion object {
        fun from(user: User): UserResponseDto =
            UserResponseDto(
                id = user.id!!,
                email = user.email,
                displayName = user.displayName,
                profilePictureUrl = user.profilePictureUrl,
            )
    }
}
