# Tasks: Product Landing Page & Cookies Consent

**Input**: Design documents from `/specs/001-landing-page-cookies/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Vitest for unit tests and Playwright for E2E tests are required for the "Golden Path" (landing page visibility and cookie consent).

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and Nuxt 4 / Bun environment setup

- [X] T001 Initialize Nuxt 4 project using `bun create nuxt@4.4.0 frontend` with standard modules (@nuxtjs/i18n, @pinia/nuxt, shadcn-nuxt). This is an interactive command that requires human input. Ensure that tailwindcss was installed as a transitive dependency. 
- [X] T002 Configure `frontend/nuxt.config.ts` with `compatibilityVersion: 4` and ShadCN component directory
- [X] T003 Initialize ShadCN Vue using `bunx shadcn-vue@latest init` in `frontend/`
- [X] T004 Setup internationalization structure in `frontend/i18n/` (locales/ and i18n.config.ts)
- [X] T005 [P] Configure Vitest and Playwright in `frontend/` for Nuxt 4
- [X] T006 [P] Initialize ShadCN MCP for AI assistance (`bunx --bun shadcn-vue@latest mcp init --client claude`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core logic and shared components required for all user stories

- [X] T030 Create policy version config at `frontend/app/config/cookiePolicy.ts` exporting `COOKIE_POLICY_VERSION` (semver string, e.g. `'1.0.0'`) and `COOKIE_POLICY_UPDATED_AT` (ISO date string). This is the single source of truth — bumping the version here triggers re-consent for all returning visitors.
- [X] T007 Implement Cookie Consent Store in `frontend/app/stores/useConsentStore.ts` (using Pinia). On initialization, compare the stored `ConsentRecord.version` against `COOKIE_POLICY_VERSION` from `frontend/app/config/cookiePolicy.ts`; if they differ or no record exists, set `hasConsented` to `false` so the banner is shown.
- [X] T008 Implement Cookie Logic Composable in `frontend/app/composables/useCookieConsent.ts`. Must expose: `hasConsented` (computed), `isOutdated` (true when stored version ≠ current version), and `accept()` (writes a fresh `ConsentRecord` with current version and timestamp).
- [X] T009 Create Base Layout in `frontend/app/layouts/default.vue` with i18n support
- [X] T010 Setup translation files in `frontend/i18n/locales/en.json` (and other languages if applicable)

**Checkpoint**: Infrastructure ready - UI implementation can begin

---

## Phase 3: User Story 2 - Cookies Policy Agreement (Priority: P1) 🎯 MVP

**Goal**: Ensure legal compliance by showing and persisting cookie consent.

**Independent Test**: First-time visitor sees the banner; clicking "Agree" hides it and persists the state.

### Tests for User Story 2
- [X] T011 [P] [US2] Unit test for `useConsentStore.ts` in `frontend/tests/unit/stores/consent.test.ts`. Cover: no record → banner shown; valid record with matching version → banner hidden; valid record with outdated version → banner shown; `accept()` writes correct version.
- [X] T012 [P] [US2] E2E test for Cookie Banner visibility, dismissal, and "Cookie Policy" link navigation in `frontend/tests/e2e/cookies.spec.ts`. Verify: banner visible on first visit; clicking "Cookie Policy" navigates to `/cookie-policy` without dismissing banner; returning to `/` still shows banner (no auto-consent); clicking "Agree" hides banner and it does not reappear on next visit; simulating an outdated stored version (inject stale `ConsentRecord` via `localStorage.setItem` before page load) causes banner to reappear.

### Implementation for User Story 2
- [X] T013 [P] [US2] Install ShadCN Banner/Alert/Button components needed for the notice
- [X] T014 [US2] Create Stateless Cookie Banner component in `frontend/app/components/shared/CookieBanner.vue`. Must include: an "Agree" button wired to `useCookieConsent`, and a "Cookie Policy" `<NuxtLink>` pointing to `/cookie-policy`. All copy (banner text, button label, link label) must be sourced from i18n keys in `frontend/i18n/locales/en.json` (Constitution VII).
- [X] T015 [US2] Integrate Cookie Banner into `frontend/app/app.vue` or `default.vue` layout
- [X] T016 [US2] Implement "Agree" logic using `useCookieConsent` composable
- [X] T028 [US3] Create Cookie Policy page at `frontend/app/pages/cookie-policy.vue` (stateless, SSR-rendered). Page must display: current policy version and last-updated date (imported from `cookiePolicy.ts`), and structured placeholder sections for what cookies are collected, purpose, and retention period. Must use `<NuxtLink>` to return to the landing page.
- [X] T029 [US3] Add Cookie Policy page copy to `frontend/i18n/locales/en.json` (section titles, placeholder body text, back-link label) and add SEO meta tags to the page via `useSeoMeta`.

**Checkpoint**: MVP Part 1 (Cookies + Cookie Policy Page) complete and testable.

---

## Phase 4: User Story 1 - Understand Product Value (Priority: P1) 🎯 MVP

**Goal**: Display the value proposition of the SaaS Marketplace.

**Independent Test**: Visitor can read the platform description and commission model on the root page.

### Tests for User Story 1
- [X] T017 [P] [US1] E2E test for Landing Page content visibility in `frontend/tests/e2e/landing.spec.ts`. Must include `isInViewport` assertions for the Hero section at two viewports: mobile base (375×667) and desktop xl (1280×800), verifying the primary product description is visible above the fold without scrolling (SC-003).

### Implementation for User Story 1
- [X] T018 [P] [US1] Install ShadCN Card, Button, and Typography components
- [X] T019 [US1] Create Hero component in `frontend/app/components/shared/Hero.vue` (stateless)
- [X] T020 [US1] Create Features component in `frontend/app/components/shared/Features.vue` (stateless)
- [X] T021 [US1] Create Commission Model component in `frontend/app/components/shared/CommissionInfo.vue` (stateless)
- [X] T022 [US1] Implement Landing Page in `frontend/app/pages/index.vue` using the stateless components
- [X] T023 [US1] Add all copy to `frontend/i18n/locales/en.json` for externalization

**Checkpoint**: MVP Part 2 (Landing Page) complete. Entire feature functional.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T024 [P] Verify Mobile-First design on all components using Tailwind responsive classes
- [X] T025 [P] Optimize SSR performance and SEO tags in `frontend/app/pages/index.vue`
- [X] T031 [SC-004] Run Lighthouse CI against the local dev server (`bunx lighthouse http://localhost:3000 --output json --output-path ./lighthouse-report.json`) and assert LCP < 500ms. Fail the task if the threshold is not met; address SSR or asset optimizations until it passes.
- [X] T026 Run full Playwright test suite to ensure no regressions
- [X] T027 Final documentation update and code cleanup

---

## Dependencies & Execution Order

1. **Phase 1 (Setup)** is mandatory and blocks all subsequent work.
2. **Phase 2 (Foundational)** provides the logic for the cookies story.
3. **Phase 3 (User Story 2 + 3)** and **Phase 4 (User Story 1)** can technically be done in parallel, but Phase 3 is recommended first for compliance. T028/T029 (Cookie Policy page) depend on T014/T016 being complete.
4. **Phase 5 (Polish)** depends on both user stories being complete.
