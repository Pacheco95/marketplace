# Feature Specification: Google OAuth Login

**Feature Branch**: `005-google-oauth-login`  
**Created**: 2026-04-30  
**Status**: Draft

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Explicit Google Login via Button (Priority: P1)

A visitor who is not logged in navigates to the `/login` page and clicks the "Login with Google" button. They are taken through the Google sign-in flow and, upon granting access, are redirected to their `/profile` page.

**Why this priority**: This is the foundational login flow that all other user stories depend on. Without a working login mechanism, no authenticated experience can be delivered.

**Independent Test**: Can be fully tested by visiting `/login` as an unauthenticated user, clicking the button, completing the Google sign-in, and verifying the redirect to `/profile` and the welcome message.

**Acceptance Scenarios**:

1. **Given** a visitor is not logged in, **When** they visit `/login` and click "Login with Google", **Then** they are taken through the Google authentication flow.
2. **Given** a visitor completes Google authentication successfully, **When** redirected back to the application, **Then** they arrive at `/profile` and see a personalized welcome message using their name.
3. **Given** a visitor is already logged in, **When** they visit `/login`, **Then** they are redirected directly to `/profile` without seeing the login page.

---

### User Story 2 - One Tap Auto-Prompt for Users with Active Google Session (Priority: P2)

When a user who is not logged into the application but has an active Google account session visits the application (any page) or the `/login` page, they see a Google One Tap / Sign In With Google popup automatically. They can dismiss it or accept it to log in without navigating to a dedicated login page.

**Why this priority**: This dramatically reduces friction for users who are already signed into Google, increasing conversion without requiring explicit navigation to the login page.

**Independent Test**: Can be tested by visiting the application or `/login` with an active Google session while not logged in to the application, confirming the One Tap prompt appears and that accepting it logs the user in and redirects to `/profile`.

**Acceptance Scenarios**:

1. **Given** a visitor has an active Google session and is not logged into the application, **When** they visit the application (any page), **Then** a Google One Tap popup appears.
2. **Given** the One Tap popup is visible, **When** the user accepts it, **Then** they are logged in and redirected to `/profile`.
3. **Given** a visitor is already logged into the application, **When** they visit any page, **Then** no login popup appears.
4. **Given** the One Tap popup is visible, **When** the user dismisses it, **Then** it disappears and they can continue browsing unauthenticated.

---

### User Story 3 - Navigation Header with User State Context Menu (Priority: P3)

A user can see a context menu button in the top-right corner of every page. For logged-in users it shows their profile picture (or initials if no photo is available), and when clicked reveals a "Welcome, [name]" message along with links to their profile and a log-out option. For unauthenticated users it provides a "Login with Google" option.

**Why this priority**: This is the primary persistent UI surface that makes the user's authentication state visible and provides access to profile and logout at all times.

**Independent Test**: Can be tested independently of the login flow by mocking a logged-in session and verifying the context menu renders the correct avatar, welcome message, and menu items; and separately by verifying the unauthenticated variant shows the login option.

**Acceptance Scenarios**:

1. **Given** a user is logged in, **When** they look at any page, **Then** their profile picture (or initials) is visible in the top-right corner.
2. **Given** a logged-in user clicks the context menu, **Then** they see "Welcome, [their name]", a link to their profile page, and a "Log out" option.
3. **Given** a user is not logged in, **When** they look at any page, **Then** a context menu button is visible in the top-right corner.
4. **Given** an unauthenticated user clicks the context menu, **Then** they see a "Login with Google" option.
5. **Given** a logged-in user clicks "Log out" from the context menu, **Then** their session is ended and they are returned to an unauthenticated state.
6. **Given** a logged-in user without a Google profile picture, **When** viewing the context menu, **Then** their initials are displayed in place of a profile photo.

---

### User Story 4 - Protected Profile Page (Priority: P4)

Authenticated users can visit `/profile` and see a personalized welcome message. Unauthenticated visitors who try to access `/profile` are automatically redirected to `/login`.

**Why this priority**: The profile page establishes the pattern for route-level access control, which is required by other protected routes in the future.

**Independent Test**: Can be fully tested by visiting `/profile` while unauthenticated (verify redirect to `/login`) and while authenticated (verify personalized content is shown).

**Acceptance Scenarios**:

1. **Given** a logged-in user visits `/profile`, **Then** they see a welcome message that includes their name.
2. **Given** an unauthenticated visitor navigates to `/profile`, **When** the page loads, **Then** they are redirected to `/login`.
3. **Given** a user is redirected from `/profile` to `/login` and subsequently logs in, **Then** they are redirected to `/profile`.

---

### User Story 5 - Persistent Session & Token Expiry (Priority: P5)

A logged-in user's session persists across browser restarts for a configurable duration (defaulting to 30 days). If their session token is revoked before that period ends, after the next api call that realizes the token is invalid the user should be redirected to `/login` with a notification that their session has expired.

**Why this priority**: Persistent sessions reduce friction for returning users. Token revocation handling is essential for security and is part of the spec's explicit requirements.

**Independent Test**: Can be tested by simulating a revoked or expired token and verifying the redirect to `/login` and the session-expired notification appear. Persistence can be verified by closing and reopening the browser and confirming the user remains logged in within the configured window.

**Acceptance Scenarios**:

1. **Given** a user logs in successfully, **When** they close and reopen the browser within the configured session window, **Then** they are still logged in without needing to authenticate again.
2. **Given** a user's token has been revoked by the system, **When** they next visit the application or perform any authenticated action, **Then** they are redirected to `/login`.
3. **Given** a user is redirected due to token revocation, **Then** they see a notification stating their session has expired and they need to log in again.
4. **Given** a token has not been revoked, **When** the configurable session duration elapses, **Then** the user's session is considered expired and they must authenticate again.

---

### User Story 6 - Login Error Handling (Priority: P6)

If any error occurs during the Google login flow (e.g., network failure, access denied, OAuth error), the user sees a clear error banner on the current page with an explanation and a prompt to try again.

**Why this priority**: Error states must be handled gracefully to avoid users being stuck or confused. This is a quality-of-life requirement rather than a core business flow.

**Independent Test**: Can be tested by simulating OAuth errors (e.g., user denies permission, network failure) and verifying the error banner appears with actionable guidance.

**Acceptance Scenarios**:

1. **Given** an error occurs during the Google login flow, **When** the user is returned to the application, **Then** a visible error banner appears describing the issue.
2. **Given** an error banner is displayed, **Then** it includes a message instructing the user to try again.
3. **Given** a user dismisses the error banner or retries, **Then** the banner disappears and they can attempt login again.

---

### Edge Cases

- What happens if a user's Google account has no display name? The system should fall back to displaying the email address in the welcome message.
- What happens if a user has no profile picture and their name has no Latin characters to derive initials from? A generic user avatar icon should be used as a final fallback.
- What happens if the Google One Tap prompt is repeatedly dismissed? After a reasonable number of dismissals, the prompt should stop appearing for that session to avoid annoying the user.
- What happens if a user's session expires mid-visit (e.g., token expires while on a protected page)? They should be redirected to `/login` on the next authenticated action, not immediately interrupted.
- What happens if a user opens the application in multiple browser tabs and logs out from one? Other tabs should reflect the logged-out state on the next navigation or authenticated action.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: The application MUST provide a `/login` page with a "Login with Google" button visible to all visitors.
- **FR-002**: The application MUST present a Google One Tap / Sign In With Google prompt to visitors who have an active Google session but are not logged into the application, both on the `/login` page and on general application pages.
- **FR-003**: Visitors who are already logged in MUST NOT see the One Tap login prompt or be shown the login page content when visiting `/login`; they MUST be redirected to `/profile` instead.
- **FR-004**: After a successful login through any flow (button or One Tap), the user MUST be redirected to `/profile`.
- **FR-005**: The application MUST display a persistent context menu button in the top-right corner on all pages.
- **FR-006**: For logged-in users, the context menu button MUST show the user's profile picture; if no profile picture is available, the user's initials MUST be displayed instead.
- **FR-007**: When a logged-in user opens the context menu, the application MUST display a "Welcome, [user's name]" greeting along with links to the profile page and a log-out action.
- **FR-008**: When an unauthenticated user opens the context menu, the application MUST display a "Login with Google" option.
- **FR-009**: The application MUST provide a `/profile` route that is accessible only to authenticated users.
- **FR-010**: Unauthenticated visitors who access `/profile` MUST be redirected to `/login`.
- **FR-011**: Logged-in users who visit `/profile` MUST see a personalized welcome message using their name.
- **FR-012**: The application MUST maintain the user's authenticated session across browser restarts for a configurable duration, with a default of 30 days.
- **FR-013**: If a user's session token is revoked, the application MUST redirect them to `/login` on their next interaction and display a notification that their session has expired.
- **FR-014**: The session expiry duration MUST be configurable without code changes.
- **FR-015**: If any error occurs during the Google login flow, the application MUST display an error banner visible to the user with an appropriate message and an instruction to try again.
- **FR-016**: A logged-in user MUST be able to log out through the context menu, ending their session immediately.

### Key Entities

- **User**: A person who has authenticated via Google. Key attributes: unique identifier from Google, display name, email address, profile picture URL (optional). Linked to a session.
- **Session**: A record representing an active login. Key attributes: reference to the user, creation time, expiry time, revocation status. Governs access to protected resources.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Users can complete the full login flow (from clicking "Login with Google" to landing on `/profile`) in under 30 seconds on a standard internet connection.
- **SC-002**: 100% of unauthenticated requests to `/profile` are redirected to `/login` — no protected content is ever shown to unauthenticated visitors.
- **SC-003**: Logged-in users returning within the configured session window are never prompted to log in again (0% unnecessary re-authentication within the session period).
- **SC-004**: 100% of revoked-token access attempts result in a redirect to `/login` with the session-expired notification displayed.
- **SC-005**: The error banner appears within 3 seconds of any login flow failure, without requiring a page reload.
- **SC-006**: The Google One Tap prompt attempts to render within 2 seconds of page load for eligible visitors (active Google session, not logged in to application).

## Assumptions

- Google is the only supported login provider for this feature; email/password or other OAuth providers are out of scope.
- The entire backend does not yet exist and will be built from scratch as part of this feature.
- Users are assumed to have a Google account; no fallback authentication method (e.g., magic link, email/password) is in scope.
- The session expiry default of 30 days is configurable via a server-side configuration value, not requiring a code deployment to change.
- Mobile browser support is in scope for all flows (login, profile, context menu), but a dedicated native mobile app is out of scope.
- The `/profile` page in this feature scope shows only a welcome message; a richer profile editing experience is a separate feature.
- The One Tap prompt is suppressed after the user explicitly dismisses it to avoid disrupting the browsing experience; the specific cooldown period follows Google's own One Tap dismissal policy.
- If a user accesses a protected page while unauthenticated and is redirected to `/login`, the application stores the original destination and redirects there after a successful login (assumed standard behavior).
