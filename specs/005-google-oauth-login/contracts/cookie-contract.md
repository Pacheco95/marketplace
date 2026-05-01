# Cookie Contract: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30

## Cookies Set by the Backend

Two HTTP-only cookies control the authenticated session. They are set by the backend on the server side; the browser JavaScript never reads or writes them.

### `marketplace_access_token`

| Attribute | Value                                          |
| --------- | ---------------------------------------------- |
| Name      | `marketplace_access_token`                     |
| Value     | Keycloak-issued JWT (short-lived access token) |
| Max-Age   | 900 seconds (15 minutes)                       |
| Path      | `/api`                                         |
| Domain    | (same origin as backend)                       |
| HttpOnly  | Yes                                            |
| Secure    | Yes (HTTPS only; `false` in local dev)         |
| SameSite  | `Lax`                                          |

The access token is a Keycloak JWT. The backend validates it by verifying the signature against Keycloak's JWKS endpoint. Expired access tokens trigger the frontend to call `POST /api/v1/auth/refresh`.

### `marketplace_refresh_token`

| Attribute | Value                                                         |
| --------- | ------------------------------------------------------------- |
| Name      | `marketplace_refresh_token`                                   |
| Value     | Keycloak refresh token (opaque string)                        |
| Max-Age   | Configurable in Keycloak (default: 30 days = 2592000 seconds) |
| Path      | `/api/v1/auth/refresh`                                        |
| Domain    | (same origin as backend)                                      |
| HttpOnly  | Yes                                                           |
| Secure    | Yes (HTTPS only; `false` in local dev)                        |
| SameSite  | `Lax`                                                         |

The restricted path (`/api/v1/auth/refresh`) means the refresh token cookie is **only sent by the browser when calling the refresh endpoint**, never on other API calls. This limits exposure in the event of a misconfigured or compromised API endpoint.

## Cookie Lifecycle

| Event                                | Access Token Cookie  | Refresh Token Cookie          |
| ------------------------------------ | -------------------- | ----------------------------- |
| Successful login (button or One Tap) | Set                  | Set                           |
| Access token expires                 | Browser receives 401 | Unchanged                     |
| Successful refresh                   | Replaced (new token) | Replaced (rotated)            |
| Refresh token expired/revoked        | —                    | — (401 from refresh endpoint) |
| Logout                               | Deleted (Max-Age=0)  | Deleted (Max-Age=0)           |

## Refresh Token Rotation

Keycloak is configured with refresh token rotation enabled. Each call to `POST /api/v1/auth/refresh` consumes the current refresh token and issues a new one. Reuse of a consumed refresh token signals session replay and Keycloak will revoke the entire session.

## Local Development

In the `local` Spring profile, `Secure=false` is set so cookies work over plain HTTP (`http://localhost:8080`). This is the only deviation from the production cookie configuration.
