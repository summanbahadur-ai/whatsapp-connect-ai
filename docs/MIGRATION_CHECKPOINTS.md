# Java backend migration implementation plan

**Goal:** Reproduce the current CRM in a separate Java 25/Spring Boot/PostgreSQL
project while preserving the React/TypeScript UI and a tested stopping point.

**Architecture:** Next.js UI on 3001, transport-only same-origin proxy to Java on
8081, JDBC repositories and separate PostgreSQL database. Java owns auth, APIs,
providers, workers and scheduler.

**Tech stack:** Java 25, pinned Spring Boot 4.1.2 baseline, Maven Wrapper, Spring
MVC/Security/Session/JDBC, Flyway, PostgreSQL 18, existing frontend dependencies.

**Spec:** [SPECIFICATION.md](SPECIFICATION.md). Read the architecture and security
design alongside it. This plan is not authorization to execute all checkpoints.

For agentic implementers: execute inline, one approved checkpoint at a time,
using the executing-plans and applicable verification/debugging skills. Do not
delegate or start multiple modules in parallel unless the user asks for it.

## Global constraints and gate protocol

**Git approval: always ask before each future commit and each future push. The
current CP01 commit/push is explicitly authorized; later checkpoints are not.**

- Java release **25**; no preview features or TypeScript business backend.
- New project `D:\whatsapp-connect-ai`; new DB `whatsapp_connect_ai`.
- Never mutate the original repo, `omni_connect`, old uploads or old Meta callback.
- Preserve current UI and contract behavior; record auth/CSRF differences explicitly.
- Do not install tools, create a database or send real messages simply by reading
  this plan. Execute the next user-approved checkpoint only.
- Each numbered checkbox is one small action. For behavior changes, first write
  a meaningful failing test and observe the failure; then implement and pass it.
- At every gate: run relevant unit/integration/contract/browser checks plus all
  previously enabled module regressions; run build/type/lint checks as applicable.
- Record exact commands/results in `docs/checkpoints/CPxx.md` and update STATUS.
  Commit locally after checks, with `checkpoint/CPxx` tag when useful. No remote
  push, deploy, data import or destructive cleanup is implied.
- **STOP after every checkpoint.** Give the user its URL, tiny manual test steps,
  observed results and limitations. Wait for acceptance/continue before the next.
- If a gate fails, repair that checkpoint; never continue into another module to
  obscure the failure. Preserve failing evidence and the last-known-good version.

## Review focus across all checkpoints

1. Forged/stale session, removed membership and known cross-account IDs must fail.
2. Cookie/CSRF/proxy behavior must work after login/logout/reload and across ports.
3. Database rollback/pool reuse and privileged-worker paths must retain isolation.
4. Duplicate events, restart and ambiguous provider sends must not double-reply.
5. Hidden settings/editor options and actual handler contracts must not be lost
   merely because a happy-path screenshot looks correct.

## Phase 1 — Structure, architecture and executable foundation

### CP00 — Specification and inventory (this delivery)

- [x] Inventory the source working tree, routes/pages, SQL tables and module types.
- [x] Write architecture, security, setup and checkpoint specifications.
- [x] Create a separate initial folder tree and safe reference snapshots.
- [x] User authorized CP01 implementation; separate-database credentials remain to be confirmed at CP02.

Gate: documents/links/inventories are valid; no application/runtime/database claims.
Stop: review the documents before any code generation.

### CP01 — Tools and minimal Java foundation

Files: `backend/pom.xml`, Maven Wrapper, application class, security/health
configuration, initial tests, root ignore rules and environment templates.

1. [x] Check JDK/Javac 25, Node/npm, PostgreSQL, Git and free ports; record versions.
2. [x] Initialize a new local Git repository (no remote publishing), preserve the source license, then pin Boot/dependencies and Maven Wrapper; set compiler release to 25.
3. [x] Add a failing startup/health test and a protected-route unauthorized test.
4. [x] Generate the smallest Java application that passes; no business stubs.
5. [x] Restrict exposed Actuator data; enable only health/readiness as appropriate.
6. [x] Run wrapper version, `mvnw.cmd verify` and Java startup on 8081.
7. [x] Record results, commit and stop.

Manual gate: health returns 200 without sensitive configuration; protected API
returns 401. A successful compile alone is not a database/auth checkpoint.

### CP02 — New PostgreSQL foundation

Files: database bootstrap/readme, data-source/transaction configuration, Flyway
V1–V3, `AccountContext`/JDBC integration tests and test database settings.

1. [ ] Confirm new DB/role credentials privately; verify the target is not `omni_connect`.
2. [ ] Create isolated test DB or Testcontainers configuration and a disposable fixture.
3. [ ] Write failing migration/RLS/transaction/pool-reuse tests under actual app roles.
4. [ ] Create new database/roles, Java identity/session tables, reviewed domain schema and grants. Domain tables needed by the dashboard exist before CP07, even while their APIs remain disabled.
5. [ ] Implement explicit auth/app/worker data sources and transaction-local identity.
6. [ ] Port only necessary provisioning/profile-sync functions; test rollback/no duplicates.
7. [ ] Run fresh migration and second no-op migration; test two accounts and rollback.
8. [ ] Document backup/connection checks, commit and stop.

Manual gate: pgAdmin shows the new database and Flyway history. The original
database has not changed. No contacts/messages are imported.

### CP03 — Frontend structure and Java transport

Files: `frontend/` UI copy, lockfile, `next.config.ts`, typed API transport,
module-availability manifest and browser tests.

1. [ ] Copy UI/components/routes/assets/translations with license; exclude all private/generated files.
2. [ ] Audit and remove frontend imports of SQL, Better Auth server, workers and business API handlers.
3. [ ] Configure `/api/**` and `/media/**` rewrites to Java; preserve cookies/raw request bytes.
4. [ ] Add browser/HTTP tests for proxy health, Set-Cookie, byte integrity and backend unavailability.
5. [ ] Keep login page visual layout; mark unfinished feature screens unavailable.
6. [ ] Verify no old-app URL fallback or frontend database/provider credentials.
7. [ ] Run frontend typecheck/lint/build and Java tests; commit and stop.

Manual gate: frontend on 3001 renders the existing login UI; a browser request
reaches Java on 8081. Signing in is not claimed until CP04/05 pass.

## Phase 2 — Signup, login and authenticated dashboard

### CP04 — Signup and owner provisioning

Files: auth/account DTOs/controllers/services/repositories, password encoder,
signup auth-adapter methods and SQL integration/browser tests.

1. [ ] Capture actual signup UI calls and establish the target request/response contract.
2. [ ] Write failing duplicate-email, invalid-password and provisioning-rollback tests.
3. [ ] Implement password hashing and atomic identity/profile/account/default creation.
4. [ ] Add CSRF acquisition and safe signup fetch through the frontend adapter.
5. [ ] Test new user, duplicate user, failure halfway through defaults and two concurrent signups.
6. [ ] Verify stored hashes are not plaintext and responses/logs contain no credentials.
7. [ ] Run all foundation regressions, commit and stop.

Manual gate: signup creates exactly one user/account/profile and expected defaults
in the new DB. A failed signup leaves no partially usable identity/account.

### CP05 — Login, sessions, logout and protected shell

Files: SecurityFilterChain/session configuration, auth endpoints, Java-auth client
adapter, `use-auth` hook, protected layout/proxy checks and browser tests.

1. [ ] Write failing tests for valid/invalid login, forged/expired cookie and CSRF rejection.
2. [ ] Implement Java authentication/session rotation/persistence and auth-context lookup.
3. [ ] Replace remaining Better Auth UI calls while preserving existing login visuals.
4. [ ] Wire header/sidebar/profile/account permissions to the trusted Java response.
5. [ ] Implement logout/session revocation, expiry and Java-backed page authorization.
6. [ ] Test browser refresh, two tabs, server restart, bad password, logout/back/reload and removed membership.
7. [ ] Run role matrix and all previous regressions; commit and stop.

Manual gate: signup/login opens dashboard shell; reload stays signed in; signout
blocks protected data. Dashboard business charts may remain explicitly pending.

### CP06 — Password recovery, email and security settings

1. [ ] Write reset/change-password/email-verification tests, including expiry/replay/redirect attacks.
2. [ ] Implement hashed one-time tokens, configured Java Mail and private development mail capture.
3. [ ] Wire existing forgot/reset-password and Security settings screens to Java.
4. [ ] Implement current-password check, session invalidation and verified email/profile sync.
5. [ ] Test unknown email generic response, reused token, concurrent reset and mail failure.
6. [ ] Verify production refuses false delivery success and secrets are absent from logs.
7. [ ] Run complete auth/browser regression, commit and stop.

Manual gate: retrieve a development reset link locally, reset password, old password
fails, new password works, existing sessions are invalidated per documented policy.

### CP07 — Dashboard and navigation first slice

1. [ ] Inventory the dashboard's current aggregates, filters, defaults and error/loading states.
2. [ ] Write SQL tests for empty/new account, seeded activity and cross-account counts.
3. [ ] Implement dashboard queries and frontend wiring without fabricated data.
4. [ ] Verify sidebar/header, theme/locales and mobile navigation with Java auth.
5. [ ] Run role/browser/regression checks and stop after a stable commit.

Manual gate: new account shows valid empty metrics, not errors; fixture activity
changes expected metrics. Later module gates extend dashboard integration coverage.

## Phase 3 — Settings, one section at a time

### CP08 — Profile, appearance and avatar storage foundation

1. [ ] Write profile ownership/update and avatar path/type/size/access tests.
2. [ ] Implement profile endpoints and storage metadata/filesystem adapters for avatars.
3. [ ] Wire Profile/Appearance settings, preserve theme and all four locales.
4. [ ] Test profile change reflected in header, reload, invalid upload and another user's avatar mutation.
5. [ ] Verify file cleanup and profile sync; run prior regressions, commit and stop.

Manual gate: change name/avatar/theme/language; reload and verify retained behavior.

### CP09 — Account, members, invitations and ownership

1. [ ] Write role matrix, last-owner, stale membership and invitation concurrency tests.
2. [ ] Implement account update/member management/invite-peek/accept/revoke routes.
3. [ ] Implement ownership transfer and permission checks using trusted DB membership.
4. [ ] Wire Members settings and join page, preserving existing account model.
5. [ ] Test two invited users, expired/replayed invite, revoked member and concurrent acceptance.
6. [ ] Run SQL/RLS/API/browser regressions; commit and stop.

Manual gate: owner invites a user who joins only the intended account; viewer
cannot change roles; removal blocks future requests even with an existing session.

### CP10 — WhatsApp connection settings (no automatic live sends)

1. [ ] Write MetaClient fake tests for config validation, number/WABA mismatch and provider errors.
2. [ ] Implement AES-GCM codec and known-vector round-trip tests with fake keys.
3. [ ] Implement config/save/test/reset/details/registration checks and subscription logic.
4. [ ] Preserve test-number blank-PIN behavior and clearly distinguish credentials from registration health.
5. [ ] Wire WhatsApp settings; redact saved secrets and never disclose decrypted values.
6. [ ] Test repeated save, another account claiming the number and subscription failure.
7. [ ] Offer an explicit test-number/manual connection test without altering the old Meta app callback.
8. [ ] Record simulated versus live results separately, commit and stop.

Manual gate: UI handles successful and failed connection checks accurately. Inbox
delivery is not claimed until CP17 end-to-end testing.

### CP11 — Template settings and media handles

1. [ ] Write contract tests for actual category/header case, required variables and forbidden operations.
2. [ ] Extend storage for template media and bounded safe fetching/upload handles.
3. [ ] Implement template CRUD/sync/submit and Meta adapter operations.
4. [ ] Preserve samples, header/body/footer/button types, language and Authentication restrictions.
5. [ ] Wire template UI and verify provider error/status/quality handling with fixtures.
6. [ ] Run role/storage/template regression; optional deliberate live template test, commit and stop.

Manual gate: create/edit a local draft and simulate sync/rejection/approval. Do not
claim a template approved by Meta from a fake provider response.

### CP12 — Quick replies

1. [ ] Write text/interactive validation and exact create/update response contract tests.
2. [ ] Implement Java CRUD, including PATCH `{ok:true}` behavior where source requires it.
3. [ ] Wire Quick Replies settings and test ownership, empty text and malformed interactive payloads.
4. [ ] Run browser/role regressions, commit and stop; inbox insertion is tested at CP16/17.

### CP13 — Fields, tags and deal configuration

1. [ ] Write tag/custom-field CRUD, sort order, use/deletion constraints and currency validation tests.
2. [ ] Implement settings repositories/endpoints with account isolation.
3. [ ] Wire Fields/Tags and Deals settings, preserving legacy `?tab=` aliases and defaults.
4. [ ] Test duplicate/invalid inputs, viewer denial, known foreign IDs and reload persistence.
5. [ ] Run settings overview/navigation regressions, commit and stop.

### CP14 — API-key settings

1. [ ] Write tests for secure generation, hashing, one-time display, expiry/revoke and exact scope validation.
2. [ ] Implement API-key management and a stateless key-auth filter, separate from session auth.
3. [ ] Wire API settings; never return plaintext again after creation.
4. [ ] Test owner/admin creation versus viewer/agent denial, revoke and cross-account access.
5. [ ] Run all settings/auth regressions, commit and stop. Public business endpoints follow at CP27.

## Phase 4 — Business modules

### CP15 — Contacts

1. [ ] Write list/search/pagination and create/update/delete contract tests from source handlers.
2. [ ] Implement contact repositories/services/controllers with RLS and child ownership checks.
3. [ ] Add notes, tag links and custom values with their actual array/map contracts.
4. [ ] Port bulk import validation/results and test duplicate/invalid rows and rollback semantics.
5. [ ] Wire list/detail/forms/import; test Unicode, empty fields and account isolation.
6. [ ] Run integration/API/browser/settings regressions, commit and stop.

Manual gate: create/search/edit/tag/note/import/delete test contacts and verify
another account cannot access them by UUID or child ID.

### CP16 — Inbox read/state/polling

1. [ ] Write conversation list/detail/history pagination and assignment/status/read tests.
2. [ ] Implement Java inbox queries, bounded polling and unread-state updates (`unread_count:0` only).
3. [ ] Wire inbox layout/contact pane/history and quick-reply insertion using fixture messages.
4. [ ] Add assignment and permission validation (agent user ID versus profile row ID).
5. [ ] Test no-result/large history, unauthorized assignment, stale requests and reload.
6. [ ] Run contacts/auth/UI regression, commit and stop. Real sending remains unavailable until CP17.

### CP17 — Messaging, inbound webhooks and durable workers

1. [ ] Write raw-body HMAC/challenge/signature failure/phone routing/deduplication tests.
2. [ ] Add Flyway durable inbound/send-work schema and claim/retry/dead-letter tests.
3. [ ] Implement webhook persistence-before-ack, worker recovery and status/template-event routing.
4. [ ] Port text/template/media/interactive/reply/reaction validation and Meta send adapter.
5. [ ] Port media proxy/mirroring, public signed URLs, SSRF guards and storage ownership.
6. [ ] Test duplicate/out-of-order callbacks, provider acceptance plus DB failure and ambiguous timeout.
7. [ ] Run end-to-end fake-provider send/receive/history/status tests with two accounts/workers.
8. [ ] Deliberately configure a new-project Meta test callback; receive one real message and reply.
9. [ ] Record live prerequisites/results, all regressions, commit and stop.

Manual gate: actual test-number incoming message appears and CRM reply reaches the
verified phone. A connection-check green badge is not this checkpoint's evidence.

### CP18 — Pipelines and deals

1. [ ] Write pipeline/stage ordering, contact linkage, amounts/currency and deal transition tests.
2. [ ] Implement Java CRUD/move/won/lost and transactional ordering.
3. [ ] Wire board/forms/detail behavior preserving drag/drop UI.
4. [ ] Test concurrent moves, foreign IDs, deleted stages and dashboard aggregates.
5. [ ] Run full enabled-module regression, commit and stop.

### CP19 — Broadcasts

1. [ ] Write draft 201 versus launch 202 contracts, template variables and recipient limit/dedup tests.
2. [ ] Implement durable launch/schedule/batch worker and per-recipient status/reply tracking.
3. [ ] Wire list/create/detail/status screens with progress and actionable failure states.
4. [ ] Test restart, partial provider failure, repeat launch, cancellation/other controls only if source supports them.
5. [ ] Verify no duplicate sends from concurrent workers or ambiguous retry.
6. [ ] Run fake-provider/role/integration/UI regressions; optional small permitted live test, commit and stop.

### CP20 — Flow editor, persistence and validation

1. [ ] Map all ten current node types, graph JSON, triggers, variables and gallery templates.
2. [ ] Write graph validation/ownership/clone/activation tests, including dangling/loop edges.
3. [ ] Implement flow/nodes CRUD, gallery, run-history read endpoints and Java validators.
4. [ ] Preserve the existing React Flow editor and graph layout; wire save/load/clone/activation.
5. [ ] Test reload, variable preservation, invalid graph and foreign IDs; keep execution disabled until CP21.
6. [ ] Run UI/API/regressions, commit and stop.

### CP21 — Flow runtime, input, handoff and timeouts

1. [ ] Port source behavior tests for keyword/first-inbound/manual triggers and engine precedence.
2. [ ] Implement persisted cursor/vars/events, all nodes, interactive selection and collect-input resume.
3. [ ] Implement durable waits/timeouts/fallback/pause-by-agent with scheduler claim coordination.
4. [ ] Test replay, restart, two workers, invalid reply, media/condition/tag/handoff and termination.
5. [ ] Test order-status keyword → ask order number → capture number → confirmation/handoff as a parity example.
6. [ ] Explicitly exclude a new HTTP lookup node unless separately approved; existing source lacks that node.
7. [ ] Run conversation/browser/workflow regressions, commit and stop.

### CP22 — Automation builder and configuration

1. [ ] Map eight trigger and thirteen step types, nested branches and gallery inputs from source.
2. [ ] Write validation/flattening/tree round-trip/clone/toggle/manual-trigger contract tests.
3. [ ] Implement CRUD/gallery/log reads and wire existing automation editor UI.
4. [ ] Test branch preservation, unsupported types, invalid references and cross-account graphs.
5. [ ] Keep runtime execution explicitly unavailable until CP23; run regressions, commit and stop.

### CP23 — Automation runtime and scheduler

1. [ ] Port trigger/matching/branch/wait/runtime behavior tests, including exact/contains/word differences.
2. [ ] Implement all supported actions, persisted wait/resume, logs and bounded event recursion.
3. [ ] Implement source-supported time scheduling semantics and account timezone handling; do not infer cron behavior from comments alone.
4. [ ] Implement safe external webhook action calls with fake endpoints for tests.
5. [ ] Test two workers/restart, tag-trigger loops, manual trigger and source engine precedence.
6. [ ] Run flow/inbox/broadcast/automation integration regressions, commit and stop.

### CP24 — AI configuration and knowledge

1. [ ] Write provider-config secret-redaction/update retention, permission and knowledge ownership tests.
2. [ ] Implement encrypted provider config, knowledge document CRUD/process/chunking and retrieval storage.
3. [ ] Port full-text and embedding behavior with Java provider fakes; preserve dimension/array handling.
4. [ ] Wire AI/Knowledge settings and document processing states/errors.
5. [ ] Test malformed documents, provider timeout, reprocessing/delete cleanup and cross-account retrieval.
6. [ ] Run database/API/browser regressions, commit and stop; no paid calls required for this gate.

### CP25 — AI drafts, playground, auto-reply and usage

1. [ ] Write context/privacy/limits/handoff/pause and usage-recording tests.
2. [ ] Implement OpenAI/Anthropic adapters, prompts/context and draft/playground endpoints.
3. [ ] Implement auto-reply integration in the existing event precedence with deduplication and limits.
4. [ ] Wire existing agent/playground/draft controls and usage views.
5. [ ] Test failures, missing key, paused conversation, duplicate event and no unintended customer send for drafts.
6. [ ] Run deterministic fake-provider regressions; optional explicitly recorded paid smoke test, commit and stop.

### CP26 — Notifications, presence and final dashboard integration

1. [ ] Write assignment-notification, read/count/presence expiry and aggregate consistency tests.
2. [ ] Implement list/read/count and online/away endpoints; preserve source accepted states.
3. [ ] Wire polling, badges and inbox indicators; test focus/reconnect/stale session.
4. [ ] Recheck dashboard against seeded contacts/messages/deals/broadcasts across accounts.
5. [ ] Run all enabled UI/role regressions, commit and stop.

### CP27 — Public API and signed outbound webhooks

1. [ ] Turn every baseline `/api/v1` operation into a contract test with exact required scope.
2. [ ] Implement contacts/conversations/messages/broadcasts/me endpoints and cursor pagination.
3. [ ] Preserve structured template params, actual success statuses and public error envelopes.
4. [ ] Implement webhook endpoint management, one-time secret display, signatures and durable delivery/retry.
5. [ ] Test SSRF/redirect/DNS guards, key revocation/expiry and known foreign UUIDs.
6. [ ] Verify browser cookies cannot substitute for API keys, and keys cannot authorize session endpoints.
7. [ ] Run API/SQL/worker/client regressions, commit and stop.

### CP28 — OpenAPI and interactive API Docs

1. [ ] Reconcile all implemented Java operations against the frozen inventory and intentional differences ledger.
2. [ ] Publish authenticated Java OpenAPI JSON, with schemas/statuses/path parameters and no configured secrets.
3. [ ] Keep the existing API Docs UI, adapting CSRF and specification URL as needed.
4. [ ] Preserve Test API read-only behavior, current-origin guard, key-memory clearing and machine-only restrictions.
5. [ ] Browser-test session GET/write with CSRF, API-key request, expiry, download and navigation reset.
6. [ ] Validate contract and route completeness, run full regression/build, commit and stop.

### CP29 — Optional MCP parity

1. [ ] Review baseline MCP tool names/read-only options/transport requirements and whether user enables this adapter.
2. [ ] If enabled, select a Java-25-compatible official MCP Java SDK and pin/test its supported transport.
3. [ ] Implement equivalent tools as a Java adapter over scoped `/api/v1`; no direct DB or owner credentials.
4. [ ] Test protocol/tool schemas, error mapping, scope denial, read-only mode and URL/credential handling.
5. [ ] Record accepted opt-out as optional if disabled; never silently retain a TypeScript business service.
6. [ ] Run adapter/API regression and stop. Other app modules do not depend on MCP.

## Phase 5 — Production readiness and final acceptance

### CP30 — Operations, deployment and full parity sign-off

1. [ ] Reconcile every source page, operation, table and module feature in MODULE_PARITY; no unexplained missing rows.
2. [ ] Add reproducible scripts/CI for Java verify, frontend type/lint/build and browser tests.
3. [ ] Build optional Java/frontend containers and same-origin proxy; prove uploads/session persistence after restart.
4. [ ] Document SMTP/secrets/HTTPS/readiness/shutdown/job ownership and production migration procedure.
5. [ ] Back up and restore a disposable new-project DB plus uploads; verify old encryption/import policy separately.
6. [ ] Test two users/accounts, role changes, concurrent workers, provider failures and all main user journeys.
7. [ ] Run dependency/license audit and record unresolved risks without unrelated forced upgrades.
8. [ ] Run real test-number send/receive/template checks and optional AI smoke test with explicit limits.
9. [ ] Confirm no TypeScript business backend remains and new app works with old app stopped.
10. [ ] Record full evidence, outstanding provider-dependent checks, final local commit/tag and stop.

Manual gate: user signs in, configures settings, manages contacts/deals, exchanges
messages, runs flows/automations, exercises API Docs and confirms enabled optional
features. Deployment/publishing and old-data import are separate user decisions.

## Per-checkpoint command discipline

Use `backend\mvnw.cmd verify` for Java unit/integration/contract tests, and frontend
typecheck/lint/build plus Playwright for affected browser journeys once configured.
Do not replace real PostgreSQL integration tests with mocks or skip them without
recording a blocked gate. Re-run previous module regressions before calling a gate
stable. Save concise logs in ignored tooling output; checkpoint docs contain results,
not credentials. Do not re-run expensive checks repeatedly without changed code or
new evidence requiring them.
