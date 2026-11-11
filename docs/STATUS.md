# Migration status and resume instructions

Updated: 11 November 2026.

## Git approval requirement

User preference: ALWAYS ask for explicit approval before every future Git commit and before every future push. Checkpoint instructions do not grant automatic permission. The user explicitly authorized the current CP01 commit and push on 11 November 2026; that approval does not carry forward to later checkpoints.

## Current stable checkpoint

**CP01 Java foundation implemented and verified; user review pending.**

- Java 25.0.1, Spring Boot 4.1.2, Maven 3.9.12, Wrapper 3.3.4.
- Local repository initialized; branch `feature/cp01-java-foundation`.
- Packaged Java application runs at http://localhost:8081.
- Public health/liveness/readiness; all other paths denied; no default login/user.
- Eight real-HTTP integration tests pass through Maven Failsafe during verify.
- VS Code extension recommendations and ignored machine-local JDK settings prepared.
- No frontend copied, no database provisioned/queried, no authentication module yet.
- No provider calls, customer messages, secret copying or original project changes.
- No remote publishing or deployment performed.

See [CP01 evidence and manual tests](checkpoints/CP01.md).

## Test this checkpoint

1. Open http://localhost:8081/actuator/health: expect HTTP 200 and status UP.
2. Open http://localhost:8081/api/account: expect HTTP 401 and unauthorized JSON.
3. Open D:\whatsapp-connect-ai in VS Code and inspect backend/pom.xml.
4. In its terminal, run `.\scripts\maven.ps1 verify`: expect eight tests, no failures.

There is no frontend/login screen at CP01. That is scheduled later; an unauthorized
API response is expected here. The original application's UI remains separate.

## Next permitted checkpoint

**CP02: separate PostgreSQL foundation**, only after the user accepts CP01 and
says to continue. Do not run CP02 automatically. It will confirm private new-database
credentials and create whatsapp_connect_ai with appropriate roles and migrations.
Never point it at the working omni_connect database or execute reference SQL blindly.

## Resume

Open this directory as the workspace. Read AGENTS.md, this file and
docs/checkpoints/CP01.md, then the specification/security design and CP02 plan.
Keep work within one approved checkpoint. Test, record results, commit and stop.
Record user review honestly; do not mark it accepted before receiving acceptance.

## Reference inventory

The source baseline contains 90 API route files, 151 documented operations,
24 pages, 41 source tables and 122 source test files. CP00 documentation validation
covered links, OpenAPI, handler coverage and private-configuration patterns. These
are inventory facts, not features already migrated to Java.
