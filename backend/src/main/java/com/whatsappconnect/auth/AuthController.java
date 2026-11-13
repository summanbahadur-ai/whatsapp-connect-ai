package com.whatsappconnect.auth;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("database")
@RequestMapping("/api/auth")
public class AuthController {
    private final SignupService signup;
    private final SessionService sessions;
    public AuthController(SignupService signup,SessionService sessions){this.signup=signup;this.sessions=sessions;}
    @PostMapping(value="/sign-in/email",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionService.View> signin(@Valid @RequestBody SigninRequest input,
            jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response) {
        var view=sessions.authenticate(input);
        var previous=request.getSession(false);if(previous!=null)previous.invalidate();
        var session=request.getSession(true);session.setAttribute("WC_USER_ID",view.user().id().toString());
        session.setAttribute("WC_AUTH_AT",System.currentTimeMillis());
        session.setAttribute(org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,view.user().id().toString());
        var csrfCookies=new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        csrfCookies.setCookieName("WC_CSRF");csrfCookies.setCookiePath("/");
        csrfCookies.setCookieCustomizer(builder->builder.httpOnly(true).sameSite("Lax"));
        csrfCookies.saveToken(null,request,response);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view);
    }
    @GetMapping("/get-session")
    public ResponseEntity<SessionService.View> session(jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body((SessionService.View)request.getAttribute("WC_AUTH_VIEW"));
    }
    @PostMapping("/sign-out")
    public ResponseEntity<Map<String,Boolean>> signout(jakarta.servlet.http.HttpServletRequest request) {
        var session=request.getSession(false);if(session!=null)session.invalidate();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("success",true));
    }
    @GetMapping("/csrf")
    public ResponseEntity<Map<String,String>> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @PostMapping(value="/sign-up/email",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,UserResponse>> signup(@Valid @RequestBody SignupRequest input) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
            .body(Map.of("user",signup.signup(input)));
    }
}
