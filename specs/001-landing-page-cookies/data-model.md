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
    - `timestamp` must be current.

## State Transitions
1. **Initial State**: `hasConsented` is `false`. Banner is visible.
2. **Action**: User clicks "Agree".
3. **Transition**: `ConsentRecord` is created and persisted. `hasConsented` becomes `true`.
4. **Final State**: Banner is hidden for the current and future visits (until TTL or manual clear).
