# Essential verification

The user's follow-up replaces the original minimum of 25 tests. The suite has **7 essential tests**: 2 JUnit unit tests and 5 Spring Boot/MockMvc integration scenarios. Tests check behavior rather than getters or mirrored implementation details.

| Scenario | Main coverage |
| --- | --- |
| Unit: explanation requirements | Resolution/reopen validation; reset timestamps on reopen |
| Unit: lifecycle boundaries | Even admins cannot skip OPEN straight to CLOSED |
| Integration: accounts | Registration, normalized duplicate email, protected fields, generic login failures, invalid/expired tokens, deactivation with an issued token |
| Integration: permissions | Hidden ticket 404, cross-student comment denial, unassigned staff denial, admin routes, visibility/counts |
| Integration: workflow | Assignment/reassignment, comments, all transitions, invalid transitions, reason/summary checks, retained assignee, closed restrictions, rollback/history count, SQL append-only trigger |
| Integration: filters | Literal title search, status/priority/category, pagination bounds, enum/sort rejection, inactive category/staff |
| Integration: concurrency | Two simultaneous JPA transactions update the same version; exactly one commits; stale API update returns 409 with no event |

The PostgreSQL integration database starts from Flyway migrations with Hibernate validation. It is not H2. In CI/default `verify`, Testcontainers starts `postgres:17.6-alpine`. The explicitly selected local mode reuses a dedicated **campusdesk_test** database on real PostgreSQL, with truncation between scenarios. Never point integration tests at a database containing valuable data.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-local.ps1
powershell -ExecutionPolicy Bypass -File scripts/test-local.ps1
```

For Docker-enabled hosts: `./mvnw -B -ntp verify`. Unit-only check: `./mvnw test`. Reports: `target/surefire-reports` and `target/failsafe-reports`.

## Actual execution record

14 October 2026, Java 21.0.12.1 / local PostgreSQL 17.6, Maven Wrapper 3.9.11:

- Final `scripts/test-local.ps1` / Maven `verify`: **BUILD SUCCESS**.
- Unit: **2 tests, 0 failures, 0 errors, 0 skipped**.
- PostgreSQL integration: **5 tests, 0 failures, 0 errors, 0 skipped** (50.44 seconds).
- Total: **7 executed tests passed**; full build/test command took 1 minute 4 seconds.
- Flyway migrations V1 and V2 applied to the separate test database; Hibernate schema validation passed.
- Generated OpenAPI endpoint returned HTTP 200 and application health reported UP in the local startup check.
- Final runtime Swagger UI returned HTTP 200; OpenAPI describes 18 operations and correctly marks public registration as requiring no token.
- `python scripts/walkthrough.py` completed against the final HTTP server: ticket 1 reached CLOSED with five ordered creation/assignment/status events.
- The local benchmark completed with 10,000 synthetic tickets; real plans and separate database/API measurements are recorded in `docs/benchmark.md`.
- Render configuration received lightweight YAML/shell checks, including converting its database URL to JDBC and preserving explicitly configured `DB_URL`. These are configuration checks, not a hosted deployment test.

The first run found a percent-encoded MockMvc URI being encoded twice by its template builder. Using the URI overload corrected that test fixture. A subsequent build encountered Windows' running-jar lock; the runner now executes a runtime copy so `target/` remains rebuildable. These initial failed checks are not counted as passes.

Local PostgreSQL and Testcontainers are distinct execution paths. **Full application Docker image/Compose, Testcontainers startup and remote GitHub Actions have not been verified.** No skipped or unexecuted tests are counted as passing.

Render build diagnosis, 14 October 2026: a fresh matching Linux base-image container reproduced the Maven distribution checksum failure. It lacked `unzip`, so Maven Wrapper substituted the tar.gz archive while validating the ZIP checksum. Installing `unzip` made fresh-cache `./mvnw -B -ntp --version` succeed. The Dockerfile now includes that build prerequisite; checksum verification remains enabled. No additional application tests were run for this Dockerfile-only fix.
