package com.whatsappconnect.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseFoundationIT {
    static PostgresFixture fixture;
    static Flyway flyway;
    static int migrations;

    @BeforeAll static void prepare() throws Exception {
        fixture = new PostgresFixture();
        flyway = Flyway.configure().dataSource(fixture.url, "wc_owner", fixture.password)
                .locations("classpath:db/migration").cleanDisabled(true).load();
        migrations = flyway.migrate().migrationsExecuted;
    }

    @AfterAll static void cleanup() throws Exception { if (fixture != null) fixture.close(); }

    @Test void freshInstallHasThreeMigrationsAndSecondRunDoesNothing() {
        assertThat(migrations).isEqualTo(3);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test void signupProvisioningAndCredentialsRollbackTogether() throws Exception {
        UUID id = UUID.randomUUID();
        try (var db = fixture.connect("wc_auth")) {
            db.setAutoCommit(false);
            try (var insert = db.prepareStatement("INSERT INTO identity.users(id,name,email) VALUES(?, 'Rollback user', ?)");
                 var credentials = db.prepareStatement("INSERT INTO identity.password_credentials(user_id,password_hash) VALUES(?, '{bcrypt}test-hash')")) {
                insert.setObject(1,id); insert.setString(2,id+"@test.invalid"); insert.executeUpdate();
                credentials.setObject(1,id); credentials.executeUpdate();
            }
            db.rollback();
        }
        try (var db = fixture.connect("wc_owner"); var query = db.prepareStatement("SELECT (SELECT count(*) FROM identity.users WHERE id=?) + (SELECT count(*) FROM public.profiles WHERE user_id=?) + (SELECT count(*) FROM public.accounts WHERE owner_user_id=?)")) {
            for (int n=1;n<=3;n++) query.setObject(n,id);
            try (var rows=query.executeQuery()) { rows.next(); assertThat(rows.getLong(1)).isZero(); }
        }
    }

    @Test void successfulIdentityInsertCreatesOneOwnerProfileAndAccount() throws Exception {
        UUID id=fixture.user(UUID.randomUUID()+"@provision.invalid");
        assertThat(fixture.account(id)).isNotNull();
        try(var db=fixture.connect("wc_owner");var query=db.prepareStatement("SELECT count(*) FROM public.profiles WHERE user_id=? AND account_role='owner'")) {
            query.setObject(1,id);try(var rows=query.executeQuery()){rows.next();assertThat(rows.getLong(1)).isEqualTo(1);}
        }
    }

    @Test void knownForeignContactCannotBeReadChangedOrDeleted() throws Exception {
        UUID userA=fixture.user(UUID.randomUUID()+"@a.invalid"), userB=fixture.user(UUID.randomUUID()+"@b.invalid");
        UUID accountA=fixture.account(userA), id=UUID.randomUUID();
        try (var db=fixture.as(userA); var insert=db.prepareStatement("INSERT INTO public.contacts(id,user_id,account_id,name,phone) VALUES(?,?,?,'Test contact',?)")) {
            insert.setObject(1,id); insert.setObject(2,userA); insert.setObject(3,accountA); insert.setString(4,id.toString()); insert.executeUpdate(); db.commit();
        }
        try (var db=fixture.as(userB); var query=db.prepareStatement("SELECT count(*) FROM public.contacts WHERE id=?");
             var update=db.prepareStatement("UPDATE public.contacts SET name='forged' WHERE id=?"); var delete=db.prepareStatement("DELETE FROM public.contacts WHERE id=?")) {
            query.setObject(1,id); try(var rows=query.executeQuery()){rows.next(); assertThat(rows.getLong(1)).isZero();}
            update.setObject(1,id); assertThat(update.executeUpdate()).isZero();
            delete.setObject(1,id); assertThat(delete.executeUpdate()).isZero(); db.rollback();
        }
        try(var db=fixture.as(userA); var query=db.prepareStatement("SELECT name FROM public.contacts WHERE id=?")) {
            query.setObject(1,id); try(var rows=query.executeQuery()){assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("Test contact");} db.rollback();
        }
    }

    @Test void forgedAccountInsertAndViewerWriteAreDeniedByPostgres() throws Exception {
        UUID a=fixture.user(UUID.randomUUID()+"@a.invalid"), b=fixture.user(UUID.randomUUID()+"@b.invalid");
        try(var db=fixture.as(a)) {
            assertThatThrownBy(()->insertContact(db,a,fixture.account(b))).isInstanceOf(SQLException.class)
                    .satisfies(error->assertThat(((SQLException)error).getSQLState()).isEqualTo("42501")); db.rollback();
        }
        try(var db=fixture.connect("wc_owner");var statement=db.prepareStatement("UPDATE public.profiles SET account_role='viewer' WHERE user_id=?")) {
            statement.setObject(1,a);statement.executeUpdate();
        }
        try(var db=fixture.as(a)) {
            assertThatThrownBy(()->insertContact(db,a,fixture.account(a))).isInstanceOf(SQLException.class); db.rollback();
        }
    }

    @Test void identityNeverSurvivesCommitOrRollbackOnSameConnection() throws Exception {
        UUID user=fixture.user(UUID.randomUUID()+"@context.invalid");
        try(var db=fixture.as(user)) {
            db.commit();
            try(var statement=db.createStatement();var rows=statement.executeQuery("SELECT app.current_user_id()")){rows.next();assertThat(rows.getObject(1)).isNull();}
            try(var setting=db.prepareStatement("SELECT set_config('app.user_id',?,true)")){setting.setString(1,user.toString());setting.execute();}
            db.rollback();
            try(var statement=db.createStatement();var rows=statement.executeQuery("SELECT app.current_user_id()")){rows.next();assertThat(rows.getObject(1)).isNull();}
        }
    }

    @Test void authAndApplicationRolesCannotReadEachOthersPrivateTables() throws Exception {
        try(var db=fixture.connect("wc_auth");var statement=db.createStatement()) {
            assertThatThrownBy(()->statement.executeQuery("SELECT * FROM public.contacts")).isInstanceOf(SQLException.class);
        }
        try(var db=fixture.connect("wc_app");var statement=db.createStatement()) {
            assertThatThrownBy(()->statement.executeQuery("SELECT * FROM identity.password_credentials")).isInstanceOf(SQLException.class);
        }
    }

    @Test void javaSessionAndSingleUseTokenTablesExist() throws Exception {
        try(var db=fixture.connect("wc_auth");var statement=db.createStatement();var rows=statement.executeQuery("SELECT to_regclass('identity.spring_session'),to_regclass('identity.spring_session_attributes'),to_regclass('identity.verification_tokens')")) {
            rows.next(); for(int n=1;n<=3;n++) assertThat(rows.getObject(n)).isNotNull();
        }
    }

    @Test void identityEmailIsCaseInsensitiveAndUpdatesSynchronizeProfile() throws Exception {
        String email=UUID.randomUUID()+"@identity.invalid";
        UUID id=fixture.user(email);
        assertThatThrownBy(()->fixture.user(email.toUpperCase(java.util.Locale.ROOT))).isInstanceOf(SQLException.class);
        try(var db=fixture.connect("wc_auth");var update=db.prepareStatement("UPDATE identity.users SET name='Updated name',email=? WHERE id=?")) {
            update.setString(1,"changed-"+email);update.setObject(2,id);update.executeUpdate();
        }
        try(var db=fixture.connect("wc_owner");var query=db.prepareStatement("SELECT full_name,email FROM public.profiles WHERE user_id=?")) {
            query.setObject(1,id);try(var rows=query.executeQuery()){
                assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("Updated name");assertThat(rows.getString(2)).isEqualTo("changed-"+email);
            }
        }
    }

    @Test void applicationCannotPromoteItsOwnMembership() throws Exception {
        UUID id=fixture.user(UUID.randomUUID()+"@membership.invalid");
        try(var db=fixture.as(id);var update=db.prepareStatement("UPDATE public.profiles SET account_role='admin' WHERE user_id=?")) {
            update.setObject(1,id);
            assertThatThrownBy(update::executeUpdate).isInstanceOf(SQLException.class);
            db.rollback();
        }
    }
    private static void insertContact(Connection db, UUID user, UUID account) throws SQLException {
        try(var insert=db.prepareStatement("INSERT INTO public.contacts(user_id,account_id,name,phone) VALUES(?,?,'Test',?)")) {
            insert.setObject(1,user);insert.setObject(2,account);insert.setString(3,UUID.randomUUID().toString());insert.executeUpdate();
        }
    }
}
