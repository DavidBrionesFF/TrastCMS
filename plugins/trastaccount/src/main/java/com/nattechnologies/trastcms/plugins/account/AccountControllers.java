package com.nattechnologies.trastcms.plugins.account;

import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/account")
class PublicAccountController {
    private final AccountService service;
    PublicAccountController(AccountService service) { this.service = service; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.AccountProfileResponse register(
            @Valid @RequestBody AccountDtos.RegisterRequest request) {
        return service.register(request);
    }
}

@RestController
@RequestMapping("/api/account")
class CustomerAccountController {
    private final AccountService service;
    private final UserAccountService users;

    CustomerAccountController(AccountService service, UserAccountService users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    AccountDtos.Overview overview(Authentication authentication) {
        return service.overview(authentication.getName());
    }

    @PutMapping("/profile")
    ApiDtos.AccountProfileResponse updateProfile(
            @Valid @RequestBody ApiDtos.AccountProfileRequest request,
            Authentication authentication) {
        return users.updateProfile(authentication.getName(), request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(
            @Valid @RequestBody ApiDtos.PasswordChangeRequest request,
            Authentication authentication) {
        users.changePassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword());
    }
}

@RestController
@RequestMapping("/api/admin/account-portal")
class AccountAdminController {
    private final AccountService service;
    AccountAdminController(AccountService service) { this.service = service; }

    @GetMapping("/status")
    AccountDtos.AdminStatus status() { return service.adminStatus(); }
}
