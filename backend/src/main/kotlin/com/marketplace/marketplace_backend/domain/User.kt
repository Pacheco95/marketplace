package com.marketplace.marketplace_backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "users")
open class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    open var id: UUID? = null,
    @Column(name = "keycloak_subject", unique = true, nullable = false)
    open var keycloakSubject: String,
    @Column(name = "email", unique = true, nullable = false)
    open var email: String,
    @Column(name = "display_name")
    open var displayName: String? = null,
    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    open var profilePictureUrl: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
)
