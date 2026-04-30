---
description: 'Task list for implementing Code Quality, Linting & Test Automation'
---

# Tasks: Code Quality, Linting & Test Automation

**Input**: Design documents from `/specs/004-code-quality-tooling/`  
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: This feature is itself test infrastructure; no additional unit tests are required _for the feature_. The acceptance criteria are validated end-to-end by the [quickstart.md](./quickstart.md) walkthrough (T028).

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently. The five user stories from the spec map to phases 3–7.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks).
- **[Story]**: Maps task to a user story for traceability (US1, US2, US3, US4, US5).
- All paths below are repo-root relative unless they explicitly start at `frontend/`.

## Path Conventions

This feature uses a **monorepo layout** with tooling at the **repository root** and the existing application in `frontend/`. The repo-root `package.json` declares Bun workspaces with `frontend` as the only member. See `Project Structure` in [plan.md](./plan.md) for the full layout.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the workspace skeleton, install all dev dependencies, and activate Husky. After this phase, `bun install` at the root produces a working dev environment with Husky active but no hooks yet — this is intentional, hooks land in their respective story phases.

- [x] T001 Create the root `package.json` at `/Users/michael/repositories/marketplace/package.json` declaring `"name": "marketplace"`, `"private": true`, `"type": "module"`, `"workspaces": ["frontend"]`, the `devDependencies` listed in [plan.md](./plan.md) Technical Context (`prettier@^3`, `eslint@^9`, `@nuxt/eslint@latest`, `@nuxt/eslint-config@latest`, `eslint-config-prettier@^9`, `husky@^9`, `lint-staged@^15`), and the full set of scripts from [contracts/npm-scripts.md](./contracts/npm-scripts.md): `format`, `format:check`, `lint`, `lint:fix`, `test:unit`, `test:integration`, `test:e2e`, `test`, `prepare`. Use `bun --cwd frontend run <inner>` for scripts that forward into the workspace.

- [x] T002 Run `bun install` from the repo root to install all devDependencies and trigger the `prepare` script. Verify: (a) `bun.lock` exists at the repo root, (b) `.husky/_/` directory was created by Husky, (c) `git config core.hooksPath` returns `.husky`. If any verification fails, investigate before proceeding.

- [x] T003 [P] Update `/Users/michael/repositories/marketplace/.gitignore` to ignore the new tooling artifacts at the root. Add (if not already present): `node_modules/`, `.husky/_/`, `frontend/playwright-report/`, `frontend/test-results/`, `*.local`.

- [x] T004 [P] Create `/Users/michael/repositories/marketplace/.bun-version` containing a single line with the pinned Bun version (e.g. the version shown by `bun --version`). This file is consumed by `oven-sh/setup-bun@v2` in CI for deterministic builds (research/D9).

- [x] T005 [P] Create the integration-test placeholder directory: write a `.gitkeep` file at `/Users/michael/repositories/marketplace/frontend/tests/integration/.gitkeep`. This makes the path discoverable to contributors and ensures the test glob has a directory to walk even before the first integration test exists.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration files that every user story depends on. Until this phase is complete, none of the npm scripts will function and CI/hook tasks downstream will fail.

**⚠️ CRITICAL**: No user story work in Phases 3–7 may begin until Phase 2 is complete.

- [x] T006 [P] Create `/Users/michael/repositories/marketplace/.prettierrc.json` with the rules from research/D3: `{"semi": false, "singleQuote": true, "trailingComma": "all", "printWidth": 100}`. Document the rationale via the file location (no comments — JSON does not support them; rationale lives in research.md/D3).

- [x] T007 [P] Create `/Users/michael/repositories/marketplace/.prettierignore` listing the excludes from research/D3: `node_modules/`, `**/.nuxt/`, `**/.output/`, `**/dist/`, `**/build/`, `**/bun.lock`, `**/package-lock.json`, `**/yarn.lock`, `**/pnpm-lock.yaml`, `frontend/i18n/locales/*.json`, `frontend/lighthouse-report-*.json`, `frontend/playwright-report/`, `frontend/test-results/`, `**/*.snap`.

- [x] T008 [P] Create `/Users/michael/repositories/marketplace/eslint.config.js` as an ESLint v9 flat config using the **artifact-free** `createConfigForNuxt` helper from `@nuxt/eslint-config/flat` — this pattern does NOT depend on `frontend/.nuxt/eslint.config.mjs` existing, so T008 has no ordering dependency on T009 or on `nuxi prepare`. Skeleton:

  ```js
  // eslint.config.js
  import { createConfigForNuxt } from '@nuxt/eslint-config/flat'
  import prettier from 'eslint-config-prettier'

  export default createConfigForNuxt({
    features: { stylistic: false }, // Prettier owns stylistic decisions
    dirs: { src: ['frontend'] },
  })
    .append({
      ignores: [
        'node_modules/',
        '**/.nuxt/',
        '**/.output/',
        '**/dist/',
        '**/build/',
        'frontend/playwright-report/',
        'frontend/test-results/',
        'frontend/i18n/locales/*.json',
      ],
    })
    .append(prettier) // MUST be last so Prettier wins on stylistic rules (FR-008)
  ```

  Covers `*.{js,mjs,cjs,ts,vue}` (the helper's default file matcher) and ignores per data-model.md Entity 2.

- [x] T009 [P] Update `/Users/michael/repositories/marketplace/frontend/nuxt.config.ts` to register the `@nuxt/eslint` module. Add `'@nuxt/eslint'` to the `modules` array (preserving the existing `@pinia/nuxt`, `@nuxtjs/i18n`, `shadcn-nuxt` entries). The module powers the in-Nuxt-devtools ESLint integration and emits diagnostics during `nuxi dev`; it is **not** required for T008's root flat config to work (T008 uses `createConfigForNuxt` directly), so T008 and T009 are genuinely independent.

- [x] T010 [P] Create `/Users/michael/repositories/marketplace/frontend/vitest.integration.config.ts` mirroring the existing `vitest.config.ts` but with `include: ['tests/integration/**/*.test.ts']`. Use the same `defineVitestConfig` import and the same `environment: 'nuxt'` baseline. This is data-model.md Entity 4.

- [x] T011 [P] Update `/Users/michael/repositories/marketplace/frontend/playwright.config.ts`: change `retries: process.env.CI ? 2 : 0` to `retries: process.env.CI ? 1 : 0` (FR-040, SC-011); change `webServer.command` from `'bun run dev'` to `process.env.CI ? 'bun run preview' : 'bun run dev'` (research/D10). Leave the rest of the config (projects, baseURL, fullyParallel, etc.) unchanged.

- [x] T012 [P] Add a `test:integration` script to `/Users/michael/repositories/marketplace/frontend/package.json`: `"test:integration": "vitest run --config vitest.integration.config.ts --passWithNoTests"`. Leave existing scripts (`test`, `test:unit`, `test:e2e`, `test:e2e:ui`, `build`, `dev`, etc.) untouched.

**Checkpoint**: After Phase 2, the contract surface from [contracts/npm-scripts.md](./contracts/npm-scripts.md) is fully functional. `bun run format`, `bun run format:check`, `bun run lint`, `bun run lint:fix`, `bun run test:unit`, `bun run test:integration`, and `bun run test:e2e` all work — they just don't yet run from any hook or CI gate.

---

## Phase 3: User Story 1 — Block PRs that fail formatting or linting (P1) 🎯 MVP

**Goal**: Pull requests targeting `main` automatically run Prettier and ESLint; failures appear as red `format` and `lint` status checks.

**Independent Test**: Open a PR that introduces a deliberate formatting violation (e.g., wrong indentation in a `.ts` file) — the `format` check turns red and the log identifies the offending file. Open a clean PR — both checks turn green.

- [x] T013 [US1] Create `/Users/michael/repositories/marketplace/.github/workflows/pull-request.yml` with the workflow header from [contracts/ci-checks.md](./contracts/ci-checks.md): `name: pull-request`, `on: pull_request: branches: [main]`, and a `concurrency` block that groups by PR number with `cancel-in-progress: true`. Leave `jobs:` empty for now — subsequent tasks add jobs into this file.

- [x] T014 [US1] Add the `format` job to `/Users/michael/repositories/marketplace/.github/workflows/pull-request.yml`. Job ID and `name:` both `format`. Steps: `actions/checkout@v4`, `oven-sh/setup-bun@v2` with `bun-version-file: .bun-version`, cache `~/.bun/install/cache` keyed on `bun.lock`, `bun install --frozen-lockfile`, then `bun run format:check`. Runs on `ubuntu-latest`.

- [x] T015 [US1] Add the `lint` job to `/Users/michael/repositories/marketplace/.github/workflows/pull-request.yml` with the same setup steps as `format` (T014) but the final step is `bun run lint`. Job ID and `name:` both `lint`. (Edits the same file as T014; sequential.)

**Checkpoint**: After Phase 3, opening a PR triggers parallel `format` and `lint` checks. The MVP gate is functional. US1 acceptance scenarios 1–5 pass.

---

## Phase 4: User Story 2 — Block PRs that fail tests (P1)

**Goal**: PRs run unit, integration, and e2e tests in parallel CI jobs; any failing test blocks merge.

**Independent Test**: Open a PR with a deliberately failing unit test → `unit` check red. Open a PR with a deliberately failing e2e test → `e2e` check red, with an artifact bundle (Playwright trace + screenshots) downloadable from the run page. Clean PR → all three checks green.

**Depends on**: T013 (the workflow file). Conceptually independent of US1's content, but lives in the same file.

- [x] T016 [US2] Add the `unit` job to `/Users/michael/repositories/marketplace/.github/workflows/pull-request.yml` mirroring the `format`/`lint` job setup but with the final step `bun run test:unit`. Job ID and `name:` both `unit`.

- [x] T017 [US2] Add the `integration` job to `/Users/michael/repositories/marketplace/.github/workflows/pull-request.yml` with the same setup pattern; final step `bun run test:integration`. Job ID and `name:` both `integration`. The job MUST exit 0 when no integration tests exist (the `--passWithNoTests` flag in the script handles this — no extra workflow logic needed).

- [x] T018 [US2] Add the `e2e` job to `/Users/michael/repositories/marketplace/.github/workflows/pull-request.yml`. Steps in order: `actions/checkout@v4`, `oven-sh/setup-bun@v2` with `bun-version-file: .bun-version`, cache Bun install dir, `bun install --frozen-lockfile`, cache `~/.cache/ms-playwright` keyed on the resolved Playwright version, `bunx playwright install --with-deps chromium` (only on cache miss — gate with `if: steps.<cache-id>.outputs.cache-hit != 'true'`), `bun --cwd frontend run build`, `bun run test:e2e`, then on failure (`if: failure()`) upload artifacts: `actions/upload-artifact@v4` with `name: playwright-artifacts-${{ github.run_id }}`, `path: |\n  frontend/playwright-report\n  frontend/test-results`, `retention-days: 14`. Job ID and `name:` both `e2e`.

**Checkpoint**: After Phase 4, all five CI checks (`format`, `lint`, `unit`, `integration`, `e2e`) run in parallel on every PR. US2 acceptance scenarios 1–7 pass. SC-005 (≤8 min wall-clock) and SC-010 (artifacts on every e2e failure) are now measurable.

---

## Phase 5: User Story 3 — Auto-format and lint staged files before commit (P1)

**Goal**: A pre-commit hook runs lint-staged so contributors get sub-10-second formatting and lint enforcement on every commit.

**Independent Test**: Stage a deliberately-unformatted file and `git commit`. The hook reformats the staged content and the commit captures the formatted version. Stage a file with an unfixable lint error → commit aborts with a clear message.

**Depends on**: Phase 2 (configs and scripts exist; Husky activated).

- [x] T019 [US3] Add a `"lint-staged"` block to `/Users/michael/repositories/marketplace/package.json` per research/D5: keys `"*.{js,mjs,cjs,ts,vue}"` → `["eslint --fix", "prettier --write"]`, and `"*.{json,jsonc,yml,yaml,md}"` → `["prettier --write"]`. Place the block at the top level alongside `scripts`, `devDependencies`, `workspaces`.

- [x] T020 [US3] Create `/Users/michael/repositories/marketplace/.husky/pre-commit` containing `#!/usr/bin/env sh\nbun run lint-staged\n`. Make it executable: `chmod +x .husky/pre-commit`. (Husky v9 does not require a `husky.sh` source line.)

**Checkpoint**: After Phase 5, commits are protected by the pre-commit hook. US3 acceptance scenarios 1–6 pass. SC-003 (<10s on typical commit) is measurable.

---

## Phase 6: User Story 4 — Run tests locally before pushing (P2)

**Goal**: A pre-push hook runs unit + integration tests so contributors catch regressions before opening a PR.

**Independent Test**: Break a unit test, commit, then `git push` — push aborts with the failing test name. Confirm the hook does NOT start Playwright or download browser binaries.

**Depends on**: Phase 2 (test scripts exist).

- [x] T021 [US4] Create `/Users/michael/repositories/marketplace/.husky/pre-push` containing `#!/usr/bin/env sh\nbun run test:unit\nbun run test:integration\n`. Make it executable: `chmod +x .husky/pre-push`. The hook MUST NOT invoke `test:e2e` (FR-027).

**Checkpoint**: After Phase 6, pushes run unit and integration tests locally. US4 acceptance scenarios 1–6 pass. SC-004 (<60s) is measurable.

---

## Phase 7: User Story 5 — Manual commands documented (P2)

**Goal**: Every documented command from [contracts/npm-scripts.md](./contracts/npm-scripts.md) is discoverable in contributor-facing documentation, with a one-line description of when to use it.

**Independent Test**: A new contributor reads the README and `CONTRIBUTING.md` and can identify which command runs format checking, which runs unit tests, which runs e2e, etc. Running each documented command works exactly as the docs say.

**Depends on**: Phase 2 (commands exist).

- [x] T022 [P] [US5] Create `/Users/michael/repositories/marketplace/README.md` (the project currently has none) with: project name, one-paragraph description, prerequisites (Bun ≥ 1.1), install (`bun install`), and a "Common commands" section listing all eight scripts from [contracts/npm-scripts.md](./contracts/npm-scripts.md) with a one-line description of each. Reference the deeper guide in `CONTRIBUTING.md` for hooks and CI behavior. Implements FR-041.

- [x] T023 [P] [US5] Create `/Users/michael/repositories/marketplace/CONTRIBUTING.md` covering: (a) install workflow (`bun install` activates Husky; no extra steps), (b) pre-commit hook behavior including how to recover from a blocked commit and when `--no-verify` is appropriate, (c) pre-push hook behavior with the same recovery/escape-hatch guidance, (d) the five CI check names (`format`, `lint`, `unit`, `integration`, `e2e`) and how to reproduce each failure locally (point to [contracts/ci-checks.md](./contracts/ci-checks.md)), (e) link to [quickstart.md](./quickstart.md) for end-to-end validation. Implements FR-042 and FR-043.

**Checkpoint**: After Phase 7, US5 acceptance scenarios 1–8 pass. New contributors can self-onboard.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: One-time mass-format/lint-fix of the existing codebase, blame-revs hygiene, end-to-end validation, and the out-of-band branch protection note.

- [ ] T024 Run `bun run format` from the repo root to mass-format the codebase per the spec assumption "one-time mass-format and lint-fix pass". Stage all changes and commit as a single commit titled `chore: mass-format codebase via prettier` (no other changes in this commit).

- [ ] T025 Run `bun run lint:fix` from the repo root. Apply auto-fixes; manually resolve any remaining ESLint errors that are not auto-fixable. Commit as `chore: address eslint findings`. The codebase MUST report zero ESLint errors at "error" severity after this task (SC-007).

- [ ] T026 Add the commit hash from T024 to a new file `/Users/michael/repositories/marketplace/.git-blame-ignore-revs` (one hash per line). This makes `git blame` skip past the mass-format commit so blame attribution still reflects the original author. Optionally add the hash from T025 too if it's also large. Commit the file as `chore: add mass-format commit to blame-ignore-revs`.

- [x] T027 Verify SC-007 from a clean working tree: run `bun run format:check` and confirm exit 0; run `bun run lint` and confirm exit 0. If either fails, fix the offending content and amend the appropriate prior commit, or add a new fix commit.

- [ ] T028 Walk through every numbered step of [quickstart.md](./quickstart.md) (steps 1–12). Document any deviations as follow-up issues; the feature is not Done until each step matches the documented "Expected" outcome.

- [ ] T029 **Out-of-band administrative action (not implementable in code)**: a maintainer with write access to the repository MUST configure GitHub branch protection on `main` to require the five status checks (`format`, `lint`, `unit`, `integration`, `e2e`) per [contracts/ci-checks.md](./contracts/ci-checks.md) and FR-035. This task is tracked here for visibility; it is satisfied when the protection rules are in place. SC-002 (no merge while any required check is failing) is enforced by this configuration, not by code.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** — no dependencies; starts immediately.
- **Foundational (Phase 2)** — depends on Phase 1; **blocks all user stories**.
- **User Story 1 (Phase 3, P1)** — depends on Foundational. Creates the workflow file.
- **User Story 2 (Phase 4, P1)** — depends on Foundational AND on T013 (workflow file existing). Conceptually independent of US1's _behavior_ (you could ship US1 alone or US2 alone), but they share the workflow file as a host.
- **User Story 3 (Phase 5, P1)** — depends on Foundational. Independent of US1/US2.
- **User Story 4 (Phase 6, P2)** — depends on Foundational. Independent of US1/US2/US3.
- **User Story 5 (Phase 7, P2)** — depends on Foundational. Independent of all other stories.
- **Polish (Phase 8)** — depends on every prior phase. T024 specifically should run after the configs (Phase 2) so the format command knows what to do; T028 (quickstart) should run last.

### Within-Phase File Conflicts

- T013, T014, T015, T016, T017, T018 all edit `.github/workflows/pull-request.yml` → must be sequential.
- T001 and T019 both edit root `package.json` → T019 happens after Phase 1 completes.
- All foundational tasks (T006–T012) edit different files → marked [P].

### Parallel Opportunities

- **Phase 1**: T003, T004, T005 are [P] (different files). T001 → T002 are sequential (T002 depends on T001 to install).
- **Phase 2**: T006 through T012 are all [P] (different files; T009 modifies nuxt.config.ts but only adds a module entry, isolated from other foundational work).
- **Phase 3 vs Phase 5 vs Phase 6 vs Phase 7**: once Foundational is done, US1, US3, US4, US5 can be picked up by separate developers in parallel. US2 must follow T013 from US1.
- **Phase 7**: T022 and T023 are [P] (different files).

---

## Parallel Example: Phase 2 (Foundational)

```bash
# All seven foundational config tasks can run in parallel:
Task: "Create .prettierrc.json (T006)"
Task: "Create .prettierignore (T007)"
Task: "Create eslint.config.js (T008)"
Task: "Update frontend/nuxt.config.ts to register @nuxt/eslint module (T009)"
Task: "Create frontend/vitest.integration.config.ts (T010)"
Task: "Update frontend/playwright.config.ts (T011)"
Task: "Add test:integration script to frontend/package.json (T012)"
```

## Parallel Example: Multiple stories in flight

```bash
# After Foundational, three developers can pick up independent stories:
Developer A: T013, T014, T015 (US1: workflow + format/lint jobs)
Developer B: T019, T020 (US3: pre-commit hook + lint-staged config)
Developer C: T021 (US4: pre-push hook)

# A separate doc-focused contributor can write docs in parallel:
Developer D: T022, T023 (US5: README + CONTRIBUTING)

# US2 (T016–T018) waits for Developer A to land T013.
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1: Setup (T001–T005)
2. Phase 2: Foundational (T006–T012)
3. Phase 3: US1 (T013–T015)
4. **Stop and validate**: open a draft PR with a deliberate formatting violation; confirm the `format` check fails as expected. Open a clean PR; confirm both `format` and `lint` are green.
5. Land the MVP. Maintainers can already configure branch protection for `format` and `lint` (T029 partial).

### Incremental Delivery

1. MVP (above) → demo + protect.
2. Add US2 (T016–T018) → tests now run on every PR → expand branch protection to include `unit`, `integration`, `e2e`.
3. Add US3 (T019–T020) → pre-commit hook → demo locally.
4. Add US4 (T021) → pre-push hook → demo locally.
5. Add US5 (T022–T023) → docs published.
6. Polish (T024–T029) → mass-format, blame-revs hygiene, quickstart validation, branch-protection completion.

### Parallel Team Strategy

With four developers, after Phases 1–2 complete:

- Developer A: US1 (T013–T015)
- Developer B: US3 (T019–T020) and then US4 (T021)
- Developer C: US2 (T016–T018) — starts as soon as Developer A finishes T013
- Developer D: US5 (T022–T023) and then leads the polish phase (T024–T028)

T029 (branch protection) is performed once by a maintainer with admin rights, ideally just before merging the feature so the rules apply to the merge commit's downstream PRs.

---

## Notes

- **No test tasks for the feature itself**: The feature is test infrastructure; its acceptance test is the [quickstart.md](./quickstart.md) walkthrough (T028). This is consistent with the spec's checklist note: "this feature is itself test infrastructure."
- **One-time mass-format**: The big diff from T024 lives in its own commit so reviewers can audit the rule configuration (Phase 2) separately from the file rewrites (T024). The `.git-blame-ignore-revs` file (T026) keeps blame attribution intact.
- **Branch protection (T029)** is the only step that cannot be performed by code; it is an administrative GitHub setting. The spec acknowledges this explicitly under Assumptions / "Branch protection".
- **Husky activation timing**: Husky activates as soon as T002 runs (because T001 includes the `prepare` script). The `.husky/` directory exists from that moment, but the hooks (`.husky/pre-commit`, `.husky/pre-push`) only become enforcing once T020 and T021 land. This is intentional — it lets you validate the workspace install path before any local enforcement begins.
- Commit after each task or logical group; resist bundling unrelated changes.
- Stop at any checkpoint to validate the corresponding user story independently.
