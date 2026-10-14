# API examples

All paths start with `/api/v1`. JSON bodies reject unknown fields. Protected requests use `Authorization: Bearer <access-token>`. Swagger UI provides the live generated schema; enter the token through **Authorize**. Registration and login do not require authorization. `X-Correlation-ID` appears on every response.

## Endpoints and codes

| Method/path | Success | Purpose |
| --- | --- | --- |
| POST `/auth/register` | 201 | Student registration |
| POST `/auth/login` | 200 | 15-minute access token |
| GET `/users/me` | 200 | Current account DTO |
| POST `/tickets` | 201 | Create student's ticket |
| GET `/tickets` | 200 | Visible, filtered page |
| GET `/tickets/{id}` | 200 | Visible ticket |
| PATCH `/tickets/{id}/assignment` | 200 | Admin assigns/reassigns active staff |
| PATCH `/tickets/{id}/status` | 200 | Valid, permitted transition |
| POST `/tickets/{id}/comments` | 201 | Comment and history event |
| GET `/tickets/{id}/comments` | 200 | Oldest-first paginated comments |
| GET `/tickets/{id}/events` | 200 | Oldest-first paginated history |
| POST `/admin/staff` | 201 | Create staff |
| GET `/admin/users` | 200 | Discover/manage account IDs |
| PATCH `/admin/users/{id}/status` | 200 | Activate/deactivate account |
| GET `/admin/statistics` | 200 | Status counts, unassigned non-closed count, active staff |
| GET `/categories` | 200 | Page of active categories; admin may include inactive |
| POST `/categories` | 201 | Admin creates category |
| PATCH `/categories/{id}` | 200 | Admin updates category with version |

Common failures: 400 invalid body/enum/page/sort/category/assignee/missing explanation; 401 missing/invalid/expired token or wrong login; 403 forbidden operation on visible resources or admin endpoints; 404 nonexistent or invisible ticket; 409 duplicate identity/name, invalid transition, closed ticket or version conflict. Unsupported methods/content types use standard 405/415 Problem Details. Unexpected errors are sanitized 500.

## Representative JSON

Register student or create staff (different endpoints):

```json
{"name":"Asha Rao","email":"asha@example.edu","password":"YOUR_PRIVATE_PASSWORD"}
```

User response:

```json
{"id":2,"name":"Asha Rao","email":"asha@example.edu","role":"STUDENT","active":true,"createdAt":"2026-10-14T12:00:00Z","version":0}
```

Login: `{"email":"asha@example.edu","password":"YOUR_PRIVATE_PASSWORD"}`. Response contains `accessToken`, `tokenType: "Bearer"`, `expiresIn: 900`, and `user`. Placeholders above are not working credentials.

Create category: `{"name":"Facilities","description":"Campus maintenance"}`.

Create ticket:

```json
{"title":"Library fan stopped working","description":"Ceiling fan near desk 12 does not turn.","categoryId":1,"priority":"HIGH"}
```

Ticket response (illustrative IDs/times):

```json
{
  "id":1,"title":"Library fan stopped working","description":"Ceiling fan near desk 12 does not turn.",
  "categoryId":1,"categoryName":"Facilities","creator":{"id":2,"name":"Asha Rao"},"assignee":null,
  "priority":"HIGH","status":"OPEN","resolutionSummary":null,
  "createdAt":"2026-10-14T12:01:00Z","updatedAt":"2026-10-14T12:01:00Z",
  "resolvedAt":null,"closedAt":null,"version":0
}
```

Assignment: `{"assigneeId":3,"version":0}`. Assignment returns version 1, status OPEN. Progress: `{"status":"IN_PROGRESS","version":1}`. Resolve: `{"status":"RESOLVED","resolutionSummary":"Replaced the capacitor","version":2}`. Close: `{"status":"CLOSED","version":3}`. Use the actual version from each response.

Alternatively reopen a resolved ticket: `{"status":"OPEN","reason":"Fan stopped again","version":3}`. It keeps the assignee and clears the current resolution/timestamp; the old summary stays in the resolution event. Comments: `{"body":"Technician will visit this afternoon","version":1}`. Comment responses contain only comment data; GET the ticket again to retrieve its incremented version.

Account status: `{"active":false,"version":0}`. Category update: `{"name":"Facilities","description":"Campus maintenance","active":false,"version":0}`. No delete endpoints are exposed.

## Pagination/filtering

`GET /tickets?page=0&size=20&status=OPEN&priority=HIGH&categoryId=1&title=library&sort=createdAt&direction=desc`

Admins may additionally specify `assigneeId`. Filters combine with AND. Title search is a literal, case-insensitive substring; URL encode spaces, `%` and other special characters. Sorting allows only `createdAt`/`updatedAt` and `asc`/`desc`; ID uses the same direction as the stable tie-breaker. Page is 0..100000; size is 1..100. Defaults are page 0, size 20, createdAt descending. Activity defaults to oldest first. Categories sort by name/ID. Users sort by ID.

All pages return `content`, `page`, `size`, `totalElements`, `totalPages`. Counts reflect only the caller's visibility. `GET /categories?includeInactive=true` requires admin.

## Validation limits

| Field | Limit |
| --- | --- |
| User name | Nonblank, at most 100 characters |
| Email | Valid format, at most 254, normalized unique |
| Password | Nonblank, 12..64 characters, ≤72 UTF-8 bytes |
| Category name/description | Nonblank, 80 / 500 characters |
| Ticket title/description | Nonblank, 160 / 5000 characters |
| Comment | Nonblank, at most 2000 characters |
| Resolution summary / reopen reason | Nonblank for the relevant transition, at most 2000 |
| IDs / version | Positive IDs; nonnegative version required on writes to existing resources |

Enums are case-sensitive: role STUDENT/STAFF/ADMIN, priority LOW/MEDIUM/HIGH, status OPEN/IN_PROGRESS/RESOLVED/CLOSED. Role is never accepted in public request bodies.

## Errors

```json
{
  "type":"urn:campusdesk:error:validation_error","title":"Bad Request","status":400,
  "detail":"Request validation failed","instance":"/api/v1/tickets",
  "code":"VALIDATION_ERROR","correlationId":"73986600-e009-43db-8a10-d183796e6399",
  "fieldErrors":{"title":"must not be blank"}
}
```

Other stable codes include `INVALID_REQUEST`, `INVALID_PAGE`, `INVALID_SORT`, `INVALID_SEARCH`, `INVALID_CATEGORY`, `INVALID_ASSIGNEE`, `EMAIL_EXISTS`, `CATEGORY_EXISTS`, `INVALID_PASSWORD`, `INVALID_CREDENTIALS`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `INVALID_TRANSITION`, `TICKET_CLOSED`, `VERSION_CONFLICT`, `RESOLUTION_REQUIRED`, `REOPEN_REASON_REQUIRED`, `ALREADY_ASSIGNED`, `SELF_DEACTIVATION`, `DATA_CONFLICT`, `HTTP_ERROR`, `INTERNAL_ERROR`. On `VERSION_CONFLICT`, reload the resource and deliberately retry; do not automatically overwrite another user's decision.

For a runnable demonstration, start the local setup/application from README and run `python scripts/walkthrough.py`. That script issues real curl requests without hard-coded local credentials.
