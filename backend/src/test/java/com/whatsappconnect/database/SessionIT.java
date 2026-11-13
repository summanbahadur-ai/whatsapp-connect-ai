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

class SessionIT {
    static PostgresFixture fixture;
    static ConfigurableApplicationContext app;
    static JdbcTemplate owner;
    static String base;
    static final JsonMapper json=JsonMapper.builder().build();
    static final String PASSWORD="Test-only-secure-password!";
    static final String ORIGIN="http://127.0.0.1:3001";
    record Browser(HttpClient client,String token,String header) {}

    @BeforeAll static void start() throws Exception {
        fixture=new PostgresFixture(); boot(); }
    static void boot() throws Exception {
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

    static HttpResponse<String> authPost(Browser b,String path,Object body,boolean csrf) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base+path)).header("Origin",ORIGIN).header("Content-Type","application/json");
        if(csrf)request.header(b.header(),b.token());
        return b.client().send(request.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    static HttpResponse<String> get(Browser b,String path) throws Exception {
        return b.client().send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    static Browser register(String email) throws Exception {
        var b=browser();assertThat(post(b,input(email),b.token(),ORIGIN).statusCode()).isEqualTo(201);return b;
    }
    static HttpResponse<String> login(Browser b,String email,String password) throws Exception {
        return authPost(b,"/api/auth/sign-in/email",Map.of("email",email,"password",password),true);
    }
    @Test void validLoginPersistsSafeSessionAndLogoutRevokesIt() throws Exception {
        String email=email();var b=register(email);
        var login=login(b,"  "+email.toUpperCase(Locale.ROOT)+"  ",PASSWORD);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains(email).doesNotContain(PASSWORD,"password_hash","sessionId","token");
        assertThat(login.headers().allValues("set-cookie").toString()).contains("WC_SESSION=","HttpOnly","SameSite=Lax");
        assertThat(owner.queryForObject("SELECT count(*) FROM identity.spring_session",Long.class)).isGreaterThan(0);
        var session=get(b,"/api/auth/get-session");assertThat(session.statusCode()).isEqualTo(200);
        assertThat(session.body()).contains(email).doesNotContain(PASSWORD,"{bcrypt}");
        var context=get(b,"/api/auth-context");assertThat(context.statusCode()).isEqualTo(200);
        assertThat(json.readTree(context.body()).get("profile").get("account_role").asString()).isEqualTo("owner");
        var seed=get(b,"/api/auth/csrf");var token=json.readTree(seed.body());
        var fresh=new Browser(b.client(),token.get("token").asString(),token.get("headerName").asString());
        assertThat(authPost(fresh,"/api/auth/sign-out",Map.of(),false).statusCode()).isEqualTo(403);
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(200);
        assertThat(authPost(fresh,"/api/auth/sign-out",Map.of(),true).statusCode()).isEqualTo(200);
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(401);
    }
    @Test void invalidCredentialsHaveSameGenericErrorAndNoSession() throws Exception {
        String email=email();var b=register(email);
        var wrong=login(b,email,"incorrect-password");var absent=login(b,email(),"incorrect-password");
        assertThat(wrong.statusCode()).isEqualTo(401);assertThat(absent.statusCode()).isEqualTo(401);
        assertThat(wrong.body()).isEqualTo(absent.body()).doesNotContain(email,"incorrect-password");
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(401);
        assertThat(authPost(b,"/api/auth/sign-in/email",Map.of("email",email,"password",PASSWORD),false).statusCode()).isEqualTo(403);
    }
    @Test void forgedExpiredAndRemovedMembershipSessionsAreRejected() throws Exception {
        var forged=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/api/auth/get-session"))
            .header("Cookie","WC_SESSION=ZmFrZS1zZXNzaW9u").GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(forged.statusCode()).isEqualTo(401);
        String email=email();var b=register(email);assertThat(login(b,email,PASSWORD).statusCode()).isEqualTo(200);
        owner.update("UPDATE identity.spring_session SET expiry_time=0,last_access_time=0");
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(401);
        var c=browser();assertThat(login(c,email,PASSWORD).statusCode()).isEqualTo(200);
        owner.update("DELETE FROM public.profiles WHERE user_id=?",userId(email));
        assertThat(get(c,"/api/auth-context").statusCode()).isEqualTo(401);
        assertThat(get(c,"/api/auth/get-session").statusCode()).isEqualTo(401);
    }
    @Test void sessionsSurviveJavaRestartAndMembershipRolesAreReloaded() throws Exception {
        String email=email();var b=register(email);assertThat(login(b,email,PASSWORD).statusCode()).isEqualTo(200);
        app.close();boot();
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(200);
        for(String role:List.of("viewer","agent","admin","owner")) {
            owner.update("UPDATE public.profiles SET account_role=?::public.account_role_enum WHERE user_id=?",role,userId(email));
            var context=get(b,"/api/auth-context");assertThat(context.statusCode()).isEqualTo(200);
            assertThat(json.readTree(context.body()).get("profile").get("account_role").asString()).isEqualTo(role);
            assertThat(get(b,"/api/account").statusCode()).isEqualTo(403);
        }
    }
    @Test void loginReplacesPreexistingSessionAndRejectsReplayedOldCookie() throws Exception {
        String email=email();var b=register(email);
        var repository=app.getBean(org.springframework.session.jdbc.JdbcIndexedSessionRepository.class);
        org.springframework.session.Session old=repository.createSession();
        // No browser identity or role is accepted from this pre-login session.
        old.setAttribute("fixture", "anonymous");
        saveSession(repository,old);
        String cookie=Base64.getEncoder().encodeToString(old.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var jar=(CookieManager)b.client().cookieHandler().orElseThrow();var value=new HttpCookie("WC_SESSION",cookie);
        value.setPath("/");value.setVersion(0);jar.getCookieStore().add(URI.create(base),value);
        assertThat(login(b,email,PASSWORD).statusCode()).isEqualTo(200);
        assertThat(repository.findById(old.getId())).isNull();
        var replay=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/api/auth/get-session"))
            .header("Cookie","WC_SESSION="+cookie).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(replay.statusCode()).isEqualTo(401);
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(200);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    static void saveSession(org.springframework.session.SessionRepository repository,org.springframework.session.Session session){repository.save(session);}
    @Test void absoluteLifetimeExpiresEvenWhenIdleSessionIsFresh() throws Exception {
        String email=email();var b=register(email);assertThat(login(b,email,PASSWORD).statusCode()).isEqualTo(200);
        var repository=app.getBean(org.springframework.session.jdbc.JdbcIndexedSessionRepository.class);
        Map<String,? extends org.springframework.session.Session> sessions=repository.findByPrincipalName(userId(email).toString());
        assertThat(sessions).hasSize(1);
        var stored=sessions.values().iterator().next();
        stored.setAttribute("WC_AUTH_AT",System.currentTimeMillis()-604800001L);saveSession(repository,stored);
        assertThat(get(b,"/api/auth/get-session").statusCode()).isEqualTo(401);
    }
}