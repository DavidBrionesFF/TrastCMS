package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.DashboardService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint del tablero administrativo.
 *
 * <p>Devuelve métricas agregadas para el panel de control.</p>
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final DashboardService service;

    /**
     * Inyecta el servicio de tablero.
     *
     * @param service servicio de métricas y datos del dashboard
     */
    public AdminDashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ApiDtos.Dashboard dashboard(Authentication authentication) {
        return service.dashboard(authentication.getName());
    }
}
