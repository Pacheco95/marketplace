---

# Quickstart: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30

## Prerequisites

| Tool                         | Minimum Version | Install                             |
| ---------------------------- | --------------- | ----------------------------------- |
| Docker + Docker Compose      | Docker 27+      | https://docs.docker.com/get-docker/ |
| Java (OpenJDK)               | 25              | `sdk install java 25-open` (SDKMAN) |
| Bun                          | 1.1+            | https://bun.sh                      |
| Google Cloud Console account | —               | For OAuth credentials               |

## One-Time Setup

### 1. Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials
2. Create an **OAuth 2.0 Client ID** (Web application type)
3. Add Authorised JavaScript origins: `http://localhost:3000`
4. Add Authorised redirect URIs: `http://localhost:8180/realms/marketplace/broker/google/endpoint`
5. Copy the **Client ID** and **Client Secret**

### 2. Environment File

```bash
cp .env.example .env
```

Open `.env` and fill in:

```
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
```

All other values in `.env.example` are pre-filled for local development (admin/admin for Keycloak and Postgres).

### 3. Start Infrastructure

```bash
docker compose up -d postgres keycloak
```

Wait for Keycloak to be healthy (check with `docker compose ps`). This usually takes 30–60 seconds on first run.

### 4. Configure Keycloak

The implementation tasks include a Keycloak realm import file (`docker/keycloak/realm-export.json`) that pre-configures:

- The `marketplace` realm
- A confidential client for the backend (`marketplace-backend`)
- Google as an Identity Provider (reads `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` from Keycloak's environment)
- Refresh token rotation enabled
- Session max lifetime: 30 days

To import:

```bash
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/realm-export.json
```

Or open `http://localhost:8180` → Admin Console (admin/admin) → Import realm.

### 5. Run the Full Stack

```bash
docker compose up
```

This starts all four services: `postgres`, `keycloak`, `backend`, `frontend`.

Alternatively, run backend and frontend locally for faster iteration:

**Backend** (in `backend/`):

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Frontend** (in `frontend/`):

```bash
bun run dev
```

## URLs

| Service        | URL                                          | Credentials                    |
| -------------- | -------------------------------------------- | ------------------------------ |
| Frontend       | http://localhost:3000                        | —                              |
| Backend API    | http://localhost:8080/api/v1                 | —                              |
| Swagger UI     | http://localhost:8080/api/v1/swagger-ui.html | No auth (local profile)        |
| OpenAPI JSON   | http://localhost:8080/api/v1/api-docs        | —                              |
| Keycloak Admin | http://localhost:8180                        | admin / admin                  |
| PostgreSQL     | localhost:5432                               | admin / admin, db: marketplace |

## Running Tests

### Backend

```bash
# Unit tests only
cd backend && ./gradlew test

# Integration tests (requires Docker for TestContainers)
cd backend && ./gradlew integrationTest

# Format check
cd backend && ./gradlew spotlessCheck

# Format (apply)
cd backend && ./gradlew spotlessApply

# All checks
cd backend && ./gradlew check
```

### Frontend

```bash
# Unit tests
bun run test:unit

# Integration tests
bun run test:integration

# E2E tests (requires running app)
bun run test:e2e
```

## Local Profile

The backend's `local` Spring profile:

- Disables authentication on the Swagger UI (`/api/v1/swagger-ui.html`) and the OpenAPI JSON endpoint (`/api/v1/api-docs`)
- Sets `Secure=false` on cookies (HTTP instead of HTTPS)
- Enables more verbose logging

Activate with: `--spring.profiles.active=local`

## Verifying the Login Flow

### Button Flow

1. Open http://localhost:3000/login
2. Click "Login with Google"
3. Complete Google sign-in
4. Verify redirect to http://localhost:3000/profile with your name displayed

### One Tap Flow

1. Log out from the application (but stay logged into Google in the browser)
2. Navigate to http://localhost:3000
3. Verify the One Tap popup appears in the top-right corner
4. Accept it and verify redirect to `/profile`

### Token Refresh

1. Log in
2. Open browser dev tools → Application → Cookies
3. Note `marketplace_access_token` is HttpOnly (not visible in JS)
4. Wait 15 minutes or manually expire the token in Keycloak Admin Console
5. Make any authenticated request — the frontend should transparently refresh the token

### Revocation (Session Expired Flow)

1. Log in
2. In Keycloak Admin Console → Users → Sessions → Revoke all sessions for the test user
3. Navigate to any page or perform an authenticated action
4. Verify redirect to `/login` with the "session expired" notification
