# Contributing

Thanks for working on this codebase. This guide covers the day-to-day mechanics of getting set up, the local quality gates, and what CI enforces before merge.

## Setup

```sh
git clone <this-repo>
cd marketplace
bun install
```

That's it. `bun install` runs the `prepare` script, which activates Husky. From that point on, the `pre-commit` and `pre-push` hooks defined in `.husky/` run automatically.

## Local quality gates

### Pre-commit hook

Runs **on every `git commit`**. Triggers [lint-staged](https://github.com/lint-staged/lint-staged), which:

- Runs `eslint --fix` on staged `*.{js,mjs,cjs,ts,vue}` files.
- Runs `prettier --write` on staged `*.{js,mjs,cjs,ts,vue,json,jsonc,yml,yaml,md}` files.
- Re-stages anything it modified, so the commit captures the fixed content.

**What you see**:

- A clean commit completes in seconds.
- An auto-fixable issue → silently fixed, commit proceeds.
- A non-auto-fixable lint error (e.g., an unused variable) → commit aborts with a clear message identifying the file, line, and rule. Fix and re-commit.

**Will not** run any tests. Tests are reserved for `pre-push` and CI.

### Pre-push hook

Runs **on every `git push`**. Executes:

- `bun run test:unit` (Vitest, non-watch)
- `bun run test:integration` (Vitest with the integration config; no-op when the suite is empty)

If anything fails, the push is aborted. Fix the test (or the code) and push again.

**Will not** run end-to-end tests. e2e is too slow and environment-heavy for a push hook; CI runs it.

### Escape hatches

`git commit --no-verify` and `git push --no-verify` skip their respective hooks. **Use them sparingly** — typically only for genuine work-in-progress commits/pushes you intend to clean up before opening a PR. CI still enforces every check at merge time, so a hook bypass cannot land in `main`. Routine `--no-verify` is not a substitute for fixing violations.

## CI checks (Pull Request gate)

Every PR targeting `main` runs five parallel jobs in `.github/workflows/pull-request.yml`. All five must pass before merge.

| Check         | Reproduce locally          | What it catches                                                |
| ------------- | -------------------------- | -------------------------------------------------------------- |
| `format`      | `bun run format:check`     | Files Prettier would reformat                                  |
| `lint`        | `bun run lint`             | ESLint errors (unused vars, type issues, Vue/Nuxt rules, etc.) |
| `unit`        | `bun run test:unit`        | Vitest unit suite failures                                     |
| `integration` | `bun run test:integration` | Vitest integration suite failures                              |
| `e2e`         | `bun run test:e2e`         | Playwright end-to-end failures                                 |

When the `e2e` check fails, the workflow uploads a **playwright-artifacts** bundle (HTML report, traces, screenshots) to the run page so you can diagnose without rerunning locally.

These check names are stable and are configured as required status checks for branch protection on `main`. If you add a new CI check in a future PR, it should be added as non-required first, observed, then promoted via a separate branch-protection update.

## Recovering from a blocked commit or push

1. Read the error output. The hook tells you exactly which file and rule failed.
2. Fix the issue (often `bun run lint:fix` or `bun run format` is enough).
3. Re-stage (`git add <files>`) and re-run the commit / push.
4. If you genuinely need to bypass for a WIP, use `--no-verify` and plan to fix before opening a PR.

## Test writing conventions

- **Unit tests** go under `frontend/tests/unit/` with the `*.test.ts` suffix.
- **Integration tests** go under `frontend/tests/integration/` with the `*.test.ts` suffix. The wiring exists today even though the directory starts empty.
- **End-to-end tests** go under `frontend/tests/e2e/` with the `*.spec.ts` suffix.

## Code style

- **Prettier owns all stylistic decisions.** ESLint is configured to defer to Prettier on conflicts.
- The Prettier rule set is in `.prettierrc.json`. Changing it is a reviewable rule change — it affects every file in the repo.

## Local Auth Setup

The backend uses Google OAuth via Keycloak. To run the full stack locally:

### Prerequisites

- Docker Desktop (or Docker Engine + Compose)
- A Google Cloud Console project with an OAuth 2.0 client ID (Web application type)
- Java 25 (via `asdf install java temurin-25.0.3+9.0.LTS` or equivalent)

### Steps

1. **Copy the env file and fill in your credentials:**

   ```sh
   cp .env.example .env
   # Edit .env — set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, NUXT_PUBLIC_GOOGLE_CLIENT_ID
   ```

   The `KEYCLOAK_CLIENT_SECRET` can be any string for local development (e.g., `dev-secret`).

2. **Start all services:**

   ```sh
   docker compose up
   ```

   This starts Postgres (port 5432), Keycloak (port 8180), the Spring Boot backend (port 8080), and the Nuxt frontend (port 3000). Keycloak auto-imports the `marketplace` realm from `docker/keycloak/realm-export.json`.

3. **Verify the stack is up:**
   - Frontend: `http://localhost:3000`
   - Backend Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
   - Keycloak Admin: `http://localhost:8180` (user: `admin`, password: `admin`)

4. **Complete the manual quickstart verification:**

   See [`specs/005-google-oauth-login/quickstart.md`](./specs/005-google-oauth-login/quickstart.md) for the full step-by-step walkthrough including the button login flow, One Tap, session revocation, and error scenarios.

### Backend-only development

To run the backend without Docker:

```sh
# Requires a local Postgres and Keycloak, or docker compose up postgres keycloak
cd backend
JAVA_HOME=$(asdf where java) ./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile disables cookie security flags and enables Swagger UI without auth.

### Backend CI commands

```sh
cd backend
JAVA_HOME=$(asdf where java) ./gradlew spotlessCheck   # formatting
JAVA_HOME=$(asdf where java) ./gradlew test            # unit tests only
JAVA_HOME=$(asdf where java) ./gradlew integrationTest # integration tests (requires Docker)
```

## Further reading

The full feature spec for this tooling lives at [`specs/004-code-quality-tooling/`](./specs/004-code-quality-tooling/), including a [quickstart walkthrough](./specs/004-code-quality-tooling/quickstart.md) you can use to verify your local environment end-to-end.

The Google OAuth login spec lives at [`specs/005-google-oauth-login/`](./specs/005-google-oauth-login/).
