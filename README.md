# WhatsApp Connect AI

Java 25 / Spring Boot backend, React 19 / TypeScript / Next.js frontend, PostgreSQL.

**CP03 is accepted and merged into local main at a8bde03.**
**CP04 signup and CP05 login/sessions are accepted. Integration into main is approved.**
User approved committing CP04/CP05, merging into main and pushing the integrated history on13November2026.
## Run locally

Open D:\whatsapp-connect-ai in VS Code. In one terminal:

```powershell
.\scripts\start-backend.ps1 -Database
```

In a second terminal:

```powershell
.\scripts\start-frontend.ps1
```

Open http://127.0.0.1:3001/login and click **Test Java connection**.
Expect **Java backend connected.** Java health is http://localhost:8081/actuator/health.

Signup and login are available at http://127.0.0.1:3001/login. Login opens the protected navigation/account shell; dashboard metrics and other modules remain pending. See docs/checkpoints/CP05.md.

For first database setup, follow [database/README.md](database/README.md).
Java applies versioned migrations automatically; database/schema.sql is the
complete maintained schema reference, not an upgrade script.

## Documentation

- [Current status and resume instructions](docs/STATUS.md)
- [CP03 evidence, limits and manual review](docs/checkpoints/CP03.md)
- [Frontend structure, configuration and tests](frontend/README.md)
- [Backend run instructions](backend/README.md)
- [Complete specification](docs/SPECIFICATION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Database and security](docs/DATABASE_AND_SECURITY.md)
- [Checkpoint plan](docs/MIGRATION_CHECKPOINTS.md)
- [Feature parity inventory](docs/MODULE_PARITY.md)

Stop after each checkpoint for tests and user review. Always ask before each
future Git commit and push. Private configuration/runtime data are ignored.

The UI derives from the MIT-licensed Omni Connect continuation of ArnasDon/wacrm.
Retain the original license/copyright. Copied shadcn styles retain their license
under frontend/licenses.
