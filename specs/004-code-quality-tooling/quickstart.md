# Quickstart: Code Quality, Linting & Test Automation

**Date**: 2026-04-30  
**Plan**: [./plan.md](./plan.md)

This quickstart doubles as the **acceptance test** for the feature. After implementation, walking through these steps from a fresh clone should produce the described outcomes with no extra setup. If any step diverges, the implementation has a bug.

---

## 0. Prerequisites

- macOS / Linux / Windows-WSL.
- Bun ≥ 1.1 installed (`curl -fsSL https://bun.sh/install | bash`).
- Git ≥ 2.40.

No other tooling is required globally; everything is project-local devDependencies.

---

## 1. Clean clone & install

```sh
git clone <repo-url> marketplace
cd marketplace
bun install
```

**Expected**:

- A single `bun.lock` is present at the root.
- `node_modules/` exists at the root and at `frontend/node_modules/` (Bun workspaces hoists where possible).
- `.husky/_/` directory has been created (Husky activated by the `prepare` lifecycle script).
- `git config core.hooksPath` returns `.husky`.

**Validates**: FR-018, US3 acceptance scenario 5, SC-006.

---

## 2. Inspect the contract surface

```sh
bun run --silent format --help 2>&1 | head -1   # Prettier present
bun run --silent lint --help 2>&1 | head -1     # ESLint present
ls .husky                                       # pre-commit, pre-push present
ls .github/workflows                            # pull-request.yml present
```

**Validates**: the eight scripts listed in [contracts/npm-scripts.md](./contracts/npm-scripts.md) all resolve.

---

## 3. Format the codebase (one-time, expected on first rollout only)

```sh
bun run format
git diff --stat
```

**Expected** (during initial rollout): a sizeable diff. After the rollout commit lands on `main`, this step should produce **no diff** for any clean clone.

```sh
bun run format:check
```

**Expected** (on a clean tree post-rollout): exit 0, no output.

**Validates**: FR-004, FR-005, SC-007, US5 acceptance scenarios 1–2.

---

## 4. Lint the codebase

```sh
bun run lint
```

**Expected** (post-rollout, clean tree): exit 0, no errors. Warnings allowed.

```sh
bun run lint:fix
```

**Expected**: exit 0, no diff (everything already fixed).

**Validates**: FR-010, FR-011, US5 acceptance scenarios 3–4.

---

## 5. Run the test suites manually

```sh
bun run test:unit
bun run test:integration   # no-op success today (empty suite)
bun run test:e2e
```

**Expected**:

- `test:unit` runs Vitest once and exits 0.
- `test:integration` exits 0 with a "no tests found" notice (empty suite, `--passWithNoTests`).
- `test:e2e` runs Playwright; locally it starts `bun run dev` as the webServer; it should pass on a clean tree.

**Validates**: FR-013, FR-014, FR-015, US5 acceptance scenarios 5–7.

---

## 6. Acceptance test: pre-commit hook fires on bad formatting

```sh
# Introduce a deliberate formatting issue
echo "const   foo='bar'" > frontend/scratch.ts
git add frontend/scratch.ts
git commit -m "test: bad format"
```

**Expected**:

- The commit succeeds.
- `frontend/scratch.ts` was rewritten to canonical Prettier form (`const foo = 'bar'`) by the pre-commit hook before the commit captured it.
- `git show HEAD:frontend/scratch.ts` displays the formatted version.

```sh
git reset --hard HEAD~1
rm -f frontend/scratch.ts
```

**Validates**: US3 acceptance scenario 1, FR-019, FR-020.

---

## 7. Acceptance test: pre-commit hook blocks unfixable lint errors

```sh
cat > frontend/scratch.ts <<'EOF'
export const x = 1
const unused: number = 2
EOF
git add frontend/scratch.ts
git commit -m "test: unused var"
```

**Expected**:

- The commit is **aborted** with a non-zero exit and an ESLint error message identifying `unused` as an unused-variable violation.
- The working tree is unchanged (`git status` shows `frontend/scratch.ts` still staged with the same contents).

```sh
git restore --staged frontend/scratch.ts
rm -f frontend/scratch.ts
```

**Validates**: US3 acceptance scenario 3, FR-021.

---

## 8. Acceptance test: pre-push hook blocks failing tests

```sh
# Introduce a test failure without breaking the build
cat > frontend/tests/unit/__sanity__.test.ts <<'EOF'
import { describe, expect, it } from 'vitest'
describe('quickstart sanity', () => {
  it('intentionally fails', () => {
    expect(1).toBe(2)
  })
})
EOF
git add frontend/tests/unit/__sanity__.test.ts
git commit -m "test: failing sanity"   # commit succeeds (no lint error)
git push origin HEAD                   # push aborts
```

**Expected**:

- `git push` is aborted with a non-zero exit. Vitest output identifies the failing test.
- Nothing is pushed to the remote.

```sh
git reset --hard HEAD~1
rm -f frontend/tests/unit/__sanity__.test.ts
```

**Validates**: US4 acceptance scenario 1, FR-025.

---

## 9. Acceptance test: pre-push hook does NOT run e2e

The pre-push hook from §8 ran in seconds (Vitest only). It did **not** start a browser, did **not** start the Nuxt server, did **not** download Playwright binaries.

**Validates**: US4 acceptance scenario 5, FR-027.

---

## 10. Acceptance test: escape hatches work

```sh
echo "const  spacedOut    = true" > frontend/scratch.ts
git add frontend/scratch.ts
git commit --no-verify -m "wip: skipping pre-commit"   # commits unchanged content
git push --no-verify origin HEAD                       # pushes without running tests
```

**Expected**: both succeed. CI on the resulting PR will fail the `format` and any test checks that catch the issue, demonstrating that local bypass cannot bypass the merge gate.

```sh
# Cleanup
git reset --hard HEAD~1
git push --force-with-lease origin HEAD     # only if you actually pushed
rm -f frontend/scratch.ts
```

**Validates**: edge cases "Bypassing the pre-commit hook" and "Bypassing the pre-push hook"; FR-030.

---

## 11. Acceptance test: CI runs the five checks on a PR

Open a pull request from a feature branch into `main`.

**Expected**: five status checks appear on the PR:

- `format`
- `lint`
- `unit`
- `integration`
- `e2e`

All five run in parallel. On a clean PR they all turn green within ~8 minutes (SC-005). On a PR that introduces a formatting violation, the `format` check turns red while the others may pass. Failure logs identify the offending file and rule (SC-008).

**Validates**: US1, US2, FR-031–FR-040, SC-005, SC-008, SC-009.

---

## 12. Acceptance test: e2e diagnostic artifacts on CI failure

Open a PR that breaks an e2e test (e.g., delete an element a Playwright spec selects).

**Expected**: the `e2e` check turns red. The workflow run page has a downloadable artifact bundle containing `playwright-report/` and `test-results/` (traces, screenshots).

**Validates**: FR-038, SC-010.

---

## What to do if a step fails

| Symptom                                               | Likely cause                                          | Where to look                                                      |
| ----------------------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------ |
| `bun: command not found`                              | Bun not installed                                     | Step 0 of this quickstart                                          |
| Hooks don't fire                                      | `bun install` did not run, or `prepare` script failed | Re-run `bun install`; check `.husky/_/` exists                     |
| `bun run format:check` fails post-rollout             | Codebase drifted                                      | `bun run format` then commit the diff                              |
| `test:e2e` fails locally with "browser not installed" | Playwright browsers not installed                     | `bunx playwright install chromium`                                 |
| CI `e2e` fails sporadically                           | Within-budget flakiness (FR-040, SC-011)              | Check the artifact bundle; if persistent, file a quarantine ticket |

This quickstart is the canonical acceptance script. CI passes + a clean walkthrough through every numbered step = the feature is done.
