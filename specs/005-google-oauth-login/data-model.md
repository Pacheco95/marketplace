# Data Model: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30

## Overview

This feature introduces a single persistent entity: the **User**. Session lifecycle is managed entirely by Keycloak; the application database stores only the user's profile data synchronised from Google/Keycloak on first login and on subsequent updates.

## Entities

### User

Represents a person who has authenticated at least once through the Google OAuth flow via Keycloak.

| Column                | Type           | Constraints     | Description                                                   |
| --------------------- | -------------- | --------------- | ------------------------------------------------------------- |
| `id`                  | `UUID`         | PRIMARY KEY     | Application-generated surrogate key                           |
| `keycloak_subject`    | `VARCHAR(255)` | UNIQUE NOT NULL | The `sub` claim from Keycloak's JWT; permanent, never changes |
| `email`               | `VARCHAR(255)` | UNIQUE NOT NULL | User's Google email; may change if Google updates it          |
| `display_name`        | `VARCHAR(255)` | NULLABLE        | Full name from Google profile; null if not provided           |
| `profile_picture_url` | `TEXT`         | NULLABLE        | URL of Google profile photo; null if not provided             |
| `created_at`          | `TIMESTAMPTZ`  | NOT NULL        | First login timestamp (UTC)                                   |
| `updated_at`          | `TIMESTAMPTZ`  | NOT NULL        | Last profile sync timestamp (UTC)                             |

**Business rules**:

- A User record is created or updated (upserted) on every successful login, keeping `email`, `display_name`, and `profile_picture_url` in sync with the Google profile.
- `keycloak_subject` is the stable lookup key; `email` is mutable and cannot be used as the primary identifier in cross-entity foreign keys.
- If `display_name` is null, the presentation layer falls back to the email local-part (before `@`) for display.
- If `profile_picture_url` is null, the UI renders the user's initials derived from `display_name` (or a generic avatar if initials cannot be derived).

**Relationships**: None in this feature. Future features (e.g., product listings, orders) will reference `users.id` as a foreign key.

## Flyway Migrations

### `V2026_04_30T00_00_00__create_users_table.sql`

Creates the `users` table.

```sql
CREATE TABLE users (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    keycloak_subject    VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    profile_picture_url TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_keycloak_subject UNIQUE (keycloak_subject),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_keycloak_subject ON users (keycloak_subject);
CREATE INDEX idx_users_email ON users (email);
```

## JPA Entity (Kotlin)

```kotlin
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "keycloak_subject", nullable = false, unique = true, length = 255)
    val keycloakSubject: String,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "display_name", length = 255)
    var displayName: String? = null,

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    var profilePictureUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
)
```

**Note**: The `@Entity` class is `open` (enforced by the `allOpen` Gradle plugin configured for JPA annotations). No `data class` is used to avoid JPA pitfalls with equals/hashCode and lazy loading.

## Not Stored in the Application Database

- **Sessions / tokens**: Managed entirely by Keycloak. Access and refresh tokens are in HTTP-only cookies; Keycloak tracks active sessions internally.
- **Revocation state**: Keycloak handles token revocation. The backend detects revoked tokens via JWT signature validation failure or Keycloak's token introspection endpoint.
- **Login history / audit log**: Out of scope for this feature; will be addressed in a future Auditability feature (Constitution Principle V).
