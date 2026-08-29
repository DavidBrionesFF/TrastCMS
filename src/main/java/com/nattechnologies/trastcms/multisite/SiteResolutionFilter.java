package com.nattechnologies.trastcms.multisite;

import com.nattechnologies.trastcms.domain.site.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SiteResolutionFilter extends OncePerRequestFilter {
    public static final String SITE_HEADER = "X-TrastCMS-Site";

    private final CmsSiteRepository sites;
    private final CmsSiteEndpointRepository endpoints;

    public SiteResolutionFilter(CmsSiteRepository sites, CmsSiteEndpointRepository endpoints) {
        this.sites = sites;
        this.endpoints = endpoints;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            CmsSite site = resolve(request);
            if (site == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":404,\"title\":\"Sitio no encontrado\",\"message\":\"El dominio o puerto no está registrado en TrastCMS\"}");
                return;
            }
            SiteContext.set(new SiteContext.ResolvedSite(site.getId(), site.getSiteKey(), site.getName()));
            response.setHeader("X-TrastCMS-Site", site.getSiteKey());
            chain.doFilter(request, response);
        } finally {
            SiteContext.clear();
        }
    }

    private CmsSite resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        String requestedSite = request.getHeader(SITE_HEADER);
        if (requestedSite != null && !requestedSite.isBlank() && acceptsSiteHeader(path)) {
            CmsSite byKey = sites.findBySiteKeyIgnoreCase(requestedSite.trim())
                    .orElseGet(() -> sites.findById(requestedSite.trim()).orElse(null));
            if (byKey != null && byKey.getStatus() != SiteStatus.ARCHIVED) return byKey;
        }

        String host = forwardedHost(request);
        int port = forwardedPort(request);
        CmsSiteEndpoint endpoint = endpoints.findFirstByHostIgnoreCaseAndPortAndActiveTrue(host, port).orElse(null);
        if (endpoint != null && endpoint.getSite().getStatus() != SiteStatus.ARCHIVED) return endpoint.getSite();

        // A host-only fallback is safe only when that host belongs to one site.
        // This preserves common reverse-proxy setups that omit X-Forwarded-Port
        // without making port-based multisite routing ambiguous.
        var hostEndpoints = endpoints.findAllByHostIgnoreCaseAndActiveTrueOrderByPrimaryEndpointDesc(host);
        CmsSite singleHostSite = null;
        for (CmsSiteEndpoint candidate : hostEndpoints) {
            CmsSite candidateSite = candidate.getSite();
            if (candidateSite.getStatus() == SiteStatus.ARCHIVED) continue;
            if (singleHostSite == null) singleHostSite = candidateSite;
            else if (!singleHostSite.getId().equals(candidateSite.getId())) {
                singleHostSite = null;
                break;
            }
        }
        if (singleHostSite != null) return singleHostSite;

        // Infrastructure probes are instance-level and do not carry tenant data.
        if (path.startsWith("/actuator/")) {
            return sites.findById(SiteContext.DEFAULT_SITE_ID)
                    .orElseGet(() -> sites.findAllByOrderByNameAsc().stream().findFirst().orElse(null));
        }

        // Never leak the default site's content through an unregistered host.
        // Nginx/DNS mistakes should fail closed and be visible immediately.
        return null;
    }

    private boolean acceptsSiteHeader(String path) {
        // Tenant overrides are an administrative SPA concern. Public account,
        // registration, mobile and MCP traffic is always bound to the request
        // host so a caller cannot select another tenant with a crafted header.
        return path.startsWith("/api/admin/") || path.equals("/api/admin")
                || path.equals("/api/auth/me");
    }

    private String forwardedHost(HttpServletRequest request) {
        String value = first(request.getHeader("X-Forwarded-Host"));
        if (value == null || value.isBlank()) value = request.getServerName();
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("[")) {
            int closing = value.indexOf(']');
            return closing > 0 ? value.substring(0, closing + 1) : value;
        }
        int colon = value.lastIndexOf(':');
        return colon > 0 ? value.substring(0, colon) : value;
    }

    private int forwardedPort(HttpServletRequest request) {
        String port = first(request.getHeader("X-Forwarded-Port"));
        if (port != null) {
            try { return Integer.parseInt(port.trim()); } catch (NumberFormatException ignored) { }
        }
        String host = first(request.getHeader("X-Forwarded-Host"));
        if (host != null) {
            int colon = host.lastIndexOf(':');
            if (colon > -1 && !host.endsWith("]")) {
                try { return Integer.parseInt(host.substring(colon + 1)); } catch (NumberFormatException ignored) { }
            }
        }
        return request.getServerPort();
    }

    private String first(String value) {
        if (value == null) return null;
        int comma = value.indexOf(',');
        return comma < 0 ? value : value.substring(0, comma);
    }
}
