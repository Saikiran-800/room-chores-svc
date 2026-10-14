# room-chores-svc

Quarkus (Java 21, Gradle Kotlin DSL) backend for the Room Chores app: a chore catalog with work points,
roommates with skills and capacity, daily assignments, and a monthly fairness report.
Talks to CockroachDB through the PostgreSQL driver.

## Run locally

Requirements: JDK 21. Gradle is provided by the wrapper (`./gradlew`).

1. Create a file named exactly `.env` in the project root (the folder that contains `build.gradle.kts`),
   by copying the template and putting the real password in it. `.env` is gitignored.

   ```bash
   cp .env.example .env          # Windows: copy .env.example .env
   ```

   Plain `KEY=value` lines only: no quotes, no `export`, no spaces around `=`. Quarkus reads this file
   automatically when started from the project folder, so you do **not** need `source .env`
   (which doesn't exist in PowerShell / cmd anyway).

2. CockroachDB Cloud uses `sslmode=verify-full`, so the driver needs the cluster CA certificate.
   Download it once from the "Connect" dialog in the Cockroach Cloud console, but save it **inside this project**
   as `certs/root.crt` (change the `-o` path in the curl command the dialog shows). The URL in `.env.example`
   already points at it, and it is a public CA certificate, so it is safe to commit. Keeping it here, instead of
   the global `~/.postgresql/root.crt`, means it can't interfere with other Postgres connections on your machine.

3. Start in dev mode (live reload):

   ```bash
   ./gradlew quarkusDev          # Windows: gradlew.bat quarkusDev
   ```

   On first start, tables are created and ~30 starter chores are seeded if the catalog is empty.

4. Check it: `curl localhost:8080/q/health` and `curl localhost:8080/api/chores`.

Run the unit tests with `./gradlew test`. Build a runnable jar with `./gradlew build`
(then `java -jar build/quarkus-app/quarkus-run.jar`).

## API

| Method | Path | Notes |
| --- | --- | --- |
| GET / POST | `/api/roommates` | `{name, capacityPercent, skills[]}`; `?includeInactive=true` |
| PUT / DELETE | `/api/roommates/{id}` | DELETE is a soft deactivate |
| GET / POST | `/api/chores` | `?category=`, `?includeDisabled=true` |
| PUT | `/api/chores/{code}` | full update |
| POST | `/api/chores/{code}/enable` or `/disable` | |
| GET | `/api/assignments` | `?from=&to=&roommateId=&status=` (default: today ±7 days) |
| POST | `/api/assignments` | manual assignment `{choreCode, roommateId, dueDate}`; rejects ineligible roommates |
| POST | `/api/assignments/generate?date=YYYY-MM-DD` | auto-assign everything due that day |
| POST | `/api/assignments/{id}/complete` / `miss` | points are awarded only on `complete` |
| POST | `/api/assignments/{id}/swap` | `{roommateId}`; target must be eligible |
| GET | `/api/fairness?month=YYYY-MM` | earned vs fair-share points per roommate |

Errors come back as `{"error": "..."}`.

## How balancing works

- Skill-gated chores (`requiredSkill`, e.g. `COOKING`) only go to eligible roommates. `COOKING` implies `BASIC_PREP`.
- `generate` handles heaviest chores first and picks the eligible roommate with the lowest
  `points carried this month / capacity`; ties go to whoever did that chore least in the last 7 days.
- Daily chores are due every day, weekly chores on Mondays, monthly chores on the 1st; `AS_NEEDED` chores are assigned manually.
- Fair share = (total points of all assignments in the month) x (roommate capacity / total capacity).
  Missed chores still count toward the workload, so skipping them shows up as a negative delta.

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| `Datasource '<default>' was deactivated automatically because its URL is not set` | `DB_JDBC_URL` isn't visible to the app. Check that `.env` exists in the project root (not `.env.example`, not `.env.txt`) and the variable names match. Or set real environment variables instead (PowerShell: `$env:DB_JDBC_URL="..."`). |
| `Could not open SSL root certificate file ...root.crt` | `sslmode=verify-full` needs the CA cert. Make sure `certs/root.crt` exists, or use an absolute path in `sslrootcert=` if your IDE starts the app from a different working directory. |
| `password authentication failed` | Wrong `DB_USER` / `DB_PASSWORD`; the password may have been reset in the Cockroach console. |
| `Connection refused` / timeout | The cluster's network allowlist must include your current IP (Cockroach Cloud console, Networking). |

## Notes

- Schema is managed with `quarkus.hibernate-orm.database.generation=update` for now. Move to Flyway before the data matters.
- CockroachDB can occasionally ask clients to retry a transaction (SQLSTATE 40001). Not handled yet.
- If you bump `quarkusPluginVersion` / `quarkusPlatformVersion` in `gradle.properties` to 3.26+, the CORS switch is spelled
  `quarkus.http.cors.enabled=true`.
