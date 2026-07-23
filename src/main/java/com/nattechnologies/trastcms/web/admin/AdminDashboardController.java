package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.DashboardService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final DashboardService service;

    public AdminDashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ApiDtos.Dashboard dashboard(Authentication authentication) {
        return service.dashboard(authentication.getName());
    }
}
