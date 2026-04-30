# Marketplace

A multi-tenant SaaS marketplace platform. The frontend lives in `frontend/` (Nuxt 4); developer tooling (formatter, linter, hooks, CI) lives at the repository root and applies across the whole repo.

## Prerequisites

- [Bun](https://bun.sh) ≥ 1.1 (the version pinned in `.bun-version` is what CI uses)
- Git ≥ 2.40

No global packages are needed; everything else is a project-local devDependency.

## Quick start

```sh
git clone <this-repo>
cd marketplace
bun install
```

`bun install` activates the Husky git hooks automatically — no extra setup steps. After this, every commit and every push is gated by the rules in [`CONTRIBUTING.md`](./CONTRIBUTING.md).

To run the application:

```sh
bun --cwd frontend run dev
```

## Common commands

All commands run from the repository root.

| Command                    | What it does                                | When to use                                         |
| -------------------------- | ------------------------------------------- | --------------------------------------------------- |
| `bun run format`           | Format every file in place via Prettier     | After a large refactor or when `format:check` fails |
| `bun run format:check`     | Check formatting (read-only)                | Reproduce CI's `format` job locally                 |
| `bun run lint`             | Lint JS / TS / Vue files via ESLint         | Reproduce CI's `lint` job                           |
| `bun run lint:fix`         | Lint with auto-fix                          | Quick cleanup before a push                         |
| `bun run test:unit`        | Vitest unit tests (non-watch)               | Fast local feedback                                 |
| `bun run test:integration` | Vitest integration tests (no-op when empty) | After server-route or store-with-fetch changes      |
| `bun run test:e2e`         | Playwright end-to-end tests                 | After UI flow changes                               |
| `bun run test`             | Unit + integration (no e2e)                 | Default sanity check before pushing                 |

Watch mode for unit tests is available via the frontend workspace:

```sh
bun --cwd frontend run test
```

## Repository layout

```
.
├── package.json              # repo-root tooling devDeps + commands
├── eslint.config.js          # single ESLint flat config (whole repo)
├── .prettierrc.json          # single Prettier rule set (whole repo)
├── .husky/                   # git hooks (pre-commit, pre-push)
├── .github/workflows/        # CI: format / lint / unit / integration / e2e
└── frontend/                 # Nuxt 4 application
```

See [`CONTRIBUTING.md`](./CONTRIBUTING.md) for the contributor workflow, hook behavior, and how each CI check is reproduced locally.
