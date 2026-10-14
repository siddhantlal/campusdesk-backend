# Interview guide

Use these answers to explain the implementation and its tradeoffs. Open the named classes/migration while practicing; change a rule locally and rerun the essential tests to make the project your own.

1. **Why a modular monolith?** One Spring application and PostgreSQL database let ticket state and its event commit atomically. Feature packages make responsibilities clear without network calls, event brokers or distributed transactions. The project has no workload that justifies microservices.

2. **Trace an authenticated request.** `CorrelationFilter` creates an ID. Spring Security verifies the bearer JWT. The converter loads the current active user and authorities. A controller validates a request record, a service checks visibility/business rules in a transaction, the repository queries PostgreSQL, and the service maps an explicit response DTO.

3. **Authentication versus authorization?** `JwtDecoder` authenticates a signed, valid token; the account lookup establishes that the account remains active. Role annotations and `TicketService.visible` authorize the operation and specific resource. Possessing a student token does not permit reading any ticket ID.

4. **Why DTOs rather than returning entities?** Records accept only writable fields and expose only chosen response data. Strict unknown-field rejection prevents mass assignment of role/active. DTOs also avoid serializing password hashes, Hibernate proxies and arbitrary relationships.

5. **How are passwords stored?** BCrypt cost 12 in `SecurityConfig`. `UserService` rejects over-72-byte passwords to avoid BCrypt truncation. Normalized email uniqueness lives in the database. There is no plaintext password storage in entities or API responses.

6. **Can a deactivated user reuse a token?** Every protected request reloads the account, so subsequent requests fail even before token expiry. Deactivation does not necessarily stop an already authorized in-flight request. Deleting a token in the client is not server revocation.

7. **Where do lifecycle rules live?** `TicketWorkflow.validate` defines the allowed graph, acting roles and required explanations. `TicketService` owns transactional changes/timestamps/events. Admins follow the same graph and cannot set arbitrary status.

8. **How is history kept consistent?** Each mutating service method is transactional and saves both state and event. A failure at validation, SQL or optimistic lock rolls the transaction back. Comments touch the ticket parent. Events have no modification API, are Hibernate-immutable and have an UPDATE/DELETE rejection trigger.

9. **Explain optimistic locking.** Clients submit the last observed version. The service rejects an already stale request with 409. JPA includes `version` in the update predicate and increments it, so two transactions reading the same version cannot both commit. The concurrency test coordinates two real database transactions and requires one success/one conflict.

10. **Why these indexes?** Visibility is usually equality on creator/assignee, then ordering by creation time and ID. Composite indexes match this access path. The status index supports admin lists. Child ticket/time indexes support ordered history/comments. Arbitrary title substring searches may still scan; no universal index-speed claim is made.

11. **How do you paginate safely?** Maximum size is 100, allowed sort fields are fixed, and ID breaks timestamp ties. Visibility filters apply to content and count. This uses offset pagination, which is understandable but can become expensive or shift under concurrent inserts; cursor pagination would improve large/deep feeds.

12. **How did you avoid N+1 and unbounded loading?** Pageable ticket repository calls use an entity graph for their many-to-one references; count queries remain separately scoped. Comments/events have separate bounded page endpoints. There are no ticket collection mappings that accidentally initialize the entire history, and Open Session in View is disabled.

13. **Unit versus integration tests here?** Unit tests exercise the pure workflow component without Spring/database. Integration scenarios boot the actual security/controller/service/repository stack and use real PostgreSQL migrations. Testcontainers is the CI/default fixture; local mode uses an isolated PostgreSQL database without Docker. Seven essential tests group related critical assertions at the user's request.

14. **What happens when staff/category is deactivated?** Staff access stops on subsequent requests; existing assignments remain for admins to reassign and history keeps its attribution. New assignments require active STAFF. Existing ticket categories remain referenced, while inactive categories cannot be chosen for new tickets.

15. **What would change with much more usage?** Measure first: slow query plans, p95 API latency and connection-pool utilization. Consider matching additional indexes, cursor pagination, rate limiting, read caching only for suitable public data, and email/notification jobs. Add refresh-token/revocation/session design only with a product need. Separate services only where independent ownership/scaling outweighs distributed consistency cost.

## Known limitations

No password reset, email verification, attachments, notifications, refresh tokens or MFA. No UI. The public API lacks rate limiting; production TLS/gateway and least-privilege database access are operational work. Offset pagination and literal substring search have scaling limits. Runtime active status is checked at request entry. Bootstrap is a controlled startup mechanism, not an admin provisioning UI. These are deliberate scope boundaries, not hidden placeholder workflows.

## Résumé bullets

Use only after the local verification record in `docs/testing.md` confirms execution:

- Built a Java 21/Spring Boot campus service desk backend with JWT authentication and role-scoped ticket APIs backed by PostgreSQL and Flyway migrations.
- Implemented transactional ticket lifecycle and append-only history with optimistic locking, and verified critical authentication, permission and concurrency scenarios on PostgreSQL.
- Packaged a runnable backend with Maven Wrapper, a non-Docker Windows setup, Swagger documentation, a reproducible API walkthrough, and a GitHub Actions CI workflow.

The CI/Docker files are implementation capabilities; do not claim the remote CI ran or that a deployment is live without evidence.

## Unmeasured targets

No production uptime, real user count, throughput SLA or percentage improvement is claimed. A 10,000-ticket dataset is a synthetic benchmark input, not a real user adoption metric. Use benchmark numbers only with the environment, workload and actual recorded results alongside them.
