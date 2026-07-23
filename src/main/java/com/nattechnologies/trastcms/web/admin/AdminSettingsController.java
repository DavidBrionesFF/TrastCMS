package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.SiteSettingService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {
    private final SiteSettingService service;
    public AdminSettingsController(SiteSettingService service) { this.service = service; }

    @GetMapping public ApiDtos.SettingsResponse get() { return new ApiDtos.SettingsResponse(service.all()); }

    @PutMapping
    public ApiDtos.SettingsResponse update(@Valid @RequestBody ApiDtos.SettingsRequest request, Authentication auth) {
        return service.update(request.values(), auth.getName());
    }
}
