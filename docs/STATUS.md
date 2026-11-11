# Migration status and resume instructions

Updated: 11 November 2026.

## Git approval requirement
ALWAYS ask before each future commit and push. User explicitly approved CP02
commit, merge into main and push on 11 November 2026. This approval covers CP02
integration only. CP03 commit/push needs new approval.

## Accepted stable checkpoint
CP02 PostgreSQL foundation accepted. CP01 main base: 0fa9f92.
User completed pgAdmin setup, Java startup with database profile, health UP,
and confirmed identity=5/public=38 tables. No imported customer data.
Automated verification: scripts/maven.ps1 verify, BUILD SUCCESS, 26 tests,
zero failures/errors/skips. Original database remains separate.

See docs/checkpoints/CP02.md and database/README.md.
SQL changes always require incremental Flyway migrations and updated database/schema.sql.

## Current work
Integrate approved CP02 into main and push; then create
feature/cp03-frontend-java-transport from main. CP03 is authorized:
preserve the existing frontend, remove TypeScript backend dependencies, connect
same-origin API/media transport to Java, test it, and stop for review.
Login/signup remain unavailable until their own checkpoints.
Do not implement CP04 automatically. Do not commit/push CP03 without permission.

## Resume
Read AGENTS.md, this status, the specification/security architecture and CP03 in
docs/MIGRATION_CHECKPOINTS.md. Record actual results and pending manual tests.
Never copy source .env files, credentials, runtime storage or database contents.
