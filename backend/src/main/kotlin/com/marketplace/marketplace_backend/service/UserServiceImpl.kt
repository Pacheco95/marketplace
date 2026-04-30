package com.marketplace.marketplace_backend.service

import com.marketplace.marketplace_backend.domain.User
import com.marketplace.marketplace_backend.exception.AuthException
import com.marketplace.marketplace_backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
open class UserServiceImpl(
    private val userRepository: UserRepository,
) : UserService {
    @Transactional
    override fun upsertFromTokenClaims(claims: Map<String, Any>): User {
        val subject = claims["sub"] as? String ?: throw AuthException()
        val email = claims["email"] as? String ?: throw AuthException()
        val displayName = claims["name"] as? String
        val profilePictureUrl = claims["picture"] as? String

        val existing = userRepository.findByKeycloakSubject(subject)
        return if (existing.isPresent) {
            val user = existing.get()
            user.email = email
            user.displayName = displayName
            user.profilePictureUrl = profilePictureUrl
            user.updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
            userRepository.save(user)
        } else {
            userRepository.save(
                User(
                    keycloakSubject = subject,
                    email = email,
                    displayName = displayName,
                    profilePictureUrl = profilePictureUrl,
                ),
            )
        }
    }

    @Transactional(readOnly = true)
    override fun findByKeycloakSubject(subject: String): User =
        userRepository
            .findByKeycloakSubject(subject)
            .orElseThrow { AuthException() }
}
