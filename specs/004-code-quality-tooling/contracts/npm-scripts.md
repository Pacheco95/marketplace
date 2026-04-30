# Contract: NPM Scripts (Command Surface)

**Date**: 2026-04-30  
**Plan**: [../plan.md](../plan.md)  
**Specification source**: FR-004, FR-005, FR-010, FR-011, FR-013, FR-014, FR-015, FR-041

This contract specifies the command surface contributors invoke. Each script's name, location, exit-code semantics, and side effects are stable — once these names land, downstream documentation, branch-protection wiring, hook scripts, and CI jobs depend on them. Renaming a script is a contract change.

---

## Location

All scripts live in the **root `package.json`** unless explicitly noted. Most root scripts simply forward into the `frontend/` workspace; this layering means contributors run everything from the repo root, which matches the rest of the contributor experience (clone → `bun install` → run).

---

## Scripts

### `bun run format`

**Purpose**: Format every formattable file in the repository in place.  
**Implementation**: `prettier --write .`  
**Working directory**: repo root.  
**Side effects**: rewrites files on disk.  
**Exit code**: 0 on success; non-zero only on internal Prettier errors (e.g., parse failure on a file Prettier was supposed to format).  
**Spec link**: FR-004.  
**Caller(s)**: contributors during the initial mass-format and ad-hoc; not used by hooks or CI.

---

### `bun run format:check`

**Purpose**: Check formatting (read-only). Used by the `format` CI job.  
**Implementation**: `prettier --check .`  
**Working directory**: repo root.  
**Side effects**: none — read-only.  
**Exit code**: 0 if every file is formatted; non-zero if any file is not, with the offending paths printed to stdout.  
**Spec link**: FR-005.  
**Caller(s)**: `format` CI job. Contributors may run it locally to reproduce a CI failure.

---

### `bun run lint`

**Purpose**: Lint the JavaScript / TypeScript / Vue surface of the repo.  
**Implementation**: `eslint .`  
**Working directory**: repo root.  
**Side effects**: none — read-only.  
**Exit code**: 0 if no errors (warnings allowed); non-zero on any error-severity finding.  
**Spec link**: FR-010.  
**Caller(s)**: `lint` CI job; contributors locally; reproducible via this exact command.

---

### `bun run lint:fix`

**Purpose**: Lint with auto-fix.  
**Implementation**: `eslint . --fix`  
**Working directory**: repo root.  
**Side effects**: rewrites files on disk to apply auto-fixable rule corrections.  
**Exit code**: 0 if every remaining issue (post-fix) is non-error; non-zero on any remaining error.  
**Spec link**: FR-011.  
**Caller(s)**: contributors only; not used by hooks (lint-staged uses `eslint --fix` directly on staged files) or CI.

---

### `bun run test:unit`

**Purpose**: Run the Vitest unit suite once (non-watch).  
**Implementation**: `bun --cwd frontend run test:unit` (forwards to the existing `frontend` script `vitest run`).  
**Working directory**: repo root (the inner command runs in `frontend/`).  
**Side effects**: none beyond writing transient artifacts (Vitest cache).  
**Exit code**: 0 if all unit tests pass; non-zero on any failure or runtime error.  
**Spec link**: FR-013, US4 acceptance scenario 1, US5 acceptance scenario 5.  
**Caller(s)**: pre-push hook; `unit` CI job; contributors locally.

---

### `bun run test:integration`

**Purpose**: Run the Vitest integration suite once (non-watch). No-op when no integration tests exist yet.  
**Implementation**: `bun --cwd frontend run test:integration` → forwards to a new frontend script `vitest run --config vitest.integration.config.ts --passWithNoTests`.  
**Working directory**: repo root (inner command runs in `frontend/`).  
**Side effects**: none beyond Vitest cache.  
**Exit code**: 0 if all integration tests pass _or_ if the suite is empty; non-zero on any failure.  
**Spec link**: FR-014, FR-026, FR-028, edge case "Empty test suite".  
**Caller(s)**: pre-push hook; `integration` CI job; contributors locally.

---

### `bun run test:e2e`

**Purpose**: Run the Playwright e2e suite. Locally uses dev server; in CI uses preview server (driven by Playwright config CI-awareness).  
**Implementation**: `bun --cwd frontend run test:e2e` (existing frontend script `playwright test`).  
**Working directory**: repo root (inner command in `frontend/`).  
**Side effects**: starts/stops the application server; writes Playwright report to `frontend/playwright-report/` and per-spec artifacts to `frontend/test-results/`.  
**Exit code**: 0 if all specs pass within the configured retry budget; non-zero on any persistent failure.  
**Spec link**: FR-015, FR-017, US5 acceptance scenario 7.  
**Caller(s)**: `e2e` CI job; contributors locally when needed. NOT called by any local hook.

---

### `bun run test`

**Purpose**: Convenience aggregate — run unit + integration. Does NOT include e2e (e2e is too slow for a default `test` invocation).  
**Implementation**: `bun run test:unit && bun run test:integration`.  
**Exit code**: 0 if both pass; non-zero otherwise.  
**Spec link**: implicit — convenience script for contributors.  
**Caller(s)**: contributors locally; not used by hooks or CI (those call the granular scripts).

---

### `bun run prepare`

**Purpose**: Activate Husky hooks. Runs automatically on `bun install`.  
**Implementation**: `husky`.  
**Side effects**: writes `.husky/_/` directory and points `core.hooksPath` at `.husky`.  
**Exit code**: 0 unless Husky cannot be found (which means devDeps are not installed yet).  
**Spec link**: FR-018, US3 acceptance scenario 5, SC-006.  
**Caller(s)**: Bun's lifecycle (after `install`); contributors do not invoke directly.

---

## Stability guarantee

These eight script names are the **contract**. Any of the following count as a breaking contract change requiring a documentation update and a PR description that highlights it:

- Renaming a script (`test:unit` → `test:vitest`).
- Changing the side-effect category (e.g., making `format:check` modify files).
- Changing exit-code semantics (e.g., making `test:integration` exit non-zero on empty suite).

Adding new scripts is non-breaking. Adjusting the **implementation** of an existing script is non-breaking provided the script's purpose, side effects, and exit-code semantics stay the same.
