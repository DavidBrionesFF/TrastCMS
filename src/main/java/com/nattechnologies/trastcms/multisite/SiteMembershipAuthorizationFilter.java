package com.nattechnologies.trastcms.multisite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Converts the authenticated global identity into the role granted for the
 * currently resolved site while the request is processed.
 *
 * <p>The session keeps a single identity, but authorization is tenant scoped:
 * the same email can be ADMIN in Academy and CUSTOMER in Primary Site. The
 * original Authentication is restored after the downstream security chain so
 * tenant-specific authorities are never persisted into the HTTP session.</p>
 */
@Component
public class SiteMembershipAuthorizationFilter extends OncePerRequestFilter {
    private final SiteAccessService access;
    private final ObjectMapper objectMapper;

    public SiteMembershipAuthorizationFilter(SiteAccessService access, ObjectMapper objectMapper) {
        this.access = access;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!requiresSiteMembership(path)) {
            chain.doFilter(request, response);
            return;
        }

        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        if (original == null || !original.isAuthenticated() || "anonymousUser".equals(original.getName())) {
            chain.doFilter(request, response);
            return;
        }

        UserRole role = access.role(original.getName(), SiteContext.siteId()).orElse(null);
        if (role == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "status", 403,
                    "title", "Acceso al sitio denegado",
                    "message", "Su cuenta no pertenece al sitio seleccionado",
                    "site", SiteContext.siteKey()));
            return;
        }

        UsernamePasswordAuthenticationToken siteAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                original.getPrincipal(),
                original.getCredentials(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        siteAuthentication.setDetails(original.getDetails());

        SecurityContextHolder.getContext().setAuthentication(siteAuthentication);
        try {
            // SecurityConfig remains the single source of truth for endpoint role
            // requirements; it now sees the role of the current site.
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(original);
        }
    }

    private boolean requiresSiteMembership(String path) {
        // The switcher endpoint must remain reachable when a stored site key is
        // stale or a membership was revoked; it only returns the caller's own
        // memberships and lets the UI recover to a valid tenant.
        if (path.equals("/api/admin/sites/available")) return false;
        return path.startsWith("/api/admin/") || path.startsWith("/api/account/")
                || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }
}
