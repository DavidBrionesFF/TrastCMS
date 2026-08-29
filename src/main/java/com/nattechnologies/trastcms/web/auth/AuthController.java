package com.nattechnologies.trastcms.web.auth;

import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.multisite.SiteAccessService;
import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountService users;
    private final SiteAccessService siteAccess;

    public AuthController(UserAccountService users, SiteAccessService siteAccess) {
        this.users = users;
        this.siteAccess = siteAccess;
    }

    @GetMapping("/csrf")
    public ApiDtos.Csrf csrf(CsrfToken token) {
        return new ApiDtos.Csrf(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @GetMapping("/me")
    public ApiDtos.AuthUser me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return new ApiDtos.AuthUser(false, null, null, null, null, null, false,
                    SiteContext.siteId(), SiteContext.siteKey(), SiteContext.siteName());
        }
        UserAccount account = users.requireByEmail(authentication.getName());
        String role = siteAccess.role(account.getEmail(), SiteContext.siteId()).map(Enum::name).orElse(null);
        if (role == null) {
            // A valid session on another tenant must not be treated as an authenticated
            // session for a site where the identity has no membership.
            return new ApiDtos.AuthUser(false, null, null, null, null, null, false,
                    SiteContext.siteId(), SiteContext.siteKey(), SiteContext.siteName());
        }
        return new ApiDtos.AuthUser(true, account.getEmail(), account.getDisplayName(), role,
                account.getAvatarUrl(), account.getLastLoginAt(), account.isNetworkAdmin(),
                SiteContext.siteId(), SiteContext.siteKey(), SiteContext.siteName());
    }
}
