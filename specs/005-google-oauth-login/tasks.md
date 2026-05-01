# Tasks: Google OAuth Login

**Input**: Design documents from `specs/005-google-oauth-login/`
**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅ quickstart.md ✅

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no blocking dependencies on in-progress tasks)
- **[Story]**: Which user story this task belongs to (US1–US6)
- Exact file paths are included in all task descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Bootstrap both workspaces and the full local dev stack from scratch.

- [x] T001 Extract `~/Downloads/marketplace-backend.zip` and place as `backend/` at the repo root: `unzip ~/Downloads/marketplace-backend.zip && mv marketplace-backend backend`
- [x] T002 Update `backend/build.gradle.kts`: bump Kotlin plugin to `2.3.21`, Java toolchain to `25`, add `org.postgresql:postgresql` (runtimeOnly), `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-oauth2-client`, `com.diffplug.spotless` plugin, pin `org.mockito:mockito-core` to `5.23.0`, add `org.mockito.kotlin:mockito-kotlin:5.4.0` and `org.testcontainers:postgresql` to test dependencies
- [x] T003 [P] Add Spotless configuration to `backend/build.gradle.kts`: `ktlint("1.5.0")` targeting `src/**/*.kt` and `*.gradle.kts`; register a `spotlessApply` alias task; verify `./gradlew spotlessCheck` passes on the pristine scaffold
- [x] T004 Create `backend/src/main/resources/application.properties` with: `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.locations=classpath:db/migration`, `spring.security.oauth2.resourceserver.jwt.issuer-uri` (pointing to Keycloak realm), `springdoc.api-docs.path=/api/v1/api-docs`, `springdoc.swagger-ui.path=/api/v1/swagger-ui.html`, `server.servlet.context-path` left empty (paths declared in controllers); also add `app.frontend.base-url=http://localhost:3000` (used by `AuthServiceImpl` to construct the post-callback redirect to `/profile`) and `google.jwks-uri=https://www.googleapis.com/oauth2/v3/certs` (injectable in tests via override)
- [x] T005 [P] Create `backend/src/main/resources/application-local.properties` with: `server.servlet.session.cookie.secure=false`, `springdoc.swagger-ui.disable-swagger-default-url=false`, `logging.level.com.marketplace=DEBUG`
- [x] T006 Create `docker/postgres/init.sql` with a single `CREATE DATABASE keycloak;` statement (the `marketplace` database is created automatically by the `POSTGRES_DB` env var on the `postgres` service)
- [x] T007 Create `docker/keycloak/realm-export.json` pre-configuring the `marketplace` Keycloak realm: confidential backend client `marketplace-backend` with client credentials and redirect URI `http://localhost:8080/api/v1/auth/callback`, Google Identity Provider binding `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` from env, refresh token rotation enabled, access token lifespan 15 minutes, SSO session max 30 days, Token Exchange feature enabled for the realm
- [x] T008 [P] Create `docker-compose.yml` with four services: `postgres` (`postgres:18`, env `POSTGRES_DB=marketplace POSTGRES_USER=admin POSTGRES_PASSWORD=admin`, volume `postgres_data:/var/lib/postgresql/data`, init script mounted from `./docker/postgres/init.sql`, port `5432:5432`); `keycloak` (`quay.io/keycloak/keycloak:26.6.1`, command `start-dev --import-realm`, volume `keycloak_data:/opt/keycloak/data`, realm export mounted to `/opt/keycloak/data/import/realm-export.json`, env reads `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` from `.env`, port `8180:8080`, depends on `postgres`); `backend` (built from `./backend`, env wires datasource + Keycloak JWKS URI from compose network hostnames, port `8080:8080`, depends on `postgres` + `keycloak`); `frontend` (built from `./frontend`, env `NUXT_PUBLIC_API_BASE_URL=http://localhost:8080`, port `3000:3000`, depends on `backend`)
- [x] T009 [P] Create `.env.example` with entries `GOOGLE_CLIENT_ID=` and `GOOGLE_CLIENT_SECRET=` and `NUXT_PUBLIC_GOOGLE_CLIENT_ID=` (no values); add `.env` to `.gitignore` root entry
- [x] T010 [P] Create `backend/Dockerfile` as a multi-stage build: stage 1 uses `gradle:8-jdk25` to run `./gradlew bootJar --no-daemon`; stage 2 uses `eclipse-temurin:25-jre-alpine`, copies the fat JAR, exposes port `8080`, sets `ENTRYPOINT`
- [x] T011 [P] Create `frontend/Dockerfile` as a multi-stage build: stage 1 uses `oven/bun:1` to run `bun install --frozen-lockfile && bun run build`; stage 2 uses `node:22-alpine`, copies `.output/`, exposes port `3000`, sets `ENTRYPOINT ["node", ".output/server/index.mjs"]`
- [x] T012 Add three new parallel jobs to `.github/workflows/pull-request.yml`: `backend-format` (`actions/setup-java@v4` Java 25 Temurin + Gradle cache + `cd backend && ./gradlew spotlessCheck`), `backend-unit` (same setup + `cd backend && ./gradlew test -PexcludeTags=integration`), `backend-integration` (same setup + Docker daemon available + `cd backend && ./gradlew test -PincludeTags=integration`); also extend `.husky/pre-push` to run `cd backend && ./gradlew test -PexcludeTags=integration` when `backend/` files have changed (guards Constitution VIII pre-push enforcement locally)
- [x] T074 Configure JUnit 5 tag-based test split in `backend/build.gradle.kts`: in the existing `tasks.withType<Test>` block add `useJUnitPlatform { excludeTags("integration") }` so the default `./gradlew test` runs only unit tests; add a new `tasks.register<Test>("integrationTest")` task with `useJUnitPlatform { includeTags("integration") }`, `group = "verification"`, and the same JVM/classpath setup; annotate all `@SpringBootTest` integration test classes with `@Tag("integration")`

**Checkpoint**: `docker compose up` starts all four services without errors; `./gradlew spotlessCheck` passes on the clean scaffold; `./gradlew test` runs unit tests only; `./gradlew integrationTest` runs `@Tag("integration")` tests.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core backend infrastructure and frontend auth state — MUST complete before any user story begins.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T013 Create Flyway migration `backend/src/main/resources/db/migration/V2026_04_30T00_00_00__create_users_table.sql` exactly as specified in `data-model.md`: `users` table with `id UUID DEFAULT gen_random_uuid() PRIMARY KEY`, `keycloak_subject VARCHAR(255) UNIQUE NOT NULL`, `email VARCHAR(255) UNIQUE NOT NULL`, `display_name VARCHAR(255)`, `profile_picture_url TEXT`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`; plus indexes `idx_users_keycloak_subject` and `idx_users_email`
- [x] T014 [P] Create `User` JPA entity in `backend/src/main/kotlin/com/marketplace/marketplace_backend/domain/User.kt`: `@Entity @Table(name="users")` open class, `@Id @GeneratedValue(strategy=GenerationType.UUID) val id: UUID?`, all columns mapped with `@Column`, `createdAt`/`updatedAt` as `OffsetDateTime` with `ZoneOffset.UTC`, mutable `var` fields for profile data
- [x] T015 [P] Create `UserRepository` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/repository/UserRepository.kt`: interface extending `JpaRepository<User, UUID>` with `findByKeycloakSubject(subject: String): Optional<User>` and `findByEmail(email: String): Optional<User>`
- [x] T016 Create exception hierarchy in `backend/src/main/kotlin/com/marketplace/marketplace_backend/exception/`: sealed abstract class `MarketplaceException(val errorCode: ErrorCode, vararg val args: Any)` extending `RuntimeException`; `ErrorCode` enum with entries `AUTHENTICATION_FAILED`, `SESSION_EXPIRED`, `GOOGLE_CREDENTIAL_INVALID`, `TOKEN_EXCHANGE_FAILED`, `ACCESS_DENIED`, `VALIDATION_ERROR`, `INTERNAL_ERROR` — each carrying `httpStatus: HttpStatus`, `typeUriSuffix: String`, `titleKey: String`, `detailKey: String`; concrete subclasses `AuthException`, `SessionExpiredException`, `GoogleCredentialInvalidException`, `TokenExchangeFailedException`
- [x] T017 Create `GlobalExceptionHandler` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/exception/GlobalExceptionHandler.kt`: `@RestControllerAdvice`; inject `MessageSource`; for each `MarketplaceException` generate a `UUID` incident ID, build `ProblemDetail` with `type`, `title` (from `MessageSource`), `status`, `detail` (from `MessageSource` with args), `instance = URI("urn:uuid:$incidentId")`, plus a custom `timestamp` property; log the incident ID + exception at ERROR level; handle Spring's `MethodArgumentNotValidException` and generic `Exception` with fallback
- [x] T018 [P] Create `backend/src/main/resources/messages.properties` and `messages_en.properties` with i18n keys for all `ErrorCode` entries: `error.authentication-failed.title`, `error.authentication-failed.detail`, `error.session-expired.title`, `error.session-expired.detail`, `error.google-credential-invalid.title`, `error.google-credential-invalid.detail`, `error.token-exchange-failed.title`, `error.token-exchange-failed.detail`, `error.access-denied.title`, `error.access-denied.detail`, `error.validation-error.title`, `error.validation-error.detail`, `error.internal-error.title`, `error.internal-error.detail`
- [x] T019 Create `CookieTokenFilter` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/config/CookieTokenFilter.kt` extending `OncePerRequestFilter`: reads `marketplace_access_token` cookie from the incoming `HttpServletRequest`; if present, wraps the request with a `HttpServletRequestWrapper` that overrides `getHeader("Authorization")` to return `"Bearer $cookieValue"`; passes the wrapped request down the filter chain
- [x] T020 Create `SecurityConfig` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/config/SecurityConfig.kt`: `@Configuration @EnableWebSecurity` open class; `securityFilterChain` bean adds `CookieTokenFilter` before `BearerTokenAuthenticationFilter`; permits `GET /api/v1/auth/login`, `GET /api/v1/auth/callback`, `POST /api/v1/auth/one-tap`, plus SpringDoc paths (`/api/v1/api-docs/**`, `/api/v1/swagger-ui/**`, `/api/v1/swagger-ui.html`) without auth; requires authentication for all other `/api/v1/**`; configures `oauth2ResourceServer { jwt { jwkSetUri(keycloakJwksUri) } }`; disables session creation (`STATELESS`)
- [x] T021 [P] Create `SecurityConfigLocal` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/config/SecurityConfigLocal.kt`: `@Profile("local") @Configuration` class with a higher-priority `securityFilterChain` bean that permits all requests to `/api/v1/api-docs/**`, `/api/v1/swagger-ui/**`, and `/api/v1/swagger-ui.html` without any token requirement, while delegating all other paths to the parent configuration rules
- [x] T022 [P] Create `WebMvcConfig` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/config/WebMvcConfig.kt`: `@Configuration` implementing `WebMvcConfigurer`; CORS mapping for `/**` allowing origin `http://localhost:3000` (configurable via property), methods GET/POST/PUT/DELETE/OPTIONS, all headers, `allowCredentials = true`
- [x] T023 [P] Create `OpenApiConfig` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/config/OpenApiConfig.kt`: `@Configuration` producing an `OpenAPI` bean with title "Marketplace API", version "1.0.0", description; adds `CookieSecurityScheme` named `cookieAuth` using `ApiKey` in `COOKIE` location pointing to `marketplace_access_token`; applies to all secured operations via a global `SecurityRequirement`
- [x] T024 Create `KeycloakClient` interface and `KeycloakClientImpl` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/infrastructure/keycloak/`: interface declares `exchangeAuthCode(code: String, redirectUri: String): KeycloakTokenResponse`, `refreshToken(refreshToken: String): KeycloakTokenResponse`, `revokeSession(accessToken: String)`, `exchangeGoogleToken(googleIdToken: String): KeycloakTokenResponse`; also define `KeycloakTokenResponse` data class (`accessToken`, `refreshToken`, `expiresIn`, `refreshExpiresIn`); `KeycloakClientImpl` injects Spring's `RestClient` (configured with Keycloak base URL from properties) and posts to `/realms/marketplace/protocol/openid-connect/token` with appropriate form params; throws `TokenExchangeFailedException` on non-2xx responses
- [x] T025 Create `UserService` interface and `UserServiceImpl` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/service/`: interface declares `upsertFromTokenClaims(claims: Map<String, Any>): User` and `findByKeycloakSubject(subject: String): User`; `UserServiceImpl` injects `UserRepository`; `upsertFromTokenClaims` extracts `sub`, `email`, `name`, `picture` claims, calls `findByKeycloakSubject` and either creates a new `User` or updates `email`, `displayName`, `profilePictureUrl` and `updatedAt` on the existing one, then saves; `findByKeycloakSubject` throws `AuthException(AUTHENTICATION_FAILED)` if not found
- [x] T026 Update `backend/src/test/kotlin/com/marketplace/marketplace_backend/TestcontainersConfiguration.kt` to declare a `@Bean` providing a `PostgreSQLContainer("postgres:18")` shared across all integration tests; ensure `TestMarketplaceBackendApplication.kt` uses this configuration so all `@SpringBootTest` integration tests use the container's JDBC URL via dynamic property registration
- [x] T027 [P] Create `UserResponseDto` data class in `backend/src/main/kotlin/com/marketplace/marketplace_backend/web/dto/response/UserResponseDto.kt` with fields `id: UUID`, `email: String`, `displayName: String?`, `profilePictureUrl: String?`; add a static `from(user: User): UserResponseDto` companion function
- [x] T028 [P] Create `UserApi` interface in `backend/src/main/kotlin/com/marketplace/marketplace_backend/web/api/UserApi.kt` with `@Tag(name = "Users")` and `@RequestMapping("/api/v1/users")`; declare `@GetMapping("/me")` with `@Operation(summary = "Get authenticated user profile")` and `@ApiResponse(responseCode = "200")` / `@ApiResponse(responseCode = "401")`
- [x] T029 Create `UserController` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/web/controller/UserController.kt` implementing `UserApi`: inject `UserService`; `getMe()` extracts `keycloakSubject` from `SecurityContextHolder` JWT principal, calls `userService.findByKeycloakSubject(subject)`, returns `UserResponseDto.from(user)` — zero business logic in the controller
- [x] T030 [P] Create `UserProfile` TypeScript interface in `frontend/app/types/auth.ts` with fields `id: string`, `email: string`, `displayName: string | null`, `profilePictureUrl: string | null` — mirrors `UserResponseDto`; export and use this type in all composables and the auth store
- [x] T076 [P] Create `useAuthStore` Pinia store in `frontend/app/stores/useAuthStore.ts`: state `user: UserProfile | null` and `authError: string | null`; getters `isAuthenticated`, `displayName` (falls back to email local-part if no name), `initials` (up to 2 uppercase letters from displayName words, or first letter of email local-part, or empty string), `profilePictureUrl`; actions `setUser(u: UserProfile)`, `clearUser()`, `setError(msg: string)`, `clearError()`, `fetchCurrentUser()` (calls `$fetch('/api/v1/users/me')` with `credentials: 'include'`; on success calls `setUser`; on 401 calls `clearUser`; on other error logs and calls `clearUser`)
- [x] T078 [P] Add auth UI string keys to `frontend/i18n/locales/en.json` (Constitution VII — strings MUST be externalised before any UI component references them): add `auth.login.title` ("Sign in to Marketplace"), `auth.login.button` ("Login with Google"), `auth.login.subtitle`; `auth.menu.welcome` ("Welcome, {name}"), `auth.menu.profile` ("Profile"), `auth.menu.logout` ("Log out"), `auth.menu.login` ("Login with Google"); `auth.profile.welcome` ("Welcome, {name}"), `auth.profile.subline`; **T041, T052, and T057 MUST NOT begin until this task is complete**

**Checkpoint**: Backend compiles and `./gradlew test` passes with the Testcontainers Postgres; `useAuthStore.fetchCurrentUser()` in browser correctly resolves to null when no cookie is present; `en.json` contains all auth UI string keys and `bun run build` succeeds.

---

## Phase 3: User Story 1 — Explicit Google Login via Button (Priority: P1) 🎯 MVP

**Goal**: A user can click "Login with Google" on `/login`, complete the Google auth flow via Keycloak, and land on `/profile` with their name shown.

**Independent Test**: Visit `http://localhost:3000/login`, click "Login with Google", complete Google sign-in, verify redirect to `/profile` with welcome message; also verify that visiting `/login` while already authenticated redirects straight to `/profile`.

- [x] T031 [P] [US1] Create `OneTapRequestDto` data class in `backend/src/main/kotlin/com/marketplace/marketplace_backend/web/dto/request/OneTapRequestDto.kt` with `@field:NotBlank val credential: String`
- [x] T032 [US1] Create `AuthService` interface in `backend/src/main/kotlin/com/marketplace/marketplace_backend/service/AuthService.kt` with: `buildAuthorizationRedirectUrl(state: String): String`, `handleCallback(code: String, state: String, response: HttpServletResponse): Unit`, `handleOneTap(credential: String, response: HttpServletResponse): UserResponseDto`, `refresh(refreshToken: String, response: HttpServletResponse): Unit`, `logout(accessToken: String, response: HttpServletResponse): Unit`
- [x] T033 [US1] Create `AuthServiceImpl` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/service/AuthServiceImpl.kt` implementing `AuthService`: inject `KeycloakClient`, `UserService`, and `@Value("\${app.frontend.base-url}") val frontendBaseUrl: String`; `buildAuthorizationRedirectUrl` constructs Keycloak's OIDC authorization URL with `response_type=code`, `client_id`, `redirect_uri`, `scope=openid email profile`, `state`; `handleCallback` first validates that the `state` parameter matches the CSRF state cookie (throw `AuthException(AUTHENTICATION_FAILED)` if mismatch — this is a security rule, not controller logic), then calls `keycloakClient.exchangeAuthCode`, then `userService.upsertFromTokenClaims` with the JWT claims decoded from the access token, then sets both HTTP-only cookies on `HttpServletResponse` with the correct `Path`, `MaxAge`, `Secure`, `SameSite` attributes, and returns `"$frontendBaseUrl/profile"` as the redirect target; `logout` calls `keycloakClient.revokeSession`, then clears both cookies by setting `MaxAge=0`
- [x] T034 [P] [US1] Create `AuthApi` interface in `backend/src/main/kotlin/com/marketplace/marketplace_backend/web/api/AuthApi.kt` with `@Tag(name = "Auth")` and `@RequestMapping("/api/v1/auth")`; declare all five endpoints with `@Operation`, `@ApiResponse` annotations: `GET /login` (302 redirect), `GET /callback` (302 redirect), `POST /one-tap` (200 `UserResponseDto`), `POST /refresh` (204), `POST /logout` (204)
- [x] T035 [US1] Create `AuthController` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/web/controller/AuthController.kt` implementing `AuthApi`: inject `AuthService`; `login()` generates a CSRF-safe `state` UUID, stores it in a short-lived `Same-Site=Lax` cookie, calls `authService.buildAuthorizationRedirectUrl(state)`, returns `ResponseEntity.status(302).location(URI(url)).build()`; `callback(code, state, request, response)` passes `code`, `state`, and the state cookie value straight to `authService.handleCallback` (state validation is in the service); `oneTap(body, response)` delegates to `authService.handleOneTap`; `refresh(request, response)` reads the refresh cookie and delegates to `authService.refresh`; `logout(request, response)` reads the access token cookie and delegates to `authService.logout` — zero business logic in the controller
- [x] T036 [P] [US1] Unit test `AuthServiceImpl` (login + callback) in `backend/src/test/kotlin/com/marketplace/marketplace_backend/unit/service/AuthServiceImplTest.kt`: mock `KeycloakClient` and `UserService` with Mockito; verify `buildAuthorizationRedirectUrl` produces a URL with correct query params; verify `handleCallback` calls `exchangeAuthCode`, then `upsertFromTokenClaims`, then sets two cookies on the mock `HttpServletResponse`; verify `logout` calls `revokeSession` and clears cookies
- [x] T037 [P] [US1] Unit test `UserServiceImpl` in `backend/src/test/kotlin/com/marketplace/marketplace_backend/unit/service/UserServiceImplTest.kt`: verify `upsertFromTokenClaims` creates a new User when none exists; verify it updates mutable fields (email, displayName, profilePictureUrl) on an existing User; verify `findByKeycloakSubject` throws `AuthException` when user not found
- [x] T038 [US1] Integration test for the auth callback flow in `backend/src/test/kotlin/com/marketplace/marketplace_backend/integration/auth/AuthFlowIntegrationTest.kt` (annotate with `@Tag("integration")`): use `@SpringBootTest(webEnvironment=RANDOM_PORT)` with TestContainers Postgres; mock `KeycloakClient` as a `@MockitoBean`; call `GET /api/v1/auth/callback?code=xxx&state=yyy` with a matching CSRF state cookie; assert the response is a `302` redirect to `http://localhost:3000/profile` (the configured `app.frontend.base-url` + `/profile`); assert `Set-Cookie` headers contain both `marketplace_access_token` and `marketplace_refresh_token` as HttpOnly; also assert a mismatched state cookie returns `401`
- [x] T039 [P] [US1] Integration test for `GET /api/v1/users/me` in `backend/src/test/kotlin/com/marketplace/marketplace_backend/integration/user/UserMeIntegrationTest.kt` (annotate with `@Tag("integration")`): with a valid JWT cookie (mock or self-signed for tests), assert `200 OK` with correct JSON body; without a cookie, assert `401` with RFC 7807 body containing `instance` matching `urn:uuid:` pattern
- [x] T040 [P] [US1] Create `useAuth` composable in `frontend/app/composables/useAuth.ts` with: `login()` that sets `window.location.href = '/api/v1/auth/login'`; `logout()` that calls `$fetch('/api/v1/auth/logout', { method: 'POST', credentials: 'include' })` then calls `useAuthStore().clearUser()` and navigates to `/`; `refreshSession()` that calls `$fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'include' })`
- [x] T041 [US1] Create `/login` page in `frontend/app/pages/login.vue`: on mount check `useAuthStore().isAuthenticated`; if true, redirect to `/profile`; otherwise render a mobile-first centered card with the site logo, a "Login with Google" `<Button>` that calls `useAuth().login()`, and a placeholder `<AuthErrorBanner>` (wired in US6); use `useI18n().t()` for all displayed strings from the keys added in T078 (`auth.login.title`, `auth.login.button`, `auth.login.subtitle`) — no hardcoded user-facing strings (Constitution VII)
- [x] T042 [US1] Create stub `/profile` page in `frontend/app/pages/profile.vue` (to be fully implemented in US4): add `definePageMeta({ middleware: 'auth' })` **immediately** so unauthenticated visitors are redirected to `/login` even at this stub stage (this is required for SC-002 and prevents the stub being publicly accessible during development of US2/US3); display "Welcome, [displayName]" using `useAuthStore().displayName`
- [x] T075 [P] [US1] E2E test for the button login golden path in `frontend/tests/e2e/auth/login.spec.ts` (Playwright): mock or stub the backend auth endpoints using Playwright's route interception; navigate to `/login`, click "Login with Google", assert redirect to `/profile` and presence of "Welcome" text; also assert that navigating directly to `/profile` while unauthenticated redirects to `/login`

**Checkpoint**: End-to-end button login flow works: `/login` → Keycloak → `/profile` with name shown. Unauthenticated access to `/profile` redirects immediately. Backend unit and integration tests pass.

---

## Phase 4: User Story 2 — One Tap Auto-Prompt (Priority: P2)

**Goal**: Unauthenticated visitors with an active Google session automatically see the One Tap popup; accepting it logs them in and redirects to `/profile`.

**Independent Test**: Log out from the app (but stay logged in to Google in the browser). Navigate to `http://localhost:3000`. Verify the One Tap popup appears. Accept it. Verify redirect to `/profile`. Dismiss it and verify it disappears without breaking navigation.

- [x] T043 [US2] Extend `AuthServiceImpl` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/service/AuthServiceImpl.kt` with `handleOneTap`: inject `@Value("\${google.jwks-uri}") val googleJwksUri: String` (defined in T004, overridable in `application-test.properties` with a WireMock URL for unit/integration tests); use this URI to fetch Google's JWKS and validate the credential JWT signature, `aud` (must match `GOOGLE_CLIENT_ID`), and `exp`; on validation success call `keycloakClient.exchangeGoogleToken(credential)` to get Keycloak tokens; then `userService.upsertFromTokenClaims` and set cookies; throw `GoogleCredentialInvalidException` on validation failure; throw `TokenExchangeFailedException` on Keycloak exchange failure
- [x] T044 [US2] Implement `POST /api/v1/auth/one-tap` in `AuthController`: delegate `@RequestBody @Valid OneTapRequestDto` to `authService.handleOneTap`; return `ResponseEntity.ok(userResponseDto)` on success (200 with user body, cookies set by the service via `HttpServletResponse`)
- [x] T045 [P] [US2] Create Nuxt plugin `frontend/app/plugins/google-gsi.client.ts`: use `useHead` to inject the `https://accounts.google.com/gsi/client` script tag with `async defer`; export nothing (GSI attaches to `window.google`)
- [x] T046 [US2] Create `useGoogleOneTap` composable in `frontend/app/composables/useGoogleOneTap.ts`: use a module-level `let initialized = false` guard so `google.accounts.id.initialize()` is called **at most once per page load** regardless of how many callers invoke the composable (prevents double-initialization from both `app.vue` and `login.vue`); on first call, wait for `window.google` to be available, then call `google.accounts.id.initialize({ client_id: config.public.googleClientId, callback: handleCredentialResponse, auto_select: true, cancel_on_tap_outside: false })` and set `initialized = true`; `handleCredentialResponse` POSTs `credential` to `/api/v1/auth/one-tap` with `credentials: 'include'`; on success calls `useAuthStore().setUser(response)` and navigates to `/profile`; on failure calls `useAuthStore().setError(...)` (see US6); expose a `prompt()` function that calls `google.accounts.id.prompt()` — this may be called multiple times safely
- [x] T047 [US2] Update `frontend/app/app.vue`: after `useAuthStore().fetchCurrentUser()` resolves and the user is NOT authenticated, call `useGoogleOneTap().prompt()` — ensures One Tap fires on any page for eligible visitors without disturbing authenticated users
- [x] T048 [P] [US2] Update `/login` page `frontend/app/pages/login.vue` to also call `useGoogleOneTap().prompt()` on mount when the user is not authenticated, so the One Tap popup appears on the dedicated login page alongside the button
- [x] T049 [P] [US2] Add `NUXT_PUBLIC_GOOGLE_CLIENT_ID` key to `.env.example` and to `nuxt.config.ts` under `runtimeConfig.public.googleClientId` so the composable can read it from the environment
- [x] T050 [P] [US2] Unit test One Tap flow in `backend/src/test/kotlin/com/marketplace/marketplace_backend/unit/service/AuthServiceImplTest.kt`: mock Google JWKS validation (inject a test-double or spy); verify valid credential calls `exchangeGoogleToken` + `upsertFromTokenClaims` + sets cookies; verify invalid credential throws `GoogleCredentialInvalidException`; verify Keycloak exchange failure throws `TokenExchangeFailedException`
- [x] T051 [US2] Integration test for `POST /api/v1/auth/one-tap` in `backend/src/test/kotlin/com/marketplace/marketplace_backend/integration/auth/OneTapIntegrationTest.kt` (annotate with `@Tag("integration")`): set `google.jwks-uri` in `application-test.properties` to a WireMock stub URL so Google JWKS validation can be controlled; mock `KeycloakClient` as `@MockitoBean`; POST a syntactically valid but unsigned credential; assert `401` with `google-credential-invalid` type; POST with WireMock returning a valid JWKS and `KeycloakClient` mock returning tokens; assert `200` with cookies set

**Checkpoint**: Visiting the app while authenticated with Google triggers the One Tap popup. Accepting it logs in and redirects to `/profile`.

---

## Phase 5: User Story 3 — Navigation Header Context Menu (Priority: P3)

**Goal**: All pages show a top-right context menu: authenticated users see their avatar/initials, "Welcome, [name]", profile link, and logout; unauthenticated users see a "Login with Google" option.

**Independent Test**: Log in and verify the avatar (or initials badge) and "Welcome, [name]" appear in the top-right across multiple pages. Log out and verify the menu changes to the "Login with Google" option. Use a Google account without a photo to verify initials display.

- [x] T052 [US3] Create `UserMenu.vue` in `frontend/app/components/shared/UserMenu.vue`: reads `useAuthStore()`; when authenticated, renders a `<DropdownMenu>` trigger showing `<Avatar>` (profile picture) or `<AvatarFallback>` (initials); dropdown content shows "Welcome, [displayName]", a `<NuxtLink to="/profile">` item, and a "Log out" `<DropdownMenuItem>` that calls `useAuth().logout()`; when unauthenticated, renders a plain button/link "Login with Google" that calls `useAuth().login()`; all state is reactive to store changes; use `useI18n().t()` for all displayed strings from the keys added in T078 (`auth.menu.welcome`, `auth.menu.profile`, `auth.menu.logout`, `auth.menu.login`) — no hardcoded user-facing strings (Constitution VII)
- [x] T053 [US3] Update `frontend/app/layouts/default.vue`: add a `<header>` bar with the site name/logo on the left and `<UserMenu>` on the right; apply `flex justify-between items-center` layout so the header works on all viewport sizes (mobile-first)
- [x] T054 [P] [US3] Verify initials computation edge cases in `useAuthStore` (`frontend/app/stores/useAuthStore.ts`): "Jane Smith" → "JS", "Madonna" → "M", null displayName with `user@example.com` → "U", empty string → empty string (generic icon fallback in template)
- [x] T055 [P] [US3] Unit test `UserMenu.vue` in Vitest (`frontend/tests/unit/components/UserMenu.test.ts`): render with authenticated store state (assert avatar/initials visible, dropdown items present, "Welcome, Jane" text, logout calls `useAuth().logout`); render with unauthenticated state (assert login option visible, no avatar); verify logout triggers store `clearUser` and navigation

**Checkpoint**: The context menu appears on all pages. Avatar/initials display correctly for users with and without profile photos. Logout ends the session and updates the menu reactively.

---

## Phase 6: User Story 4 — Protected Profile Page (Priority: P4)

**Goal**: `/profile` shows a personalized welcome to authenticated users; unauthenticated visitors are redirected to `/login`.

**Independent Test**: Navigate to `http://localhost:3000/profile` while logged out → verify redirect to `/login`. Log in → visit `/profile` → verify "Welcome, [name]" message. After login from a deep link (`/profile`), verify the redirect lands back on `/profile` (not the generic post-login target).

- [x] T056 [US4] Create Nuxt route middleware `frontend/app/middleware/auth.ts`: export default `defineNuxtRouteMiddleware(async (to) => { const store = useAuthStore(); if (!store.isAuthenticated) { await store.fetchCurrentUser(); } if (!store.isAuthenticated) { return navigateTo('/login'); } })`; this handles both soft navigations and hard-refresh direct URL access
- [x] T057 [P] [US4] Fully implement `/profile` page in `frontend/app/pages/profile.vue`: add `definePageMeta({ middleware: 'auth' })`; display a mobile-first welcome section with `<Avatar>` (or initials), "Welcome, [displayName]" heading, and the user's email in a subline; replace the stub created in T042; use `useI18n().t()` for all displayed strings from the keys added in T078 (`auth.profile.welcome`, `auth.profile.subline`) — no hardcoded user-facing strings (Constitution VII)
- [x] T058 [P] [US4] Update `/login` page `frontend/app/pages/login.vue` to capture the `redirect` query parameter on mount (e.g., `/login?redirect=/profile`) and after successful login (button or One Tap) navigate to that path instead of the default `/profile`; this supports the "redirect after login" pattern required by the auth middleware

**Checkpoint**: `/profile` is completely inaccessible to unauthenticated visitors at the route level. Authenticated users see their personalised content. The redirect-back-after-login path works.

---

## Phase 7: User Story 5 — Persistent Session & Token Expiry (Priority: P5)

**Goal**: Sessions persist across browser restarts (up to 30 days). Revoked tokens are detected and the user is redirected to `/login` with a session-expired notification.

**Independent Test**: Log in, close and reopen the browser, visit the app → verify still logged in. In Keycloak Admin Console, revoke all sessions for the test user, then navigate to any page → verify redirect to `/login` with the "session expired" banner.

- [x] T059 [US5] Extend `AuthServiceImpl` in `backend/src/main/kotlin/com/marketplace/marketplace_backend/service/AuthServiceImpl.kt` with `refresh`: reads the refresh token string, calls `keycloakClient.refreshToken`; on success sets new `marketplace_access_token` and `marketplace_refresh_token` cookies (rotating refresh); on Keycloak error (revoked/expired), throws `SessionExpiredException` which the `GlobalExceptionHandler` maps to `401` with `type=session-expired`
- [x] T060 [US5] Implement `POST /api/v1/auth/refresh` in `AuthController`: read `marketplace_refresh_token` cookie from `HttpServletRequest`; if missing, throw `SessionExpiredException()` (consistent with T059; both map to `401 session-expired` via `GlobalExceptionHandler`); delegate to `authService.refresh`; return `204 No Content` on success
- [x] T061 [US5] Create `frontend/app/plugins/auth-interceptor.client.ts` as a Nuxt plugin: wrap `$fetch` globally (using `useFetch`/`ofetch` interceptors); on `401` response, attempt `useAuth().refreshSession()`; if refresh succeeds, retry the original request once; if refresh also returns `401`, call `useAuth().handleSessionExpired()` which clears the auth store and navigates to `/login?sessionExpired=true`
- [x] T062 [P] [US5] Add `handleSessionExpired()` to `useAuth` composable in `frontend/app/composables/useAuth.ts`: calls `useAuthStore().clearUser()`, then navigates to `/login?sessionExpired=true`
- [x] T063 [P] [US5] Integration test for token rotation in `backend/src/test/kotlin/com/marketplace/marketplace_backend/integration/auth/TokenRefreshIntegrationTest.kt` (annotate with `@Tag("integration")`): mock `KeycloakClient` returning new tokens on refresh → assert `204` and two new `Set-Cookie` headers; mock `KeycloakClient` throwing on refresh → assert `401` with `session-expired` Problem Details body and `instance` field matching `urn:uuid:` pattern; also assert missing refresh cookie → `401 session-expired`

**Checkpoint**: Returning authenticated users are not prompted to log in again. Revoked sessions trigger the redirect and notification. Token rotation is transparent to the user.

---

## Phase 8: User Story 6 — Login Error Handling (Priority: P6)

**Goal**: Any error during the Google login flow shows a visible, dismissible error banner with an actionable message.

**Independent Test**: Simulate an OAuth error (e.g., deny Google permission, or trigger a Keycloak error). Verify the error banner appears on `/login` within 3 seconds without a page reload. Verify dismissing the banner hides it and allows retrying.

- [x] T064 [P] [US6] Create `AuthErrorBanner.vue` in `frontend/app/components/shared/AuthErrorBanner.vue`: accepts `message: string` prop; renders a dismissible banner using shadcn-vue `Alert` (variant `destructive`); includes "Please try again." suffix; emits `dismiss` event; shows only when `message` is non-empty
- [x] T065 [US6] Update `/login` page `frontend/app/pages/login.vue`: on mount read `error` query param; map known error codes (`auth_failed`, `google_credential_invalid`, `token_exchange_failed`) to i18n keys via `useI18n().t()`; also map `sessionExpired` query param (from US5) to its i18n key; bind the resolved string to `<AuthErrorBanner :message="errorMessage" @dismiss="errorMessage = ''" />`
- [x] T066 [US6] Update `useGoogleOneTap` composable in `frontend/app/composables/useGoogleOneTap.ts`: in `handleCredentialResponse`, on `POST /api/v1/auth/one-tap` failure, call `useAuthStore().setError(t('auth.error.google_credential_invalid'))` so the login page can reactively display the banner without a redirect
- [x] T067 [P] [US6] Update `/login` page to also watch `useAuthStore().authError` and display it in `<AuthErrorBanner>` (covers One Tap errors that don't change the route); clear the store error on banner dismiss
- [x] T068 [P] [US6] Add auth error i18n keys to `frontend/i18n/locales/en.json`: `auth.error.auth_failed`, `auth.error.session_expired`, `auth.error.google_credential_invalid`, `auth.error.token_exchange_failed`, `auth.error.generic`, each with natural-language values plus "Please try again." embedded

**Checkpoint**: Every login failure scenario (denied permission, network error, expired One Tap credential, revoked session) shows the banner within 3 seconds. The banner is dismissible. The user can retry without a full page reload.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Final quality, style, and documentation pass across all stories.

- [x] T069 [P] Review mobile-first styling on `/login`, `/profile`, and `UserMenu.vue` using Tailwind responsive breakpoints (`sm:`, `md:`); test at 375px, 768px, and 1280px viewport widths; fix any layout overflow or unreadable text
- [x] T070 [P] Run `cd backend && ./gradlew spotlessApply` to auto-format all Kotlin source files; commit any changes as a standalone formatting commit
- [x] T071 [P] Run `./gradlew check` in `backend/` (unit + integration + spotless) and `bun run test:unit && bun run test:integration` in `frontend/`; fix any failures
- [ ] T072 Run the full quickstart.md end-to-end: `docker compose up`, complete button login flow, verify `/profile` content, revoke session in Keycloak Admin Console, verify the expired-session redirect and banner; update `quickstart.md` with any corrected commands or outputs discovered during this run
- [x] T073 [P] Update `CONTRIBUTING.md` with a "Local Auth Setup" section covering: Google Cloud Console credential creation, `.env` file setup, Keycloak realm import command, and how to run the manual quickstart verification
- [x] T077 [P] Audit auth-related frontend components for Constitution VII compliance: grep `frontend/app/pages/login.vue`, `frontend/app/components/shared/UserMenu.vue`, and `frontend/app/pages/profile.vue` for any hardcoded user-facing strings (quoted English text not wrapped in `useI18n().t()`); fix any found; verify all `auth.*` keys defined in T078 are present in `en.json` and are actually used via `t()` calls in the components — this is a verification pass, not the initial externalization (which was done in T078/T041/T052/T057)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 completion — BLOCKS all user stories
- **Phases 3–8 (User Stories)**: All depend on Phase 2; can proceed in priority order or in parallel if team size allows
- **Phase 9 (Polish)**: Depends on all desired user stories being complete

### User Story Dependencies

| Story    | Depends On                                     | Can Start After                                               |
| -------- | ---------------------------------------------- | ------------------------------------------------------------- |
| US1 (P1) | Phase 2                                        | T076 (useAuthStore) + T078 (i18n keys — required before T041) |
| US2 (P2) | US1 (needs auth cookies + /one-tap)            | T042                                                          |
| US3 (P3) | US1 (needs useAuthStore + useAuth)             | T042                                                          |
| US4 (P4) | US1 (needs /profile stub + auth middleware)    | T042                                                          |
| US5 (P5) | US1 (needs auth cookie infrastructure)         | T042                                                          |
| US6 (P6) | US1 (needs /login page) + US2 (One Tap errors) | T048                                                          |

### Within Each User Story

- DTOs and interfaces [P] → Service implementation → Controller → Tests [P]
- Frontend composables [P] → Pages → Layout updates
- Backend and frontend tasks within a story are independent [P]

### Parallel Opportunities

**Phase 1** — T003, T005, T009, T010, T011 can all run in parallel with T002/T004/T006–T008.
**Phase 2** — T014+T015 (entity + repo), T018 (backend i18n), T021 (local security), T022 (CORS), T023 (OpenAPI), T027+T028 (DTOs + UserApi), T030 (UserProfile type) + T076 (auth store) + T078 (frontend auth UI i18n keys) all run in parallel after T013 (migration must exist first).
**Phase 3** — T031+T034 (DTOs + AuthApi), T036+T037 (unit tests), T039+T040 (integration test + useAuth composable) all run in parallel.

---

## Parallel Example: User Story 1

```bash
# In parallel after T030:
Task T031: "Create OneTapRequestDto in .../dto/request/OneTapRequestDto.kt"
Task T034: "Create AuthApi interface in .../web/api/AuthApi.kt"

# Then in parallel after T033 (AuthServiceImpl):
Task T036: "Unit test AuthServiceImpl in .../unit/service/AuthServiceImplTest.kt"
Task T037: "Unit test UserServiceImpl in .../unit/service/UserServiceImplTest.kt"
Task T039: "Integration test GET /api/v1/users/me in .../integration/user/UserMeIntegrationTest.kt"
Task T040: "Create useAuth composable in frontend/app/composables/useAuth.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete **Phase 1** (Setup) — `docker compose up` works
2. Complete **Phase 2** (Foundational) — DB, security, KeycloakClient, UserService
3. Complete **Phase 3** (US1) — button login, callback, `/login` page, `/profile` stub
4. **STOP and VALIDATE**: Full button login flow end-to-end
5. Demo or continue to US2

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Working login button → **Demo 1**
3. US2 → One Tap auto-prompt added → **Demo 2**
4. US3 → Persistent header context menu → **Demo 3**
5. US4 → Fully protected `/profile` + redirect → **Demo 4**
6. US5 → Session persistence + revocation handling → **Demo 5**
7. US6 → Full error handling on login page → **Demo 6 (Complete)**

### Parallel Team Strategy

After Phase 2:

- **Developer A**: US1 (button login end-to-end)
- **Developer B**: US3 (UserMenu component, can mock auth store)
- After US1 merges: **Developer A**: US2 (One Tap), **Developer B**: US4 (profile protection)

---

## Notes

- `[P]` tasks touch different files and have no dependency on in-progress tasks — they can run concurrently
- Each user story phase should be committed as a logical unit before moving on
- Run `./gradlew spotlessCheck` before committing any Kotlin changes
- The Keycloak realm export (`T007`) is critical — an incorrect configuration blocks all auth flows; validate it early
- All `@SpringBootTest` integration test classes **must** be annotated with `@Tag("integration")` (configured in T074) so `./gradlew test` (unit only) and `./gradlew integrationTest` (integration only) split correctly
- TestContainers integration tests require Docker to be running on the developer's machine
- The `local` Spring profile (T021) is the recommended way to run the backend during frontend development
- SC-002 ("100% of unauthenticated requests to `/profile` are redirected") is fully verifiable only after T042 (auth middleware added to stub) — the stub added in T042 now includes `definePageMeta({ middleware: 'auth' })` to prevent a false-passing window
