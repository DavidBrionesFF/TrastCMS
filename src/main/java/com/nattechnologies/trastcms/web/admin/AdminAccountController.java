package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/account")
public class AdminAccountController {
    private final UserAccountService users;
    public AdminAccountController(UserAccountService users) { this.users = users; }

    @GetMapping
    public ApiDtos.AccountProfileResponse profile(Authentication authentication) {
        return users.profile(authentication.getName());
    }

    @PutMapping
    public ApiDtos.AccountProfileResponse update(
            @Valid @RequestBody ApiDtos.AccountProfileRequest request,
            Authentication authentication) {
        return users.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/password")
    public ApiDtos.ApiMessage changePassword(@Valid @RequestBody ApiDtos.PasswordChangeRequest request,
                                              Authentication authentication) {
        users.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return new ApiDtos.ApiMessage("Contraseña actualizada");
    }
}
