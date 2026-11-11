# Tools, IDE and running both projects

This is an installation/run plan, not a record that tools were installed. Actual
commands must be checked at CP01 before generating the application.

## Can we work here?

Yes. Codex can work on Java and frontend files through the terminal once this
folder is opened as the workspace and JDK 25 is configured. An IDE is optional.
IntelliJ IDEA is a suitable Java editor; VS Code with Java support is another
option. Your current PyCharm can remain useful for frontend files/terminals, but
do not assume it supplies the Java/Spring support of a Java-focused IDE.

Keep the old and new projects in separate windows/workspaces. For the new project,
open `D:\whatsapp-connect-ai`; import `backend/pom.xml` as a Maven project once it
exists. Set both project SDK and Maven runner JDK to **25**. Preview features off.

## CP01 installation checklist

1. Inspect current tools with `java -version`, `javac -version`, `node --version`,
   `npm --version`, and `git --version` (or the installed Git executable path).
2. Install/configure a JDK 25 if absent. Do not merely change the project label.
3. Check JAVA_HOME points to that JDK and the terminal/Maven runner use it.
4. Keep Node/npm compatible with the existing frontend's pinned dependencies.
5. Confirm PostgreSQL 18 is running. Create the new database only at CP03.
6. Generate a pinned Maven Wrapper (proposed Maven 3.9.x); commit wrapper files.
   Backend dependencies must resolve and compile under Java 25.
7. For real PostgreSQL tests, choose Testcontainers with Docker available, or an
   explicitly isolated local PostgreSQL test database. Do not silently skip tests
   if Docker is absent. Container deployment is optional.
8. Verify ports 3001/8081 are free; retain old application on 3000 if comparing UI.
9. Stop and record tool versions and any unmet prerequisite before proceeding.

## Future local run commands

These commands become usable only after the foundation checkpoint creates the
wrapper, package files and application code.

Backend terminal:

```powershell
cd D:\whatsapp-connect-ai\backend
# Supply private WC_* environment values locally before starting.
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Frontend terminal:

```powershell
cd D:\whatsapp-connect-ai\frontend
npm ci
npm run dev -- --port 3001
```

Browser: `http://localhost:3001`. Java health: `http://localhost:8081/actuator/health`.
No old app, Supabase service or TypeScript job runner is needed by the new backend.

Use new-project private environment files or IDE run settings, ignored by Git.
Safe template variables (not real credentials):

```text
WC_APP_DB_URL=jdbc:postgresql://localhost:5432/whatsapp_connect_ai
WC_APP_DB_USERNAME=wc_app
WC_APP_DB_PASSWORD=<private>
WC_AUTH_DB_USERNAME=wc_auth
WC_AUTH_DB_PASSWORD=<private>
WC_WORKER_DB_USERNAME=wc_worker
WC_WORKER_DB_PASSWORD=<private>
WC_MIGRATION_DB_USERNAME=wc_owner
WC_MIGRATION_DB_PASSWORD=<private>
WC_PUBLIC_URL=http://localhost:3001
WC_META_APP_SECRET=<only when testing Meta>
WC_ENCRYPTION_KEY=<new private 32-byte key>
WC_STORAGE_ROOT=D:/whatsapp-connect-ai/local-data/uploads
BACKEND_ORIGIN=http://localhost:8081
```

Auth/worker JDBC URLs may be separately configured; explicitly name/bind each
data source in Java. Environment templates do not authorize creating roles or
executing old SQL. SMTP/provider settings are added at their checkpoints.

## Future verification commands

```powershell
cd D:\whatsapp-connect-ai\backend
.\mvnw.cmd verify
cd ..\frontend
npm run typecheck
npm run lint
npm run build
npm run test:browser
```

The `verify` lifecycle must actually run integration tests (Failsafe or an equally
explicit equivalent). Commit scripts/CI configuration for these command names when
implemented. Record exact outputs and exit codes, not just the command list.

## New-project Meta tests

Use a test number and a separate Meta app/configuration where possible. A Meta app
webhook callback may be shared by its assets; changing it can divert events from
the old project. Never silently replace the working old callback. Record which
project/app owns the callback before any change.

Once the Java WhatsApp checkpoint is ready, a tunnel may forward to the frontend
origin on 3001 (which proxies `/api/whatsapp/webhook`) or directly to Java on 8081.
Set the public callback and public media origin consistently, preserve raw bytes
through the proxy, and test both inbound events and outbound replies. Use new
project credentials privately; do not copy old secrets during scaffolding.
