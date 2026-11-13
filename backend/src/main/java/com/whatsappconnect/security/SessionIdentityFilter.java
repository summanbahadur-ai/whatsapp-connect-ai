package com.whatsappconnect.security;
import java.io.IOException;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.whatsappconnect.auth.SessionService;
import com.whatsappconnect.database.AccountAccessDeniedException;

/** Session contains only an ID; current membership/permissions are reloaded on each request. */
final class SessionIdentityFilter extends OncePerRequestFilter {
    private final SessionService sessions;
    SessionIdentityFilter(SessionService sessions){this.sessions=sessions;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var session=request.getSession(false);
        if(session!=null && session.getAttribute("WC_USER_ID") instanceof String id) {
            try {
                Object issued=session.getAttribute("WC_AUTH_AT");
                if(!(issued instanceof Long at)||at>System.currentTimeMillis()||System.currentTimeMillis()-at>=604800000L)
                    throw new SessionService.InvalidCredentials();
                var view=sessions.view(UUID.fromString(id));request.setAttribute("WC_AUTH_VIEW",view);
                var context=SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(id,null,List.of()));
                SecurityContextHolder.setContext(context);
            } catch(SessionService.InvalidCredentials|AccountAccessDeniedException|IllegalArgumentException e) {
                session.invalidate();SecurityContextHolder.clearContext();
            } catch(org.springframework.dao.DataAccessException|org.springframework.transaction.TransactionException e) {
                response.setStatus(503);response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
                response.getWriter().write("{\"error\":\"Authentication is temporarily unavailable.\"}");return;
            }
        }
        chain.doFilter(request,response);
    }
}
