# CampusDesk

A Java 21 / Spring Boot backend for a campus service desk. Students raise tickets, assigned staff investigate and resolve them, and administrators manage assignment, accounts and categories. This is a modular monolith with PostgreSQL migrations, signed access tokens, a transactional audit history and explicit ticket permissions.

**The main local setup runs without Docker. The frontend and API are served together by Spring Boot and the same Render service.**

## Run locally on Windows

Requirements: Java 21, PowerShell, an internet connection for the first dependency download, and Windows `tar`. Python 3 and `curl` are needed for the walkthrough and benchmark. Maven and PostgreSQL do not need to be installed globally.

From the project directory:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-local.ps1
powershell -ExecutionPolicy Bypass -File scripts/run-local.ps1
```

Setup downloads checksum-verified PostgreSQL 17.6 binaries into `.tools`, initializes a persistent database under `.local/pgdata`, binds PostgreSQL to `127.0.0.1:5432`, and generates random local credentials. It creates separate `campusdesk` and `campusdesk_test` databases. It installs no Windows service. Repeat setup to restart PostgreSQL after a reboot. Keep `.local`; it contains your development database and credentials. If 5432 is occupied, pass `-DatabasePort 15432` on the first setup; avoid Windows reserved port ranges (`netsh interface ipv4 show excludedportrange protocol=tcp`).

The first application start creates the admin specified by the ignored `.local/local.env`. View that file locally to log in through Swagger. Set `BOOTSTRAP_ADMIN_ENABLED=false` after the first successful start. Public registration creates students; the admin creates staff. The bootstrap only creates an admin when none exists and never resets passwords. No default password is committed.

- CampusDesk frontend: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

In a second terminal:

```powershell
python scripts/walkthrough.py
powershell -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

The walkthrough uses curl to register a fresh student, create a category/staff account, create/assign/start/resolve/close a ticket, and inspect its events. It reads local credentials without printing passwords or tokens.

Stop Java with Ctrl+C; then stop the local database:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/stop-local.ps1
```

`run-local.ps1 -SkipBuild` starts the existing jar. Add `-Background` to run with logs in `.local/app.log`, then stop it with `powershell -ExecutionPolicy Bypass -File scripts/stop-app.ps1`. The runner uses a copy of the jar so tests can rebuild `target/` while the app is running on Windows. Stop the app before copying a new version into its runtime directory.

Local setup is for development: its database account owns the schema. Use a separate least-privilege runtime account and a migration account for production.

## Existing PostgreSQL / Linux / macOS

Create a database and owner using your PostgreSQL administration tools. Export `DB_URL`, `DB_USER`, `DB_PASSWORD` and `JWT_SECRET`. Generate a signing secret with `openssl rand -base64 32`. For the first admin, explicitly set `BOOTSTRAP_ADMIN_ENABLED=true`, `ADMIN_EMAIL`, `ADMIN_NAME`, and `ADMIN_PASSWORD` (12..64 characters, at most 72 UTF-8 bytes). Then:

```sh
chmod +x mvnw
./mvnw -B -ntp -DskipTests package
java -jar target/campusdesk-1.0.0.jar
```

Hibernate validates the schema; Flyway creates it from versioned SQL. All timestamps are UTC instants. `PORT` defaults to 8080. Set `CORS_ORIGINS` to an explicit comma-separated origin allowlist; it is empty by default.

## Essential tests

Your follow-up request replaces the original 25-test target: this project has **two focused unit tests and five PostgreSQL integration scenarios**. They cover authentication, visibility, lifecycle/history, validation/filtering, and real competing database updates. The integration scenarios group related workflow assertions rather than inflating test counts.

```powershell
# No Docker: isolated local PostgreSQL test database
powershell -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

```sh
./mvnw test    # unit tests only
./mvnw verify  # unit + Testcontainers PostgreSQL integration tests; Docker needed for this command
```

CI uses the wrapper, Java 21 and PostgreSQL Testcontainers. It builds the jar and uploads test reports; it does not deploy. See [testing and actual results](docs/testing.md).

## Optional Docker

```sh
cp .env.example .env
# Replace all required placeholders. Explicitly enable admin bootstrap for the first start.
docker compose up --build -d
docker compose ps
docker compose logs app
docker compose down
```

The runtime image runs as a non-root user. The database has persistent named storage and no host-exposed port. Both services have health checks. `down` preserves data; do not use `down -v` unless deliberately removing your database. Credentials are injected at runtime.

## Demo and benchmark

For twelve synthetic demo tickets and demo student/staff accounts, set `DEMO_ENABLED=true` and a strong `DEMO_PASSWORD` in your local environment, then restart the application. This is opt-in and idempotent; disable it afterward. Demo accounts use `student.demo@campusdesk.test` and `staff.demo@campusdesk.test`.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/benchmark-local.ps1
```

The benchmark inserts 10,000 synthetic tickets, saves before/after query plans, and measures both database queries and API requests separately. It intentionally leaves synthetic data in the development database. See [benchmark procedure and results](docs/benchmark.md).

## Render deployment

The root [render.yaml](render.yaml) configures a Docker web service and managed PostgreSQL using Free plans. Render builds the image remotely; local development remains non-Docker. See [step-by-step Render instructions](docs/render.md), including initial admin creation and the free database's 30-day expiry.

The frontend is included in the application jar. Redeploy the same backend service to get student, staff and admin workspaces at its root URL. See [frontend features and implementation](docs/frontend.md). There is no npm build or separate frontend hosting step.

## Documentation

- [Architecture, ER diagram, permissions and lifecycle](docs/architecture.md)
- [API examples, validation and error format](docs/api-examples.md)
- [Security and operational limitations](docs/security.md)
- [Tests and verification record](docs/testing.md)
- [Benchmark](docs/benchmark.md)
- [Interview preparation and résumé bullets](docs/interview-guide.md)
- [Environment and dependency decisions](docs/decisions.md)

## Publish to your GitHub

Suggested repository name: **campusdesk-backend**. Publishing is left to you, as requested. Create an empty repository first, then run:

```sh
git init -b main
git add .
git diff --cached --stat
git commit -m "Build CampusDesk service desk backend"
git remote add origin https://github.com/YOUR_USERNAME/campusdesk-backend.git
git push -u origin main
```

Review staged files before committing. `.env`, `.local`, `.tools` and build artifacts are ignored. `prompt.md` is preserved as your original specification.
