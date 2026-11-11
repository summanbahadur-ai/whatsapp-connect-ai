# WhatsApp Connect AI

Separate project specification for a Java 25 / Spring Boot backend, the existing
React / TypeScript / Next.js interface, and PostgreSQL.

**Status: specification and directory structure only. No runnable application has
been implemented here yet.** The working Omni Connect application is unchanged.

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
