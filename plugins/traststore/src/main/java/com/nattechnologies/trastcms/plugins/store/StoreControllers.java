package com.nattechnologies.trastcms.plugins.store;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/store")
class StorePublicController {
    private final StoreService service;

    StorePublicController(StoreService service) {
        this.service = service;
    }

    @GetMapping("/products")
    StoreDtos.PageResponse<StoreDtos.ProductResponse> products(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return service.catalog(search, page, size);
    }

    @GetMapping("/products/{slug}")
    StoreDtos.ProductResponse product(@PathVariable String slug) {
        return service.publicProduct(slug);
    }
}

@RestController
@RequestMapping("/api/admin/store")
class StoreAdminController {
    private final StoreService service;

    StoreAdminController(StoreService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    StoreDtos.Dashboard dashboard() {
        return service.dashboard();
    }

    @GetMapping("/products")
    StoreDtos.PageResponse<StoreDtos.ProductResponse> products(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.adminProducts(search, page, size);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    StoreDtos.ProductResponse create(
            @Valid @RequestBody StoreDtos.ProductRequest request) {
        return service.save(null, request);
    }

    @PutMapping("/products/{id}")
    StoreDtos.ProductResponse update(
            @PathVariable String id,
            @Valid @RequestBody StoreDtos.ProductRequest request) {
        return service.save(id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        service.delete(id);
    }

    @PostMapping("/products/{id}/inventory")
    StoreDtos.ProductResponse inventory(
            @PathVariable String id,
            @Valid @RequestBody StoreDtos.InventoryAdjustmentRequest request) {
        return service.adjust(id, request);
    }

    @GetMapping("/inventory/movements")
    StoreDtos.PageResponse<StoreDtos.InventoryMovementResponse> movements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.movements(page, size);
    }

    @PostMapping("/demo-data")
    StoreDtos.DemoDataResponse generateDemoData() {
        return service.generateDemoData();
    }

    @DeleteMapping("/demo-data")
    StoreDtos.DemoDataResponse deleteDemoData() {
        return service.deleteDemoData();
    }
}
