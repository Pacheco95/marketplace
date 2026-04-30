# Phase 0 Research: Code Quality, Linting & Test Automation

**Date**: 2026-04-30  
**Spec**: [../spec.md](./spec.md)  
**Plan**: [./plan.md](./plan.md)

This document records the resolved decisions for every NEEDS-CLARIFICATION-class question in the plan's Technical Context, plus best-practice findings for each tool. Items the spec already decided (Prettier, ESLint, lint-staged, Husky, GitHub Actions, Vitest, Playwright) are _not_ re-litigated here — only the open implementation decisions are.

---

## D1. Tooling location: root, `frontend/`, or both?

**Decision**: Install at the **repository root** as a Bun workspace; declare `frontend/` as a workspace member.

**Rationale**:

- Constitution Principle VIII requires tooling to apply "across the entire repository" — a `frontend/`-scoped install cannot lint or format files at the repo root (e.g., `.github/workflows/*.yml`, root `*.md`, `specs/`).
- Husky hooks live in `.git/hooks` (or `.husky/` referenced by `core.hooksPath`), which is a repo-wide concern. The `prepare` script that activates Husky must run during the install at the level that owns the hooks. Running it from `frontend/` would activate hooks but anchor them to the `frontend/` directory, which is fragile and surprising.
- One canonical install path (`bun install` at the root) is friendlier to new contributors and CI authors.
- Using **Bun workspaces** keeps a single lockfile (`bun.lock` at the root) so dependency resolution is reproducible across the whole repo, eliminating a class of "works on my machine" bugs caused by frontend and root having drifted.

**Alternatives considered**:

- **Install everything in `frontend/` only.** Rejected: cannot reach root-level files, fights Principle VIII, and forces Husky to be installed at a non-standard level. Rejected for the reasons above.
- **Install separately at root and `frontend/`.** Rejected: violates the "single canonical formatter / linter" requirement of Principle VIII; doubles the dependency surface area; doubles the chances of version drift.
- **Use a third tool (npm/pnpm) at the root.** Rejected: the rest of the repo uses Bun (`frontend/bun.lock` is checked in); introducing a second package manager would create a versioning headache and contradict the spec's "Package manager: Bun" assumption.

---

## D2. ESLint configuration shape

**Decision**: ESLint v9 **flat config** built on `@nuxt/eslint` + `@nuxt/eslint-config`, composed last with `eslint-config-prettier`.

**Rationale**:

- ESLint v9 is the current stable line and uses flat config (`eslint.config.js`) by default; the legacy `.eslintrc.*` formats are deprecated. Starting on flat config now avoids a forced migration later.
- `@nuxt/eslint` is the official Nuxt 4 module that exposes a project-aware ESLint config (auto-imported globals, Vue file parsing, project-specific rules) and integrates into the Nuxt devtools. It also powers the `nuxi prepare`-generated `.nuxt/eslint.config.mjs` that we extend.
- `@nuxt/eslint-config` brings the upstream rule set; `@nuxt/eslint` wires it into the project so rules see the actual Nuxt resolver state.
- `eslint-config-prettier` (composed _last_) disables every ESLint rule that conflicts with a stylistic Prettier decision. This implements **FR-008** (no formatter/linter conflicts).

**Alternatives considered**:

- **Legacy `.eslintrc.cjs` + `eslint-plugin-vue` directly.** Rejected: requires manual Nuxt-awareness wiring, is being deprecated upstream, and we'd have to migrate to flat config within ~12 months anyway.
- **Biome instead of ESLint.** Rejected: the user explicitly requested ESLint. Biome is faster but the Vue/Nuxt ecosystem is deeply ESLint-coupled; switching would also violate the user constraint.
- **`eslint-plugin-prettier` (run Prettier _as_ a lint rule).** Rejected: well-known anti-pattern. It conflates two concerns (slow lint runs, noisy output, indirection) and the maintainers explicitly recommend using `eslint-config-prettier` plus a separate Prettier invocation instead.

---

## D3. Prettier configuration

**Decision**: A repo-root `.prettierrc.json` containing the project's stylistic choices, plus a `.prettierignore` listing generated/vendored paths.

**Resolved style choices** (sensible defaults; Prettier's whole point is to minimise this debate):

- `semi: false` (matches the existing `frontend/playwright.config.ts` and `nuxt.config.ts` style: no semicolons)
- `singleQuote: true` (matches existing code)
- `trailingComma: "all"` (Prettier 3 default)
- `printWidth: 100` (slightly wider than the 80 default; comfortable for modern viewports without being unreadable on smaller screens)
- `vueIndentScriptAndStyle: false` (Prettier default; matches Vue community standard)

**Rationale**: matches the existing implicit style of the codebase to keep the one-time mass-format diff focused on truly inconsistent files rather than rewriting the whole codebase.

**File globs covered**: `*.{ts,tsx,js,mjs,cjs,vue,json,jsonc,yml,yaml,md}` — repo-wide.

**Excluded** (in `.prettierignore`):

- `**/node_modules/`, `**/.nuxt/`, `**/.output/`, `**/dist/`, `**/build/`
- `**/bun.lock`, `**/package-lock.json`, `**/yarn.lock`, `**/pnpm-lock.yaml`
- `frontend/i18n/locales/*.json` (auto-generated; treat as data, not code)
- `frontend/lighthouse-report-*.json` (CI-produced report artifacts)
- `frontend/playwright-report/`, `frontend/test-results/`
- `**/*.snap` (Vitest snapshots — formatting them produces meaningless churn)

---

## D4. Husky version & install model

**Decision**: **Husky v9**, hooks under `.husky/`, activation via root `package.json`'s `"prepare": "husky"` script.

**Rationale**:

- v9 is the current stable line. v9 simplifies hook scripts: they are plain shell with no `husky.sh` source-line.
- The `prepare` lifecycle script runs automatically on `bun install` (Bun honors npm lifecycle scripts). This implements **FR-018**: Husky installs with zero extra steps for contributors.
- Hooks are tracked in git as plain files under `.husky/` so review of hook behaviour goes through the normal PR process.

**Alternatives considered**:

- **simple-git-hooks.** Rejected: the user explicitly requested Husky.
- **`pre-commit` (Python).** Rejected: introduces a Python toolchain dependency for a JS-stack repo.

---

## D5. lint-staged configuration

**Decision**: `lint-staged` configured in the root `package.json` `"lint-staged"` field. Globs:

```jsonc
"lint-staged": {
  "*.{js,mjs,cjs,ts,vue}": ["eslint --fix", "prettier --write"],
  "*.{json,jsonc,yml,yaml,md}": ["prettier --write"]
}
```

**Rationale**:

- Order matters: `eslint --fix` first, then `prettier --write`. ESLint may make code-quality fixes that change indentation/whitespace; running Prettier last ensures the final stylistic state is canonical (Prettier-compliant).
- lint-staged automatically re-stages files modified by these commands, satisfying **FR-020(c)**.
- Non-code files (JSON / YAML / Markdown) skip ESLint entirely — ESLint has no rules for them.
- Each command is invoked with the staged file paths only; lint-staged transparently passes them as arguments. This satisfies the "staged subset" semantics of the pre-commit hook (US3).

**Edge handling**:

- When no staged files match any glob, lint-staged exits 0 (no-op) — implements **FR-022**.
- When a command fails non-zero (e.g., ESLint error not auto-fixable), lint-staged restores the working tree from a stash it took before running, exits non-zero, and the commit is aborted — implements **FR-021**.

---

## D6. Integration test wiring

**Decision**: A second Vitest config at `frontend/vitest.integration.config.ts` targeting `frontend/tests/integration/**/*.test.ts`. The `test:integration` script invokes Vitest with this config and `--passWithNoTests`.

**Rationale**:

- Independent invocability is required by **FR-014** (manual command), **FR-026** (pre-push), and **FR-032(d)** (CI). A separate config is the cleanest way to achieve this without mixing test pools.
- Integration tests will eventually be slower than unit tests (server-route invocation, store-with-real-fetch, etc.) and may need a different `environment` (e.g., `node` instead of `nuxt`). Splitting now makes future divergence trivial.
- `--passWithNoTests` makes the script exit 0 when no integration test files exist yet, satisfying the spec's "no-op when empty" requirement (**FR-014**, **FR-028**, edge case "Empty test suite").
- The placeholder directory `frontend/tests/integration/` is created with a `.gitkeep` so the path exists before any tests are written; it also gives contributors a discoverable home for their first integration test.

**Alternatives considered**:

- **One unified Vitest config with workspace projects.** Rejected: Vitest's project/workspace feature makes "run only integration" awkward and harder to script in a hook. Two configs is more boring and easier to reason about.
- **Use a different runner (e.g., Jest) for integration.** Rejected: introduces an extra framework when Vitest already covers the territory.
- **Skip integration wiring entirely until first integration test is written.** Rejected: contradicts spec assumption "the wiring … MUST exist so adding the first integration test requires no further plumbing."

---

## D7. Pre-push hook scope and runtime

**Decision**: Pre-push runs `bun run test:unit` then `bun run test:integration`. It does NOT run `bun run test:e2e`. Both inner commands use the non-watch form (`vitest run --config <config>`).

**Rationale**:

- Implements **FR-025**, **FR-026**, **FR-027** directly.
- e2e exclusion is justified at length in US4 "Why this priority" — too slow, requires browser binaries and an app server.
- Non-watch mode is mandatory in the hook (otherwise it would hang); also addressed by edge case "Test commands run in watch mode by default" in the spec.

**Performance check against SC-004 (60s budget)**:

- Current unit suite (`frontend/tests/unit/stores/`) is small. Vitest's typical run-time on a tiny suite is < 5s on a cold start, < 2s warm.
- Integration suite is empty today. Once it grows, the team can monitor SC-004 and split slow specs into a different category if needed (the spec's edge case "Pre-push runtime regressions" addresses this directly).

---

## D8. CI provider, runner, and concurrency

**Decision**: GitHub Actions on `ubuntu-latest`. One workflow file at `.github/workflows/pull-request.yml` with five top-level jobs: `format`, `lint`, `unit`, `integration`, `e2e`. Jobs run in parallel (default for top-level jobs). A `concurrency` block cancels superseded runs of the same PR.

**Rationale**:

- `pull_request` trigger with `branches: [main]` covers **FR-031** (run on every PR targeting `main`).
- Five separate jobs (rather than one job with five steps) gives **independently-named status checks**, which is required by **FR-035** for branch protection. It also lets a contributor see at a glance which gate failed without scrolling a single combined log.
- `concurrency: { group: pr-${{ github.event.pull_request.number }}, cancel-in-progress: true }` prevents stacked runs when a PR is force-pushed multiple times in quick succession; the latest commit always determines the gate state. This protects SC-009 (latency) without weakening SC-002 (no merge while failing).

**Alternatives considered**:

- **Single job with sequential steps.** Rejected: serializes wall-clock time (would blow SC-005 because e2e is the slowest job and would block format/lint/unit feedback on it).
- **Reusable workflow per check.** Rejected: not enough complexity to justify the indirection at our current scale.
- **Self-hosted runners.** Rejected: no infrastructure team or budget allocated for that.

---

## D9. Bun setup and caching in CI

**Decision**: Use `oven-sh/setup-bun@v2`, pinned via a `.bun-version` file at the repo root (or via `bun-version: <pinned>` directly in the workflow). Cache `~/.bun/install/cache` keyed on `bun.lock`.

**Rationale**:

- A pinned Bun version makes CI builds deterministic and prevents surprise regressions when `bun-latest` ships a breaking change.
- Caching `~/.bun/install/cache` is the documented Bun-on-Actions pattern; cache hits cut install from ~30s to ~3s for unchanged lockfiles.
- Cache key: `bun-${{ runner.os }}-${{ hashFiles('bun.lock') }}` with a fallback `bun-${{ runner.os }}-`.

---

## D10. Playwright in CI: caching and webServer

**Decision**: Cache `~/.cache/ms-playwright`; install browsers only on cache miss (`npx playwright install --with-deps chromium` or `bunx playwright install --with-deps chromium`). Run e2e against the **preview** server (`bun run preview`) rather than dev (`bun run dev`) by making `playwright.config.ts` CI-aware. Run `bun run build` once before e2e starts.

**Rationale**:

- Browser binaries are ~150 MB; downloading them on every CI run is wasteful and pushes wall-clock past SC-005's 8-minute target.
- Preview mode (production build, no HMR) starts faster and is more representative of production behaviour. Dev mode is fine locally but slower and noisier in CI.
- `--with-deps` ensures system libraries Playwright needs are installed (only the first run pays this cost).
- We pin to a single browser project (Chromium) for the PR check; the multi-browser run can be a nightly job if we want it later.

**`playwright.config.ts` change** (illustrative):

```ts
webServer: {
  command: process.env.CI ? 'bun run preview' : 'bun run dev',
  url: 'http://localhost:3000',
  reuseExistingServer: !process.env.CI,
  timeout: 120_000,
}
```

---

## D11. e2e flakiness and retries

**Decision**: `playwright.config.ts` `retries: process.env.CI ? 1 : 0` (down from current `2`).

**Rationale**:

- The spec's **FR-040** explicitly authorises a _single bounded_ retry; **SC-011** sets a 2% flakiness budget over a rolling 30-day window. Retrying twice (current config) is more lenient than the spec allows and would mask flakiness past the budget threshold.
- `retries: 1` still absorbs the most common transient failures (network blips, animation timing) without hiding consistently-failing specs.
- On retry, Playwright records both the original failure and the retry; artifacts are preserved.

**Artifact upload**: configure `actions/upload-artifact@v4` to upload `frontend/playwright-report/` and `frontend/test-results/` whenever the e2e job fails. Use `if: failure()` and `retention-days: 14`.

---

## D12. Documentation home

**Decision**: A new `README.md` at the repo root (the project currently has none) plus a `CONTRIBUTING.md` for the longer-form workflow guide. The frontend keeps its existing `frontend/README.md` for app-specific notes.

**Rationale**:

- A repo without a root README is unfriendly to new contributors. This feature provides the natural occasion to add one.
- The README covers the day-zero contributor experience: install, run, common commands.
- `CONTRIBUTING.md` covers the deeper hook/CI behavior (when to use `--no-verify`, how to reproduce a CI failure locally) so the README stays scannable.

---

## D13. Mass format/lint commit strategy

**Decision**: One configuration commit, then one **separate** mass-format commit titled along the lines of `chore: mass-format codebase via prettier/eslint`. Both happen on the same feature branch but are independent commits so reviewers can audit them separately.

**Rationale**:

- Reviewing a thousand-file Prettier diff in the same commit as the rule-configuration changes is impossible — reviewers would skim and rubber-stamp both.
- Keeping them separate also protects `git blame`: the mass-format commit can be added to `.git-blame-ignore-revs` so blame skips through it. (Adding `.git-blame-ignore-revs` to the repo is part of the implementation.)

---

## Summary

All NEEDS CLARIFICATION items are resolved. The plan is implementable with no remaining open questions. The next phase (`/speckit-tasks`) can produce a dependency-ordered task list against this research and the data model.
