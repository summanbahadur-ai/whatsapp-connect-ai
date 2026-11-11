package com.whatsappconnect;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIT {
    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private HttpResponse<String> request(String method, String path, String accept) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", accept)
                .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthIsPublicAndDoesNotExposeDetailsOrCreateSession() throws Exception {
        var response = request("GET", "/actuator/health", "application/json");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"")
                .doesNotContain("components", "details", "diskSpace", "jdbc", "password");
        assertThat(response.headers().firstValue("set-cookie")).isEmpty();
    }

    @Test
    void livenessAndReadinessArePublicAndMinimal() throws Exception {
        for (String probe : new String[]{"liveness", "readiness"}) {
            var response = request("GET", "/actuator/health/" + probe, "application/json");
            assertThat(response.statusCode()).as(probe).isEqualTo(200);
            assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
        }
    }

    @Test
    void anonymousApiReturnsJson401WithoutBasicChallenge() throws Exception {
        var response = request("GET", "/api/account", "application/json");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
        assertThat(response.body()).isEqualTo("{\"error\":\"unauthorized\"}");
    }

    @Test
    void browserRequestsDoNotRedirectToGeneratedLogin() throws Exception {
        var response = request("GET", "/api/account", "text/html");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    @Test
    void defaultLoginPageIsNotEnabled() throws Exception {
        assertThat(request("GET", "/login", "text/html").statusCode()).isEqualTo(401);
    }

    @Test
    void sensitiveActuatorAndUnknownPathsAreNotPublic() throws Exception {
        for (String path : new String[]{"/actuator/env", "/actuator/beans", "/actuator/configprops",
                "/actuator/loggers", "/actuator/shutdown", "/actuator", "/unknown"}) {
            var response = request("GET", path, "application/json");
            assertThat(response.statusCode()).as(path).isEqualTo(401);
            assertThat(response.body()).isEqualTo("{\"error\":\"unauthorized\"}");
        }
    }

    @Test
    void unsafeRequestsWithoutCsrfRemainForbidden() throws Exception {
        var response = request("POST", "/api/account", "application/json");
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).isEqualTo("{\"error\":\"forbidden\"}");
    }

    @Test
    void noGeneratedDevelopmentUserIsInstalled() {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
