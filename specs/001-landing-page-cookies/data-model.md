# Data Model: Landing Page & Cookies

## Entities

### Visitor
- **Description**: An unauthenticated user browsing the public site.
- **Attributes**:
    - `id` (Transient): A session identifier (if needed for analytics).
    - `hasConsented`: Boolean (derived from `ConsentRecord`).

### ConsentRecord
- **Description**: Persistence of the user's choice regarding the cookies policy.
- **Persistence Layer**: Client-side (Cookies or LocalStorage).
- **Schema**:
    ```json
    {
      "accepted": true,
      "timestamp": "ISO-8601 string",
      "version": "1.0.0"
    }
    ```
- **Validation Rules**:
    - `accepted` must be `true` (since it's an "Agree" only model for v1).
    - `timestamp` must be a valid ISO-8601 string.
    - `version` must match `COOKIE_POLICY_VERSION` from the policy config. If it does not match, the record is treated as invalid and the banner is re-shown.

### CookiePolicyConfig *(frontend constant, not persisted)*
- **Description**: Source of truth for the currently active policy version. Defined once in `frontend/app/config/cookiePolicy.ts` and imported by `useCookieConsent`.
- **Shape**:
    ```ts
    export const COOKIE_POLICY_VERSION = '1.0.0'
    export const COOKIE_POLICY_UPDATED_AT = '2026-04-29'
    ```
- **Update protocol**: Bumping `COOKIE_POLICY_VERSION` (e.g., `'1.0.0'` → `'1.1.0'`) automatically invalidates all stored `ConsentRecord`s on next page load, causing the banner to reappear for all returning visitors.

## State Transitions
1. **Initial State**: No `ConsentRecord` in storage, or stored `version` ≠ `COOKIE_POLICY_VERSION`. Banner is visible.
2. **Action**: User clicks "Agree".
3. **Transition**: `ConsentRecord` is written to storage with `accepted: true`, current timestamp, and `version: COOKIE_POLICY_VERSION`.
4. **Final State**: Banner is hidden for current and future visits as long as the stored `version` matches `COOKIE_POLICY_VERSION`.
