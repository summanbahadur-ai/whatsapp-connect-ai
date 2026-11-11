# Java 25 migration specification

Prepared: 11 November 2026. Status: proposed implementation contract for review.

## 1. Objective and boundaries

Create a separate project at `D:\whatsapp-connect-ai` with:

- Java **25**, Spring Boot and PostgreSQL for every business API and background job.
- The existing React 19 / TypeScript / Next.js frontend, preserving its screens,
  components, styling, translations, navigation and functional behavior.
- Small implementation checkpoints. Test, document and stop after each checkpoint.
- No changes to the original project, its database, uploads or Meta connection.

This request authorizes this specification and initial directory layout. It does
not mean the application migration or the database provisioning has already run.
Start coding only after this specification is reviewed. Do not implement all
modules in one uninterrupted run.

PostgreSQL is the database server, not a language runtime. Java will connect to it
using JDBC. Use a **new database `whatsapp_connect_ai`** on the existing PostgreSQL
18 installation. Do not point the Java application at the old `omni_connect`
database. Fresh test accounts and empty initial business data are the proposed
default; any later data/password migration is a separate explicit operation.

## 2. Architecture decisions

1. One repository with `frontend/`, `backend/`, `database/`, `scripts/`, `infra/`
   and `docs/`. One modular Spring Boot backend, not microservices.
2. Retain Next.js for frontend rendering and routing. It still requires Node.js.
   "Pure Java backend" means no TypeScript API handlers, authentication server,
   SQL repositories, Meta integrations, workflow engines or job workers at runtime.
   Next.js rendering and a transport-only rewrite/proxy are allowed.
3. Browser traffic uses one origin. During development: frontend port **3001**,
   Java port **8081**; original application may keep port 3000. Next rewrites
   `/api/**` and `/media/**` to Java. Production reverse proxy routes those paths
   directly to Java and page/static requests to Next.js.
4. Spring MVC, Bean Validation, Spring Security, Spring Session JDBC, Spring JDBC,
   PostgreSQL JDBC, Flyway, Java HTTP client, Java cryptography and Java Mail.
   Maven Wrapper, Java release 25. No Kotlin, Scala, Groovy or preview Java features.
5. Use Spring JDBC initially to retain existing SQL and account/RLS behavior.
   Do not combine this migration with a full ORM/schema redesign. No Hibernate
   automatic schema creation or update; only versioned Flyway migrations.
6. Proposed Spring Boot baseline: **4.1.2**, verified as Java-25-compatible in the
   official requirements. Validate the actual dependency graph on JDK 25 at CP01
   and pin versions. No untested snapshot dependencies. A later patch upgrade
   requires a recorded compatibility check.
7. Browser login uses server-side database-backed sessions, not localStorage JWTs.
   Scope-based external API keys remain distinct from browser sessions.
8. Background processing and scheduler ownership belong exclusively to Java in
   the new project. No dependency on the old app to make a migrated feature work.
9. Local uploads remain filesystem-backed with PostgreSQL metadata. External
   providers are replaceable Java ports with deterministic test doubles.
10. Keep one checked-in OpenAPI contract and a route-level parity ledger. Existing
    authored OpenAPI is the baseline, not proof of runtime correctness; handlers,
    SQL constraints and existing tests settle any discrepancy.

Alternatives considered: a Vite SPA would require replacing Next navigation,
rendering and i18n integrations; microservices would add deployment and transaction
complexity. Keeping Next for UI with a single Java backend minimizes unrelated work.

## 3. Required functional scope

Every feature in the source inventory must either pass its checkpoint or be
explicitly recorded as not implemented. Silent omissions are not acceptable.

| Area | Required behavior |
| --- | --- |
| Authentication | Signup, signin, signout, persistent sessions, expiry/revocation, password change/reset, configured email verification/change, safe redirects, validation and development email capture |
| Account ownership | Atomic owner/account/default provisioning, profile, account name/currency, owner/admin/agent/viewer permissions, membership revalidation |
| Team | Member listing, invitations/peek/accept, expiry, single-use/concurrent acceptance, role changes, removal and ownership transfer protections |
| Shell and dashboard | Existing sidebar/header/mobile layout, theme/locales, protected pages, dashboard metrics with account isolation and real aggregates |
| Settings | Overview, profile, security, appearance, WhatsApp, templates, quick replies, fields/tags, deals/currency, members and API keys |
| Contacts | Listing/search/pagination, creation/update/delete, bulk import and per-row outcomes, tags, custom values and notes; preserve current import contract |
| Inbox | Conversations, history/pagination, contact details, assignment, statuses, unread counts, presence, polling, message updates and reactions supported by source |
| WhatsApp connection | Number/WABA/token checks, encrypted credentials, optional registration PIN behavior for test numbers, registration health/details, subscriptions, test/reset |
| WhatsApp messages | Text, templates, image/video/audio/document, interactive buttons/lists, replies, filenames/captions and supported reactions; status callbacks and actionable errors |
| Templates | Local draft/create/edit/delete, sync/submit, categories/languages/headers/body/footer/buttons, samples and handles, lifecycle/quality updates and Authentication-template restrictions |
| Storage/media | Account-owned uploads, bucket/path/content checks, deletion, public signed media access where needed by Meta, inbound media mirroring and bounded safe downloads |
| Pipelines/deals | Pipeline/stage CRUD, ordering, moving deals, won/lost state, contact links, values/currency and existing summary behavior |
| Quick replies | Text and interactive variants, validation, CRUD and insertion in the inbox |
| Broadcasts | Draft/save/schedule/start/status/details/recipients, template variables, batching, deduplication and delivery/read/reply state tracking |
| Flows | Existing visual graph editor and gallery, CRUD/clone/activation/validation, keyword/first-inbound/manual triggers, input collection, branches, media, tags, handoff, timeouts, run history/events |
| Automations | Existing builder/gallery/clone/toggle/trigger/logs, all trigger and step types, nested branches, waits/resume, webhook calls and scheduled execution actually supported by source |
| AI | Settings, encrypted provider keys, provider/model selection, playground/drafts, auto-reply/pause/handoff limits, context, knowledge CRUD/processing/retrieval/embeddings and usage |
| Notifications | Assignment notifications, list/read/counts, badges and polling behavior |
| Public API | All `/api/v1` operations, exact scope checks, cursor pagination and error envelopes, API-key lifecycle and outbound signed webhook destinations/delivery |
| API documentation | Authenticated OpenAPI download and interactive explorer/Test API, session/CSRF support, scoped key entry, current-origin guard and reference-only machine operations |
| Optional MCP | Existing externally accessible tool capabilities accounted for; a Java adapter is required if enabled, with no database access and no privileged bypass |
| Operations | SQL migrations, roles/grants/RLS, job coordination, mail, logs, health/readiness, build/run scripts, backups, test reports and optional container deployment |

The complete source route, page, table and type inventories accompany this spec
in `MODULE_PARITY.md` and `reference/`. Enumerated capabilities and the generated
inventory are both required; the count of controllers alone does not establish parity.

## 4. Frontend preservation contract

- Copy source-controlled frontend/UI assets only. Do not copy `.env.local`,
  `.git`, `node_modules`, `.next`, `.cache`, runtime data, credentials or Word files.
- Keep the current dependency lock initially. Preserve en/ko/pt/es catalogs,
  accessibility, responsive layout, themes, React Flow editor and message editors.
- Necessary changes: replace Better Auth client with a typed Java-auth adapter,
  centralize browser fetch/CSRF handling, change transport rewrites and protected
  page checks, replace server-only imports and update contract types where agreed.
- Audit server components/layouts/route handlers/proxy before copying. Remove
  SQL/Better Auth business dependencies from the frontend. A cookie's presence is
  never sufficient proof of login; Java validates it. Frontend guards handle 401/403.
- Do not silently call old TypeScript APIs for unfinished modules. Keep those
  screens explicitly unavailable or show a clear "not migrated yet" state.
  Do not claim empty placeholder data is a migrated module.
- Existing `/api` paths, request keys, status codes and dashboard/public response
  envelopes should remain compatible except the explicit auth/CSRF changes.
  Record every intentional difference with its adapter and tests.

## 5. Nonfunctional acceptance requirements

- Account A cannot list/read/write/delete Account B's records, even using known IDs.
  Apply this to children, uploads, keys, exports, workflows and background jobs.
- Passwords, reset tokens, sessions, access tokens and encryption keys never enter
  logs, API examples, browser bundles, Git or test artifacts. Redact provider errors.
- Parameterized SQL; transaction-local authenticated identity; rollback and pool
  release on every path. Never run application requests as the schema owner.
- CSRF-protected browser writes; bounded input/page/upload sizes; safe URL fetching;
  no arbitrary redirect forwarding of credentials; no open CORS wildcard with cookies.
- Preserve webhook HMAC over raw bytes, replay deduplication, signature fail-closed,
  account-by-phone routing and WABA template-event routing.
- Durable queue/work claims across restart, bounded retries and single-owner
  scheduled execution. Never blindly retry a Meta send with an unknown outcome.
- Tests use a real disposable PostgreSQL instance (Testcontainers or a dedicated
  local test DB), never the working source DB. H2 is not an adequate RLS/JSONB test.
- Unit + SQL integration + API contract + role matrix + browser happy/failure tests
  at each relevant gate. Preserve existing source behavior tests as specifications.
- No automatic customer sends or AI charges during routine automated tests.
- Each implementation checkpoint has a commit/tag and an updated status record,
  with commands, results, tested URLs, manual checks, known limitations and rollback.
  No GitHub publishing is implied by a local checkpoint commit.

## 6. Stable checkpoints and review policy

"Stable" means the currently enabled modules work together, their tests pass,
and modules not implemented are clearly marked. It does not mean the entire
application is complete after login. See `MIGRATION_CHECKPOINTS.md`.

After a checkpoint: run its checks, record evidence, retain a last-known-good
commit, stop and ask the user to test/review it. Proceed to the next checkpoint
only after the user says to continue. A failed checkpoint is fixed before moving on.
Never use database restore/reset or Git hard reset to hide a failed checkpoint.

## 7. Deliberate differences and deferred items

- New database and fresh sessions/accounts by default. Old passwords, users,
  messages and WhatsApp credentials are not automatically migrated.
- New Spring Security auth contract/CSRF support is bridged by a small UI adapter.
  Do not pretend Java implements every generated Better Auth internal route.
- Java replaces Next `after()` with a durable PostgreSQL-backed inbound work queue
  before acknowledging accepted events. This is a deliberate reliability change.
- Old `collect_input.validation` is documented in source as accepted but ignored.
  Preserve that behavior initially or explicitly approve stricter validation later.
- An order-status external API node is not present among the existing ten flow
  node types. Do not invent it as migration parity. Existing automation webhooks
  are migrated; a new flow HTTP-request node is a later feature if requested.
- Redis, Kafka, Kubernetes, a new design system, multi-account-per-user redesign,
  customer Embedded Signup and new commerce features are outside initial parity.

## 8. References and implementation baseline

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html): supports Java 25; check pinned libraries as well.
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html): browser-cookie write protection and SPA integration.
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html): PostgreSQL-backed session storage and custom data source.
- [Spring SQL support](https://docs.spring.io/spring-boot/reference/data/sql.html): JDBC/data sources.
- [PostgreSQL JDBC](https://jdbc.postgresql.org/): Java database driver.

Version compatibility references checked on 11 November 2026. Source snapshot
metadata and hashes are in `reference/SOURCE_BASELINE.json`. The source working
tree, including uncommitted changes, is the feature inventory baseline; HEAD alone
does not fully describe it. No runnable checkpoint has been executed by this spec.
