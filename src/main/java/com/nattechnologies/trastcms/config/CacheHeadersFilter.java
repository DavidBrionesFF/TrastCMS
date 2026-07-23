package com.nattechnologies.trastcms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Aplica caché inmutable a bundles con hash y evita almacenar el shell de la SPA. */
public final class CacheHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/assets/")) {
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        } else if (path.equals("/") || path.equals("/index.html") || path.startsWith("/admin")
                || path.startsWith("/post/") || path.startsWith("/page/") || path.startsWith("/category/")) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }
        filterChain.doFilter(request, response);
    }
}
