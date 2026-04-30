# Feature Specification: Product Landing Page & Cookies Consent

**Feature Branch**: `001-landing-page-cookies`  
**Created**: 2026-04-29  
**Status**: Draft  
**Input**: User description: "The users should be able to reach a landing page where they can have a good understanding of the SaaS product. The project does not have a brand yet, just use a temporary placeholder name. The user must agree with the cookies policy."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Understand Product Value (Priority: P1)

As a potential customer, I want to visit the landing page so that I can understand the SaaS product's value proposition and core features.

**Why this priority**: Essential for converting visitors into users; the primary purpose of the page.

**Independent Test**: A user can navigate to the root URL and see a clear explanation of what the platform does (selling products/services with commission).

**Acceptance Scenarios**:

1. **Given** a visitor navigates to the landing page, **When** the page loads, **Then** they see a placeholder brand name and a clear description of the SaaS marketplace platform.
2. **Given** a visitor scrolls through the landing page, **When** they read the content, **Then** they understand that the platform allows selling products/services for a commission.

---

### User Story 2 - Cookies Policy Agreement (Priority: P1)

As a visitor, I want to be informed about the use of cookies and provide my consent so that my privacy preferences are respected.

**Why this priority**: Legal/compliance requirement for most modern web applications.

**Independent Test**: A first-time visitor sees a cookie notice that disappears only after they explicitly agree.

**Acceptance Scenarios**:

1. **Given** a new visitor, **When** they arrive at any page, **Then** a cookies consent banner/modal is displayed.
2. **Given** the cookies banner is visible, **When** the user clicks "Agree", **Then** the banner is dismissed and their choice is persisted for future visits.
3. **Given** the cookies banner is visible, **When** the user clicks the "Cookie Policy" link, **Then** they are navigated to the `/cookie-policy` page without dismissing the banner.
4. **Given** a returning visitor who previously agreed to an older policy version, **When** they arrive at any page, **Then** the cookie consent banner is shown again, indicating the policy has been updated.

---

### User Story 3 - Read Cookie Policy Details (Priority: P2)

As a visitor, I want to read the full details of the cookies used by the platform so that I can make an informed decision before agreeing.

**Why this priority**: Supports informed consent; required for transparency under common privacy regulations.

**Independent Test**: A visitor can navigate directly to `/cookie-policy` and read a clear, structured description of how cookies are used.

**Acceptance Scenarios**:

1. **Given** a visitor navigates to `/cookie-policy`, **When** the page loads, **Then** they see a structured overview of what cookies are collected, why, and how long they are retained.
2. **Given** a visitor arrives at `/cookie-policy` from the cookie banner link, **When** they return to the previous page, **Then** the cookie banner is still visible (consent not auto-granted by viewing the policy).

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST display a landing page at the root URL (/) featuring a placeholder brand name (e.g., "[Marketplace-SaaS]").
- **FR-002**: Landing page MUST contain sections explaining the core value proposition: multi-tenant selling, product/service listings, and the commission-based model.
- **FR-003**: System MUST display a clearly visible cookies consent notice to all unauthenticated visitors who haven't previously agreed.
- **FR-004**: System MUST allow users to explicitly "Accept" or "Agree" to the cookies policy.
- **FR-005**: System MUST persist the cookies consent state, including the version of the policy the user agreed to, so the user is not prompted again unless the active cookie policy version has changed. If the stored version does not match the current policy version, the banner MUST be shown again regardless of prior consent.
- **FR-006**: The cookies consent banner MUST include a "Cookie Policy" link that navigates to a dedicated `/cookie-policy` page. The Cookie Policy page MUST display a static placeholder overview of what cookies are collected, why, and their retention period.
- **FR-007**: System MUST support internationalization (i18n) for all user-facing strings (e.g., landing page content, cookie notice).

### Key Entities

- **Visitor**: An unauthenticated user browsing the public site.
- **ConsentRecord**: A client-side (or server-side) record of the user's agreement to the cookies policy.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 100% of first-time visitors are presented with the cookies consent notice.
- **SC-002**: Users can successfully dismiss the notice with a single click on the "Agree" button.
- **SC-003**: The primary product description (what it is and how it works) is visible "above the fold" at the mobile base viewport (375×667px, below the `sm` breakpoint of 640px) and at the desktop `xl` viewport (1280×800px). Viewports are defined by the Tailwind breakpoints in the implementation plan.
- **SC-004**: The landing page loads in under 500 milisseconds for users on a standard broadband connection.

## Assumptions

- The placeholder name "[Marketplace-SaaS]" is acceptable until a brand is decided.
- Cookies consent will be managed via local storage or a functional cookie as a reasonable default.
- No complex "Cookie Settings" (granular control) are required for v1; a simple "Agree" is sufficient.
- The landing page content will be static for the initial implementation.
- The active cookie policy version and its last-updated date are defined in a single frontend config constant (`COOKIE_POLICY_VERSION`, `COOKIE_POLICY_UPDATED_AT`). A version bump in that constant is sufficient to invalidate all prior consent records and re-trigger the banner for all returning visitors.
