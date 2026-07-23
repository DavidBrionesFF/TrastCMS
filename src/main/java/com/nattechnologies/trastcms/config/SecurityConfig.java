package com.nattechnologies.trastcms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.service.UserAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
            TrastCmsProperties properties
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
                    "/api/auth/login"
                ).permitAll()
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
                    "/api/admin/plugins/contributions"
                ).hasAnyRole("ADMIN", "EDITOR")
                .requestMatchers(
                    "/api/admin/plugins/**",
                    "/api/admin/users/**",
                    "/api/admin/themes/**",
                    "/api/admin/settings/**",
                    "/api/admin/developer/**",
                    "/api/admin/developer"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/api/admin/categories/**",
                    "/api/admin/media/**",
                    "/api/admin/crm/**"
                ).hasAnyRole("ADMIN", "EDITOR")
                .requestMatchers(
                    "/api/admin/**",
                    "/admin/**",
                    "/admin"
                ).authenticated()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(mcpEndpoint)
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
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
                    users.recordLogin(authentication.getName());
                    response.setStatus(200);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                        response.getOutputStream(),
                        Map.of("authenticated", true)
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
                            "title", "No autenticado"
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
                            "title", "Acceso denegado"
                        )
                    );
                })
            );

        return http.build();
    }
}
