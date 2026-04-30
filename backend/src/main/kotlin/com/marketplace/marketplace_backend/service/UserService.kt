package com.marketplace.marketplace_backend.service

import com.marketplace.marketplace_backend.domain.User

interface UserService {
    fun upsertFromTokenClaims(claims: Map<String, Any>): User

    fun findByKeycloakSubject(subject: String): User
}
