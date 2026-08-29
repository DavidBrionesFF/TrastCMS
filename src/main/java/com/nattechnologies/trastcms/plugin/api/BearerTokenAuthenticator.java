package com.nattechnologies.trastcms.plugin.api;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Public contract for bundled plugins that authenticate stateless Bearer API requests.
 * Implementations own their tokens and persistence; the core only installs the
 * authenticated principal in Spring Security.
 */
public interface BearerTokenAuthenticator {
    default int order() { return 100; }
    boolean supports(String requestPath);
    UserDetails authenticate(String rawToken);
}
