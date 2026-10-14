# room-chores-svc

Quarkus 3.20 / Java 21 / Gradle (Kotlin DSL) backend for a shared-household chores app: chore catalog with
work points, roommates with skills and capacity, daily assignments, monthly fairness report.
Uses the PostgreSQL driver against CockroachDB Cloud. The frontend lives in the sibling repo `room-chores-app`.

## Status: written but never compiled or run

This code was authored in an environment with no access to Maven Central, so it has only been syntax-checked.
First job in any session: get `./gradlew build` and `./gradlew quarkusDev` working and fix whatever breaks.
It has never talked to the real database.

## Commands

- `./gradlew quarkusDev` — dev mode on port 8080 (Windows: `gradlew.bat quarkusDev`)
- `./gradlew test` — unit tests (`AssignmentServiceTest`, pure logic, no DB)
- `./gradlew build` — compile, test, package

## Configuration

- DB settings come from a `.env` file in the project root (`DB_JDBC_URL`, `DB_USER`, `DB_PASSWORD`),
  auto-loaded by Quarkus. See `.env.example`. Never commit `.env`; never print the password in output or logs.
- The CockroachDB CA certificate lives at `certs/root.crt` and is referenced with `sslrootcert=certs/root.crt`
  in `DB_JDBC_URL`. Do not rely on `~/.postgresql/root.crt` (global, affects other projects).
- CORS allows `http://localhost:5173` (Remix dev server). The frontend calls this API from its server side anyway.

## Layout

- `model/` — Panache entities (`Roommate`, `Chore`, `Assignment`). UUID ids are assigned in field initializers;
  `Chore` is keyed by a String `code`.
- `service/AssignmentService` — `generate` (greedy balancing) and `fairness` (earned vs fair share).
- `api/` — JAX-RS resources. `ApiExceptionMapper` turns thrown JAX-RS exceptions into `{"error": "..."}`.
  Query-param dates are Strings parsed by `ApiDates`.
- `seed/CatalogSeeder` — inserts ~30 starter chores when the `chores` table is empty.

## Rules of the domain

- Points describe the effort of ONE completion; frequency is separate; never double-count bundles and subtasks.
- Points are awarded only on `complete`. `MISSED` earns 0 but still counts as household workload.
- Roommates are soft-deleted (`active=false`) so history stays intact.
- `COOKING` implies `BASIC_PREP` for eligibility.

## Known gaps (do not silently "fix" without asking)

- Schema is `hibernate-orm.database.generation=update`; plan is to move to Flyway.
- No handling of CockroachDB transaction retries (SQLSTATE 40001).
- No integration tests, no auth.

## Isolation: personal project on a work machine

- Stay inside this folder. Do not change global git config, `~/.gradle`, `~/.ssh`, or system environment variables.
- Use repo-local git identity only (`git config user.name` / `user.email` without `--global`).
- If dependency resolution fails because a work mirror or proxy is configured in `~/.gradle` (init scripts,
  `gradle.properties`), report it and propose a project-level override instead of editing global files.
