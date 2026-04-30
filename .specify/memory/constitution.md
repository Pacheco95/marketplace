<!--
Sync Impact Report
- Version change: 1.2.0 → 1.3.0
- List of modified principles:
  - None (Renamed/Redefined)
- Added sections:
  - VIII. Code Quality Tooling Across the Whole Project (Principle)
- Removed sections: None
- Templates requiring updates:
  - .specify/templates/plan-template.md (✅ no change required — uses generic Constitution Check gate)
  - .specify/templates/spec-template.md (✅ no change required — does not hardcode principle list)
  - .specify/templates/tasks-template.md (✅ no change required — does not hardcode principle list)
- Follow-up TODOs: None
-->

# Marketplace Constitution

## Core Principles

### I. Multi-Tenant Data Sovereignty

All seller data, including product listings, customer information, and financial records, MUST be logically isolated. Cross-tenant data leakage is a critical failure.
**Rationale**: Trust is the foundation of a SaaS marketplace.

### II. Transactional Integrity & Commission Accuracy

Every sale MUST be processed through a verified transactional flow that calculates and applies the platform commission precisely. Commission logic must be centralized and immutable by tenants.
**Rationale**: The business model depends on accurate commission collection.

### III. Service-Oriented Extensibility

The platform SHOULD be built with a decoupled frontend and a robust, versioned API backend. This allows for future mobile applications or third-party integrations.
**Rationale**: Flexibility to scale the ecosystem.

### IV. Quality Assurance for Critical Paths

Automated tests (unit, integration, and E2E) are MANDATORY for the "Golden Path": seller registration, product publishing, checkout flow, and payment reconciliation.
**Rationale**: Zero downtime or bugs in the revenue-generating path.

### V. Auditability & Transparent Reporting

All financial events, including sales, refunds, and commission payouts, MUST generate immutable audit logs. Sellers must have access to transparent reports of their transactions.
**Rationale**: Compliance and seller trust.

### VI. Mobile-First Design

The platform's user interface MUST be designed and developed for mobile devices first. Desktop enhancements SHOULD be layered on top of the functional mobile experience.
**Rationale**: Ensuring accessibility and usability for the widest range of sellers and buyers.

### VII. Internationalization (i18n) by Default

The platform MUST be built to support multiple languages and locales from the start. All user-facing strings MUST be externalized using standard i18n libraries and patterns.
**Rationale**: To reach a global market of sellers and buyers and ensure accessibility across different regions.

### VIII. Code Quality Tooling Across the Whole Project

Automated code quality tooling — a single canonical formatter, a single canonical linter, and the unit, integration, and end-to-end test suites — MUST be applied uniformly across the entire repository, not selectively per workspace, contributor, or file. Formatting and linting MUST be enforced both locally (pre-commit) and in CI on every pull request. Tests MUST be enforced in CI on every pull request, and the fast suites (unit and integration) SHOULD additionally be enforced locally (pre-push). Generated output, vendored code, and lockfiles MAY be excluded only via explicit, reviewed ignore lists. No pull request MAY merge while any required quality check is failing, and bypass mechanisms (e.g., `--no-verify`) MUST NOT be a substitute for fixing violations.
**Rationale**: Uniform enforcement prevents style drift, lint debt, and behavioral regressions from accumulating in unmonitored corners of the codebase, keeps reviewer attention on substance rather than mechanics, and gives every contributor the same reproducible quality bar from day one.

## Security & Compliance

The platform must adhere to OWASP Top 10 security standards. All sensitive data must be encrypted at rest and in transit. Payment processing must comply with PCI-DSS standards.

## Development Workflow

All changes must be submitted via Pull Requests. No PR can be merged without at least one approval and a passing test suite. All features must include documentation and relevant tests.

## Governance

This Constitution supersedes all other practices. Amendments require a formal review and a version bump. All PRs must verify compliance with these principles. Use `.specify/memory/constitution.md` as the source of truth.

**Version**: 1.3.0 | **Ratified**: 2026-04-29 | **Last Amended**: 2026-04-30
