# Quickstart: Engineering Excellence & CI/CD

This document outlines how to adhere to the project's new engineering standards.

## Local Development Enforcement

1. **Prerequisites**: Ensure you have run `bun install`.
2. **Husky**: Git hooks are automatically installed via `bun run prepare` (configured to run on `postinstall`).
3. **Commit Process**: 
   - When you `git commit`, `lint-staged` will automatically run ESLint and Prettier on your staged files.
   - If linting errors occur, the commit will be blocked. Fix them and retry.
   - If formatting errors occur, they will be automatically fixed; you may need to re-stage the changes.

## CI/CD Pipeline

- All Pull Requests to `main` branch are automatically checked by GitHub Actions.
- Pipeline will fail if:
  - Linting or formatting checks fail.
  - Test suite does not pass.
  - Test coverage is below 90%.

## Common Commands

All commands run from the project root (or within `frontend/`):

| Command | Purpose |
|---------|---------|
| `bun run lint` | Run ESLint across the project |
| `bun run format` | Run Prettier across the project |
| `bun run test` | Run tests in watch mode |
| `bun run test:coverage` | Run tests and generate coverage report |
