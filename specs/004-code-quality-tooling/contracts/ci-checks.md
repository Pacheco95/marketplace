# Contract: GitHub Actions Check Names

**Date**: 2026-04-30  
**Plan**: [../plan.md](../plan.md)  
**Specification source**: FR-031 through FR-040, US1, US2

This contract specifies the **names** of the GitHub Actions checks that this feature publishes. Names are how branch protection rules and external observers (status badges, reviewer dashboards) refer to checks; renaming any of these is a breaking change to the contract because it requires every consumer (the GitHub branch-protection settings, any external integrations) to be updated in lockstep.

---

## Workflow file

`.github/workflows/pull-request.yml`

**Triggers**:

- `pull_request` (default activity types, on PRs targeting `main`).

**Concurrency group**:

```yaml
concurrency:
  group: pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```

This cancels superseded runs of the same PR (e.g., a force-push) so the most recent commit is always the one being graded.

---

## Stable check names (the contract)

| Check name    | Job ID        | Spec FR           | What it does                                                                                    |
| ------------- | ------------- | ----------------- | ----------------------------------------------------------------------------------------------- |
| `format`      | `format`      | FR-032(a), FR-019 | Runs `bun run format:check`. Fails on any unformatted file.                                     |
| `lint`        | `lint`        | FR-032(b), FR-019 | Runs `bun run lint`. Fails on any error-severity ESLint finding.                                |
| `unit`        | `unit`        | FR-032(c)         | Runs `bun run test:unit`. Fails on any unit test failure.                                       |
| `integration` | `integration` | FR-032(d)         | Runs `bun run test:integration`. Passes when the suite is empty.                                |
| `e2e`         | `e2e`         | FR-032(e), FR-037 | Runs `bun run test:e2e` against a built+previewed app. Uploads diagnostic artifacts on failure. |

These five check names — `format`, `lint`, `unit`, `integration`, `e2e` — are the names a maintainer enters in **Settings → Branches → Branch protection rules → Require status checks to pass before merging** to make them required. Any rename without coordinated branch-protection updates would silently disable the gate (the renamed check is a "new" check and is not yet required).

---

## Job-level requirements

### All jobs

- Run on `ubuntu-latest`.
- Use `oven-sh/setup-bun@v2` with a pinned Bun version (research/D9).
- Cache `~/.bun/install/cache` keyed on `bun.lock`.
- Run `bun install --frozen-lockfile` before any project script (CI must use the locked versions per FR-034 and the spec's "Dependency lockfile drift" edge case).
- Run with no required secrets (FR-039).
- Have stable `name:` matching the check name above.

### Job: `format`

- Single step after install: `bun run format:check`.

### Job: `lint`

- Single step after install: `bun run lint`.

### Job: `unit`

- Single step after install: `bun run test:unit`.

### Job: `integration`

- Single step after install: `bun run test:integration`.

### Job: `e2e`

- Cache `~/.cache/ms-playwright` keyed on the Playwright version pinned in `frontend/package.json`.
- After install: `bunx playwright install --with-deps chromium` (only if the cache missed; otherwise skip the system-deps download).
- Build the Nuxt app: `bun --cwd frontend run build`.
- Run e2e: `bun run test:e2e` (Playwright `webServer` is CI-aware and starts the preview server).
- On failure (`if: failure()`): upload `frontend/playwright-report/` and `frontend/test-results/` as workflow artifacts (`actions/upload-artifact@v4`, retention 14 days). Implements FR-038, SC-010.

---

## Parallelism

All five jobs are top-level and run in parallel by default. Total wall-clock time is bounded by the slowest job (e2e), which is the design goal of FR-036 and SC-005.

No `needs:` dependencies between jobs — each does its own install. This trades a small amount of redundant install time for the ability to parallelize fully and for each check to be retryable independently from the GitHub UI.

---

## Failure → diagnostic flow

The CI failure log MUST identify:

| Check failure | What the log identifies                                            |
| ------------- | ------------------------------------------------------------------ |
| `format`      | The list of unformatted files (Prettier's default output).         |
| `lint`        | Each offending file, line, column, and ESLint rule ID.             |
| `unit`        | The failed test name, source file:line, and the failing assertion. |
| `integration` | Same as `unit`.                                                    |
| `e2e`         | The failed spec, step, and a link to the uploaded artifact bundle. |

This implements SC-008's 95%-of-failures-self-diagnosable target.

---

## Stability guarantee

The five check names are the contract. Adding new checks is non-breaking. Removing a check is a breaking change _only if_ it had been configured as a required status check (which would now be permanently failing). Renaming is always a breaking change.

A maintainer who configures branch protection should mirror the five names exactly. If at some future point the team adds a sixth (e.g., `typecheck`), it will be added as a non-required check first, observed for stability, then promoted to required via a separate branch-protection update.
