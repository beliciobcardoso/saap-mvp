# Plan 021: Update README and .env.example to document mandatory JWT_SECRET and ACTION_TOKEN_SECRET

> **Executor instructions**: Read plan fully. This is a documentation fix. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: docs/dx
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

README says `cp .env.example .env`, but `.env.example` (if it exists) doesn't mention that `JWT_SECRET` and `ACTION_TOKEN_SECRET` are required without defaults. Following the README, the app fails on boot. Solution: update docs and env file to be clear.

## Scope

**In scope**:
- Update `.env.example` to include placeholders for JWT_SECRET and ACTION_TOKEN_SECRET with comments
- Update README setup section to explain the requirement
- Optional: add a setup script that validates env vars on boot

**Out of scope**:
- Do NOT change how the env vars are handled in code

## Steps

### Step 1: Create/update .env.example

Create or edit `.env.example`:

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/saap_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT (REQUIRED — min 32 chars, must differ from ACTION_TOKEN_SECRET)
JWT_SECRET=YOUR_SECRET_HERE_MIN_32_CHARS_REQUIRED
ACTION_TOKEN_SECRET=YOUR_ACTION_SECRET_HERE_MIN_32_CHARS_REQUIRED

# App
APP_BASE_URL=http://localhost:8080
```

**Verify**: File exists with examples and warnings

### Step 2: Update README.md

Find the setup section and add:

```markdown
### Local Setup

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. **IMPORTANT**: Edit `.env` and set `JWT_SECRET` and `ACTION_TOKEN_SECRET` to unique values of at least 32 characters each:
   ```bash
   JWT_SECRET=$(openssl rand -hex 32)
   ACTION_TOKEN_SECRET=$(openssl rand -hex 32)
   ```
   These are REQUIRED and have no defaults.

3. Start the app:
   ```bash
   ./mvnw spring-boot:run
   ```
```

**Verify**: README updated with clear instructions

### Step 3: Compile and verify (no code change)

No code to compile. Just verify the docs are readable and accurate.

## Test plan

Manual: follow the README instructions and verify the app starts.

## Done criteria

- [ ] `.env.example` includes JWT_SECRET and ACTION_TOKEN_SECRET with warnings
- [ ] README setup section documents the requirement and how to generate secrets
- [ ] Following the README instructions results in a working local setup

## STOP conditions

Stop and report if:

- No `.env.example` exists and there's no config file pattern in the project (escalate to understand config loading)

## Maintenance notes

**Policy**: Any environment variable without a default should be documented in `.env.example` and README with an example or generation command.
