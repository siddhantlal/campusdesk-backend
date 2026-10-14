Act as a senior Java backend engineer and a patient technical mentor.

Build a complete backend project called CampusDesk: a campus service desk where students submit issues, staff handle assigned tickets, and administrators manage assignments, users and categories.

Context
I am a final-year Computer Science student preparing for placements in India. This project should demonstrate backend engineering, relational database design, API development, authentication, authorisation, testing, Docker and CI.

The code must be understandable enough for me to study, modify and defend in interviews. Prefer a well-structured modular monolith with clear responsibilities. Avoid unnecessary infrastructure, abstractions and design patterns.

Build the actual application, run it, test it and document it. Do not stop after giving me architecture advice or a collection of code snippets.

1. Technology and environment

Use:
- Java 21.
- A stable Spring Boot release compatible with Java 21.
- Maven, including Maven Wrapper.
- Spring Web, Spring Security and Bean Validation.
- Spring Data JPA.
- PostgreSQL.
- Flyway for database migrations.
- JUnit 5, Spring Boot Test and Testcontainers.
- OpenAPI documentation with Swagger UI.
- Docker and Docker Compose.
- GitHub Actions for continuous integration.

Before choosing dependency versions, inspect the environment and verify compatibility using official documentation where available. Avoid snapshot or pre-release dependencies.

If an existing repository is present, inspect its instructions and files first. Preserve unrelated work.

Do not introduce Redis, Kafka, Kubernetes, microservices, GraphQL or a frontend unless a concrete requirement makes them necessary. They are not required for this project.

2. Scope and workflow

Support three roles:
- STUDENT
- STAFF
- ADMIN

Students:
- Register and log in.
- Create tickets.
- View and search their own tickets.
- View the details and history of their own tickets.
- Comment on their own tickets.
- Close a resolved ticket.
- Reopen a resolved ticket with a reason.

Staff:
- Log in.
- View only tickets assigned to them.
- Comment on assigned tickets.
- Move assigned tickets into progress.
- Resolve assigned tickets with a resolution summary.

Administrators:
- View all tickets.
- Assign and reassign tickets to active staff.
- Manage categories.
- Create staff accounts.
- Activate or deactivate accounts.
- View aggregate ticket statistics.
- Read ticket history.

Treat the permission rules as explicit requirements. A logged-in user must not gain access merely by guessing another ticket ID.

3. Ticket lifecycle

Use these statuses:
OPEN
IN_PROGRESS
RESOLVED
CLOSED

Supported transitions:
- New ticket -> OPEN.
- OPEN -> IN_PROGRESS: assigned staff or admin.
- IN_PROGRESS -> RESOLVED: assigned staff or admin; resolution summary required.
- RESOLVED -> CLOSED: ticket owner or admin.
- RESOLVED -> OPEN: ticket owner or admin; reopening reason required.

Reopening keeps the existing assignee unless an admin changes it.
Assignment does not automatically change status.
Closed tickets cannot be modified or commented on.
Admins must follow the same lifecycle rules; no arbitrary status override.

Reject invalid transitions with a meaningful error.

Maintain an append-only ticket event history for:
- Creation.
- Assignment and reassignment.
- Status changes.
- Reopening.
- Comment creation.

Update ticket state and its corresponding history event within one transaction.

4. Data model

Design appropriate entities, relationships, constraints and migrations.

At minimum include:

User:
- ID.
- Name.
- Normalised unique email.
- Password hash.
- Role.
- Active flag.
- Created and updated timestamps.

Category:
- ID.
- Unique name.
- Description.
- Active flag.

Ticket:
- ID.
- Title.
- Description.
- Category.
- Creator.
- Optional staff assignee.
- Priority: LOW, MEDIUM, HIGH.
- Status.
- Resolution summary when applicable.
- Created, updated, resolved and closed timestamps as appropriate.
- Optimistic-lock version.

Comment:
- ID.
- Ticket.
- Author.
- Body.
- Created timestamp.

TicketEvent:
- ID.
- Ticket.
- Actor.
- Event type.
- Relevant previous and new values.
- Optional reason.
- Timestamp.

Use UTC timestamps.
Use database foreign keys and sensible indexes.
Do not physically delete users or categories referenced by tickets.
Deactivated categories cannot be selected for new tickets.
Deactivated staff cannot receive new assignments.

Explain how deactivating an assigned staff member affects their existing tickets.

5. Authentication and security

Use Spring Security with short-lived signed JWT access tokens.

Requirements:
- Hash passwords using a supported adaptive password encoder.
- Public registration always creates a STUDENT.
- Reject attempts to set role or active status through public registration.
- Only administrators can create staff accounts.
- Do not expose an API that lets users promote themselves to ADMIN.
- Provide a documented, explicit mechanism for creating the initial admin.
- Keep signing secrets outside source control.
- Validate token signature and expiry.
- Check the current user's active status and permissions on protected requests.
- Explain that account deactivation blocks access even if a token has not expired.
- Do not implement refresh tokens unless needed.
- Do not claim that client-side token deletion revokes an issued token.
- Do not log passwords, tokens or sensitive request bodies.
- Restrict CORS through configuration.
- Explain the CSRF decision for the chosen bearer-token authentication model.
- Return generic login failures that do not reveal whether an email exists.
- Never return password hashes or internal entity fields in API responses.

Use request and response DTOs. Never bind incoming JSON directly to persistence entities.

For a ticket a caller is not permitted to see, return a consistent response that does not disclose its contents or existence.

6. API design

Design clear REST endpoints, including:

Authentication:
POST /api/v1/auth/register
POST /api/v1/auth/login
GET /api/v1/users/me

Tickets:
POST /api/v1/tickets
GET /api/v1/tickets
GET /api/v1/tickets/{id}
PATCH /api/v1/tickets/{id}/assignment
PATCH /api/v1/tickets/{id}/status
POST /api/v1/tickets/{id}/comments
GET /api/v1/tickets/{id}/comments
GET /api/v1/tickets/{id}/events

Administration:
POST /api/v1/admin/staff
PATCH /api/v1/admin/users/{id}/status
GET /api/v1/admin/statistics

Categories:
GET /api/v1/categories
POST /api/v1/categories
PATCH /api/v1/categories/{id}

You may refine these paths if you explain the improvement.

Ticket listing must support:
- Pagination.
- Filtering by status, priority and category.
- Optional assignee filtering for administrators.
- Search by title.
- Sorting by createdAt or updatedAt.
- A stable secondary sort.
- A bounded maximum page size.
- An allowlist of sortable fields.

Apply role-based visibility before returning results and before calculating total counts.

Document status codes and provide realistic request/response examples.

7. Validation and errors

Define and document limits for names, titles, descriptions, comments and passwords.

Reject:
- Blank required fields.
- Invalid emails and enum values.
- Duplicate emails.
- Inactive or nonexistent categories.
- Assignment to a student or inactive staff member.
- Unsupported status transitions.
- Excessive page sizes or unsupported sorting.
- Requests containing protected fields where appropriate.

Use a consistent error format, preferably Problem Details, including:
- HTTP status.
- Stable application error code.
- Human-readable message.
- Request path.
- Field validation errors where applicable.
- Correlation ID.

Do not return stack traces or database internals.

Handle optimistic-lock conflicts with a clear conflict response. Demonstrate that concurrent updates do not silently overwrite each other.

8. Architecture and implementation quality

Organise code by feature, such as:
auth
users
tickets
categories
common

Keep controllers focused on HTTP concerns.
Keep business rules in services or domain components.
Keep database access in repositories.
Use constructor injection and explicit transaction boundaries.

Avoid:
- Generic base controllers and services.
- Unnecessary interfaces for every class.
- A single giant service.
- Returning JPA entities from controllers.
- Unbounded collection loading.
- Accidental N+1 queries.
- Hibernate automatically creating or modifying production schemas.

Use Flyway as the schema authority and configure Hibernate to validate the schema.

Comment on non-obvious decisions. Avoid comments that merely repeat the code.

9. Automated tests

Write meaningful tests for critical behaviour, including:

Authentication:
- Student registration succeeds.
- Duplicate email is rejected.
- Wrong credentials fail.
- Expired or invalid tokens fail.
- Public registration cannot grant staff/admin privileges.
- Deactivated users cannot access protected resources.

Authorisation:
- Students cannot read or comment on another student's tickets.
- Staff cannot access unassigned tickets.
- Students and staff cannot call administrator endpoints.
- List results and counts respect visibility rules.
- Responses never expose password hashes.

Ticket behaviour:
- Valid creation.
- Invalid category rejection.
- Assignment and reassignment.
- Every allowed transition.
- Rejected transitions.
- Resolution summary validation.
- Reopening reason validation.
- Closed-ticket restrictions.
- History events are recorded correctly.
- Failed operations do not leave partial state/history updates.
- Concurrent updates produce a conflict instead of silent data loss.

Data access:
- Pagination, filtering and stable sorting.
- Search behaviour.
- Database constraints and migrations.

Use PostgreSQL Testcontainers for integration tests rather than substituting H2 for PostgreSQL-specific behaviour.

Aim for at least 25 meaningful automated tests, but do not inflate the count with trivial assertions. Report the actual number and outcomes.

If Docker or another prerequisite is unavailable, explain which checks could not run. Do not report skipped or unexecuted tests as passing.

10. Docker and CI

Provide:
- A multi-stage Dockerfile.
- A non-root application container.
- Docker Compose for the application and PostgreSQL.
- Health checks.
- Persistent database storage.
- An .env.example with placeholders.
- A .gitignore that excludes secrets and build artifacts.

The README should explain how to start the project from a fresh clone.

Do not bake credentials or signing secrets into images.
Development bootstrap accounts must be explicitly enabled and configurable. Do not create default production credentials.

Create a GitHub Actions workflow that:
- Sets up the correct Java version.
- Uses Maven Wrapper.
- Runs unit and integration tests.
- Builds the application.
- Publishes test reports as artifacts when useful.

Call it CI. Do not call it CI/CD unless an actual deployment step exists.

11. Demonstration and performance evaluation

Create an opt-in development/demo seed mechanism with synthetic users, categories and tickets.

Provide a benchmark setup that can generate 10,000 synthetic tickets.

Choose one meaningful ticket-listing or filtering query and:
- Capture its query plan.
- Explain the selected index.
- Compare before/after behaviour if feasible.
- Keep dataset, hardware and workload constant.
- Record warm-up, concurrency, sample size and test duration.
- Report median and p95 latency, error rate and throughput where measured.
- Clearly distinguish application API measurements from database-only measurements.

Never invent benchmark results, user counts, uptime or percentage improvements.

If the benchmark cannot run, provide the executable procedure and mark results as NOT MEASURED.

12. Documentation and interview preparation

Produce:
- README.md.
- docs/architecture.md.
- docs/api-examples.md.
- docs/security.md.
- docs/testing.md.
- docs/benchmark.md.
- docs/interview-guide.md.
- A Mermaid entity-relationship diagram.
- A permission matrix.
- A ticket lifecycle diagram.
- A runnable curl walkthrough or API collection.

The walkthrough should demonstrate:
1. Start the system.
2. Create the initial admin.
3. Register a student.
4. Create a category and staff account.
5. Create a ticket.
6. Assign it.
7. Move it into progress.
8. Resolve it.
9. Close it.
10. Inspect its history.

Keep local credentials outside committed example files.

The interview guide should explain:
- Why this is a modular monolith.
- Request flow through Spring Security, controllers, services and repositories.
- Authentication versus authorisation.
- Transactions and history consistency.
- Optimistic locking.
- Index selection.
- Pagination.
- DTOs versus entities.
- Unit versus integration tests.
- Known limitations.
- What would change if usage increased substantially.

Include 15 realistic interview questions with answers tied to this implementation.

Finally, write 3 résumé bullets using only implemented and verified capabilities. Include numerical results only when actually measured. Put unmeasured targets in a separate section.

13. Execution approach

Work incrementally:

Phase 1: Inspect environment, record assumptions, define schema and permission rules.
Phase 2: Bootstrap application, migrations and authentication.
Phase 3: Implement ticket, category and administrator workflows.
Phase 4: Add tests and fix failures.
Phase 5: Add Docker, CI, demo data and API documentation.
Phase 6: Run the end-to-end walkthrough and benchmark where possible.
Phase 7: Review security, documentation and implementation consistency.

At each phase:
- Implement the files.
- Run the relevant checks.
- Explain what changed in plain language.
- Record any unresolved limitation.
- Continue to the next phase without requesting approval for routine choices.

Ask only when a missing fact would materially change the application. Otherwise make a sensible choice and document it.

Do not deploy to paid infrastructure, publish a repository or modify external accounts without my explicit instruction.

Completion criteria:
- A fresh database starts successfully using migrations.
- The application runs through Docker Compose.
- The documented student-to-staff-to-admin workflow works.
- Permission tests pass.
- The test/build results are reported accurately.
- Documentation matches the actual implementation.
- There are no placeholder implementations in the critical workflow.
- I receive exact commands to run, test and demonstrate the project.

Start by inspecting the workspace and environment, then proceed with implementation.