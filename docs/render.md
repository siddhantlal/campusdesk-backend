# Deploy CampusDesk on Render

Yes: use a Render **Docker Web Service** and Render PostgreSQL. Local Windows development remains Java + PostgreSQL without Docker. Render builds the Dockerfile from GitHub; you do not need Docker on your own machine for that deployment path. Render does not provide a native JVM runtime. See [Render Docker support](https://render.com/docs/docker).

## Blueprint setup

1. Push this project to your GitHub as `campusdesk-backend` using the README commands.
2. In Render choose **New → Blueprint**, connect the repository, and use the root `render.yaml`.
3. Review the resources: the blueprint explicitly chooses a Free web service and Free PostgreSQL 17 database in the same region. If your workspace already has its one allowed free database, connect that existing database instead of creating another and update the blueprint database references.
4. Enter your private `ADMIN_EMAIL` and a strong `ADMIN_PASSWORD` (12..64 characters and at most 72 UTF-8 bytes) when Render prompts. The blueprint generates a random signing secret and wires database credentials; no secrets belong in GitHub.
5. Create the blueprint. Render builds the image, starts Java on its injected `PORT`, and probes `/actuator/health`. Flyway creates the schema on the managed database; Hibernate validates it. No Compose or local `.local` data is uploaded.
6. When healthy, open `https://YOUR-SERVICE.onrender.com/swagger-ui.html`, log in at `/api/v1/auth/login`, and paste `accessToken` into Swagger **Authorize**. Register a student and create categories/staff using the admin token.
7. After the initial administrator exists, change `BOOTSTRAP_ADMIN_ENABLED` in `render.yaml` to `'false'` and push the change. Also remove `ADMIN_PASSWORD` from the web service's environment after bootstrap is disabled. Keep your password privately for login. Dashboard-only changes can be overwritten by a later blueprint sync, so update the file as well.

The bootstrap does not overwrite an existing admin or reset its password. The app has no account promotion endpoint. Demo/benchmark seeding is disabled in hosted configuration.

The container startup script converts Render's internal `postgresql://` URL to the required JDBC scheme, strips the URL's credentials (provided separately), and requires TLS. The service and database share a region and private networking; the blueprint disables public database access. Render's internal certificates are self-signed, so its documented `sslmode=require` encrypts the connection without CA/hostname verification. See [Render PostgreSQL connections](https://render.com/docs/postgresql-creating-connecting).

The app already reads `PORT`; Spring binds to all interfaces by default. The configured health endpoint is public and exposes no database details. Render's port requirements are documented in [web services](https://render.com/docs/web-services#port-binding).

## Manual setup instead

Create PostgreSQL 17, then a Docker Web Service from your repository. Choose plans yourself and keep both resources in the same region. Set:

| Environment variable | Value |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://INTERNAL_HOST:5432/DATABASE_NAME?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | Database credentials from Render |
| `JWT_SECRET` | Base64 encoding of at least 32 random bytes |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` for initial admin, then `false` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Your private administrator credentials |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError` |
| `DEMO_ENABLED` | `false` |
| `CORS_ORIGINS` | Empty for Swagger/same-origin; exact frontend origins if one is added |

Use `/actuator/health` as the health-check path. Keep the Docker command unset so the image entrypoint is used. Do not paste a plain `postgresql://` URL into `DB_URL`; that variable must use the JDBC scheme. Alternatively use `DATABASE_URL` for the plain URL and still supply separate `DB_USER`/`DB_PASSWORD`.

## Free-tier limits and verification

Render's free web services sleep after inactivity, so cold starts can be slow. **Free Render PostgreSQL expires after 30 days**, with subsequent deletion unless upgraded during the grace period. It is suitable for a temporary demonstration; choose a durable database plan/provider for a lasting placement portfolio and review its cost before creating resources. See [Render free-tier rules](https://render.com/docs/free).

Java's memory cap in the blueprint leaves space for non-heap memory on small instances; actual hosted memory/startup behavior still needs observation. Seven essential tests, the workflow and benchmark passed locally. A Render image-build attempt failed during Maven bootstrap; the corrected bootstrap has since been verified in the matching Linux base image. **A successful Render deployment and full application image build have not been confirmed.**

## Maven checksum failure during image build

If Maven Wrapper reports `Failed to validate Maven distribution SHA-256` before compilation, check that the build stage installs `unzip`. The wrapper downloads a tar.gz instead of the configured ZIP when `unzip` is absent; those archives have different checksums. The Dockerfile now installs `unzip` before running Maven so it downloads the ZIP matching the pinned checksum. Keep checksum verification enabled.

Verified on 14 October 2026 in a fresh `eclipse-temurin:21-jdk-jammy` container: the original setup reproduced the exact checksum failure; installing `unzip` allowed `./mvnw -B -ntp --version` to succeed with Maven 3.9.11 and Java 21.0.12.1. This focused check did not rerun the application test suite or claim that the hosted service was healthy.

After pushing this Dockerfile change, choose **Manual Deploy → Deploy latest commit** in Render. If Render still shows the old Dockerfile, use **Clear build cache & deploy** and confirm the selected commit includes the fix.
