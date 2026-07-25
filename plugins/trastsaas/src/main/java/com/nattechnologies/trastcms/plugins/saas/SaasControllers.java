package com.nattechnologies.trastcms.plugins.saas;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/public/saas")
class SaasPublicController {
    private final SaasService service;

    SaasPublicController(SaasService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    SaasDtos.CatalogResponse catalog() {
        return service.publicCatalog();
    }

    @GetMapping("/settings")
    SaasDtos.SettingsResponse settings() {
        return service.settings();
    }

    @GetMapping("/products/{productKey}/plans")
    List<SaasDtos.PlanResponse> plans(@PathVariable String productKey) {
        return service.publicPlans(productKey);
    }

    @PostMapping("/licenses/validate")
    SaasDtos.LicenseResult validate(@Valid @RequestBody SaasDtos.LicenseRequest request) {
        return service.validate(request, false);
    }

    @PostMapping("/licenses/activate")
    SaasDtos.LicenseResult activate(@Valid @RequestBody SaasDtos.LicenseRequest request) {
        return service.validate(request, true);
    }

    @PostMapping("/licenses/heartbeat")
    SaasDtos.LicenseResult heartbeat(@Valid @RequestBody SaasDtos.LicenseRequest request) {
        return service.heartbeat(request);
    }

    @PostMapping("/licenses/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@Valid @RequestBody SaasDtos.DeactivateRequest request) {
        service.deactivate(request);
    }

    @PostMapping("/usage")
    Map<String, Object> usage(@Valid @RequestBody SaasDtos.UsageRequest request) {
        return service.usage(request);
    }

    @GetMapping("/orders/{orderNumber}/claim")
    SaasDtos.ClaimResponse claim(
            @PathVariable String orderNumber,
            @RequestParam String token) {
        return service.claim(orderNumber, token);
    }

    @PostMapping("/releases/latest")
    SaasDtos.ReleaseResponse latest(
            @RequestParam String productKey,
            @RequestParam String platform,
            @RequestParam(defaultValue = "stable") String channel,
            @Valid @RequestBody SaasDtos.LicenseRequest request) {
        return service.latest(productKey, platform, channel, request);
    }
}

@RestController
@RequestMapping("/api/admin/saas")
class SaasAdminController {
    private final SaasService service;

    SaasAdminController(SaasService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    SaasDtos.Dashboard dashboard() {
        return service.dashboard();
    }

    @GetMapping("/products")
    List<SaasDtos.ProductResponse> products() {
        return service.products();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    SaasDtos.ProductResponse product(@Valid @RequestBody SaasDtos.ProductRequest request) {
        return service.saveProduct(null, request);
    }

    @PutMapping("/products/{id}")
    SaasDtos.ProductResponse product(
            @PathVariable String id,
            @Valid @RequestBody SaasDtos.ProductRequest request) {
        return service.saveProduct(id, request);
    }

    @GetMapping("/plans")
    List<SaasDtos.PlanResponse> plans() {
        return service.plans();
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    SaasDtos.PlanResponse plan(@Valid @RequestBody SaasDtos.PlanRequest request) {
        return service.savePlan(null, request);
    }

    @PutMapping("/plans/{id}")
    SaasDtos.PlanResponse plan(
            @PathVariable String id,
            @Valid @RequestBody SaasDtos.PlanRequest request) {
        return service.savePlan(id, request);
    }

    @GetMapping("/licenses")
    List<SaasDtos.LicenseResponse> licenses() {
        return service.licenses();
    }

    @PostMapping("/licenses")
    @ResponseStatus(HttpStatus.CREATED)
    SaasDtos.LicenseResponse issue(@Valid @RequestBody SaasDtos.IssueLicenseRequest request) {
        return service.issue(request);
    }

    @PutMapping("/licenses/{id}/status")
    SaasDtos.LicenseResponse licenseStatus(
            @PathVariable String id,
            @Valid @RequestBody SaasDtos.LicenseStatusRequest request) {
        return service.updateLicenseStatus(id, request);
    }

    @GetMapping("/subscriptions")
    List<SaasDtos.SubscriptionResponse> subscriptions() {
        return service.subscriptions();
    }

    @PutMapping("/subscriptions/{id}")
    SaasDtos.SubscriptionResponse subscription(
            @PathVariable String id,
            @Valid @RequestBody SaasDtos.SubscriptionStatusRequest request) {
        return service.updateSubscription(id, request);
    }

    @PostMapping("/releases")
    @ResponseStatus(HttpStatus.CREATED)
    SaasDtos.ReleaseResponse release(@Valid @RequestBody SaasDtos.ReleaseRequest request) {
        return service.saveRelease(null, request);
    }

    @GetMapping("/settings")
    SaasDtos.SettingsResponse settings() {
        return service.settings();
    }

    @PutMapping("/settings")
    SaasDtos.SettingsResponse settings(@Valid @RequestBody SaasDtos.SettingsRequest request) {
        return service.saveSettings(request);
    }

    @PostMapping("/demo-data")
    SaasDtos.DemoDataResponse generateDemoData() {
        return service.generateDemoData();
    }

    @DeleteMapping("/demo-data")
    SaasDtos.DemoDataResponse deleteDemoData() {
        return service.deleteDemoData();
    }
}
