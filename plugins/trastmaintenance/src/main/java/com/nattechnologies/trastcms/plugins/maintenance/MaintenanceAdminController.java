package com.nattechnologies.trastcms.plugins.maintenance;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/admin/maintenance")
public class MaintenanceAdminController {

    private final MaintenancePluginGuard guard;
    private final MaintenanceService service;

    public MaintenanceAdminController(MaintenancePluginGuard guard, MaintenanceService service) {
        this.guard = guard;
        this.service = service;
    }

    @ModelAttribute
    void requireEnabled() {
        guard.requireEnabled();
    }

    @GetMapping("/backups")
    public Page<MaintenanceDtos.BackupJobResponse> listBackups(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.listBackups(page, size);
    }

    @PostMapping("/backups")
    public MaintenanceDtos.BackupJobResponse triggerBackup(@RequestBody MaintenanceDtos.BackupRequest request, Principal principal) {
        return service.triggerBackup(request.backupType(), principal != null ? principal.getName() : "system");
    }

    @DeleteMapping("/backups/{id}")
    public void deleteBackup(@PathVariable String id) {
        service.deleteBackup(id);
    }

    @GetMapping("/backups/{id}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String id) {
        Resource res = service.downloadBackup(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.getFilename() + "\"")
                .body(res);
    }

    @PostMapping("/backups/{id}/upload-drive")
    public void uploadToDrive(@PathVariable String id) {
        service.uploadToGoogleDrive(id);
    }

    @GetMapping("/cache/stats")
    public MaintenanceDtos.CacheStatsResponse cacheStats() {
        return service.getCacheStats();
    }

    @PostMapping("/cache/evict")
    public String evictAllCaches() {
        service.evictAllCaches();
        return "Cachés vaciadas exitosamente";
    }

    @GetMapping("/health")
    public MaintenanceDtos.HealthResponse health() {
        return service.getSystemHealth();
    }

    @PostMapping("/optimize")
    public String optimize() {
        service.optimizeDb();
        return "Base de datos optimizada";
    }

    @GetMapping("/settings")
    public MaintenanceDtos.MaintenanceSettingsResponse getSettings() {
        return service.getSettings();
    }

    @PutMapping("/settings")
    public void saveSettings(@RequestBody MaintenanceDtos.MaintenanceSettingsRequest req) {
        service.saveSettings(req);
    }
}
