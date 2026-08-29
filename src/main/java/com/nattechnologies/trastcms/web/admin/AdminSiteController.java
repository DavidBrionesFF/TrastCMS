package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.SiteService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sites")
public class AdminSiteController {
    private final SiteService service;
    public AdminSiteController(SiteService service) { this.service = service; }

    @GetMapping public List<ApiDtos.SiteResponse> list(Authentication auth) { return service.list(auth.getName()); }
    @GetMapping("/available") public List<ApiDtos.SiteMembershipSummary> available(Authentication auth) { return service.available(auth.getName()); }
    @GetMapping("/current") public ApiDtos.SiteResponse current() { return service.current(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.SiteResponse create(@Valid @RequestBody ApiDtos.SiteRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @PutMapping("/{id}")
    public ApiDtos.SiteResponse update(@PathVariable String id, @Valid @RequestBody ApiDtos.SiteRequest request, Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @PutMapping("/{id}/plugins")
    public ApiDtos.SiteResponse plugins(@PathVariable String id, @RequestBody ApiDtos.PluginSelectionRequest request, Authentication auth) {
        return service.configurePlugins(id, request, auth.getName());
    }

    @PostMapping("/{id}/provision")
    public ApiDtos.SiteResponse provision(@PathVariable String id, @RequestBody(required = false) Map<String, String> request, Authentication auth) {
        return service.reprovision(id, request == null ? null : request.get("blueprint"), auth.getName());
    }
}
