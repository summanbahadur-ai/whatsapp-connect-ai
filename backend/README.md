# Java backend — checkpoint CP01

Java **25**, Spring Boot **4.1.2**, Maven **3.9.12** (Wrapper **3.3.4**).
Only health probes are public. Business APIs/login/database are not implemented yet.

From a PowerShell terminal in `D:\whatsapp-connect-ai`:

```powershell
# Uses JDK 25 from JAVA_HOME or your .jdks folder; caches stay on D:.
.\scripts\maven.ps1 --version
.\scripts\maven.ps1 verify
.\scripts\maven.ps1 spring-boot:run -Dspring-boot.run.profiles=local
```

If PowerShell treats the `-D` argument as a parameter, pass it as a quoted string.
No environment-file loading or database setup is needed at CP01.

Open <http://localhost:8081/actuator/health>. Expect HTTP 200 and `status: UP`.
Liveness/readiness are `/actuator/health/liveness` and `/actuator/health/readiness`.
Probe group names may be included; component names and details are never shown.

Open <http://localhost:8081/api/account>. Expect HTTP 401 and
`{"error":"unauthorized"}`. This is a deliberate security gate, not a migrated API.
There is no login page or usable development username/password at this checkpoint.

`FoundationIT` starts a real HTTP server on a random port. Maven Failsafe runs it
during `verify`; `mvn test` alone does not run the integration tests. Reports are
under `backend/target/failsafe-reports/`. Avoid `-DskipTests` when verifying.

The helper temporarily selects JDK 25, restores shell environment settings when
finished, and stores Maven downloads/repository/temp under the new project's
`.cache/`. It does not change Windows JAVA_HOME or the original project.

VS Code: open the whole `D:\whatsapp-connect-ai` folder, install recommended Java
and Spring extension packs, and let Java import `backend/pom.xml`. This machine's
JDK settings are local/ignored; extension recommendations are shared in Git.

Stop the backend with **Ctrl+C**. New database setup is CP02 and requires the next
user review/continue. Do not run the reference SQL snapshots as migrations.
