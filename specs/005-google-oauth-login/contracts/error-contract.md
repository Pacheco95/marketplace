# Error Contract: Google OAuth Login

**Branch**: `005-google-oauth-login` | **Date**: 2026-04-30

## Format

All error responses use **RFC 7807 Problem Details** (supported natively by Spring Boot 4.x via `ProblemDetail`). The `Content-Type` is `application/problem+json`.

```json
{
  "type": "https://marketplace.com/errors/authentication-failed",
  "title": "Authentication Failed",
  "status": 401,
  "detail": "Your session has expired. Please log in again.",
  "instance": "urn:uuid:550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-04-30T12:00:00.000Z"
}
```

| Field       | Type              | Description                                                                                 |
| ----------- | ----------------- | ------------------------------------------------------------------------------------------- |
| `type`      | URI               | Machine-readable error category URI                                                         |
| `title`     | string            | Human-readable short summary (i18n)                                                         |
| `status`    | integer           | HTTP status code                                                                            |
| `detail`    | string            | Human-readable explanation of this specific occurrence (i18n)                               |
| `instance`  | URI               | `urn:uuid:{uuid}` — the **traceable incident ID**; quote this when opening a support ticket |
| `timestamp` | ISO 8601 datetime | UTC timestamp of when the error occurred                                                    |

## Traceable Incident ID

The `instance` field contains a `urn:uuid` URI wrapping a randomly generated UUID. This ID is:

- Logged server-side alongside the full stack trace and request context
- Returned to the client in the error response
- Never reused

Users and support teams can use this ID to correlate a client-side error to the specific server-side log entry.

## i18n

The `title` and `detail` fields are **always localised** based on the request's `Accept-Language` header. Message keys are defined in `src/main/resources/messages.properties` (default English) and locale-specific files (`messages_pt.properties`, etc.). If the requested locale is not supported, English is used as fallback.

## Error Types Introduced by This Feature

| Type URI suffix             | HTTP Status | Scenario                                                   |
| --------------------------- | ----------- | ---------------------------------------------------------- |
| `authentication-failed`     | 401         | Missing, expired, or invalid access token                  |
| `session-expired`           | 401         | Token was revoked by the system (detected on next request) |
| `google-credential-invalid` | 401         | One Tap credential failed Google signature validation      |
| `token-exchange-failed`     | 502         | Keycloak Token Exchange call failed                        |
| `access-denied`             | 403         | Authenticated user lacks permission for the resource       |
| `validation-error`          | 400         | Request body failed validation                             |
| `internal-error`            | 500         | Unexpected server error                                    |

## Frontend Error Handling

- **401 `authentication-failed`**: Frontend intercepts on any API call, calls `POST /api/v1/auth/refresh`. If refresh also returns 401, redirects to `/login`.
- **401 `session-expired`**: Frontend redirects to `/login` and displays the session-expired notification banner.
- **Any error during login flow**: Frontend shows the error banner with the `detail` message and instruction to try again. The `instance` UUID may optionally be shown in small text for support purposes.
