# Implementation Plan: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/005-google-oauth-login/spec.md`

## Summary

Implement end-to-end Google OAuth login for the marketplace platform. A new Spring Boot 4.0.6 / Kotlin 2.3.21 backend is built from scratch (placed in `backend/`) using Keycloak 26.6.1 as the IAM/Identity Broker that federates Google OAuth2. The backend acts as a confidential OAuth2 client + JWT resource server, setting HTTP-only Secure cookies containing short-lived access tokens and rotating refresh tokens. The existing Nuxt 4 frontend gains a `/login` page (with Google One Tap auto-prompt and a "Login with Google" button), a protected `/profile` page, and a persistent top-right context menu reflecting auth state. A single `docker compose up` starts all four services (Postgres, Keycloak, backend, frontend). CI is extended with backend formatting and test jobs.

## Technical Context

**Language/Version**: Kotlin 2.3.21 / JVM 25 (backend); TypeScript 5.x / Nuxt 4 (frontend)  
**Primary Dependencies**:

- Backend: Spring Boot 4.0.6, Spring Security (OAuth2 Resource Server + Client), SpringDoc OpenAPI 3.x, Keycloak 26.6.1, JPA/Hibernate, Flyway, Spotless (ktlint), Mockito 5.23.0, TestContainers
- Frontend: Nuxt 4 (existing), Pinia (existing), @nuxtjs/i18n (existing), shadcn-vue (existing), Google Identity Services (new — external script)

**Storage**: PostgreSQL 18 (application data); Keycloak uses a separate `keycloak` database on the same Postgres instance  
**Testing**: JUnit 5 + Mockito 5.23.0 (unit); Spring Boot Test + TestContainers (integration); Vitest (frontend unit/integration); Playwright (E2E)  
**Target Platform**: Linux server (Docker); developer laptops (macOS/Linux/Windows-WSL)  
**Project Type**: Full-stack web application — decoupled Nuxt frontend + Spring Boot REST backend (Constitution Principle III)  
**Constraints**:

- HTTP-only cookies only; no `localStorage` token storage
- No static methods in Kotlin code
- Controller interfaces carry all OpenAPI annotations; controllers contain no business logic
- Services use dependency inversion (interfaces + implementations)
- All error messages internationalised via `MessageSource`
- Migrations versioned with UTC ISO 8601 datetime keys
- Spotless formatting enforced locally and in CI (Constitution Principle VIII)

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design — still passing._

| Principle                                         | Relevance                                                                                                                                                                                  | Evaluation                  |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------- |
| I. Multi-Tenant Data Sovereignty                  | The `users` table has no tenant isolation in this feature. Login is platform-wide. Tenant isolation applies to future seller/buyer entities; this feature only stores the identity record. | ✅ pass-by-scope            |
| II. Transactional Integrity & Commission Accuracy | Not applicable — no financial transactions in this feature                                                                                                                                 | ✅ pass-by-vacuity          |
| III. Service-Oriented Extensibility               | Directly served. Backend is a versioned REST API (`/api/v1`); frontend is a separate Nuxt app. The auth API will be consumed by future mobile apps.                                        | ✅ pass                     |
| IV. Quality Assurance for Critical Paths          | Directly served. Login is the entry gate to all critical paths. Unit + integration tests cover auth flows; E2E covers the golden paths.                                                    | ✅ pass                     |
| V. Auditability & Transparent Reporting           | Partial. RFC 7807 traceable error IDs (incident UUIDs) are logged. Full financial audit logging is out of scope for this feature.                                                          | ✅ pass-by-scope            |
| VI. Mobile-First Design                           | Frontend UI (login page, context menu, profile page) must be designed mobile-first with Tailwind responsive utilities.                                                                     | ✅ pass (enforced in tasks) |
| VII. Internationalization (i18n) by Default       | Directly served. Backend error messages use `MessageSource` + `Accept-Language`. Frontend uses existing `@nuxtjs/i18n`. All user-facing strings externalised.                              | ✅ pass                     |
| VIII. Code Quality Tooling                        | Directly served. Backend adds Spotless (ktlint). CI gains `backend-format`, `backend-unit`, `backend-integration` jobs. Husky pre-push runs backend unit tests if backend files changed.   | ✅ pass                     |

**Gate result**: PASS. No unjustified deviations.

## Project Structure

### Documentation (this feature)

```text
specs/005-google-oauth-login/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions and rationale
├── data-model.md        # Phase 1 — database entities and migrations
├── quickstart.md        # Phase 1 — developer setup guide
├── contracts/
│   ├── rest-api.md      # API endpoint contract
│   ├── cookie-contract.md  # Cookie specification
│   └── error-contract.md   # RFC 7807 error format + incident IDs
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
.
├── docker-compose.yml               # NEW — all 4 services (postgres, keycloak, backend, frontend)
├── .env.example                     # NEW — GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
├── docker/
│   ├── postgres/
│   │   └── init.sql                 # NEW — creates keycloak + marketplace databases
│   └── keycloak/
│       └── realm-export.json        # NEW — pre-configured marketplace realm
├── .github/
│   └── workflows/
│       └── pull-request.yml         # UPDATE — add backend-format, backend-unit, backend-integration jobs
│
├── backend/                         # NEW — Spring Boot app (from Spring Initializr zip)
│   ├── build.gradle.kts             # UPDATE — Kotlin 2.3.21, Java 25, Spotless, Mockito pin, PG driver, Keycloak
│   ├── settings.gradle.kts
│   ├── gradlew / gradlew.bat
│   ├── gradle/wrapper/
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/marketplace/marketplace_backend/
│       │   │   ├── MarketplaceBackendApplication.kt
│       │   │   ├── config/
│       │   │   │   ├── SecurityConfig.kt          # OAuth2 resource server + cookie filter
│       │   │   │   ├── SecurityConfigLocal.kt     # local profile: Swagger auth disabled
│       │   │   │   ├── WebMvcConfig.kt            # CORS
│       │   │   │   └── OpenApiConfig.kt           # SpringDoc base path + info
│       │   │   ├── web/
│       │   │   │   ├── api/                       # Interfaces with OpenAPI annotations
│       │   │   │   │   ├── AuthApi.kt
│       │   │   │   │   └── UserApi.kt
│       │   │   │   ├── controller/
│       │   │   │   │   ├── AuthController.kt
│       │   │   │   │   └── UserController.kt
│       │   │   │   └── dto/
│       │   │   │       ├── request/
│       │   │   │       │   └── OneTapRequestDto.kt
│       │   │   │       └── response/
│       │   │   │           └── UserResponseDto.kt
│       │   │   ├── service/
│       │   │   │   ├── AuthService.kt             # Interface
│       │   │   │   ├── AuthServiceImpl.kt
│       │   │   │   ├── UserService.kt             # Interface
│       │   │   │   └── UserServiceImpl.kt
│       │   │   ├── repository/
│       │   │   │   └── UserRepository.kt
│       │   │   ├── domain/
│       │   │   │   └── User.kt                    # JPA entity
│       │   │   ├── exception/
│       │   │   │   ├── MarketplaceException.kt    # Base sealed exception class
│       │   │   │   ├── AuthException.kt
│       │   │   │   └── GlobalExceptionHandler.kt  # @RestControllerAdvice, RFC 7807 + UUID
│       │   │   └── infrastructure/
│       │   │       └── keycloak/
│       │   │           ├── KeycloakClient.kt       # Interface
│       │   │           └── KeycloakClientImpl.kt
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-local.properties    # local profile overrides
│       │       ├── messages.properties             # default (English) i18n messages
│       │       ├── messages_en.properties
│       │       └── db/migration/
│       │           └── V2026_04_30T00_00_00__create_users_table.sql
│       └── test/
│           ├── kotlin/com/marketplace/marketplace_backend/
│           │   ├── MarketplaceBackendApplicationTests.kt
│           │   ├── TestcontainersConfiguration.kt
│           │   ├── TestMarketplaceBackendApplication.kt
│           │   ├── unit/
│           │   │   ├── service/
│           │   │   │   ├── AuthServiceImplTest.kt
│           │   │   │   └── UserServiceImplTest.kt
│           │   │   └── web/
│           │   │       └── controller/
│           │   │           ├── AuthControllerTest.kt
│           │   │           └── UserControllerTest.kt
│           │   └── integration/
│           │       ├── auth/
│           │       │   └── AuthFlowIntegrationTest.kt
│           │       └── user/
│           │           └── UserMeIntegrationTest.kt
│           └── resources/
│               └── application-test.properties
│
└── frontend/                        # EXISTING — Nuxt 4
    ├── app/
    │   ├── pages/
    │   │   ├── index.vue            # UPDATE — add One Tap trigger for unauthenticated visitors
    │   │   ├── login.vue            # NEW — /login page with button + One Tap prompt
    │   │   └── profile.vue          # NEW — /profile page (protected)
    │   ├── components/
    │   │   └── shared/
    │   │       ├── UserMenu.vue     # NEW — top-right context menu (avatar/initials/login)
    │   │       └── AuthErrorBanner.vue  # NEW — error banner for login failures
    │   ├── composables/
    │   │   ├── useAuth.ts           # NEW — auth actions (login, logout, refresh, one-tap)
    │   │   └── useGoogleOneTap.ts   # NEW — Google GSI One Tap integration
    │   ├── middleware/
    │   │   └── auth.ts              # NEW — redirect unauthenticated users to /login
    │   ├── stores/
    │   │   └── useAuthStore.ts      # NEW — Pinia store: user state, isAuthenticated
    │   └── layouts/
    │       └── default.vue          # UPDATE — add <UserMenu> to top-right header
    ├── i18n/locales/
    │   └── en.json                  # UPDATE — add auth-related strings
    └── plugins/
        └── google-gsi.client.ts     # NEW — loads Google Identity Services script
```

**Structure Decision**: Web application with fully decoupled frontend (`frontend/`) and backend (`backend/`). The backend is a new directory at the repo root, extracted from the Spring Initializr zip. Docker Compose at the root ties all services together. This follows Constitution Principle III.

## Phase 0: Research

See [research.md](./research.md). Key decisions:

| Topic           | Decision                                                                                                                        |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| IAM             | Keycloak 26.6.1, Google as Identity Provider                                                                                    |
| Token transport | HTTP-only Secure cookies (access 15 min / refresh 30 days, rotated)                                                             |
| One Tap flow    | Google GSI on frontend → POST to backend → Keycloak Token Exchange                                                              |
| Button login    | OIDC auth code flow: frontend → backend → Keycloak → callback → cookies                                                         |
| Frontend auth   | Manual Pinia store + Nuxt middleware (no @nuxtjs/auth-next — Nuxt 4 incompatible)                                               |
| Error format    | RFC 7807 ProblemDetail + UUID instance (traceable incident ID)                                                                  |
| i18n errors     | Spring MessageSource, Accept-Language resolution                                                                                |
| Formatting      | Spotless (ktlint) for backend; existing Prettier for frontend                                                                   |
| Migrations      | `V2026_04_30_00_00_00__description.sql` (UTC datetime, underscores only — `T` separator is not valid in Flyway version strings) |

## Phase 1: Design & Contracts

### Data Model

See [data-model.md](./data-model.md). Single entity: `users` table with `keycloak_subject` as stable identifier. Keycloak manages sessions internally.

### API Contracts

See [contracts/rest-api.md](./contracts/rest-api.md), [contracts/cookie-contract.md](./contracts/cookie-contract.md), [contracts/error-contract.md](./contracts/error-contract.md).

Summary:

- `GET /api/v1/auth/login` → OIDC redirect to Keycloak
- `GET /api/v1/auth/callback` → code exchange, set cookies, redirect to `/profile`
- `POST /api/v1/auth/one-tap` → validate Google credential, exchange via Keycloak, set cookies
- `POST /api/v1/auth/refresh` → rotate tokens
- `POST /api/v1/auth/logout` → clear cookies, end Keycloak session
- `GET /api/v1/users/me` → authenticated user profile

### Quickstart

See [quickstart.md](./quickstart.md).

## Implementation Notes

### Backend Build File Changes

The Spring Initializr scaffold (`~/Downloads/marketplace-backend.zip`) must be placed in `backend/` with these changes to `build.gradle.kts`:

- Kotlin plugin version: `2.2.21` → `2.3.21`
- Java toolchain: `24` → `25`
- Add: `id("com.diffplug.spotless") version "7.0.3"` (or latest stable)
- Add: `implementation("org.postgresql:postgresql")` (runtime)
- Add: `implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")`
- Add: `implementation("org.springframework.boot:spring-boot-starter-oauth2-client")`
- Add: `testImplementation("org.mockito:mockito-core:5.23.0")`
- Add: `testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")`
- Add: `testImplementation("org.testcontainers:postgresql")`
- Override BOM Mockito version to `5.23.0`

### Spring Security Cookie Filter

A `CookieTokenFilter : OncePerRequestFilter` reads `marketplace_access_token` from the request cookies and writes its value as `Authorization: Bearer <token>` to a wrapped request, before Spring Security's JWT validation filter runs. This keeps the standard OAuth2 resource server validation unchanged.

### Keycloak Token Exchange for One Tap

One Tap delivers a Google ID token to the frontend. The backend posts this to Keycloak's token endpoint with `grant_type=urn:ietf:params:oauth:grant-type:token-exchange` and `subject_issuer=google`. Keycloak validates the Google token against its configured Google IDP and issues a Keycloak access + refresh token pair. The Token Exchange feature must be enabled in the Keycloak realm configuration (done in `realm-export.json`).

### Exception Design

```
MarketplaceException (sealed class)
├── AuthException(code: ErrorCode, args: Array<Any>)
│   ├── SessionExpiredException
│   ├── GoogleCredentialInvalidException
│   └── TokenExchangeFailedException
└── ValidationException(field: String, code: ErrorCode)
```

Each exception carries an `ErrorCode` enum that maps to:

- A `messages.properties` key for the i18n message
- An RFC 7807 `type` URI suffix
- An HTTP status code

`GlobalExceptionHandler` catches all `MarketplaceException` subclasses, generates a UUID incident ID, logs it with the full context, and returns a `ProblemDetail` response.

### Frontend One Tap Integration

A Nuxt plugin (`plugins/google-gsi.client.ts`) loads `https://accounts.google.com/gsi/client` on the client side. The `useGoogleOneTap` composable initialises One Tap on mount for pages where the user is not authenticated. The `google.accounts.id.initialize()` callback POSTs the credential to `/api/v1/auth/one-tap` and, on success, updates the auth store and navigates to `/profile`.

### Local Spring Profile

`application-local.properties` sets:

```properties
springdoc.swagger-ui.csrf.enabled=false
server.servlet.session.cookie.secure=false
```

`SecurityConfigLocal.kt` (annotated `@Profile("local") @Configuration`) permits all requests to `/api/v1/swagger-ui.html` and `/api/v1/api-docs` without authentication, overriding the default security configuration.

### Docker Compose

`docker-compose.yml` defines four services with persistent named volumes:

- `postgres` (postgres:18) — serves both `marketplace` and `keycloak` databases via `docker/postgres/init.sql`
- `keycloak` (quay.io/keycloak/keycloak:26.6.1) — runs in `start-dev` mode, imports `realm-export.json` on startup, reads `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` from `.env`
- `backend` — built from `backend/Dockerfile`, reads DB + Keycloak config from compose environment
- `frontend` — built from `frontend/Dockerfile`, served on port 3000

### CI Extension

New jobs added to `.github/workflows/pull-request.yml`:

| Job                   | Trigger | Steps                                                                        |
| --------------------- | ------- | ---------------------------------------------------------------------------- |
| `backend-format`      | always  | `actions/setup-java@v4` (Java 25) + Gradle cache + `./gradlew spotlessCheck` |
| `backend-unit`        | always  | same setup + `./gradlew test`                                                |
| `backend-integration` | always  | same setup + Docker (for TestContainers) + `./gradlew integrationTest`       |

## Implementation Notes (Post-Build)

### Flyway + PostgreSQL compatibility

**PostgreSQL version**: `postgres:17` is required. Flyway 11.14.1 (the version managed by Spring Boot 4.0.6) does not yet support PostgreSQL 18.

**`flyway-database-postgresql` module**: Must be declared explicitly in `build.gradle.kts`. Flyway 10+ split database-specific support into separate modules; `spring-boot-starter-flyway` pulls in `flyway-core` only, which cannot connect to PostgreSQL without this additional module.

**Migration filename format**: Flyway version strings accept only digits and `.`/`_` as separators. The ISO 8601 `T` between date and time (`V2026_04_30T00_00_00`) causes the file to be silently skipped. Use `V2026_04_30_00_00_00` (replace `T` with `_`).

## Complexity Tracking

> No Constitution violations requiring justification.
