package com.whatsappconnect.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/** Owns only a random database inside the isolated test cluster on port 55432. */
final class PostgresFixture implements AutoCloseable {
    final String name = "whatsapp_connect_ai_test_" + UUID.randomUUID().toString().replace("-", "");
    final String password = UUID.randomUUID().toString();
    final String url = "jdbc:postgresql://127.0.0.1:55432/" + name;
    private final String adminPassword = System.getenv("CP02_TEST_ADMIN_PASSWORD");

    PostgresFixture() throws SQLException {
        if (adminPassword == null || adminPassword.isBlank())
            throw new IllegalStateException("Use scripts/maven.ps1 verify with the isolated PostgreSQL test fixture configured.");
        try (var db = admin(); var statement = db.createStatement()) {
            for (String role : new String[]{"wc_owner", "wc_auth", "wc_app", "wc_worker"}) {
                statement.execute("DO $$ BEGIN IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='" + role + "') THEN CREATE ROLE " + role + "; END IF; END $$");
                statement.execute("ALTER ROLE " + role + " WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION "
                        + (role.equals("wc_worker") ? "BYPASSRLS" : "NOBYPASSRLS") + " PASSWORD '" + password + "'");
            }
            statement.execute("CREATE DATABASE " + name + " OWNER wc_owner");
        }
    }

    private Connection admin() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://127.0.0.1:55432/postgres", "wc_test_admin", adminPassword);
    }

    Connection connect(String role) throws SQLException { return DriverManager.getConnection(url, role, password); }

    UUID user(String email) throws SQLException {
        UUID id = UUID.randomUUID();
        try (var db = connect("wc_auth"); var insert = db.prepareStatement("INSERT INTO identity.users(id,name,email) VALUES(?,?,?)")) {
            insert.setObject(1, id); insert.setString(2, "Fixture user"); insert.setString(3, email); insert.executeUpdate();
        }
        return id;
    }

    UUID account(UUID user) throws SQLException {
        try (var db = connect("wc_owner"); var query = db.prepareStatement("SELECT account_id FROM public.profiles WHERE user_id=?")) {
            query.setObject(1, user);
            try (var rows = query.executeQuery()) { rows.next(); return rows.getObject(1, UUID.class); }
        }
    }

    Connection as(UUID user) throws SQLException {
        var db = connect("wc_app");
        db.setAutoCommit(false);
        try (var setting = db.prepareStatement("SELECT set_config('app.user_id',?,true)")) {
            setting.setString(1, user.toString()); setting.execute();
        }
        return db;
    }

    @Override public void close() throws SQLException {
        if (!name.matches("whatsapp_connect_ai_test_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe fixture name");
        try (var db = admin(); var statement = db.createStatement()) {
            statement.execute("DROP DATABASE " + name + " WITH (FORCE)");
        }
    }
}
