# Environment and decisions

Inspected on 14 October 2026 (Asia/Kolkata): the workspace contained only `prompt.md`, no existing Git repository or repository instructions. Installed Java is Temurin 21.0.12.1. Maven and GitHub CLI were absent; Docker's daemon was initially stopped. The user subsequently requested fewer, essential tests, a non-Docker version, and no GitHub push. Those instructions take precedence over the original test count and Docker-first completion criteria.

Selected stable versions:

| Component | Selection | Reason |
| --- | --- | --- |
| Java | 21 | Requested LTS baseline |
| Spring Boot | 3.5.16 | Stable 3.5 line; supports Java 17..25 |
| Springdoc | 2.8.17 | Official compatibility matrix maps Boot 3.5 to Springdoc 2.8 |
| Maven | 3.9.11 | Above Boot's minimum 3.6.3, wrapper checksum pinned |
| Maven Wrapper | 3.3.4, only-script | Official Apache scripts; no wrapper jar needed |
| PostgreSQL | 17.6 | Same PostgreSQL major/version for portable runtime and container fixtures |
| JUnit / Hibernate / Security / Flyway / Testcontainers | Boot BOM managed | Keep the framework's tested dependency set |

Official references checked before dependency selection:

- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Springdoc compatibility matrix](https://springdoc.org/)
- [Maven Wrapper configuration](https://maven.apache.org/tools/wrapper/)
- [Testcontainers PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)
- [Zonky portable PostgreSQL distribution](https://github.com/zonkyio/embedded-postgres)

Maven Central metadata confirmed the selected artifacts exist. Portable PostgreSQL is a development convenience, not an in-process application dependency. The app still uses the standard PostgreSQL JDBC protocol. Before production use, review current supported PostgreSQL minor releases, images and dependency security advisories.

Routine scope choices: administrator account creation is a startup-only bootstrap, no promotion API; inaccessible and nonexistent tickets both produce 404; administrators can inspect resolved tickets and follow lifecycle transitions without arbitrary overrides; assignment requires a staff ID and cannot unassign; closed is terminal. Category updates replace the submitted name/description/active fields and require their current version. Listing users was added so administrators can discover IDs for status management.

Frontend follow-up, 15 October 2026: the user requested a frontend served by the same Render service. Native HTML/CSS/JavaScript modules are packaged as Spring Boot static resources and use same-origin bearer-authenticated API calls. This adds no Node build, second hosting service, extra database or frontend-specific CORS requirement. Admin user listing now supports bounded server-side name/email, role and active-status filters for account management and staff assignment.
