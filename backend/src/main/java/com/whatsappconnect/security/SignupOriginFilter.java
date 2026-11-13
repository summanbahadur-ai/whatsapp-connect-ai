package com.whatsappconnect.security;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

/** Explicit browser-origin check supplements CSRF; proxy headers do not grant trust. */
final class SignupOriginFilter extends OncePerRequestFilter {
    private final Set<String> allowed;
    SignupOriginFilter(Set<String> allowed){this.allowed=Set.copyOf(allowed);}
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())||!Set.of("/api/auth/sign-up/email","/api/auth/sign-in/email","/api/auth/sign-out").contains(request.getServletPath());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String origin=request.getHeader("Origin");
        if(origin==null||!allowed.contains(origin)) {
            response.setStatus(403);response.setContentType("application/json");
            response.setHeader("Cache-Control","no-store");
            response.getWriter().write("{\"error\":\"forbidden\"}");
            return;
        }
        chain.doFilter(request,response);
    }
}
