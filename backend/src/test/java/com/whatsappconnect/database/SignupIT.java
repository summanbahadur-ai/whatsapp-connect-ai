package com.whatsappconnect.database;

import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.whatsappconnect.WhatsAppConnectApplication;
import static org.assertj.core.api.Assertions.assertThat;

class SignupIT {
    static PostgresFixture fixture;
    static ConfigurableApplicationContext app;
    static JdbcTemplate owner;
    static String base;
    static final JsonMapper json=JsonMapper.builder().build();
    static final String PASSWORD="Test-only-secure-password!";
    static final String ORIGIN="http://127.0.0.1:3001";
    record Browser(HttpClient client,String token,String header) {}

    @BeforeAll static void start() throws Exception {
        fixture=new PostgresFixture();
        var settings=new HashMap<String,Object>();
        settings.put("wc.database.url",fixture.url);settings.put("server.port",0);
        for(String purpose:new String[]{"migration","auth","app","worker"}) {
            settings.put("wc.database."+purpose+".username",purpose.equals("migration")?"wc_owner":"wc_"+purpose);
            settings.put("wc.database."+purpose+".password",fixture.password);
        }
        app=new SpringApplicationBuilder(WhatsAppConnectApplication.class).profiles("database","test")
            .initializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("signupFixture",settings))).run();
        base="http://127.0.0.1:"+((WebServerApplicationContext)app).getWebServer().getPort();
        owner=new JdbcTemplate(app.getBean("migrationDataSource",DataSource.class));
    }
    @AfterAll static void stop() throws Exception {
        if(app!=null)app.close();if(fixture!=null)fixture.close();
    }
    static Browser browser() throws Exception {
        var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        var client=HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        var response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(response.headers().allValues("set-cookie").toString()).contains("HttpOnly","SameSite=Lax").doesNotContain("WC_SESSION","JSESSIONID");
        var body=json.readTree(response.body());
        return new Browser(client,body.get("token").asString(),body.get("headerName").asString());
    }
    static Map<String,String> input(String email) {return Map.of("name","  Signup user  ","email",email,"password",PASSWORD);}
    static HttpResponse<String> post(Browser browser,Object body,String token,String origin) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base+"/api/auth/sign-up/email"))
            .timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        if(token!=null)request.header(browser.header(),token);
        if(origin!=null)request.header("Origin",origin);
        String payload=body instanceof String?(String)body:json.writeValueAsString(body);
        return browser.client().send(request.POST(HttpRequest.BodyPublishers.ofString(payload)).build(),HttpResponse.BodyHandlers.ofString());
    }
    static String email(){return UUID.randomUUID()+"@signup.invalid";}
    static long[] counts(){return new long[]{owner.queryForObject("SELECT count(*) FROM identity.users",Long.class),owner.queryForObject("SELECT count(*) FROM identity.password_credentials",Long.class),owner.queryForObject("SELECT count(*) FROM public.accounts",Long.class),owner.queryForObject("SELECT count(*) FROM public.profiles",Long.class)};}
    static UUID userId(String email){return owner.queryForObject("SELECT id FROM identity.users WHERE email=?",UUID.class,email.toLowerCase(Locale.ROOT));}

    @Test void signupCreatesExactlyOneOwnerAndHashedCredentialWithoutLoggingIn() throws Exception {
        var browser=browser();String email=email();long[] before=counts();
        var response=post(browser,input("  "+email.toUpperCase(Locale.ROOT)+"  "),browser.token(),ORIGIN);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(response.body()).doesNotContain(PASSWORD,"password_hash","password","SQLException");
        var user=json.readTree(response.body()).get("user");
        UUID id=UUID.fromString(user.get("id").asString());
        assertThat(user.get("name").asString()).isEqualTo("Signup user");
        assertThat(user.get("email").asString()).isEqualTo(email);
        assertThat(user.get("emailVerified").asBoolean()).isFalse();
        assertThat(user.get("createdAt").asString()).isNotBlank();
        long[] after=counts();for(int n=0;n<4;n++)assertThat(after[n]).isEqualTo(before[n]+1);
        var profile=owner.queryForMap("SELECT p.account_role,a.default_currency,a.owner_user_id FROM public.profiles p JOIN public.accounts a ON a.id=p.account_id WHERE p.user_id=?",id);
        assertThat(profile.get("account_role").toString()).isEqualTo("owner");
        assertThat(profile.get("default_currency")).isEqualTo("USD");
        assertThat(profile.get("owner_user_id")).isEqualTo(id);
        String hash=owner.queryForObject("SELECT password_hash FROM identity.password_credentials WHERE user_id=?",String.class,id);
        assertThat(hash).startsWith("{bcrypt}$2").doesNotContain(PASSWORD);
        assertThat(app.getBean(PasswordEncoder.class).matches(PASSWORD,hash)).isTrue();
        var account=browser.client().send(HttpRequest.newBuilder(URI.create(base+"/api/account")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(account.statusCode()).isEqualTo(401);
        assertThat(response.headers().allValues("set-cookie").toString()).doesNotContain("WC_SESSION","JSESSIONID");
    }
    @Test void duplicateEmailIncludingDifferentCaseHasNoExtraProvisioning() throws Exception {
        var browser=browser();String email=email();long[] before=counts();
        assertThat(post(browser,input(email),browser.token(),ORIGIN).statusCode()).isEqualTo(201);
        var duplicate=post(browser,input(email.toUpperCase(Locale.ROOT)),browser.token(),ORIGIN);
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("EMAIL_ALREADY_EXISTS").doesNotContain("SQL","duplicate key",PASSWORD);
        long[] after=counts();for(int n=0;n<4;n++)assertThat(after[n]).isEqualTo(before[n]+1);
    }
    @Test void invalidInputsNeverCreateRows() throws Exception {
        var browser=browser();long[] before=counts();
        for(var invalid:List.of(Map.of("name","User","email",email(),"password","short"),
                Map.of("name","User","email",email(),"password","😀".repeat(20)),
                Map.of("name","","email",email(),"password",PASSWORD),
                Map.of("name","User","email","invalid-email","password",PASSWORD),
                Map.of("name","x".repeat(101),"email",email(),"password",PASSWORD))) {
            var response=post(browser,invalid,browser.token(),ORIGIN);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).doesNotContain(PASSWORD,"😀","SQLException","BindingResult");
        }
        assertThat(post(browser,"{bad json",browser.token(),ORIGIN).statusCode()).isEqualTo(400);
        assertThat(counts()).containsExactly(before);
    }
    @Test void missingForgedOrCrossBrowserCsrfAndForeignOrMissingOriginAreRejected() throws Exception {
        var a=browser();var b=browser();long[] before=counts();
        for(String token:new String[]{null,"forged",b.token()})
            assertThat(post(a,input(email()),token,ORIGIN).statusCode()).isEqualTo(403);
        for(String origin:new String[]{null,"http://localhost:3000","https://evil.invalid"})
            assertThat(post(a,input(email()),a.token(),origin).statusCode()).isEqualTo(403);
        assertThat(counts()).containsExactly(before);
    }
    @Test void passwordFailureRollsBackIdentityAccountAndProfile() throws Exception {
        var browser=browser();String email=email();long[] before=counts();
        owner.execute("CREATE FUNCTION identity.reject_fixture_credentials() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF (SELECT email FROM identity.users WHERE id=NEW.user_id)='"+email+"' THEN RAISE EXCEPTION 'fixture credential failure'; END IF; RETURN NEW; END $$");
        owner.execute("CREATE TRIGGER reject_fixture_credentials BEFORE INSERT ON identity.password_credentials FOR EACH ROW EXECUTE FUNCTION identity.reject_fixture_credentials()");
        try {
            var response=post(browser,input(email),browser.token(),ORIGIN);
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).doesNotContain("fixture credential failure","SQLException","INSERT",PASSWORD);
            assertThat(counts()).containsExactly(before);
        } finally {
            owner.execute("DROP TRIGGER reject_fixture_credentials ON identity.password_credentials");
            owner.execute("DROP FUNCTION identity.reject_fixture_credentials()");
        }
    }
    @Test void sameEmailRaceCreatesOnlyOneAccount() throws Exception {
        var a=browser();var b=browser();String email=email();long[] before=counts();
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()) {
            var first=tasks.submit(()->post(a,input(email),a.token(),ORIGIN).statusCode());
            var second=tasks.submit(()->post(b,input(email.toUpperCase(Locale.ROOT)),b.token(),ORIGIN).statusCode());
            assertThat(List.of(first.get(),second.get())).containsExactlyInAnyOrder(201,409);
        }
        long[] after=counts();for(int n=0;n<4;n++)assertThat(after[n]).isEqualTo(before[n]+1);
    }
    @Test void concurrentDistinctUsersHaveSeparateAccountsAndRandomPasswordSalts() throws Exception {
        var a=browser();var b=browser();String one=email(),two=email();
        try(var tasks=Executors.newVirtualThreadPerTaskExecutor()) {
            var first=tasks.submit(()->post(a,input(one),a.token(),ORIGIN).statusCode());
            var second=tasks.submit(()->post(b,input(two),b.token(),ORIGIN).statusCode());
            assertThat(first.get()).isEqualTo(201);assertThat(second.get()).isEqualTo(201);
        }
        assertThat(fixture.account(userId(one))).isNotEqualTo(fixture.account(userId(two)));
        var hashes=owner.queryForList("SELECT password_hash FROM identity.password_credentials WHERE user_id IN(?,?)",String.class,userId(one),userId(two));
        assertThat(hashes).hasSize(2);assertThat(hashes.get(0)).isNotEqualTo(hashes.get(1));
    }

    @Test void postedClaimsCannotSelectAnotherAccountOrVerifyEmail() throws Exception {
        UUID foreign=fixture.user(email());
        UUID foreignAccount=fixture.account(foreign);
        var browser=browser();
        var forged=new HashMap<String,Object>(input(email()));
        forged.put("id",foreign.toString());forged.put("account_id",foreignAccount.toString());
        forged.put("account_role","admin");forged.put("emailVerified",true);
        var response=post(browser,forged,browser.token(),ORIGIN);
        assertThat(response.statusCode()).isEqualTo(201);
        var user=json.readTree(response.body()).get("user");
        UUID created=UUID.fromString(user.get("id").asString());
        assertThat(created).isNotEqualTo(foreign);
        assertThat(fixture.account(created)).isNotEqualTo(foreignAccount);
        assertThat(user.get("emailVerified").asBoolean()).isFalse();
        assertThat(owner.queryForObject("SELECT account_role FROM public.profiles WHERE user_id=?",String.class,created)).isEqualTo("owner");
    }

    @Test void requestDiagnosticsCannotExposeCredentials() {
        var request=new com.whatsappconnect.auth.SignupRequest("User","private@signup.invalid",PASSWORD);
        assertThat(request.toString()).doesNotContain(PASSWORD,"private@signup.invalid","User");
    }
}
