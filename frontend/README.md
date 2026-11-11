# Frontend: CP03 structure and Java transport

This is the existing React 19/TypeScript/Next.js UI, prepared for the Java backend.
The original layouts, components, themes, translations, assets and business
screens are preserved. Only login presentation and connection checking are enabled.
Business screens show an explicit unavailable page and make no business API calls.

## Run: two terminals

From D:\whatsapp-connect-ai:

1. Java terminal: .\scripts\start-backend.ps1 -Database
2. Frontend terminal: .\scripts\start-frontend.ps1
3. Open http://127.0.0.1:3001/login.
4. Click **Test Java connection**. Expect **Java backend connected.**
5. /contacts or /settings should say the module is not available yet.
6. Signing in is intentionally unavailable until the Java authentication checkpoints.

Dependencies are already installed on this machine. On another machine run npm ci
inside frontend first. Next needs Node>=20.9.0; verified here with Node 24.19.0.
Stop with Ctrl+C. No PostgreSQL or provider credentials belong in frontend.

## Structure

- src/app: preserved pages/layouts, CSS and icon rendering. No business route handlers.
- src/components: preserved module UI; migration/ contains the availability gate
  and Java connection indicator.
- src/hooks: preserved browser hooks, ready for future module integration.
- src/lib/api/client.ts: typed same-origin browser fetch and safe failure messages.
- src/lib/auth/client.ts: temporary unavailable compatibility interface only;
  it never signs in, invents sessions, or calls the old Better Auth backend.
- src/lib/module-availability.ts: enabled/pending module list.
- src/lib: browser presentation/validation helpers. No SQL/provider engines.
- src/types: API-facing presentation types; contact import DTO no longer imports SQL code.
- src/i18n + messages: original en/ko/pt/es catalogs.
- next.config.ts: rendering configuration and transport rewrites only.
- config: server-only Java target validation, security headers and copied UI lint baseline.
- src/styles/shadcn.css + licenses/shadcn-LICENSE.md: original CSS and license;
  the unused shadcn CLI/MCP backend tooling dependency is removed.

## Transport

Browser -> Next3001 -> Java8081. Defaults use 127.0.0.1.
Only optional JAVA_BACKEND_ORIGIN lives in frontend/.env.local. Use the supplied
.env.example if needed; never copy source .env.local or Java database settings.

- /api/health -> Java /actuator/health (public readiness demonstration).
- /api/** -> Java /api/**.
- /media/** -> Java /media/**.
- No fallback to original port3000, no database access, no authentication decisions.
- Cookies, authorization, CSRF header, HTTP statuses and raw body bytes are forwarded.
- Next normalizes query encoding (a+b becomes a%20b); decoded values are preserved.
  Do not assume a byte-identical raw query string.
- Java remains responsible for authentication, sessions, CSRF, validation and business logic.
- APIs/media are no-store. Original security-header policy is retained.

## Verification

From project root run .\scripts\verify-frontend.ps1. This runs unit/boundary
tests, lint, typecheck, production build and six Chrome protocol tests.
Stop the review frontend before running the full verification command. The browser tests own a fixture on18081 and test frontend on3011; they never
send provider messages or access PostgreSQL. Chrome must be installed.

With real Java8081 and frontend3001 already running, run npm run test:java inside
frontend for a separate real-server browser smoke check and desktop/mobile screenshots.

Next and eslint-config-next are pinned to16.4.6 for inherited security fixes.
The original source dependency lock was the starting point. Targeted compatible
security updates were applied; no force/major audit fix was used.
Copied UI lint findings remain visible in config/legacy-lint-baseline.mjs:
remove each entry when migrating/testing that module. New transport/gating files
are not covered. Do not describe a warning-bearing lint run as warning-free.

See ../docs/checkpoints/CP03.md for exact observed results and remaining findings.
