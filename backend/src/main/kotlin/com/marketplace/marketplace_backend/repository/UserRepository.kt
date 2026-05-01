package com.marketplace.marketplace_backend.repository

import com.marketplace.marketplace_backend.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByKeycloakSubject(subject: String): Optional<User>

    fun findByEmail(email: String): Optional<User>
}
