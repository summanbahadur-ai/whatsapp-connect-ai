package com.whatsappconnect.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** Java owns auth; every unfinished business API stays denied. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain foundationSecurity(HttpSecurity http,Environment environment, org.springframework.beans.factory.ObjectProvider<com.whatsappconnect.auth.SessionService> sessions) throws Exception {
        boolean database=environment.matchesProfiles("database");
        if(database) {
            var cookies=new CookieCsrfTokenRepository();
            cookies.setCookieName("WC_CSRF");cookies.setCookiePath("/");
            cookies.setCookieCustomizer(builder->builder.httpOnly(true).sameSite("Lax"));
            http.csrf(csrf->csrf.csrfTokenRepository(cookies));
            Set<String> origins=Arrays.stream(environment.getProperty("wc.auth.allowed-origins",
                "http://localhost:3001,http://127.0.0.1:3001").split(","))
                .map(String::strip).collect(Collectors.toUnmodifiableSet());
            http.addFilterBefore(new SignupOriginFilter(origins),CsrfFilter.class);
            http.addFilterBefore(new SessionIdentityFilter(sessions.getObject()),org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        }
        return http
                .authorizeHttpRequests(authorize -> {
                        if(database) {
                            authorize.requestMatchers(HttpMethod.GET,"/api/auth/csrf").permitAll();
                            authorize.requestMatchers(HttpMethod.POST,"/api/auth/sign-up/email","/api/auth/sign-in/email").permitAll();
                            authorize.requestMatchers(HttpMethod.GET,"/api/auth/get-session","/api/auth-context").authenticated();
                            authorize.requestMatchers(HttpMethod.POST,"/api/auth/sign-out").authenticated();
                        }
                        authorize
                        .requestMatchers(HttpMethod.GET, "/actuator/health",
                                "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .anyRequest().denyAll(); })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // Keep Spring's CSRF protection for unsafe requests.
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "unauthorized"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "forbidden")))
                .build();
    }

    private static void writeError(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
