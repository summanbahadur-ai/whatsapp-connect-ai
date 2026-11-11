package com.whatsappconnect.database;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTransactionsIT {
    static PostgresFixture fixture;
    static AnnotationConfigApplicationContext context;
    static AccountTransactions transactions;

    @BeforeAll static void prepare() throws Exception {
        fixture=new PostgresFixture();
        context=new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("database","test");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fixture",Map.ofEntries(
                Map.entry("wc.database.url",fixture.url),
                Map.entry("wc.database.app.username","wc_app"), Map.entry("wc.database.app.password",fixture.password),
                Map.entry("wc.database.auth.username","wc_auth"), Map.entry("wc.database.auth.password",fixture.password),
                Map.entry("wc.database.worker.username","wc_worker"), Map.entry("wc.database.worker.password",fixture.password),
                Map.entry("wc.database.migration.username","wc_owner"), Map.entry("wc.database.migration.password",fixture.password),
                Map.entry("wc.database.app.pool-size","1"))));
                context.register(DatabaseConfiguration.class);context.refresh();
        transactions=context.getBean(AccountTransactions.class);
    }

    @AfterAll static void cleanup() throws Exception { if(context!=null)context.close();if(fixture!=null)fixture.close(); }

    @Test void everyPoolUsesItsIntendedDatabaseRole() {
        for(var entry:Map.of("appDataSource","wc_app","authDataSource","wc_auth","workerDataSource","wc_worker","migrationDataSource","wc_owner").entrySet()) {
            var jdbc=new JdbcTemplate(context.getBean(entry.getKey(),DataSource.class));
            assertThat(jdbc.queryForObject("SELECT current_user",String.class)).isEqualTo(entry.getValue());
        }
        assertThat(context.getBean(Flyway.class).migrate().migrationsExecuted).isZero();
    }

    @Test void poolReuseAndFailureCannotLeakIdentity() throws Exception {
        UUID a=fixture.user(UUID.randomUUID()+"@a.invalid"),b=fixture.user(UUID.randomUUID()+"@b.invalid");
        int first=transactions.withAccount(a,AccountRole.VIEWER,scope->{
            assertThat(scope.context().accountId()).isEqualTo(uncheckedAccount(a));
            assertThat(scope.jdbc().queryForObject("SELECT app.current_user_id()",UUID.class)).isEqualTo(a);
            return scope.jdbc().queryForObject("SELECT pg_backend_pid()",Integer.class);
        });
        assertThatThrownBy(()->transactions.withAccount(b,AccountRole.VIEWER,scope->{throw new IllegalStateException("forced rollback");})).isInstanceOf(IllegalStateException.class);
        int second=transactions.withAccount(b,AccountRole.VIEWER,scope->{
            assertThat(scope.jdbc().queryForObject("SELECT app.current_user_id()",UUID.class)).isEqualTo(b);
            return scope.jdbc().queryForObject("SELECT pg_backend_pid()",Integer.class);
        });
        assertThat(second).isEqualTo(first);
        var jdbc=new JdbcTemplate(context.getBean("appDataSource",DataSource.class));
        assertThat(jdbc.queryForObject("SELECT app.current_user_id()",UUID.class)).isNull();
    }

    @Test void permissionsAreReloadedAndNestedScopeSwitchesAreRejected() throws Exception {
        UUID user=fixture.user(UUID.randomUUID()+"@role.invalid");
        transactions.withAccount(user,AccountRole.OWNER,scope->null);
        try(var db=fixture.connect("wc_owner");var statement=db.prepareStatement("UPDATE public.profiles SET account_role='viewer' WHERE user_id=?")){statement.setObject(1,user);statement.executeUpdate();}
        assertThatThrownBy(()->transactions.withAccount(user,AccountRole.AGENT,scope->null)).isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(()->transactions.withAccount(UUID.randomUUID(),AccountRole.VIEWER,scope->null)).isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(()->transactions.withAccount(user,AccountRole.VIEWER,scope->transactions.withAccount(user,AccountRole.VIEWER,inner->null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void jdbcSessionsCanBeSavedReadAndRevoked() {
        var source=context.getBean("authDataSource",DataSource.class);
        var jdbcRepository=new JdbcIndexedSessionRepository(new JdbcTemplate(source),new org.springframework.transaction.support.TransactionTemplate(new JdbcTransactionManager(source)));
        jdbcRepository.setTableName("identity.spring_session");
        jdbcRepository.setDefaultMaxInactiveInterval(Duration.ofDays(7));
        @SuppressWarnings({"unchecked","rawtypes"}) org.springframework.session.SessionRepository<org.springframework.session.Session> repository=(org.springframework.session.SessionRepository)jdbcRepository;
        var session=repository.createSession();session.setAttribute("fixture","roundtrip");repository.save(session);
        assertThat(repository.findById(session.getId()).getAttribute("fixture").toString()).isEqualTo("roundtrip");
        repository.deleteById(session.getId());assertThat(repository.findById(session.getId())).isNull();
    }

    @Test void originalDatabaseAndUnexpectedLoginRolesAreRejectedBeforeConnecting() {
        assertThatThrownBy(()->DatabaseConfiguration.validateUrl("jdbc:postgresql://localhost:5432/omni_connect",false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->DatabaseConfiguration.validateUrl(fixture.url,false)).isInstanceOf(IllegalArgumentException.class);
        assertThat(DatabaseConfiguration.validateUrl("jdbc:postgresql://localhost:5432/whatsapp_connect_ai",false)).endsWith("/whatsapp_connect_ai");
    }

    private static UUID uncheckedAccount(UUID user) {
        try{return fixture.account(user);}catch(Exception error){throw new IllegalStateException(error);}
    }
}
