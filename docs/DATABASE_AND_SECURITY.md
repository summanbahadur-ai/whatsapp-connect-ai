# PostgreSQL, authentication and security design

## Database separation and migration ownership

- Existing app: `omni_connect`. New app: proposed `whatsapp_connect_ai`.
- Same installed PostgreSQL 18 server is acceptable; separate database, roles,
  file roots and environment settings are required. Tests use another disposable DB.
- Create nothing in the database until CP03. Do not reuse the chat-shared password
  or copy `.env.local`; obtain/set local connection details privately at that stage.
- Schema references copied to `docs/reference/source-sql/` are read-only design
  input. **Never execute them wholesale against the new database.** They contain
  the old identity/session/provisioning model and grants that require adaptation.
- Executable migrations live only in `backend/src/main/resources/db/migration/`.
  Flyway owns migration history. Disable competing auto-DDL/schema initialization.
  Role creation is separate bootstrap administration, never an app request.
- Keep relational constraints, UUIDs, JSONB, arrays, timezone-aware timestamps,
  full-text search, RLS and account functions where applicable. No pgvector is
  required merely to reproduce the current `real[]` embedding implementation.
- Proposed new migrations: V1 Java identity and Spring sessions; V2 reviewed domain
  schema (including the contact/message/deal tables needed for initial dashboard
  aggregates); V3 reviewed provisioning, roles, grants and RLS. Later migrations add
  Java-specific queue/work schemas and genuine schema changes. Do not claim
  a module migrated simply because its table exists. Migrations are append-only
  after a checkpoint; use forward repair migrations and tested backups.

## Database roles and account isolation

Use distinct least-privilege credentials:

| Role | Purpose |
| --- | --- |
| `wc_owner` | Schema ownership and migrations; never normal HTTP traffic |
| `wc_auth` | Identity, password/reset verification and JDBC session tables; tightly scoped provisioning interface |
| `wc_app` | CRM requests subject to RLS and explicit account predicates |
| `wc_worker` | Authenticated provider/key/background processing; scoped repository predicates even if privileged |

Spring data sources/transaction managers must select these roles explicitly.
Spring Session uses the auth data source, not a default migration/owner connection.
Flyway may use migration-only credentials at bootstrap/deploy; do not expose them
to application request code. Limit pools to the local database's connection budget.

Within a CRM transaction, set trusted identity with parameterized
`SELECT set_config('app.user_id', ?, true)` on the **same physical connection**
used for repository SQL. Resolve membership again before work; use the role/account
found in PostgreSQL, never a posted `account_id` or browser role. Do not use session-
global settings or assume an `@Transactional` annotation automatically chooses the
correct data source. Test success, rollback, reused connection and concurrent users.

Preserve current one-active-account-per-user behavior. Keep last-owner protections,
role restrictions and SQL/account checks on child records. Use integration tests
running as `wc_app`, not a superuser, to prove RLS. Audit SECURITY DEFINER functions:
fixed search_path, minimal grants and no arbitrary account provisioning by users.

## Identity and session model

- Retain `identity.users` IDs/name/email/image fields needed by domain references;
  define Java-owned password credentials and hashed verification/reset-token tables.
- Better Auth's `identity.sessions` and generated route internals are not reused
  as Spring sessions. Use the PostgreSQL Spring Session schema (for example
  `identity.spring_session` and `identity.spring_session_attributes`) via Flyway.
- Use an explicit Spring Security password encoder with stored algorithm identifier
  (BCrypt initial choice; pin cost after testing on this machine). No plaintext,
  reversible password encryption or assumption of Better Auth hash compatibility.
- Minimum password length initially matches source (8); bounded maximum protects
  the encoder. Case/normalization and duplicate-email semantics are contract-tested.
- Signup provisioning must have one transaction boundary. Match the actual source
  signup defaults (account/profile, owner role and currency). Default pipelines and
  stages are seeded by the pipeline module, not by the final signup trigger. Do
  not invent signup side effects or run both a Java provisioner and
  an old trigger that creates another account. A forced failure leaves no orphan.
- New project starts with fresh identities/sessions. Optional user/password import
  requires a separately tested encoder compatibility/reset plan and approval.
- Session cookie: proposed `WC_SESSION`, HttpOnly, Path `/`, SameSite=Lax; Secure
  on HTTPS. Seven-day maximum age to match the original configured duration;
  explicitly set/test idle expiry and absolute lifetime rather than relying on
  Spring defaults. Rotate session ID on login, revoke on logout/password reset.
- Store only stable principal identifiers/authorities needed by the session; avoid
  opaque serialization of large domain entities and ensure restart compatibility.

## Explicit Java auth endpoints and frontend adapter

These are proposed target endpoints, not already working routes:

| Method/path | Request/result |
| --- | --- |
| GET `/api/auth/csrf` | Returns `{token, headerName}` with private/no-store; creates/materializes a CSRF token for the browser |
| POST `/api/auth/sign-up/email` | `{name,email,password}`; 201 `{user}` plus session cookie if source auto-login behavior is retained |
| POST `/api/auth/sign-in/email` | `{email,password}`; 200 `{user}`, rotates cookie; invalid credentials 401 |
| GET `/api/auth/get-session` | `{user,session}` or JSON `null` when anonymous, matching adapter expectation; never returns password/session secret |
| POST `/api/auth/sign-out` | 200 `{success:true}`; invalidates server session and cookie |
| POST `/api/auth/request-password-reset` | `{email,redirectTo?}`; generic 200 response without user enumeration; allowed redirects only |
| POST `/api/auth/reset-password` | `{token,newPassword}`; single-use token; expires/reset-revokes sessions |
| POST `/api/auth/change-password` | Current/new password; requires login/current password; rotates/revokes sessions by explicit policy |
| POST `/api/auth/change-email` | New email; verified email-change flow and current session authorization |
| GET or POST verification endpoint | Token validation, expiry, one-time use and allowlisted redirect; exact URL fixed and tested at CP06 |
| GET `/api/auth-context` | Existing `{profile,account}` envelope for trusted user/account/role |

Account creation/login errors may retain a documented auth `{error:{code,message}}`
shape through the adapter. Capture actual current client calls in the CP04 tests;
update the target OpenAPI and adapter together, never fake Better Auth internals.
Additional current auth operations discovered in the source inventory must be
explicitly mapped or documented as library-only with no active UI consumer.

`frontend/src/lib/auth/client.ts` becomes a small typed adapter preserving calls
used by pages/hooks. Browser fetch helper gets/refreshes CSRF tokens, sends the
returned header on unsafe session requests and uses same-origin credentials.
Refresh CSRF after login/logout. Support multipart/binary requests without forcing
JSON headers. Handle expired sessions and concurrent fetches deterministically.

## Three separate authentication boundaries

1. Browser APIs: Spring session + current membership/role; CSRF for unsafe methods,
   including login/signup/logout. Exact origin validation supplements CSRF.
2. Public `/api/v1`: stateless Bearer API key only, hashed at rest and exact scope
   enforcement. Do not let a browser session substitute for a missing API key.
   Exempt CSRF only on this stateless chain, not on all `/api/**` routes.
3. Provider integrations: webhook GET challenge matches saved verify token;
   webhook POST checks Meta HMAC raw bytes. Cron/manual integration endpoints,
   if retained for parity, require server secrets and cannot execute via Swagger.

Use no wildcard credentialed CORS. Default to the same-origin proxy; if a direct
local cross-origin API mode is added, explicitly allow only configured origins and
test cookie/CSRF behavior. Never place browser secrets in `NEXT_PUBLIC_*`.

## Provider secrets, URLs, uploads and email

- Encrypt per-account Meta/AI secrets with AES-256-GCM. Preserve the source wire
  encoding when importing is later authorized: `ivHex:ciphertextHex:tagHex`.
  Read-only legacy CBC support only if needed for an approved import. Add Java/TS
  known-vector tests using fake keys; do not copy production encryption material.
- Incoming Meta events require the App Secret from the specific configured Meta
  app. Verify exact raw body, reject missing/bad signatures, compare safely.
- Reject SSRF/private/loopback/link-local targets for outbound webhooks, media and
  automation HTTP calls, including DNS and redirects. Apply timeout/body limits.
- Resolve storage paths beneath a configured root, never trust user path segments.
  Enforce ownership, content limits and upload type. Signed public media access has
  expiry and constant-time verification; private uploads are not a public directory.
- Development mail is captured privately under new-project local-data/mail.
  Production requires configured SMTP, expiry/use-once tokens and no false success.
- Log request correlation and redacted provider codes, never passwords/tokens,
  raw sensitive request bodies or SQL parameters.

## Reliability boundary

Persist webhook queue before 200, with replay keys, bounded retention, attempt count,
lease, due time and dead-letter state. Workers use row locks/SKIP LOCKED or advisory
locks on appropriate dedicated connections. Persist workflow/broadcast progress.
Test process crash/restart and two concurrent workers. External sends cannot be
made exactly-once by a database transaction alone: record unknown outcomes and
reconcile them rather than claiming a retry is safe after every network timeout.
