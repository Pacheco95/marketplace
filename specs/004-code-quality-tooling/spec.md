# Feature Specification: Code Quality, Linting & Test Automation

**Feature Branch**: `004-code-quality-tooling`  
**Created**: 2026-04-30  
**Status**: Draft  
**Input**: User description: "Install prettier, eslint and lint-staged. Setup Husky git hooks and also setup a github actions that validates code formatting and linting for all pull requests." Extended: "Also include running tests, both unit, integration tests (if applicable) and e2e tests."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Block Pull Requests That Fail Formatting or Linting (Priority: P1)

As a maintainer, I want pull requests to be automatically validated for code formatting and linting compliance so that the `main` branch is protected from style drift, lint regressions, and reviewer time wasted on stylistic feedback.

**Why this priority**: This is the only enforcement layer that cannot be bypassed by a contributor (intentionally or accidentally). Without CI validation, any local hook can be skipped (`git commit --no-verify`) and unenforced rules silently degrade the codebase. This must work even if no other story is delivered.

**Independent Test**: Open a pull request that introduces a formatting violation (e.g., wrong indentation) or a lint error (e.g., an unused variable). The PR's required check must fail, and the failure output must clearly identify the offending file(s) and rule(s). Open a clean PR — the check must pass.

**Acceptance Scenarios**:

1. **Given** a pull request that contains formatting violations, **When** CI runs, **Then** the formatting check fails with a non-zero exit and the failure log lists the files that are not formatted.
2. **Given** a pull request that contains lint errors, **When** CI runs, **Then** the lint check fails with a non-zero exit and the failure log lists the offending files, lines, and rules.
3. **Given** a pull request whose code is fully formatted and lint-clean, **When** CI runs, **Then** all formatting and lint checks complete successfully.
4. **Given** a pull request is opened or updated, **When** CI starts, **Then** the formatting and lint validation jobs are triggered automatically without manual intervention.
5. **Given** the CI workflow is reused across many PRs, **When** dependencies have not changed, **Then** the workflow uses cached dependencies to keep total runtime under the performance target (see Success Criteria).

---

### User Story 2 - Block Pull Requests That Fail Tests (Priority: P1)

As a maintainer, I want every pull request to run the project's automated test suites — unit, integration (when applicable), and end-to-end — so that no change reaches `main` if it breaks existing behavior, and so reviewers can trust the green check before approving.

**Why this priority**: Per Constitution Principle IV ("Quality Assurance for Critical Paths"), automated tests on the Golden Path are mandatory; per the Development Workflow section, no PR can merge without a passing test suite. Without CI test execution, that constitutional rule is not enforceable. This story is co-equal with US1: linting catches style and trivial bugs; tests catch behavioral regressions. Both must run on every PR.

**Independent Test**: Push a branch that intentionally breaks a unit test (e.g., changes a function to return the wrong value). Open a PR — the unit-test CI check must fail with output identifying the failed test and assertion. Push a fix — the check must pass. Repeat the same exercise for an end-to-end test (e.g., breaking the landing page so a Playwright spec fails to find an element). Cleanup branches after tests succeed.

**Acceptance Scenarios**:

1. **Given** a pull request introduces a failing unit test, **When** CI runs, **Then** the unit-test check fails with a non-zero exit and the log identifies the failed test name, the assertion that failed, and the source location.
2. **Given** a pull request introduces a failing end-to-end test, **When** CI runs, **Then** the e2e check fails with a non-zero exit and the log includes the failing test name, the failing step, and any captured artifacts (screenshots, traces, videos) needed to diagnose the failure.
3. **Given** the project later adopts a distinct integration-test category, **When** a pull request introduces a failing integration test, **Then** the integration-test check fails independently of the unit and e2e checks.
4. **Given** a pull request whose tests all pass locally, **When** CI runs against the same commit, **Then** the unit, integration (when applicable), and e2e checks all complete successfully.
5. **Given** an end-to-end test requires a built application, **When** the e2e check runs in CI, **Then** the workflow builds (or otherwise prepares) the application and starts the necessary servers/browsers automatically, with no manual setup.
6. **Given** a flaky e2e test fails sporadically, **When** CI runs, **Then** the workflow surfaces the failure with enough artifacts (trace, screenshot, log) for a contributor to reproduce or triage it; the workflow MAY support a single bounded retry per failing spec to absorb known flakiness without masking real regressions.
7. **Given** unit tests can run in parallel with formatting/lint checks, **When** CI runs, **Then** the test jobs and the lint/format jobs do not block one another, so total wall-clock time is bounded by the slowest single job.

---

### User Story 3 - Auto-Format and Lint Staged Files Before Commit (Priority: P1)

As a contributor, I want my changed files to be automatically formatted and lint-checked when I commit so that I get fast local feedback and rarely push code that will fail CI.

**Why this priority**: Local enforcement closes the feedback loop in seconds rather than minutes, dramatically reducing the rate of CI failures and speeding up everyone's iteration cycle. It also keeps formatting consistent without requiring contributors to remember to run commands manually.

**Independent Test**: With Husky installed (`bun install` runs the prepare step), make a deliberately unformatted change to a file and run `git commit`. The pre-commit hook must format the staged portion of the file, re-stage the formatted result, run lint on the staged files, and either complete the commit (if lint passes) or abort it (if lint fails) with a clear error message.

**Acceptance Scenarios**:

1. **Given** a contributor stages a file with formatting issues only, **When** they run `git commit`, **Then** the pre-commit hook reformats the staged content, re-stages it, and the commit completes successfully with the formatted version.
2. **Given** a contributor stages a file with lint errors that the linter can auto-fix, **When** they run `git commit`, **Then** the auto-fixable issues are corrected, the corrected content is re-staged, and the commit completes successfully.
3. **Given** a contributor stages a file with non-auto-fixable lint errors, **When** they run `git commit`, **Then** the commit is aborted, the working tree is left unchanged, and the error output identifies the offending file, line, and rule.
4. **Given** a contributor stages files in directories that are not subject to linting (e.g., generated assets, lock files), **When** they run `git commit`, **Then** the hook skips those files and does not block the commit.
5. **Given** a fresh clone of the repository, **When** the contributor runs the standard install command, **Then** the Husky hooks are installed automatically with no manual extra step.
6. **Given** a contributor commits with no staged files matching the linter or formatter globs, **When** the hook runs, **Then** it exits successfully without errors.

---

### User Story 4 - Run Tests Locally Before Pushing (Priority: P2)

As a contributor, I want fast tests (unit and, where applicable, integration) to run automatically before I push my branch so that I catch broken tests before opening a pull request and don't waste a CI cycle to discover regressions I could have seen in seconds.

**Why this priority**: A pre-push hook strikes the right tradeoff between feedback speed and developer flow. Running unit/integration tests on every commit (in pre-commit) would make commits painfully slow; running them only in CI delays feedback by minutes. Pre-push is fast enough to tolerate, late enough to skip during work-in-progress commits, and early enough to prevent obviously-broken PRs from being opened. End-to-end tests are intentionally NOT in this hook because they are too slow and environment-heavy to run on every push; they remain a CI-only gate.

**Independent Test**: With Husky installed, deliberately break a unit test, then run `git push`. The push must be aborted with a clear message identifying the failing test. Revert the breaking change and run `git push` again — the push proceeds. Verify that the hook does not attempt to run end-to-end tests.

**Acceptance Scenarios**:

1. **Given** a contributor has staged-and-committed a change that breaks a unit test, **When** they run `git push`, **Then** the pre-push hook executes the unit test suite, the suite fails, and the push is aborted with a non-zero exit code and a human-readable summary of which tests failed.
2. **Given** a contributor's local unit test suite passes, **When** they run `git push`, **Then** the push completes normally.
3. **Given** the project has a distinct integration test suite (current or future), **When** the pre-push hook runs, **Then** it also executes the integration suite under the same pass/fail rules as unit tests.
4. **Given** the contributor needs to push a work-in-progress branch, **When** they run `git push --no-verify`, **Then** the hook is bypassed (escape hatch), but US2 in CI will still catch any failures before merge.
5. **Given** the pre-push hook would run end-to-end tests, **When** designing the hook, **Then** it MUST NOT run them — e2e is reserved for CI per this story's "Why this priority" rationale.
6. **Given** the pre-push hook completes successfully, **When** measured on a typical developer laptop, **Then** total runtime is within the performance target (see Success Criteria).

---

### User Story 5 - Run Formatting, Linting, and Tests Manually Across the Whole Codebase (Priority: P2)

As a contributor or maintainer, I want documented commands to run formatting, linting, and the full test suites (unit, integration, e2e) across the entire codebase so that I can fix the codebase in bulk, audit current compliance, diagnose CI failures locally, or run a full pre-merge sanity check.

**Why this priority**: Critical for the initial rollout (the codebase will need a one-time mass-format), for diagnosing why CI is failing, for reproducing flaky e2e failures, and for periodic maintenance. Lower priority than P1 because it's a developer convenience that is unlocked once the underlying tools are configured — but absolutely required for anyone who needs to fix a CI failure without guessing.

**Independent Test**: From a fresh clone after install, run each documented command (format-write, format-check, lint, lint-fix, unit, integration if applicable, e2e). Each must execute successfully (or fail meaningfully on real issues) without crashing on missing setup, and must be discoverable in the project's contributor documentation.

**Acceptance Scenarios**:

1. **Given** the project is installed, **When** a contributor runs the documented format-write command, **Then** every supported file is formatted in place and the command exits successfully.
2. **Given** the project is installed, **When** a contributor runs the documented format-check command, **Then** the command exits zero if all files are formatted and non-zero with a list of offenders otherwise — without modifying any files.
3. **Given** the project is installed, **When** a contributor runs the documented lint command, **Then** the linter reports a complete set of issues across the whole repository and exits non-zero if any errors exist.
4. **Given** the project is installed, **When** a contributor runs the documented lint-fix command, **Then** auto-fixable lint issues are corrected in place and the remaining issues are reported.
5. **Given** the project is installed, **When** a contributor runs the documented unit-test command, **Then** the unit test suite executes once (non-watch) and exits zero on success or non-zero with a summary on failure.
6. **Given** the project is installed and an integration suite exists, **When** a contributor runs the documented integration-test command, **Then** that suite executes and reports pass/fail without requiring manual environment setup beyond what is documented.
7. **Given** the project is installed, **When** a contributor runs the documented e2e command, **Then** Playwright launches the necessary browsers and runs the e2e suite; on failure, traces/screenshots are written to a known location for inspection.
8. **Given** the README or contributor guide, **When** a new contributor reads it, **Then** they can find every command above documented in a discoverable location, with a one-line description of when to use each.

---

### Edge Cases

#### Lint & format

- **Bypassing the pre-commit hook**: A contributor uses `git commit --no-verify` to skip Husky. The CI check in User Story 1 MUST still catch any violations so the bypass cannot land in `main`.
- **First clone without dependencies installed**: A contributor clones and tries to commit before running install. The hook either does not exist yet (install not run) or installs cleanly via the standard install workflow — committing without install must not corrupt the working tree or leave a half-installed hook.
- **Generated, vendored, or third-party files**: Build output, lockfiles, package manifests, generated translations, and similar files MUST be excluded from both formatting and linting so they are not rewritten or flagged.
- **Files outside the frontend workspace**: Markdown, YAML, JSON, and similar files at the repository root (e.g., `.github/`, `specs/`) — the spec must specify which of these are in scope. See Assumptions.
- **Conflicting tool configurations**: Formatter and linter rules MUST not contradict each other (e.g., the linter MUST NOT flag style decisions the formatter makes). Stylistic rules in the linter that overlap with the formatter MUST be disabled.
- **Empty commits or commits touching only excluded files**: The pre-commit hook MUST exit successfully without error when no files match its globs.
- **Commits during a merge or rebase**: The pre-commit hook MUST behave correctly during merge/rebase operations and not block resolution of conflicts that are themselves unrelated to formatting.
- **Long-running CI on large diffs**: The CI workflow MUST complete within the performance target even when a PR touches many files.
- **Dependency lockfile drift**: Adding lint/format/test tooling MUST update the lockfile; CI MUST run with the locked versions to guarantee reproducibility.

#### Tests

- **Bypassing the pre-push hook**: A contributor uses `git push --no-verify` to skip the hook. CI in User Story 2 MUST still catch any failing tests so the bypass cannot land in `main`.
- **Pre-push runtime regressions**: If unit + integration runtime grows beyond the pre-push performance target, the hook becomes painful and contributors will start bypassing it. The performance target in Success Criteria is an explicit guardrail; if it's exceeded, the team must either parallelize, scope-down what runs pre-push, or split slow tests into a different category — not silently tolerate the regression.
- **Empty test suite or suite not yet present (integration)**: If no integration tests exist, the integration command and CI check MUST exit zero (success) rather than error, so the absence of integration tests does not block PRs. As soon as an integration suite is added, the existing wiring picks it up automatically.
- **Flaky end-to-end tests**: An e2e test fails because of timing, network, or environment instability rather than a real regression. The CI workflow MUST capture traces, screenshots, and console logs as artifacts on failure; it MAY support a single bounded retry per failing spec to absorb known flakiness; but it MUST NOT mask consistently-failing tests by retrying indefinitely.
- **End-to-end tests requiring a running server**: Playwright specs need the application to be running. The CI workflow MUST start the application (or mock backend) before e2e runs and tear it down afterward, with no manual orchestration required.
- **Browser binaries / Playwright dependencies in CI**: The CI runner MUST install Playwright browsers (or use a containerized runner that already has them) before e2e specs execute. Cold-start of browser binaries is a known cost; caching is allowed.
- **Tests that depend on environment variables or secrets**: If any test category requires secrets (e.g., a third-party API key for a true integration test), the CI workflow MUST inject them via repository/organization secrets, and MUST gracefully skip those specs (rather than fail) when running on a fork PR where secrets are unavailable.
- **Test commands run in watch mode by default**: The default `bun run test` is `vitest` in watch mode. The CI workflow and the pre-push hook MUST use the non-watch (`vitest run`) form, otherwise they will hang.
- **Parallel CI jobs and shared resources**: Test, lint, and format jobs MAY run in parallel in CI; they MUST NOT contend for the same port, file system path, or other shared resource that would cause false failures.
- **Fork PRs without write-permission secrets**: If e2e tests need secrets, fork PRs may be unable to run them. The workflow MUST degrade gracefully (skip with a visible warning) on fork PRs and MUST run the full suite on internal-branch PRs.

## Requirements _(mandatory)_

### Functional Requirements

#### Formatting

- **FR-001**: The project MUST adopt Prettier as its single source of truth for code formatting.
- **FR-002**: A Prettier configuration file MUST exist at a discoverable location and define the project's formatting rules; an ignore file MUST exclude generated, vendored, and lockfile content from formatting.
- **FR-003**: Prettier MUST cover, at minimum, the following file types present in the repository: TypeScript (`.ts`), Vue (`.vue`), JavaScript (`.js`/`.mjs`), JSON, YAML, and Markdown.
- **FR-004**: A documented command MUST exist to write formatting changes across the whole repository.
- **FR-005**: A documented command MUST exist to check formatting (read-only) across the whole repository, exiting non-zero on any unformatted file.

#### Linting

- **FR-006**: The project MUST adopt ESLint as its linter for JavaScript, TypeScript, and Vue files.
- **FR-007**: ESLint MUST be configured to understand TypeScript and Vue Single-File Components, including the existing Nuxt 4 conventions.
- **FR-008**: ESLint configuration MUST integrate with Prettier such that formatting decisions are owned exclusively by Prettier and never conflict with ESLint rules.
- **FR-009**: An ESLint ignore configuration MUST exclude generated output (e.g., Nuxt build output, type-generation artifacts), vendored files, and dependencies.
- **FR-010**: A documented command MUST exist to run lint across the whole repository, exiting non-zero on any error.
- **FR-011**: A documented command MUST exist to run lint with auto-fix across the whole repository.

#### Test categories & manual commands

- **FR-012**: The project MUST recognize three test categories: **unit** (existing, Vitest-based), **integration** (optional today, mandatory wiring so that adding tests later requires no further configuration), and **end-to-end** (existing, Playwright-based).
- **FR-013**: A documented command MUST exist to run the full unit test suite once (non-watch), exiting non-zero on any failure.
- **FR-014**: A documented command MUST exist to run the integration test suite. If no integration tests yet exist, the command MUST exit successfully (no-op) rather than error.
- **FR-015**: A documented command MUST exist to run the end-to-end test suite, including any application/server bootstrap required for Playwright to run.
- **FR-016**: Each test command MUST emit machine-parseable failure output (test name, location, assertion or step) so failures can be diagnosed without re-running with debug flags.
- **FR-017**: End-to-end test failures MUST produce diagnostic artifacts (Playwright traces and/or screenshots) at a documented path, both locally and in CI.

#### Local pre-commit enforcement

- **FR-018**: Husky MUST be installed and MUST install its git hooks automatically as part of the standard project install workflow (no extra manual step required for contributors).
- **FR-019**: A pre-commit hook MUST run lint-staged.
- **FR-020**: lint-staged MUST be configured to: (a) run Prettier write on the staged subset of every supported file type, (b) run ESLint with auto-fix on staged JS/TS/Vue files, and (c) re-stage modified files so the commit captures the corrected content.
- **FR-021**: When lint-staged detects errors that cannot be auto-fixed, the commit MUST be aborted with a non-zero exit code and a human-readable error message; the working tree MUST be left in a recoverable state.
- **FR-022**: The pre-commit hook MUST exit successfully (no-op) when no staged files match any configured glob.
- **FR-023**: The pre-commit hook MUST complete within the performance target on a typical commit (see Success Criteria).
- **FR-024**: The pre-commit hook MUST NOT run unit, integration, or end-to-end tests; tests are reserved for the pre-push hook (US4) and CI (US2) to keep commits fast.

#### Local pre-push enforcement

- **FR-025**: A pre-push hook MUST run the unit test suite (non-watch) and abort the push on any failure.
- **FR-026**: When an integration test suite exists, the pre-push hook MUST also run it under the same pass/fail rules.
- **FR-027**: The pre-push hook MUST NOT run the end-to-end test suite (e2e is CI-only per US4 rationale).
- **FR-028**: The pre-push hook MUST exit successfully when no test suites apply to the changes being pushed (e.g., docs-only push) or when configured suites contain no tests.
- **FR-029**: The pre-push hook MUST complete within the performance target on a typical push (see Success Criteria).
- **FR-030**: A standard `--no-verify` escape hatch MUST remain available for work-in-progress pushes; CI MUST still enforce all checks before merge.

#### Continuous Integration enforcement

- **FR-031**: A GitHub Actions workflow MUST run on every pull request targeting any branch where validation is desired (at minimum `main`).
- **FR-032**: The workflow MUST execute, as separate verifiable checks, (a) a Prettier formatting check (read-only), (b) an ESLint check, (c) the unit test suite, (d) the integration test suite (no-op when none exist), and (e) the end-to-end test suite.
- **FR-033**: The workflow MUST fail the pull request when any check above reports an error or test failure.
- **FR-034**: The workflow MUST install dependencies using the project's existing package manager and lockfile, and SHOULD cache dependencies (and Playwright browser binaries) between runs to meet the performance target.
- **FR-035**: The workflow's check names MUST be stable and discoverable so they can be configured as required status checks for branch protection.
- **FR-036**: Format, lint, and the three test jobs SHOULD run in parallel (subject to shared-resource constraints) to bound wall-clock time by the slowest single job.
- **FR-037**: For end-to-end runs, the workflow MUST automatically install Playwright browsers (or use a runner image that includes them), build/start the application as required, and tear down resources after the run.
- **FR-038**: On end-to-end failure, the workflow MUST upload traces, screenshots, and any other artifacts produced by Playwright so contributors can diagnose without rerunning locally.
- **FR-039**: The workflow MUST run with no required secrets for format, lint, unit, and (today's) e2e checks. If a future test category requires secrets, the workflow MUST gracefully skip those specs on fork PRs rather than fail.
- **FR-040**: The workflow MAY support a single bounded retry per failing end-to-end spec to absorb known flakiness; it MUST NOT silently retry indefinitely or mask consistently-failing specs.

#### Documentation & contributor onboarding

- **FR-041**: Contributor-facing documentation (e.g., the project README or a `CONTRIBUTING` guide) MUST document every command introduced by FR-004, FR-005, FR-010, FR-011, FR-013, FR-014, and FR-015, with a one-line description of when to use each.
- **FR-042**: Documentation MUST describe how the pre-commit and pre-push hooks behave and how to recover from blocked commits or pushes (including the `--no-verify` escape hatch and when its use is appropriate).
- **FR-043**: Documentation MUST list every CI check name (formatting, lint, unit, integration, e2e) and identify which are required for merge, plus how to reproduce each one locally.

### Key Entities

- **Format Configuration**: The Prettier rule set that defines the project's canonical formatting (indentation, line width, trailing commas, etc.) plus an ignore list. It is the single source of truth for "is this file formatted correctly?".
- **Lint Configuration**: The ESLint rule set governing code-quality and correctness rules (unused variables, type safety, Vue/Nuxt-specific rules, accessibility, etc.) plus an ignore list. It is the single source of truth for "is this file lint-clean?".
- **Unit Test Suite**: The Vitest-based suite under `frontend/tests/unit/`. Fast, isolated, runs without a real browser or backend. Executed in pre-push and CI.
- **Integration Test Suite**: A category for tests that exercise multiple layers without launching a full browser session (e.g., Nuxt server-route tests, store-with-API tests). Optional today; the wiring (commands, CI job, pre-push hook coverage) MUST exist so adding the first integration test requires no further infrastructure work.
- **End-to-End Test Suite**: The Playwright-based suite under `frontend/tests/e2e/`. Runs against a built application with real browsers. Executed in CI only.
- **Pre-Commit Hook**: A Husky-managed git hook that runs lint-staged before each commit, applies fixes to the staged subset of files, and either completes or aborts the commit. Does NOT run tests.
- **Pre-Push Hook**: A Husky-managed git hook that runs unit (and integration, when present) tests before each push and aborts the push on failure. Does NOT run end-to-end tests.
- **Pull Request Validation Workflow**: A GitHub Actions workflow that runs formatting, lint, unit, integration, and end-to-end checks against every pull request and reports their results as required (or required-when-applicable) status checks.
- **Staged File Set**: The subset of files under `git add` at the moment of a commit; the pre-commit hook operates only on this set, not the whole repository.
- **Diagnostic Artifacts**: Files produced by failing test runs (Playwright traces, screenshots, videos, console logs) at a known path locally and uploaded as workflow artifacts in CI.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 100% of pull requests merged to `main` after rollout contain only files that pass the configured formatting and lint rules (verified by passing CI checks).
- **SC-002**: 0 pull requests can be merged to `main` while any required CI check (format, lint, unit, integration when applicable, e2e) is failing — enforced via required status checks.
- **SC-003**: A contributor running a typical commit (≤20 changed files, ≤2,000 changed lines) sees the pre-commit hook complete in under 10 seconds on a recent developer laptop.
- **SC-004**: The pre-push hook (unit + integration) completes in under 60 seconds on a typical change on a recent developer laptop. If this threshold is consistently exceeded, the team adjusts hook scope rather than tolerates the regression.
- **SC-005**: The pull request validation workflow completes in under 8 minutes end-to-end (install + format + lint + unit + integration + e2e) on a typical PR, with caching enabled — driven by the slowest single job (e2e), with the others running in parallel.
- **SC-006**: A new contributor can clone the repository, run the project's documented install command, and have all hooks active without performing any extra manual installation step — verified by a clean-clone walkthrough.
- **SC-007**: After the initial rollout commit (one-time mass-format), the percentage of files that pass `prettier --check` is 100%, and the codebase reports zero ESLint errors at the configured rule severity for "error".
- **SC-008**: The CI failure log for a violating PR identifies, for at least 95% of failures (lint, unit, integration, e2e combined), the exact file path, line number (when applicable), test name (when applicable), and rule/check name, so a contributor can resolve the issue without a maintainer's help.
- **SC-009**: Average CI feedback latency from "PR opened/updated" to "format & lint result reported" is under 5 minutes at the 95th percentile; full-suite feedback (including e2e) under 10 minutes at the 95th percentile.
- **SC-010**: Diagnostic artifacts (Playwright trace, screenshots) are available for 100% of failing end-to-end runs in CI and are downloadable from the workflow run page.
- **SC-011**: End-to-end flakiness — defined as a spec failing on a retry-then-passing run — does not exceed 2% of total e2e runs over any rolling 30-day window. Above that threshold, the offending specs are quarantined and tracked for fix.

## Assumptions

- **Workspace scope**: The primary code under linting and formatting lives in `frontend/` (the existing Nuxt 4 application). Repository-level files (root `*.md`, `.github/workflows/*.yml`, `specs/`) are in scope for Prettier (Markdown/YAML formatting) and out of scope for ESLint (which targets JS/TS/Vue only). The implementation may choose to install the tooling at the repository root, in `frontend/`, or both — that is a planning concern, not a specification one.
- **Package manager**: The project uses Bun (evidenced by `frontend/bun.lock`); install and CI commands SHOULD use Bun. If a constraint emerges that requires a Node-based package manager for compatibility (e.g., a tool's published binary), planning may revisit this.
- **Husky version**: A modern Husky release (v9 or later) is assumed, where hooks are installed via a `prepare` script run automatically on `bun install`. No legacy `huskyrc` or v4-style configuration is expected.
- **lint-staged scope**: lint-staged operates only on the staged subset of files. It is intentionally not run against the whole repository in the hook; that is the responsibility of CI and the manual whole-repo commands.
- **CI provider**: GitHub Actions is the chosen CI provider as explicitly stated in the user request. No alternative CI is in scope.
- **Branch protection**: Whether the new CI checks are configured as _required_ status checks via GitHub branch protection is an administrative action that a maintainer performs in the GitHub UI/settings; this spec asserts that the checks MUST be stable and named so this configuration is possible (FR-035) but does not itself perform the configuration.
- **Existing code state**: The current `frontend/` codebase has not been formatted or linted with these tools. A one-time mass-format and lint-fix pass will be required as part of rollout; this is acceptable churn and will produce a single large diff that is intentionally separate from rule configuration in the implementation phase.
- **No new runtime dependencies**: All tooling introduced is `devDependencies`. None of it ships in production runtime bundles.
- **Editor integration**: Editor/IDE plugin configuration (VS Code, JetBrains, etc.) is out of scope for this feature. Contributors are free to install editor integrations themselves; documentation may mention them but they are not a deliverable.
- **Hooks in scope**: Pre-commit (format/lint) and pre-push (tests) are the only Husky hooks in scope for this feature. Other hooks (commit-msg for conventional-commit enforcement, post-merge, etc.) are out of scope and may be added separately.
- **Existing test stacks are retained**: Vitest is the unit test runner (already present in `frontend/package.json`). Playwright is the end-to-end runner (already present). This feature does NOT introduce new test frameworks; it wires the existing ones into hooks, CI, and documentation.
- **Integration test category**: No dedicated integration test directory or configuration exists today. This spec requires the wiring (command, CI job, pre-push hook coverage) to be in place so that adding the first integration test in the future requires no further plumbing. The exact location/layout (e.g., `frontend/tests/integration/`) is a planning decision.
- **End-to-end is CI-only**: e2e tests are deliberately excluded from local hooks because of their runtime, browser-binary, and server-bootstrap costs. Contributors run them manually (US5) when needed; CI runs them on every PR (US2).
- **Flakiness policy**: A bounded single retry per failing e2e spec is permitted in CI to absorb known flakiness without masking real regressions. The 2% flakiness budget in SC-011 is an explicit guardrail, not an implementation detail.
- **Fork PRs**: Today, none of the planned checks require secrets, so fork PRs run the full suite without degradation. If future secret-dependent tests are added, fork-PR behavior will need to be revisited; the workflow should already be structured to skip-with-warning rather than fail on missing secrets.
- **Constitutional alignment**: This feature directly serves Principle IV ("Quality Assurance for Critical Paths") — formatting, lint, unit, integration, and e2e tests on every PR jointly form the "passing test suite" gate the Development Workflow section requires before merge.
