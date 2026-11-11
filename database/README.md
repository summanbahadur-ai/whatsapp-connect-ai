# PostgreSQL setup and schema changes

This project uses a **new database named whatsapp_connect_ai**. Keep the original
omni_connect database for the original application. PostgreSQL 18 is installed
already; no second PostgreSQL installation or Docker is required.

## First local setup: tiny steps

1. Open this project in VS Code: D:\whatsapp-connect-ai.
2. In its PowerShell terminal run:
   ` .\scripts\configure-local-database.ps1 `
   This generates private random credentials in ignored local-data/config. It does
   not connect to PostgreSQL or change any database.
3. Open pgAdmin, connect to your existing local server as its administrator, and
   open Query Tool on the postgres database.
4. Open and execute **database/bootstrap/00_create_roles.sql**.
5. Open and execute **local-data/config/set-role-passwords.sql**. This is private:
   do not commit, send, or copy its passwords into documentation.
6. Open **database/bootstrap/01_create_database.sql**. Enable Query Tool
   auto-commit and execute CREATE DATABASE separately from the remaining GRANT
   statements. CREATE DATABASE cannot run in a transaction. Run it once.
7. Refresh Databases in pgAdmin. You should see whatsapp_connect_ai.
8. Back in the project terminal run:
   ` .\scripts\start-backend.ps1 -Database `
   Java connects with the new roles and Flyway applies V1, V2 and V3 automatically.
   A later start validates migration checksums and applies only new migrations.
9. Visit http://localhost:8081/actuator/health. Expect UP. Inspect the new database:
   identity has Java credentials/session tables; public has 37 domain tables and
   Flyway history. There is no login UI at CP02; HTTP APIs remain protected.

If the generated configuration already exists, keep it. The generator deliberately
refuses to overwrite credentials. If your PostgreSQL port differs from 5432, edit
only WC_DB_URL inside local-data/config/database.json. Its database name must stay
whatsapp_connect_ai. Do not use the old database or its application credentials.

The owner credential is used for migrations only; auth, app and worker use separate
connection pools. Worker bypasses RLS for future background tasks and therefore
requires explicit account predicates in every future worker repository.

## The main SQL file

**database/schema.sql** is the complete current schema, including identity,
domain tables, functions, triggers, RLS, indexes and grants. It is generated from
the numbered migrations. It contains no passwords or old customer data.

For normal setup use the steps above: **do not manually run schema.sql as well**.
The master file supports review and fresh manual installations. It refuses to run
on an already initialized database or on the old database. A manual installation
must run as wc_owner inside a newly bootstrapped database. Because this bypasses
Flyway history, it requires an explicit, reviewed Flyway baseline at version 3
before Java starts. Automatic baseline-on-migrate is disabled. Prefer normal setup.

For every future SQL structure change:

1. Add a new immutable numbered Flyway migration (V4 next after CP02 acceptance).
2. Run ` .\scripts\sync-schema.ps1 `.
3. Run ` .\scripts\sync-schema.ps1 -Check ` and ` .\scripts\maven.ps1 verify `.
4. Include the migration and updated database/schema.sql in the same reviewed change.

Never edit an already applied migration or replay the master file to upgrade an
existing database. Flyway supplies incremental updates. Back up real databases
before deploying later migrations.

## Automated verification

Run ` .\scripts\maven.ps1 verify `. The helper starts a separate, local-only
test PostgreSQL cluster at 127.0.0.1:55432 under .cache/cp02-postgres-test.
It discovers installed PostgreSQL through the Windows registry; alternatively set
WC_PG_BIN to your PostgreSQL bin directory. It does not use the server on 5432.
Tests create randomly named empty databases and drop only those databases.

Tests cover schema/master synchronization, fresh migration, repeat migration,
rollback, account RLS, membership permissions, transaction-local identity,
connection pool reuse, database-role separation and JDBC session persistence.
The original health/security HTTP checks remain included.

Test credentials and logs are ignored under .cache. Tests are sequential; do not
run multiple verification processes at once because the isolated test roles are
shared. To stop the test server after verification, use your PostgreSQL pg_ctl:
` pg_ctl -D "D:\whatsapp-connect-ai\.cache\cp02-postgres-test" -m fast -w stop `.

## Checkpoint boundary

CP02 supplies database infrastructure and empty tables. Authentication endpoints,
password hashing/login flows, frontend, dashboard and WhatsApp behavior migrate
in later checkpoints. A table existing does not mean its Java module is migrated.
