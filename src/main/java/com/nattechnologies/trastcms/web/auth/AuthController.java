package com.nattechnologies.trastcms.web.auth;

import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountService users;

    public AuthController(UserAccountService users) { this.users = users; }

    @GetMapping("/csrf")
    public ApiDtos.Csrf csrf(CsrfToken token) {
        return new ApiDtos.Csrf(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @GetMapping("/me")
    public ApiDtos.AuthUser me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return new ApiDtos.AuthUser(false, null, null, null, null, null);
        }
        UserAccount account = users.requireByEmail(authentication.getName());
        return new ApiDtos.AuthUser(true, account.getEmail(), account.getDisplayName(), account.getRole().name(), account.getAvatarUrl(), account.getLastLoginAt());
    }
}
