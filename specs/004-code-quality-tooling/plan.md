# Implementation Plan: Code Quality, Linting & Test Automation

**Branch**: `004-code-quality-tooling` | **Date**: 2026-04-30 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/004-code-quality-tooling/spec.md`

## Summary

Install and wire **Prettier**, **ESLint** (Nuxt-aware flat config), **lint-staged**, and **Husky v9** at the **repository root** so quality enforcement spans the whole repo (not just `frontend/`). Configure a **pre-commit hook** that runs lint-staged (Prettier write + ESLint auto-fix on staged files) and a **pre-push hook** that runs the unit and integration test suites (Vitest, non-watch). Add a **GitHub Actions pull-request workflow** with five parallel jobs — format, lint, unit, integration, e2e — using the existing Bun + Vitest + Playwright stack, with dependency and Playwright-browser caching, artifact upload on e2e failure, and a single bounded retry to absorb e2e flakiness. Add the missing integration-test wiring (no-op when empty), a one-time mass-format/lint-fix commit, and contributor documentation.

The structural decision driving the plan is to introduce a **Bun workspace at the repo root** so a single `bun install` reaches every package, the Husky `prepare` script runs at the right level, dev tooling lives in one place, and `frontend/` continues to own only its application dependencies.

## Technical Context

**Language/Version**: TypeScript 5.x (via Nuxt 4), JavaScript ESM, YAML for CI, shell for hooks. Runtime/package manager: Bun ≥ 1.1 (matches the existing `frontend/bun.lock`).  
**Primary Dependencies** (all `devDependencies`, all repo-root):

- Formatter: `prettier@^3`
- Linter: `eslint@^9`, `@nuxt/eslint`, `@nuxt/eslint-config`, `eslint-config-prettier` (disables stylistic conflicts with Prettier)
- Hook plumbing: `husky@^9`, `lint-staged@^15`
- Existing (unchanged): `vitest@^4` and `@playwright/test@^1.59` remain in `frontend/`.

**Storage**: N/A.  
**Testing**: Vitest (unit + integration), Playwright (e2e). The integration project reuses the Vitest runner via a separate config so unit and integration runs remain independently invocable.  
**Target Platform**: Developer laptops (macOS / Linux / Windows-WSL) for hooks; GitHub-hosted Ubuntu runners (`ubuntu-latest`) for CI.  
**Project Type**: Monorepo with one application workspace today (`frontend/`), plus repo-wide tooling at the root. Backend may be added later — quality tooling will already cover it.  
**Performance Goals**:

- Pre-commit: < 10s on a typical commit (SC-003)
- Pre-push: < 60s on a typical change (SC-004)
- CI full suite: < 8 min wall-clock (SC-005), driven by the slowest job (e2e)

**Constraints**:

- One canonical formatter, one canonical linter (Constitution VIII)
- e2e is CI-only; pre-push must NOT run it (FR-027, US4 rationale)
- Bounded single retry on failing e2e specs; no unbounded retry (FR-040, SC-011)
- No new runtime dependencies (Spec Assumption "No new runtime dependencies")
- CI must run with no required secrets for the current check set (FR-039)

**Scale/Scope**:

- Repository: one application workspace (`frontend/`), hundreds of source files post-rollout, tens of MD/YAML/JSON files at the root
- Contributor base: small (single-digit) today, but tooling must scale with growth
- Initial mass-format diff: large but one-time and isolated to its own commit

## Constitution Check

_Gate: passed — re-evaluated post-design and still passing. See `Constitution Re-Check` near the end of this document._

The constitution lives at `.specify/memory/constitution.md` (v1.3.0). Each principle is evaluated for relevance to this feature:

| Principle                                           | Relevance                                                                                                                                                                                                                                              | Evaluation         |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------ |
| I. Multi-Tenant Data Sovereignty                    | Not applicable (developer tooling; no tenant data)                                                                                                                                                                                                     | ✅ pass-by-vacuity |
| II. Transactional Integrity & Commission Accuracy   | Not applicable                                                                                                                                                                                                                                         | ✅ pass-by-vacuity |
| III. Service-Oriented Extensibility                 | Marginal — root tooling will cover any future backend without re-plumbing                                                                                                                                                                              | ✅ pass            |
| IV. Quality Assurance for Critical Paths            | **Directly served**. This feature is the enforcement mechanism that makes the constitutional rule "no PR merges without a passing test suite" actually enforceable. Unit/integration/e2e all run in CI on every PR.                                    | ✅ pass            |
| V. Auditability & Transparent Reporting             | Marginal — CI logs and uploaded artifacts are accessible from the GitHub UI; failures are auditable                                                                                                                                                    | ✅ pass            |
| VI. Mobile-First Design                             | Not applicable (no UI delivered)                                                                                                                                                                                                                       | ✅ pass-by-vacuity |
| VII. Internationalization (i18n) by Default         | Not applicable (no user-facing strings delivered)                                                                                                                                                                                                      | ✅ pass-by-vacuity |
| VIII. Code Quality Tooling Across the Whole Project | **Directly served**. Tooling installed at the repo root, covers the whole repository (not just `frontend/`), enforced both locally (pre-commit + pre-push) and in CI, with explicit reviewed ignore lists; no required check is bypassable past merge. | ✅ pass            |

**Gate result**: PASS. No deviations to record. The Complexity Tracking table at the end is intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/004-code-quality-tooling/
├── plan.md              # This file
├── research.md          # Phase 0 — resolved decisions and rationale
├── data-model.md        # Phase 1 — concrete configuration "entities"
├── quickstart.md        # Phase 1 — validation walkthrough for the feature
├── contracts/
│   ├── npm-scripts.md      # Documented commands surface (US5)
│   ├── git-hooks.md        # Pre-commit and pre-push contracts
│   └── ci-checks.md        # GitHub Actions check-name contract
└── checklists/
    └── requirements.md  # Created by /speckit-specify
```

### Source Code (repository root)

```text
.
├── package.json                       # NEW — repo-root: workspaces + dev-tooling devDeps + scripts
├── bun.lock                           # NEW — single root lockfile (Bun workspaces)
├── .prettierrc.json                   # NEW — Prettier rules (single source of truth)
├── .prettierignore                    # NEW — generated/vendored excludes
├── eslint.config.js                   # NEW — ESLint v9 flat config (Nuxt-aware)
├── .gitignore                         # UPDATE — ensure node_modules/.nuxt/etc. ignored at root
├── .husky/
│   ├── pre-commit                     # NEW — runs lint-staged
│   └── pre-push                       # NEW — runs unit + integration tests
├── .github/
│   └── workflows/
│       └── pull-request.yml           # NEW — 5 parallel jobs (format/lint/unit/integration/e2e)
├── README.md                          # UPDATE (or NEW) — contributor docs
├── CONTRIBUTING.md                    # NEW — long-form contributor guide
└── frontend/
    ├── package.json                   # UPDATE — added scripts (test:integration etc.); workspaces-ready
    ├── nuxt.config.ts                 # UPDATE — register `@nuxt/eslint` module
    ├── playwright.config.ts           # UPDATE — webServer command CI-aware; retries=1
    ├── vitest.config.ts               # UPDATE — unit-only project (split from integration)
    ├── vitest.integration.config.ts   # NEW — integration project; --passWithNoTests
    └── tests/
        ├── unit/                      # existing
        ├── integration/               # NEW (placeholder with .gitkeep)
        │   └── .gitkeep
        └── e2e/                       # existing
```

**Structure Decision**: **Repo-root tooling with a Bun workspace.** The root `package.json` declares `"workspaces": ["frontend"]` so `bun install` at the root resolves both packages and produces a single `bun.lock`. Dev tooling (Prettier, ESLint, Husky, lint-staged, eslint-config-prettier, Nuxt ESLint config) lives only in the root `package.json` as `devDependencies`. The frontend `package.json` keeps its application dependencies and gains a small set of test scripts (`test:integration`) that this feature wires up. Husky's `prepare` script runs at the root — the only correct level since hooks are git-repo-wide.

This structure is the only one that cleanly satisfies Constitution Principle VIII ("across the whole project"): a frontend-scoped `package.json` cannot own root-level Markdown/YAML formatting or lifecycle hooks, and a duplicated tooling install in two locations would violate the "single canonical formatter / linter" requirement.

## Phase 0: Outline & Research

See [research.md](./research.md). All NEEDS CLARIFICATION items resolved. Key decisions:

1. **Tooling location**: repo root via Bun workspace.
2. **ESLint config**: ESLint v9 flat config built on `@nuxt/eslint` + `@nuxt/eslint-config`, composed with `eslint-config-prettier` last so Prettier wins on stylistic rules.
3. **Prettier config**: shared `.prettierrc.json` at the root; covers `*.{ts,tsx,js,mjs,cjs,vue,json,jsonc,yml,yaml,md}`.
4. **Husky version**: v9 with hooks in `.husky/`; `prepare` script at root.
5. **lint-staged**: configured in root `package.json` `lint-staged` field; per-glob commands for JS/TS/Vue (eslint --fix → prettier --write) and for non-code files (prettier --write only); re-staging is automatic in lint-staged.
6. **Integration test wiring**: a separate Vitest config (`vitest.integration.config.ts`) targeting `frontend/tests/integration/**/*.test.ts`. Vitest's `passWithNoTests` flag is passed by the script for explicit safety.
7. **Pre-push hook scope**: runs `bun run test:unit` and `bun run test:integration` (both non-watch). Does NOT run `test:e2e`.
8. **CI provider**: GitHub Actions, `ubuntu-latest`. One workflow file `pull-request.yml` with one job per check; `concurrency` cancels superseded runs.
9. **Bun setup in CI**: `oven-sh/setup-bun@v2` with a pinned Bun version. Cache `~/.bun/install/cache` keyed on `bun.lock`.
10. **Playwright in CI**: cache `~/.cache/ms-playwright`; install browsers when cache miss; build the Nuxt app once and run e2e against the preview server (faster than dev). Override `playwright.config.ts` `webServer.command` to be CI-aware: `bun run preview` in CI (after `bun run build`), `bun run dev` locally.
11. **e2e flakiness policy**: `retries: 1` in CI (down from current 2) to align with FR-040 / SC-011.
12. **Documentation home**: `README.md` at the repo root (currently the project lacks one); plus a brief `CONTRIBUTING.md` that goes deeper on the hooks/CI flow. The frontend keeps its existing README.
13. **Mass format/lint commit**: handled as a separate, clearly-named commit during rollout (`chore: mass-format codebase via prettier/eslint`), kept independent of the rule-configuration commit so reviewers can audit the rules without wading through whole-file diffs.

## Phase 1: Design & Contracts

### Entities (configuration artifacts)

See [data-model.md](./data-model.md). Each entity from the spec is realized as a concrete file with a documented owner and update process:

- **Format Configuration** → `.prettierrc.json` + `.prettierignore`
- **Lint Configuration** → `eslint.config.js` (flat config) + ignore patterns inside it
- **Unit Test Suite** → `frontend/vitest.config.ts` + `frontend/tests/unit/**`
- **Integration Test Suite** → `frontend/vitest.integration.config.ts` + `frontend/tests/integration/**`
- **End-to-End Test Suite** → `frontend/playwright.config.ts` + `frontend/tests/e2e/**`
- **Pre-Commit Hook** → `.husky/pre-commit` (delegates to `lint-staged`)
- **Pre-Push Hook** → `.husky/pre-push` (delegates to test scripts)
- **Pull Request Validation Workflow** → `.github/workflows/pull-request.yml`
- **Diagnostic Artifacts** → Playwright `playwright-report/`, `test-results/` (uploaded as workflow artifacts)

### Contracts

This feature does not expose a network or RPC API. The "external interfaces" — the contracts that downstream consumers (contributors, CI configuration, branch-protection rules) will rely on — are documented under [contracts/](./contracts/):

1. **[contracts/npm-scripts.md](./contracts/npm-scripts.md)** — the stable command surface (`format`, `format:check`, `lint`, `lint:fix`, `test:unit`, `test:integration`, `test:e2e`). Each script's name, location, exit-code semantics, and side effects are specified.
2. **[contracts/git-hooks.md](./contracts/git-hooks.md)** — pre-commit and pre-push hook behavior contracts: what they run, what they re-stage, how they fail, when they no-op, and the `--no-verify` escape hatch.
3. **[contracts/ci-checks.md](./contracts/ci-checks.md)** — GitHub Actions check-name contract. Stable check names so a maintainer can configure them as required status checks for branch protection (FR-035) without worrying about the names drifting.

### Quickstart

[quickstart.md](./quickstart.md) is a step-by-step walkthrough that doubles as the acceptance test for the feature: clone, install, observe hooks installed, deliberately break formatting/lint/unit-test, observe each gate fire correctly, then revert and observe success.

### Agent context update

The `CLAUDE.md` SPECKIT block has been updated to point at this plan so any subsequent assistant invocation finds the technical context directly.

## Constitution Re-Check (post-design)

Re-evaluated after Phase 1 design — still PASS, no new violations introduced:

- The repo-root tooling decision is the **simpler** of the two real options ("root" vs "duplicated frontend+root"); it directly serves Principle VIII without complicating the project layout.
- The split of unit and integration into separate Vitest configs is a small structural cost paid to satisfy FR-014, FR-026, and FR-032(d) (integration must be independently invocable in pre-push and CI). The simpler "merge them into one suite" alternative was rejected because it would couple slow integration tests to fast pre-push runs (or, conversely, prevent integration tests from running pre-push at all).
- The `concurrency` group in CI prevents wasted runs but does not weaken the gate (the latest commit's run is what gets enforced).
- Bounded retry (`retries: 1`) is in tension with "100% reproducibility" only superficially — flakiness is captured in artifacts and bounded by SC-011's 2% budget; this matches FR-040.

## Complexity Tracking

> Fill ONLY if Constitution Check has violations that must be justified.

No violations. Section intentionally empty.
