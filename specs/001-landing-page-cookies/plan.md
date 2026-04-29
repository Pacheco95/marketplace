# Implementation Plan: Product Landing Page & Cookies Consent

**Branch**: `001-landing-page-cookies` | **Date**: 2026-04-29 | **Spec**: `/specs/001-landing-page-cookies/spec.md`
**Input**: Feature specification from `/specs/001-landing-page-cookies/spec.md`

## Summary
This is a greenfield project. Implement a high-performance, SEO-ready landing page for the marketplace SaaS using **Nuxt 4**. The implementation will include a cookies consent banner to ensure compliance. The frontend will follow the Nuxt 4 directory structure, utilizing ShadCN Nuxt for the design system, Pinia for state management (limited to pages), and composables for business logic.

## Technical Context

**Language/Version**: TypeScript, Nuxt 4 (v4.4.0)
**Primary Dependencies**: Bun (Package Manager), ShadCN Vue (UI), Tailwind CSS (via ShadCN), Pinia (State), Radix Vue (via ShadCN), @nuxtjs/i18n (Internationalization)
**Storage**: LocalStorage/Cookies for consent persistence (version-aware: stored `ConsentRecord.version` is compared against `COOKIE_POLICY_VERSION` on every page load; mismatch re-triggers the banner)
**Testing**: Vitest (Unit), Playwright (E2E)
**Target Platform**: Web (SSR enabled for SEO)
**Project Type**: Web Application (Frontend)
**i18n Support**: @nuxtjs/i18n (Mandatory per Constitution VII)
**Performance Goals**: <500ms load time (LCP), 100/100 Lighthouse SEO score

**Constraints**: Mobile-First Design, Stateless components, No business logic in components, Nuxt 4 `app/` structure
**Scale/Scope**: Landing page + Cookie Policy page + Cookie consent mechanism

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

1. **Multi-Tenant Data Sovereignty**: N/A for this landing page.
2. **Transactional Integrity & Commission Accuracy**: N/A.
3. **Service-Oriented Extensibility**: Frontend is decoupled; uses Nuxt SSR.
4. **Quality Assurance for Critical Paths**: Mandatory tests for cookie consent dismissal and landing page visibility.
5. **Auditability & Transparent Reporting**: N/A.
6. **Mobile-First Design**: UI components must be verified on mobile breakpoints first. Use the default Tailwind CSS v4 breakpoints (CRITICAL):

   | Prefix | Min Width | Media Query |
   |--------|-----------|-------------|
   | `sm`   | 40rem (640px)  | `@media (width >= 40rem)` |
   | `md`   | 48rem (768px)  | `@media (width >= 48rem)` |
   | `lg`   | 64rem (1024px) | `@media (width >= 64rem)` |
   | `xl`   | 80rem (1280px) | `@media (width >= 80rem)` |
   | `2xl`  | 96rem (1536px) | `@media (width >= 96rem)` |

   Design starts at base (< 40rem / 640px) and layers up. No unprefixed utility should break the mobile layout.

## Project Structure (Nuxt 4 Layout)

### Documentation (this feature)

```text
specs/001-landing-page-cookies/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (generated separately)
```

### Source Code (repository root)

```text
frontend/
├── app/                 # Nuxt 4 App Directory
│   ├── assets/
│   ├── components/      # Stateless UI components
│   │   ├── ui/          # ShadCN components
│   │   └── shared/      # Custom wrapper components
│   ├── composables/     # Business logic
│   ├── layouts/
│   ├── middleware/
│   ├── pages/           # Page-level components
│   │   ├── index.vue    # Landing page (US1)
│   │   └── cookie-policy.vue  # Cookie Policy details page (US3/FR-006)
│   ├── stores/          # Pinia stores
│   └── app.vue
├── server/              # Nitro server routes
├── shared/              # Shared logic (Nuxt 4 feature)
├── i18n/                # Internationalization (locales, config)
├── public/
├── nuxt.config.ts       # compatibilityVersion: 4
└── package.json
```

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | - | - |
