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
- No executable Java/Next scaffold exists at the initial documentation checkpoint.
  Do not claim the documented future file tree or commands are already implemented.
- Read relevant installed Next.js guides before frontend implementation; preserve
  framework-generated assistant instructions if Next adds them later.
