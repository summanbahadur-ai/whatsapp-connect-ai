# WhatsApp Connect AI

Separate project specification for a Java 25 / Spring Boot backend, the existing
React / TypeScript / Next.js interface, and PostgreSQL.

**Status: CP02 accepted. Java/PostgreSQL foundation is verified; local database setup and startup were confirmed by the user. CP03 frontend migration is next.**

Read these in order:

1. [Complete specification](docs/SPECIFICATION.md)
2. [Architecture and project structure](docs/ARCHITECTURE.md)
3. [Database and security design](docs/DATABASE_AND_SECURITY.md)
4. [Small migration steps and test gates](docs/MIGRATION_CHECKPOINTS.md)
5. [Feature and endpoint parity inventory](docs/MODULE_PARITY.md)
6. [Tools, IDE and local execution](docs/SETUP_AND_IDE.md)
7. [Current status and resume instructions](docs/STATUS.md)

Reference contracts and source SQL are under `docs/reference/`. Reference SQL is
not an executable migration set for this new application. No credentials, user
data, uploaded files, installed dependencies or generated builds are copied.

The first implementation phase is the project foundation. The second is signup,
login and the authenticated dashboard shell. Settings follow; business modules
are migrated individually. Stop at every checkpoint for tests and user review.

The source is the MIT-licensed Omni Connect continuation of ArnasDon/wacrm.
Retain the original license and copyright when copying the frontend or other code.

## Run the CP01 backend

From PowerShell in this folder:

```powershell
.\scripts\maven.ps1 verify
.\scripts\maven.ps1 spring-boot:run
```

Health: <http://localhost:8081/actuator/health> (200, UP). Protected API:
<http://localhost:8081/api/account> (401, expected). No login/UI exists yet.
See [backend instructions](backend/README.md) and [CP01 evidence](docs/checkpoints/CP01.md).
For database-enabled startup, follow [database/README.md](database/README.md). Stop for CP02 manual testing and review before CP03.
