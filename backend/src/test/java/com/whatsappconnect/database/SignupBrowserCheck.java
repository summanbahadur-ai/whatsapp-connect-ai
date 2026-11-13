package com.whatsappconnect.database;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.whatsappconnect.WhatsAppConnectApplication;
import static org.assertj.core.api.Assertions.assertThat;

/** Explicit browser gate: run verify -Dit.test=SignupBrowserCheck. Owns no user data. */
class SignupBrowserCheck {
    @Test void browserSignupPersistsOnlyInDisposableDatabase() throws Exception {
        try(var fixture=new PostgresFixture()) {
            var settings=new HashMap<String,Object>();
            settings.put("wc.database.url",fixture.url);settings.put("server.port",0);
            settings.put("wc.auth.allowed-origins","http://127.0.0.1:3012");
            for(String purpose:new String[]{"migration","auth","app","worker"}) {
                settings.put("wc.database."+purpose+".username",purpose.equals("migration")?"wc_owner":"wc_"+purpose);
                settings.put("wc.database."+purpose+".password",fixture.password);
            }
            try(var app=new SpringApplicationBuilder(WhatsAppConnectApplication.class).profiles("database","test")
                    .initializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("browserFixture",settings))).run()) {
                int port=((WebServerApplicationContext)app).getWebServer().getPort();
                String email="browser-"+UUID.randomUUID()+"@signup.invalid";
                Path project=Path.of("..").toAbsolutePath().normalize();
                Path log=project.resolve(".cache/cp04-signup-browser.log");
                var processBuilder=new ProcessBuilder("cmd.exe","/d","/c","npm.cmd","run","test:signup")
                    .directory(project.resolve("frontend").toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
                processBuilder.environment().remove("CP02_TEST_ADMIN_PASSWORD");
                processBuilder.environment().put("SIGNUP_TEST_JAVA_ORIGIN","http://127.0.0.1:"+port);
                processBuilder.environment().put("SIGNUP_TEST_EMAIL",email);
                var process=processBuilder.start();
                try {
                    assertThat(process.waitFor(180,TimeUnit.SECONDS)).as("Browser gate timeout; see "+log).isTrue();
                    assertThat(process.exitValue()).as("Browser gate; see "+log).isZero();
                } finally { if(process.isAlive())process.destroyForcibly(); }
                var jdbc=new JdbcTemplate(app.getBean("migrationDataSource",DataSource.class));
                for(String table:new String[]{"identity.users","identity.password_credentials","public.accounts","public.profiles"})
                    assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class)).as(table).isEqualTo(1);
                UUID user=jdbc.queryForObject("SELECT id FROM identity.users WHERE email=?",UUID.class,email);
                assertThat(jdbc.queryForObject("SELECT account_role FROM public.profiles WHERE user_id=?",String.class,user)).isEqualTo("owner");
                String hash=jdbc.queryForObject("SELECT password_hash FROM identity.password_credentials WHERE user_id=?",String.class,user);
                assertThat(app.getBean(PasswordEncoder.class).matches("Browser-only-password!24",hash)).isTrue();
            }
        }
    }
}
