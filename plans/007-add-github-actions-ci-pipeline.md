# Plan 007: Add GitHub Actions CI pipeline for automated test & build

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- .github`
> If the directory exists and changed, compare the workflow files below against the live versions before proceeding.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

No CI pipeline exists (`.github/workflows/` is absent). Merges happen without automated testing or build validation. Plans 1-6 (critical fixes) have no gating; a broken change can be merged and deployed. Solution: create a basic GitHub Actions workflow that runs `./mvnw clean verify` on every push and PR, blocking merges if tests fail or coverage drops below 80%.

## Current state

- **Directory**: `.github/workflows/` — does not exist
- **Effect**: No automated test/build gate on PRs; only local `./mvnw verify` protects code quality

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Check for .github | `ls -la .github/workflows/ 2>/dev/null` | should list workflow files after step 1 |
| Verify syntax | `cat .github/workflows/test.yml` | valid YAML |
| Dry-run locally (optional) | `./mvnw clean verify` | BUILD SUCCESS |

## Scope

**In scope**:
- Create `.github/workflows/test.yml` — runs tests on push/PR
- Create `.github/workflows/build.yml` (optional) — builds WAR on tags for release
- No changes to code, pom.xml, or configuration

**Out of scope**:
- Do NOT set up deployment pipelines (only build/test gating)
- Do NOT configure Docker builds or container registries
- Do NOT add branch protection rules (that's in repo settings, outside the scope of this plan)

## Git workflow

- Branch: `chore/007-add-ci-pipeline`
- Commit message: `chore: add github actions ci pipeline for automated testing`
- Do NOT push unless instructed

## Steps

### Step 1: Create .github/workflows/ directory and test.yml

Create the directory:

```bash
mkdir -p .github/workflows
```

Then create `.github/workflows/test.yml`:

```yaml
name: Test & Verify Build

on:
  push:
    branches: [ main, developer ]
  pull_request:
    branches: [ main, developer ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: saap_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven

    - name: Compile
      run: ./mvnw clean compile

    - name: Run tests
      run: ./mvnw test
      env:
        DATABASE_URL: jdbc:postgresql://localhost:5432/saap_db
        DATABASE_USERNAME: postgres
        DATABASE_PASSWORD: postgres
        JWT_SECRET: test-secret-key-minimum-32-chars-required-for-jwt
        ACTION_TOKEN_SECRET: action-token-secret-key-minimum-32-chars-required-for-jwt

    - name: Verify coverage
      run: ./mvnw verify

    - name: Upload coverage reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: jacoco-report
        path: target/site/jacoco/
        retention-days: 7
```

**Verify**: `test -f .github/workflows/test.yml && echo "workflow created"` → prints "workflow created"

### Step 2: Verify the workflow syntax (optional, but recommended)

```bash
cat .github/workflows/test.yml | grep -E "^(name|on|jobs)" | head -5
```

**Verify**: Workflow structure is visible (valid YAML)

### Step 3: Create a .gitignore entry for workflow artifacts (optional)

If not already present, ensure `.github/` is NOT in `.gitignore`:

```bash
grep "\.github" .gitignore || echo ".github/ is not ignored (good)"
```

**Verify**: `.github/` should NOT appear in `.gitignore` (we want the workflow files committed)

### Step 4: Commit and prepare for push

```bash
git add .github/workflows/test.yml
git status
```

**Verify**: `.github/workflows/test.yml` listed as new file

### Step 5: Local dry-run (optional, but recommended)

Before pushing, verify the pipeline logic by running the commands locally:

```bash
./mvnw clean compile
./mvnw test
./mvnw clean verify
```

**Verify**: All commands exit 0 (BUILD SUCCESS)

## Test plan

No new code tests needed. The workflow itself is validated by GitHub when the PR is created:
- GitHub will run the workflow on the PR branch
- The workflow will show as a required check in the PR
- If tests fail, the merge button becomes disabled
- View logs at `https://github.com/[owner]/[repo]/actions` to debug

Manual verification after merging (approx):
1. Push to a feature branch
2. Create a PR
3. GitHub Actions starts automatically
4. Workflow runs `./mvnw clean verify`
5. If coverage < 80%, workflow fails and merge is blocked
6. If coverage >= 80%, workflow succeeds and merge is allowed

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `.github/workflows/test.yml` exists and is valid YAML (can be parsed by GitHub)
- [ ] Workflow is triggered on `push` to `main` and `developer`, and on `pull_request` to the same branches
- [ ] Workflow has a `postgres:16-alpine` service for integration tests (matches Testcontainers setup)
- [ ] Workflow runs: `./mvnw clean compile`, `./mvnw test`, `./mvnw clean verify`
- [ ] Environment variables `DATABASE_URL`, `JWT_SECRET`, `ACTION_TOKEN_SECRET` are set to valid values (test defaults)
- [ ] Coverage report is uploaded as artifact (optional, but included)
- [ ] `git status` shows `.github/workflows/test.yml` as new
- [ ] `./mvnw clean verify` exits 0 locally (drift check — if not, fix build before pushing)
- [ ] `plans/README.md` status row for plan 007 updated to DONE

## STOP conditions

Stop and report back if:

- GitHub Actions is not enabled on the repository — the org or repo settings may have actions disabled; check `Settings > Actions > General`
- The workflow fails on the first run (e.g., "Postgres connection refused") — adjust environment variables or postgres service options
- Maven cache action (`actions/setup-java@v4` with `cache: maven`) causes build failures — remove the `cache: maven` line and rely on fresh `~/.m2` each run (slower, but safer for debugging)
- Coverage drops below 80% in the CI run — this is expected if code was added without tests; the workflow is doing its job (blocking the merge). Fix by adding tests and re-pushing.

## Maintenance notes

**Future changes**: 
- If you add more branches (e.g., `staging`, `release`), update the `on.push.branches` and `on.pull_request.branches` arrays
- If Spring Boot version changes significantly, update the `java-version` setup step
- If Postgres version drifts, update the `postgres:16-alpine` image tag

**For reviewers**: Ensure the workflow is accessible to all developers (no secret branch protection overrides that bypass the checks).

**Deferred**: Deployment to staging/prod, artifact publishing, and SAST/DAST scanning are outside this plan.

**Documentation**: Update README.md to mention "CI checks are run automatically on PRs via GitHub Actions" and link to the Actions tab for logs.
