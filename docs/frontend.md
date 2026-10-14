# CampusDesk frontend

The frontend lives in `src/main/resources/static`: Spring Boot serves the HTML, CSS and native JavaScript modules from the same service as the API. Open `/` at your backend's URL after redeploying. The Dockerfile already copies resources into the application jar, so Render builds and serves both parts together. No separate web service, Node build, environment variable containing an API URL, or additional CORS origin is needed.

## Workspaces

- Students register/sign in, see role-scoped counts and requests, filter/paginate requests, create tickets, comment, read history, close resolved requests and reopen them with a reason.
- Staff sign in, see their assigned requests, comment, start work and resolve with a required summary.
- Administrators see campus statistics and all tickets, assign/reassign active staff, manage categories, create staff, and activate/deactivate accounts.

Request details expose conversation and history tabs, separate activity pagination, resolution summaries and lifecycle actions appropriate to the current account. Admin user search/filtering is performed on the server and remains paginated. Category and staff pickers fetch only one bounded page at a time. Ticket forms submit the current version; conflicts stop the operation and require reviewing fresh data.

The design uses a dark green navigation rail, soft green/purple accents, readable request tables, clear empty/error/loading states, native modal dialogs and a responsive mobile menu. Decorative artwork is code-native; no external fonts, images or script CDN is required. Keyboard focus, form labels, reduced-motion support and live notifications are included.

## Files

- `app/app.js`: session startup, navigation and role-specific shell.
- `app/api.js`: same-origin API calls, Problem Details and session token handling.
- `app/auth.js`: login/student registration.
- `app/dashboard.js`: live overview and scoped counts.
- `app/tickets.js`: ticket list/details, creation, conversation, history and lifecycle.
- `app/admin.js`: categories and account management.
- `app/ui.js`: shared dialogs, escaping, pagination and form handling.
- `app/styles.css`: application and mobile styling.

URLs use hash navigation (for example `/#/tickets/1`), so deep links do not need a server-side SPA rewrite. Only the frontend's GET resources are public; protected APIs still require authentication. All displayed API/user-provided text is escaped. Passwords are not persisted. Access tokens are stored in sessionStorage for the current tab and cleared on logout or an unauthorized protected request. This storage is accessible to JavaScript; defending against XSS remains important. Client logout does not revoke an issued JWT, and account permissions/active status are checked by the backend on every protected request. There are no refresh tokens; sign in again when the session ends.

## Local run and Render

Use the existing commands:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-local.ps1
```

Open `http://localhost:8080/`. If a background instance is already running, stop it with `scripts/stop-app.ps1` before launching the updated runtime copy.

Commit the frontend and small API/security updates, push your repository, then deploy the latest commit on the existing Render web service. The homepage appears at the same `.onrender.com` URL, with Swagger still available at `/swagger-ui.html`. Leave `BOOTSTRAP_ADMIN_ENABLED=false` once your initial administrator exists.

## Focused browser verification

The optional local-only browser check uses Node and Playwright with an installed Chrome/Edge. These tools are not application dependencies and are not needed by Render. With local PostgreSQL and the app running:

```powershell
npm.cmd install --prefix .tools/ui-check --no-audit --no-fund playwright@1.64.1
node scripts/check-frontend.mjs
```

It reads the local administrator credentials from ignored `.local/local.env`, creates synthetic development accounts/data, and checks the student/staff/admin workflow, literal text escaping, session restoration/logout, and mobile layout/navigation. It refuses non-local URLs. Screenshots are saved under `.local/frontend-check/`; synthetic records are retained in the development database. `BROWSER_EXECUTABLE` can select another installed Chromium browser. The latest run passed on 15 October 2026; see `docs/testing.md`.
