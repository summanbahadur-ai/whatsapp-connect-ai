# Project implementation instructions

Read `README.md`, `docs/STATUS.md`, `docs/SPECIFICATION.md`,
`docs/ARCHITECTURE.md`, `docs/DATABASE_AND_SECURITY.md` and the next checkpoint
in `docs/MIGRATION_CHECKPOINTS.md` before implementing.

- Java 25/Spring Boot owns every business API and background job. Keep the existing
  React/TypeScript/Next.js UI. Next is allowed only frontend rendering/transport work.
- Work on one user-approved checkpoint, test it, update its evidence and STATUS,
  then stop for user review. Do not automatically implement the next module.
- Do not modify the original repo or `omni_connect` database. Use separate data,
  roles, runtime storage and provider configuration in this project.
- Never copy secrets, `.env.local`, uploads or customer data from the source.
- CP02 Java/PostgreSQL foundation is accepted; read docs/STATUS.md for current CP03 work.
  Describe migrated modules precisely; preserved/gated UI does not mean its Java API exists.
- Read relevant installed Next.js guides before frontend implementation; preserve
  framework-generated assistant instructions if Next adds them later.

## Git approval requirement

User preference: ALWAYS ask for explicit approval before every future Git commit and before every future push. Checkpoint instructions do not grant automatic permission. The user explicitly authorized the current CP01 commit and push on 11 November 2026; that approval does not carry forward to later checkpoints.

## SQL structure changes
Always add an incremental Flyway migration and regenerate database/schema.sql with
scripts/sync-schema.ps1 whenever SQL structure changes. Commit both together only
after user approval. Never rewrite an accepted/applied migration. Verify the master
snapshot and real PostgreSQL tests. See database/README.md.
