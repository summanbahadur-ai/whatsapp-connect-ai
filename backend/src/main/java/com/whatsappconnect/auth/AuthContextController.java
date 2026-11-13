package com.whatsappconnect.auth;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("database")
public class AuthContextController {
    @GetMapping("/api/auth-context")
    public ResponseEntity<SessionService.View> context(jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body((SessionService.View)request.getAttribute("WC_AUTH_VIEW"));
    }
}
