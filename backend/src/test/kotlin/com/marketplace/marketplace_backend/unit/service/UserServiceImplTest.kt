package com.marketplace.marketplace_backend.unit.service

import com.marketplace.marketplace_backend.domain.User
import com.marketplace.marketplace_backend.exception.AuthException
import com.marketplace.marketplace_backend.repository.UserRepository
import com.marketplace.marketplace_backend.service.UserServiceImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class UserServiceImplTest {
    private val userRepository: UserRepository = mock()
    private val service = UserServiceImpl(userRepository)

    private val claims =
        mapOf(
            "sub" to "keycloak-sub-001",
            "email" to "jane@example.com",
            "name" to "Jane Smith",
            "picture" to "https://example.com/photo.jpg",
        )

    @Test
    fun `upsertFromTokenClaims creates new user when none exists`() {
        whenever(userRepository.findByKeycloakSubject("keycloak-sub-001")).thenReturn(Optional.empty())
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

        service.upsertFromTokenClaims(claims)

        val captor = argumentCaptor<User>()
        verify(userRepository).save(captor.capture())
        val saved = captor.firstValue
        assert(saved.keycloakSubject == "keycloak-sub-001")
        assert(saved.email == "jane@example.com")
        assert(saved.displayName == "Jane Smith")
        assert(saved.profilePictureUrl == "https://example.com/photo.jpg")
    }

    @Test
    fun `upsertFromTokenClaims updates mutable fields on existing user`() {
        val existing =
            User(
                keycloakSubject = "keycloak-sub-001",
                email = "old@example.com",
                displayName = "Old Name",
                profilePictureUrl = null,
            )
        whenever(userRepository.findByKeycloakSubject("keycloak-sub-001")).thenReturn(Optional.of(existing))
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

        service.upsertFromTokenClaims(claims)

        assert(existing.email == "jane@example.com")
        assert(existing.displayName == "Jane Smith")
        assert(existing.profilePictureUrl == "https://example.com/photo.jpg")
        verify(userRepository).save(existing)
    }

    @Test
    fun `findByKeycloakSubject throws AuthException when user not found`() {
        whenever(userRepository.findByKeycloakSubject(any())).thenReturn(Optional.empty())

        assertThrows<AuthException> {
            service.findByKeycloakSubject("unknown-sub")
        }
    }
}
