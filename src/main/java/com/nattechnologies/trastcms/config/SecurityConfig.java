package com.nattechnologies.trastcms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.plugin.api.BearerTokenAuthenticator;
import com.nattechnologies.trastcms.security.PluginBearerTokenFilter;
import com.nattechnologies.trastcms.multisite.SiteResolutionFilter;
import com.nattechnologies.trastcms.multisite.SiteMembershipAuthorizationFilter;
import com.nattechnologies.trastcms.multisite.SiteAccessService;
import com.nattechnologies.trastcms.multisite.SiteContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            UserAccountService users,
            TrastCmsProperties properties,
            List<BearerTokenAuthenticator> bearerAuthenticators,
            SiteResolutionFilter siteResolutionFilter,
            SiteMembershipAuthorizationFilter siteMembershipAuthorizationFilter,
            SiteAccessService siteAccess
    ) throws Exception {
        String mcpEndpoint = properties.getMcp().normalizedEndpoint();

        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()
                .requestMatchers(
                    "/post/**",
                    "/page/**",
                    "/category/**",
                    "/developers",
                    "/.well-known/mcp.json",
                    mcpEndpoint
                ).permitAll()
                .requestMatchers(
                    "/api/public/**",
                    "/api/auth/csrf",
                    "/api/auth/me",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/token",
                    "/api/auth/token/refresh",
                    "/api/compat/learnpress/v1/token",
                    "/api/compat/learnpress/v1/token/refresh",
                    "/api/compat/learnpress/v1/token/register"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/compat/learnpress/v1/courses/**").permitAll()
                .requestMatchers(
                    "/api/mobile/academy/**",
                    "/api/compat/learnpress/v1/**"
                ).authenticated()
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/api/account/**"
                ).authenticated()
                // Membership discovery is deliberately tenant-agnostic so the
                // site switcher can recover from a stale/revoked selection. The
                // service returns only memberships of the authenticated identity.
                .requestMatchers("/api/admin/sites/available").authenticated()
                .requestMatchers(
                    "/api/admin/plugins/contributions"
                ).hasAnyRole("ADMIN", "EDITOR")
                .requestMatchers(
                    "/api/admin/plugins/**",
                    "/api/admin/users/**",
                    "/api/admin/themes/**",
                    "/api/admin/settings/**",
                    "/api/admin/developer/**",
                    "/api/admin/developer",
                    "/api/admin/mail/**",
                    "/api/admin/account-portal/**",
                    "/api/admin/translation/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/api/admin/categories/**",
                    "/api/admin/media/**",
                    "/api/admin/crm/**",
                    "/api/admin/commerce/**",
                    "/api/admin/store/**",
                    "/api/admin/saas/**",
                    "/api/admin/travel/**",
                    "/api/admin/academy/**"
                ).hasAnyRole("ADMIN", "EDITOR")
                .requestMatchers(
                    "/api/admin/**"
                ).hasAnyRole("ADMIN", "EDITOR", "AUTHOR")
                .requestMatchers(
                    "/admin/**",
                    "/admin"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    mcpEndpoint,
                    "/api/public/saas/**",
                    "/api/public/crm/reputation/**",
                    "/api/public/commerce/paypal/webhook",
                    "/api/auth/register",
                    "/api/auth/token",
                    "/api/auth/token/**",
                    "/api/mobile/academy/**",
                    "/api/compat/learnpress/v1/**")
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .addFilterBefore(
                siteResolutionFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .addFilterAfter(
                new PluginBearerTokenFilter(bearerAuthenticators),
                SiteResolutionFilter.class
            )
            .addFilterAfter(
                siteMembershipAuthorizationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .addFilterAfter(
                new CsrfCookieFilter(),
                UsernamePasswordAuthenticationFilter.class
            )
            .addFilterAfter(
                new CacheHeadersFilter(),
                CsrfCookieFilter.class
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.migrateSession())
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    if (siteAccess.role(authentication.getName(), SiteContext.siteId()).isEmpty()) {
                        jakarta.servlet.http.HttpSession session = request.getSession(false);
                        if (session != null) session.invalidate();
                        org.springframework.security.core.context.SecurityContextHolder.clearContext();
                        response.setStatus(403);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getOutputStream(), Map.of(
                                "authenticated", false,
                                "message", "Su cuenta no pertenece a este sitio",
                                "site", SiteContext.siteKey()));
                        return;
                    }
                    users.recordLogin(authentication.getName());
                    response.setStatus(200);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                        response.getOutputStream(),
                        Map.of("authenticated", true, "site", SiteContext.siteKey())
                    );
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                        response.getOutputStream(),
                        Map.of(
                            "authenticated", false,
                            "message", "Credenciales inválidas"
                        )
                    );
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(204)
                )
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                        response.getOutputStream(),
                        Map.of(
                            "status", 401,
                            "title", "No autenticado",
                            "authenticated", false,
                            "message", "Debe iniciar sesión para continuar"
                        )
                    );
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                        response.getOutputStream(),
                        Map.of(
                            "status", 403,
                            "title", "Acceso denegado",
                            "message", "La sesión segura venció o no tiene permisos para esta operación"
                        )
                    );
                })
            );

        return http.build();
    }
}
