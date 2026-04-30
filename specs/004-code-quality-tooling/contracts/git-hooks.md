# Contract: Git Hooks (Husky)

**Date**: 2026-04-30  
**Plan**: [../plan.md](../plan.md)  
**Specification source**: FR-018 through FR-030, US3, US4, edge cases under "Lint & format" and "Tests"

This contract specifies what each Husky-managed git hook runs, when it succeeds, when it fails, when it no-ops, and how a contributor escapes it. Hook contents are checked in to `.husky/`; this document is the prose specification they implement.

---

## Hook 1: `pre-commit`

**File**: `.husky/pre-commit` (executable shell script)

**What it runs**: `bun run lint-staged` (which in turn runs the per-glob commands from the root `package.json` `lint-staged` field).

**Per-glob behavior** (from research/D5):

| Glob                         | Commands (in order)                     |
| ---------------------------- | --------------------------------------- |
| `*.{js,mjs,cjs,ts,vue}`      | `eslint --fix`, then `prettier --write` |
| `*.{json,jsonc,yml,yaml,md}` | `prettier --write`                      |

**Success contract** (commit completes):

- All staged files matching at least one glob were processed without unrecoverable error.
- Auto-fixable issues (formatting, auto-fixable lint rules) were corrected.
- Modified files were re-staged automatically by lint-staged.
- The commit captures the _post-fix_ content.

**Failure contract** (commit aborts, exit non-zero):

- Any non-auto-fixable lint error in a staged JS/TS/Vue file → commit aborted, lint-staged restores the working tree to its pre-run state, error output identifies the file/line/rule (FR-021).
- Any internal Prettier parse error → commit aborted with a clear error.

**No-op contract** (exit 0 silently):

- No staged files match any glob (e.g., committing only `.gitignore` changes — wait, actually `.gitignore` is fine since it's not formatted; but committing only generated images, etc.) → lint-staged exits 0 immediately. The commit completes (FR-022, US3 acceptance scenario 6).

**Side effects on success**:

- Modified-by-fix files are re-staged.
- The working tree may differ from `git diff --cached` for files that lint-staged fixed _and_ re-staged — that is the intended behavior, since the commit captures the corrected version.

**MUST NOT run**:

- Unit, integration, or e2e tests (FR-024). Tests are reserved for the pre-push hook and CI.

**Performance**: typically < 10s on a commit of ≤20 files / ≤2,000 lines (SC-003). lint-staged's per-glob parallelism (it spawns commands concurrently across globs by default) helps stay under budget.

**Escape hatch**: `git commit --no-verify`. CI in US1 still catches violations before merge, so the bypass cannot land in `main`. Contributors should use `--no-verify` only for genuine work-in-progress commits they intend to fix before pushing.

---

## Hook 2: `pre-push`

**File**: `.husky/pre-push` (executable shell script)

**What it runs** (sequential):

1. `bun run test:unit`
2. `bun run test:integration`

Both inner commands are non-watch (`vitest run` form).

**Success contract** (push proceeds):

- Both inner commands exit 0.
- The integration command may be a no-op success when no integration tests exist (FR-028 + the `--passWithNoTests` flag in `test:integration`).

**Failure contract** (push aborts, exit non-zero):

- Any test failure in either suite (FR-025, FR-026).
- Any runtime error in Vitest itself (config error, parse error in a spec file).

**No-op contract**:

- The `test:integration` script is a no-op success when no integration tests exist; the hook itself does not skip — it always invokes both commands.

**MUST NOT run**:

- e2e tests (FR-027). Justified at length in US4 "Why this priority": e2e is too slow, requires browser binaries and an app server, and would push pre-push past its 60s budget (SC-004).

**Performance**: < 60s on a typical change (SC-004). When this budget is exceeded, the team is required by the spec's "Pre-push runtime regressions" edge case to take corrective action (parallelize, scope-down what runs pre-push, or split slow tests into a different category) — not silently tolerate the regression.

**Escape hatch**: `git push --no-verify`. CI in US2 still catches failing tests before merge. Contributors should use `--no-verify` only for genuine work-in-progress pushes (typically branches not yet open as PRs).

---

## Activation

Both hooks are activated by the **root `package.json` `prepare` script**, which runs automatically on `bun install` (FR-018). Contributors clone, run `bun install` once, and the hooks are live for the lifetime of the working copy.

A fresh clone where `bun install` has not yet been run will have no hooks installed at all — committing or pushing will succeed without enforcement, which is acceptable because the contributor cannot have actually broken anything they haven't installed yet (and the CI gate still enforces the rules at merge time).

---

## Stability guarantee

The **shape** of each hook (which scripts it runs, which it must not run, success/failure/no-op semantics) is the contract. The implementation may be replaced (e.g., switching from `bun run lint-staged` to a direct invocation, moving from sequential to parallel test execution in pre-push) provided the contract is preserved.

A **breaking change** to this contract — adding e2e to pre-push, removing the lint step from pre-commit — requires a documentation update and a PR description that highlights it.
