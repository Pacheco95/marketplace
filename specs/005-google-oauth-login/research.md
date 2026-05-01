# Research: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30

## Technology Decisions

### Backend Stack

**Decision**: Kotlin 2.3.21 / Java 25 / Spring Boot 4.0.6  
**Rationale**: Specified by the team. The Spring Initializr scaffold (from `~/Downloads/marketplace-backend.zip`) was generated with Kotlin 2.2.21 / Java 24; these will be bumped to the specified versions in `build.gradle.kts`.  
**Note**: Spring Boot 4.x continues the Spring Framework 6 baseline with full virtual-threads support (Java 21+) and first-class Jakarta EE 10.

**Decision**: Gradle with Kotlin DSL (`build.gradle.kts`)  
**Rationale**: Specified by team. Kotlin DSL gives type-safe build scripts and IDE completion. The Initializr scaffold includes the Gradle wrapper.

**Decision**: Spotless plugin (latest stable) with ktlint  
**Rationale**: Spotless is the standard Gradle formatting plugin for JVM projects. It wraps ktlint for Kotlin code style enforcement. The developer runs `./gradlew spotlessApply` to format; `./gradlew spotlessCheck` is wired into CI. Constitution Principle VIII mandates uniform formatting enforcement.  
**Gradle config**: `id("com.diffplug.spotless")` with `ktlint` target on `src/**/*.kt` and `*.gradle.kts`.

**Decision**: Mockito 5.23.0 + mockito-kotlin  
**Rationale**: Specified. Spring Boot 4.x test starter includes Mockito but we pin to 5.23.0. `mockito-kotlin` (the Kotlin extension library) is added alongside to make mock DSL idiomatic in Kotlin.

**Decision**: JUnit 5 (JUnit Jupiter, latest via Spring Boot BOM)  
**Rationale**: Spring Boot 4.x test starter already provides JUnit 5. No separate version pin needed; BOM manages it.

**Decision**: TestContainers with PostgreSQL 18 module  
**Rationale**: Specified. Integration tests spin up a real Postgres 18 container via TestContainers. This matches the production database version, avoiding mock/prod divergence. Flyway migrations run inside the container on test startup.

### Identity & Auth

**Decision**: Keycloak 26.6.1 as the Identity Provider (IAM) and Google OAuth2 Identity Broker  
**Rationale**: Keycloak manages all user sessions, token issuance, and revocation. Google is configured as an Identity Provider inside Keycloak, so the application never directly handles Google credentials for the standard button-based login flow. This centralises IAM and keeps the Spring Boot backend as a stateless OAuth2 Resource Server.

**Decision**: HTTP-only Secure cookies for token transport (access token + refresh token)  
**Rationale**: HTTP-only cookies prevent XSS token theft. Separate cookies with different paths limit the refresh token's exposure surface. This is the recommended pattern for browser-based apps with a backend-for-frontend (BFF) element.

- `marketplace_access_token`: short-lived (15 min), `Path=/api`, `HttpOnly; Secure; SameSite=Lax`
- `marketplace_refresh_token`: up to 30 days (configurable in Keycloak), `Path=/api/v1/auth/refresh`, `HttpOnly; Secure; SameSite=Lax`

**Decision**: Spring Boot acts as both OAuth2 Confidential Client (for auth code flow) and OAuth2 Resource Server (for JWT validation)  
**Rationale**: The backend initiates and completes the OIDC authorization code flow against Keycloak, then stores tokens in cookies. Subsequent requests are validated by reading the access token cookie and verifying its JWT signature against Keycloak's public keys (JWKS endpoint). This avoids the browser ever holding tokens in `localStorage`.

**Decision**: Keycloak Token Exchange for Google One Tap flow  
**Rationale**: Google One Tap delivers a Google ID token to the browser JavaScript. The backend receives this credential, validates its Google signature, then exchanges it for a Keycloak token via the OAuth 2.0 Token Exchange grant (`grant_type=urn:ietf:params:oauth:grant-type:token-exchange`). This requires the Token Exchange feature to be enabled in the Keycloak realm. This keeps Keycloak as the single source of truth for sessions.  
**Alternative considered**: Validate Google token directly on backend, create user in DB, and issue own JWT. Rejected because it creates two parallel IAM systems.

**Decision**: Spring Security OAuth2 Resource Server + custom cookie filter  
**Rationale**: Spring Security's `oauth2ResourceServer().jwt()` handles JWT validation. A custom `OncePerRequestFilter` extracts the access token from the cookie and writes it to the `Authorization` header before the security filter chain processes it, so no other code needs to change.

### Data Persistence

**Decision**: PostgreSQL 18 via JPA (Hibernate) + Flyway migrations  
**Rationale**: Specified. The application stores only user profile data (synchronised from Keycloak/Google); Keycloak stores sessions internally in its own database.

**Decision**: Flyway migration version format: `V{UTC-ISO-8601-datetime}__description.sql`  
**Rationale**: Specified ("UTC ISO 8601 datetime keys"). In filenames, colons and dashes are replaced with underscores: `2026-04-30T00:00:00Z` → `V2026_04_30T00_00_00`. Example: `V2026_04_30T00_00_00__create_users_table.sql`.

**Decision**: JPA with CriteriaBuilder for complex queries, JPQL for simple queries  
**Rationale**: Specified. CriteriaBuilder provides type-safe, composable predicates. Simple lookups use Spring Data JPA derived queries.

### Error Handling

**Decision**: RFC 7807 Problem Details (Spring's `ProblemDetail`) + traceable UUID  
**Rationale**: Spring Boot 3+ has built-in `ProblemDetail` support. Each error response includes an `instance` field containing a `urn:uuid:{uuid}` that acts as a traceable incident ID users can quote in support tickets. This is the industry-standard approach for machine-readable, traceable HTTP error responses.  
**Alternatives considered**: Custom error envelope; rejected as non-standard. Correlation-ID-only approach; combined with Problem Details instance field for maximum traceability.

**Decision**: i18n for all error `detail` and `title` messages  
**Rationale**: Specified (Constitution Principle VII + explicit user requirement). Error message strings live in `messages.properties` / `messages_{locale}.properties`. `MessageSource` is injected into the global exception handler. The `Accept-Language` header determines locale resolution.

### Frontend

**Decision**: Manual auth composables + Pinia store; no `@nuxtjs/auth-next`  
**Rationale**: `@nuxtjs/auth-next` targets Nuxt 2 and is not compatible with Nuxt 4. Since the backend handles all token operations via HTTP-only cookies, the frontend does not need a client-side auth library to manage tokens. A `useAuthStore` Pinia store tracks user identity (fetched from `/api/v1/users/me`), and a Nuxt route middleware protects authenticated pages. This is simpler and gives full control over the cookie-based session model.

**Decision**: Google Identity Services (GIS) `accounts.google.com/gsi/client` for One Tap  
**Rationale**: The official Google library for One Tap / Sign In With Google. Loaded via Nuxt's `useHead` composable (or a Nuxt plugin) as an external script. The frontend calls `google.accounts.id.initialize()` and `google.accounts.id.prompt()` for unauthenticated visitors with an active Google session.

**Decision**: Nuxt route middleware for protected routes  
**Rationale**: A `middleware/auth.ts` file checks `useAuthStore().isAuthenticated` and redirects to `/login` if false. Applied globally or per-page using `definePageMeta({ middleware: 'auth' })`.

### Infrastructure

**Decision**: Docker Compose for local development (all services)  
**Rationale**: Specified. A single `docker compose up` starts PostgreSQL 18, Keycloak 26.6.1, the Spring Boot backend, and the Nuxt frontend. Persistent named volumes prevent data loss across restarts.

**Decision**: Single PostgreSQL instance with two databases via init script  
**Rationale**: The app uses `marketplace` DB; Keycloak uses `keycloak` DB. Using one Postgres service with a `docker-entrypoint-initdb.d` init script that creates both databases avoids running two Postgres containers.

**Decision**: `.env.example` with all variable keys, no values  
**Rationale**: Specified. Developers copy `.env.example` to `.env` and fill in Google OAuth Client ID/Secret and other secrets. The `docker-compose.yml` reads from `.env`. Simple dev values (admin/admin) are used inside the compose file for Keycloak/Postgres credentials; sensitive values (Google Client ID/Secret) are in `.env`.

### CI

**Decision**: Extend existing `pull-request.yml` with backend jobs  
**Rationale**: The project already has a GitHub Actions workflow for frontend quality checks. New jobs (`backend-format`, `backend-unit`, `backend-integration`) are added in parallel. Backend jobs use `actions/setup-java` with Java 25 (GraalVM or Temurin) and Gradle caching.

## Resolved NEEDS CLARIFICATION Items

All items resolved by explicit user input. No outstanding clarifications.
