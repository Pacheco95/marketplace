# REST API Contract: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30

## Base URL

All endpoints are under the API base path. In development: `http://localhost:8080`.

## Auth Endpoints — `/api/v1/auth`

### GET /api/v1/auth/login

Initiates the OIDC authorization code flow. Redirects the browser to Keycloak's Google login page.

**Auth required**: None  
**Request body**: None  
**Query parameters**: None

**Response**:

- `302 Found` — Location header points to Keycloak's authorization URL  
  Keycloak will handle the Google OAuth consent and redirect back to `/api/v1/auth/callback`.

---

### GET /api/v1/auth/callback

OIDC callback endpoint. Exchanges the authorization code for Keycloak tokens, upserts the user in the database, sets HTTP-only cookies, and redirects to the frontend profile page.

**Auth required**: None  
**Request body**: None  
**Query parameters**:

| Name            | Type   | Required | Description                            |
| --------------- | ------ | -------- | -------------------------------------- |
| `code`          | string | Yes      | Authorization code from Keycloak       |
| `state`         | string | Yes      | OIDC state parameter (CSRF protection) |
| `session_state` | string | No       | Keycloak session state                 |

**Response**:

- `302 Found` — Redirects to `/profile` (frontend). Sets `marketplace_access_token` and `marketplace_refresh_token` cookies.
- `302 Found` — Redirects to `/login?error=auth_failed` on failure. The `error` query parameter is displayed as an error banner by the frontend.

---

### POST /api/v1/auth/one-tap

Validates a Google One Tap credential (ID token received by browser JavaScript), exchanges it for Keycloak tokens via Token Exchange, upserts the user, and sets HTTP-only cookies.

**Auth required**: None  
**Content-Type**: `application/json`

**Request body**:

```json
{
  "credential": "eyJhbGciOiJS..."
}
```

| Field        | Type   | Required | Description                                                     |
| ------------ | ------ | -------- | --------------------------------------------------------------- |
| `credential` | string | Yes      | Google ID token from `google.accounts.id.initialize()` callback |

**Response**:

- `200 OK` — Cookies set. Response body: the authenticated user profile (same as `GET /api/v1/users/me`).
- `401 Unauthorized` — Invalid or expired Google credential. Problem Details body.
- `502 Bad Gateway` — Keycloak token exchange failed. Problem Details body.

---

### POST /api/v1/auth/refresh

Rotates the access and refresh tokens using the refresh token cookie. Both cookies are updated.

**Auth required**: Valid `marketplace_refresh_token` cookie  
**Request body**: None

**Response**:

- `204 No Content` — New cookies set.
- `401 Unauthorized` — Refresh token missing, expired, or revoked. Cookies cleared. The frontend must redirect to `/login`.

---

### POST /api/v1/auth/logout

Ends the user's session. Clears both cookies and calls Keycloak's end-session endpoint to invalidate the server-side session.

**Auth required**: Valid `marketplace_access_token` cookie  
**Request body**: None

**Response**:

- `204 No Content` — Cookies cleared. Session ended.
- `401 Unauthorized` — No valid session. Still clears any existing cookies.

---

## User Endpoints — `/api/v1/users`

### GET /api/v1/users/me

Returns the authenticated user's profile data. Used by the frontend to initialise the auth store after page load.

**Auth required**: Valid `marketplace_access_token` cookie

**Response**:

- `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "Jane Smith",
  "profilePictureUrl": "https://lh3.googleusercontent.com/..."
}
```

| Field               | Type        | Nullable | Description                   |
| ------------------- | ----------- | -------- | ----------------------------- |
| `id`                | UUID string | No       | Application user ID           |
| `email`             | string      | No       | Google email                  |
| `displayName`       | string      | Yes      | Full name from Google profile |
| `profilePictureUrl` | string      | Yes      | Google profile photo URL      |

- `401 Unauthorized` — No valid session. Problem Details body. The frontend must redirect to `/login` with session-expired notification.

---

## OpenAPI Generation Note

The API contract above is informational. The actual OpenAPI/Swagger specification is **auto-generated** by Spring (SpringDoc) from the OpenAPI annotations on the controller interfaces (`AuthApi`, `UserApi`). The generated spec is available at `/api/v1/api-docs` (JSON) and `/api/v1/swagger-ui.html` (UI) when the application is running. The `local` Spring profile disables authentication on the Swagger UI for easy manual testing.
