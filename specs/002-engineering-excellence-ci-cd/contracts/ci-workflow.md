# CI Pipeline Contract

The CI pipeline exposes a defined interface to the repository.

- **Trigger**: `pull_request` to `main` branch.
- **Output**: CI status checks (success/failure) visible in GitHub PR.
- **Artifacts**: 
  - Coverage reports (upload as artifacts for inspection).
  - Test logs.
- **Failure Conditions**:
  - `lint` exit code != 0
  - `test` exit code != 0
  - Coverage < 90%
