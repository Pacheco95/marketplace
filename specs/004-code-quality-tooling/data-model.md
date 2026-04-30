# Phase 1 Data Model: Code Quality, Linting & Test Automation

**Date**: 2026-04-30  
**Plan**: [./plan.md](./plan.md)

This feature has no runtime data model. The spec's "Key Entities" are configuration artifacts — files in the repository whose contents define the project's quality enforcement. This document catalogues each entity as a concrete file (or fileset) with: location, purpose, owner, update process, validation rules, and relationships to other entities.

---

## Entity 1: Format Configuration

**Realisation**: `.prettierrc.json` (repo root) + `.prettierignore` (repo root)

**Purpose**: Single source of truth for "is this file formatted correctly?"

**Owner**: Maintainers. Changes to this file are _reviewable rule changes_, not stylistic bikeshedding within a PR — they affect every file in the repo.

**Update process**: Modifying `.prettierrc.json` requires (a) the change itself, and (b) a follow-up `bun run format` and a re-commit so the codebase remains 100% conformant per **SC-007**. CI will fail otherwise.

**Validation rules**:

- File MUST be valid JSON.
- File MUST set the keys decided in research/D3 (`semi`, `singleQuote`, `trailingComma`, `printWidth`).
- `.prettierignore` MUST cover lockfiles, generated output (`.nuxt/`, `.output/`, `dist/`), Vitest snapshots, Playwright reports, lighthouse reports, and the auto-generated i18n locale files listed in research/D3.

**Relationships**:

- Read by **Lint Configuration** indirectly via `eslint-config-prettier`, which disables conflicting stylistic ESLint rules.
- Read by **Pre-Commit Hook** via lint-staged (Prettier write step).
- Read by **Pull Request Validation Workflow** in the `format` job (Prettier check step).
- Read by the `format` and `format:check` npm scripts (manual command surface, US5).

---

## Entity 2: Lint Configuration

**Realisation**: `eslint.config.js` (repo root, ESLint v9 flat config)

**Purpose**: Single source of truth for "is this file lint-clean?". Governs code-quality and correctness rules; no stylistic rules (those are owned by Prettier).

**Owner**: Maintainers, with bias toward conservative additions. New rules at severity `"error"` should be discussed in the PR description, since they widen the failure surface for every contributor.

**Update process**:

- Adding a _warning_ rule (severity `"warn"`) is generally safe — it shows up in output but does not fail CI.
- Adding an _error_ rule should be paired with a `bun run lint:fix` pass and any necessary code changes in the same PR so the codebase stays conformant (**SC-007**).

**Validation rules**:

- Config MUST extend `@nuxt/eslint-config` and include the Nuxt-aware preset from `@nuxt/eslint`.
- Config MUST end its rule chain with `eslint-config-prettier` so that no stylistic ESLint rule conflicts with Prettier (**FR-008**).
- Config MUST ignore (via `ignores` in flat config): `node_modules/`, `**/.nuxt/`, `**/.output/`, `**/dist/`, `**/build/`, `frontend/playwright-report/`, `frontend/test-results/`, `frontend/i18n/locales/*.json` (data file).
- Config MUST cover `*.{js,mjs,cjs,ts,vue}` — i.e., the JavaScript/TypeScript/Vue surface of the repo.

**Relationships**:

- Read by **Pre-Commit Hook** via lint-staged (`eslint --fix` step).
- Read by **Pull Request Validation Workflow** in the `lint` job.
- Read by the `lint` and `lint:fix` npm scripts.

---

## Entity 3: Unit Test Suite

**Realisation**: `frontend/vitest.config.ts` (config) + `frontend/tests/unit/**/*.test.ts` (specs)

**Purpose**: Fast, isolated tests of components, stores, composables, utility functions. Runs without a real browser or backend.

**Owner**: All contributors — adding tests is part of every feature PR per Constitution Principle IV.

**Update process**: Add or modify spec files freely; the config is rarely touched. Major config changes (e.g., switching `environment`) follow the same maintainer-approval bar as Lint Configuration changes.

**Validation rules**:

- Config MUST keep `environment: 'nuxt'` so component tests have access to Nuxt auto-imports/composables (matches existing setup).
- Config MUST keep `include: ['tests/unit/**/*.test.ts']` so it does not pick up integration files.
- Specs MUST live under `frontend/tests/unit/` and follow the `*.test.ts` naming convention.

**Relationships**:

- Run by `bun run test:unit` (manual).
- Run by **Pre-Push Hook** (FR-025).
- Run by **Pull Request Validation Workflow** in the `unit` job (FR-032(c)).

---

## Entity 4: Integration Test Suite

**Realisation**: `frontend/vitest.integration.config.ts` (config) + `frontend/tests/integration/**/*.test.ts` (specs, currently empty)

**Purpose**: Tests that exercise multiple layers without a full browser session — for example, Nuxt server-route handlers under test, store-plus-API interactions, or i18n routing under test.

**Owner**: All contributors. New integration tests are encouraged whenever a unit test cannot capture the desired behavior but a Playwright e2e is overkill.

**Update process**: Add specs under `frontend/tests/integration/`; the config and CI wiring are already in place. The first integration spec authored requires no extra plumbing.

**Validation rules**:

- Config MUST target `tests/integration/**/*.test.ts` only — no overlap with unit specs.
- Config MAY use a different `environment` (e.g., `node`) if appropriate for backend-style tests; default mirrors the unit config until a divergence is needed.
- Manual command and CI step MUST pass `--passWithNoTests` so an empty suite is a no-op success (FR-014, FR-028).

**Relationships**:

- Run by `bun run test:integration` (manual).
- Run by **Pre-Push Hook** (FR-026).
- Run by **Pull Request Validation Workflow** in the `integration` job (FR-032(d)).

---

## Entity 5: End-to-End Test Suite

**Realisation**: `frontend/playwright.config.ts` (config) + `frontend/tests/e2e/**/*.spec.ts` (specs)

**Purpose**: Full-stack browser-driven tests of user flows. Runs against a built application.

**Owner**: All contributors. The Golden Path flows (per Constitution IV — seller registration, product publishing, checkout, payment reconciliation) MUST be covered by e2e specs as those flows are built.

**Update process**: Add specs under `frontend/tests/e2e/`. Modify `playwright.config.ts` only for project-level changes (browsers, retries, baseURL).

**Validation rules** (delta from current state):

- `retries`: MUST be `process.env.CI ? 1 : 0` (down from current `2`) to align with FR-040 / SC-011.
- `webServer.command`: MUST be CI-aware — `bun run preview` in CI (after `bun run build`), `bun run dev` locally.
- Specs MUST live under `frontend/tests/e2e/` and follow the `*.spec.ts` naming convention.

**Relationships**:

- Run by `bun run test:e2e` (manual, US5 acceptance scenario 7).
- NOT run by any local hook (FR-027).
- Run by **Pull Request Validation Workflow** in the `e2e` job (FR-032(e), FR-037).

---

## Entity 6: Pre-Commit Hook

**Realisation**: `.husky/pre-commit`

**Purpose**: Format and lint staged files before they enter a commit. Fast, runs on every commit.

**Contents** (illustrative):

```sh
#!/usr/bin/env sh
bun run lint-staged
```

(In Husky v9 the helper boilerplate is no longer required.)

**Validation rules**:

- File MUST be executable (`chmod +x`).
- File MUST NOT run any test command (FR-024).
- File MUST defer to lint-staged for the actual work.
- File MUST exit non-zero on lint-staged failure so the commit aborts.

**Relationships**:

- Reads **Format Configuration** and **Lint Configuration** transitively via lint-staged.
- Activated by the root `package.json` `prepare` script (Husky v9).

---

## Entity 7: Pre-Push Hook

**Realisation**: `.husky/pre-push`

**Purpose**: Run fast tests (unit + integration) before a push so contributors catch regressions before opening a PR.

**Contents** (illustrative):

```sh
#!/usr/bin/env sh
bun run test:unit
bun run test:integration
```

**Validation rules**:

- File MUST be executable.
- File MUST NOT run e2e tests (FR-027).
- Each command MUST be the non-watch form of Vitest (otherwise the hook would hang — spec edge case "Test commands run in watch mode by default").
- File MUST exit non-zero if any inner command exits non-zero.

**Relationships**:

- Activated by the root `package.json` `prepare` script.
- Tests **Unit Test Suite** and **Integration Test Suite**.

---

## Entity 8: Pull Request Validation Workflow

**Realisation**: `.github/workflows/pull-request.yml`

**Purpose**: Enforce all required checks on every pull request targeting `main`. Stable check names so a maintainer can configure them as required status checks for branch protection (FR-035).

**Stable check names** (the contract — see [contracts/ci-checks.md](./contracts/ci-checks.md)):

- `format` — Prettier check
- `lint` — ESLint check
- `unit` — Vitest unit tests
- `integration` — Vitest integration tests (no-op when empty)
- `e2e` — Playwright

**Validation rules**:

- Workflow MUST trigger on `pull_request` events.
- Workflow MUST run with no required secrets for any of the five jobs as currently scoped (FR-039).
- Workflow MUST cache Bun install cache and Playwright browsers (FR-034).
- Workflow MUST upload Playwright traces/screenshots on e2e failure (FR-038).
- Workflow MUST set a `concurrency` group on PR number to cancel superseded runs.
- Job names MUST match the stable contract above.

**Relationships**:

- Reads every other entity in this document (the configs and specs it validates).
- Produces **Diagnostic Artifacts** on e2e failure.

---

## Entity 9: Diagnostic Artifacts

**Realisation**: `frontend/playwright-report/` (HTML report), `frontend/test-results/` (per-spec traces, screenshots, videos)

**Purpose**: Reproducibility and triage of failing e2e runs.

**Owner**: Generated automatically by Playwright; not edited by hand.

**Validation rules**:

- Both directories MUST be in `.gitignore` (no checked-in test artifacts).
- The CI workflow MUST upload them with `actions/upload-artifact` when the `e2e` job fails (FR-038).
- Retention: 14 days (workflow setting).

**Relationships**:

- Produced by **End-to-End Test Suite** runs.
- Consumed by humans triaging failures via the GitHub Actions run page.

---

## Entity 10: Staged File Set

**Realisation**: Not a file — a runtime concept. The set of files under `git add` at the moment a commit is created.

**Purpose**: Defines the scope on which the **Pre-Commit Hook** operates.

**Validation rules**:

- Pre-commit hook (via lint-staged) MUST process only this set, never the whole repo (US3 acceptance scenario 4 implies, FR-019/FR-020 require).
- When this set is empty (or contains only files matching no glob), the hook MUST exit 0 (FR-022).

**Relationships**:

- Consumed by **Pre-Commit Hook** via lint-staged's git plumbing.

---

## Cross-cutting note: ignore lists

Three places hold ignore lists, and they MUST stay coherent:

| File                               | Owns ignores for                       |
| ---------------------------------- | -------------------------------------- |
| `.prettierignore`                  | Files Prettier never reads or rewrites |
| `eslint.config.js` (`ignores` key) | Files ESLint never lints               |
| `.gitignore`                       | Files git never tracks                 |

By convention: any path in `.gitignore` (e.g., `node_modules/`, `.nuxt/`, `dist/`) MUST also be in both `.prettierignore` and the ESLint `ignores` list. Generated output that _is_ git-tracked (none today, but possible) would only need to be in the tooling-specific ignores. This convention is enforceable manually during review; if the lists drift in practice, a future PR can introduce a script to verify them.
