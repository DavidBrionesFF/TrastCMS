package com.nattechnologies.trastcms.security;

import com.nattechnologies.trastcms.plugin.api.BearerTokenAuthenticator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/** Delegates Bearer authentication to plugin contracts without knowing their internals. */
public class PluginBearerTokenFilter extends OncePerRequestFilter {
    private final List<BearerTokenAuthenticator> authenticators;

    public PluginBearerTokenFilter(List<BearerTokenAuthenticator> authenticators) {
        this.authenticators = authenticators.stream()
                .sorted(Comparator.comparingInt(BearerTokenAuthenticator::order))
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return authenticators.stream().noneMatch(authenticator -> authenticator.supports(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String rawToken = header.substring(7).trim();
                for (BearerTokenAuthenticator authenticator : authenticators) {
                    if (!authenticator.supports(request.getRequestURI())) continue;
                    try {
                        UserDetails user = authenticator.authenticate(rawToken);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, rawToken, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        break;
                    } catch (RuntimeException ignored) {
                        SecurityContextHolder.clearContext();
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
