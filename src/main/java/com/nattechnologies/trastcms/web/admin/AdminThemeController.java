package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.ThemeMenuService;
import com.nattechnologies.trastcms.service.ThemeService;
import com.nattechnologies.trastcms.service.ThemeDemoBundleService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/themes")
public class AdminThemeController {
    private final ThemeService service;
    private final ThemeMenuService menus;
    private final ThemeDemoBundleService demoBundles;

    public AdminThemeController(
            ThemeService service,
            ThemeMenuService menus,
            ThemeDemoBundleService demoBundles) {
        this.service = service;
        this.menus = menus;
        this.demoBundles = demoBundles;
    }

    @GetMapping
    public List<ApiDtos.ThemeResponse> list() {
        return service.list();
    }

    @GetMapping("/menus")
    public ApiDtos.ThemeMenusResponse menus() {
        return menus.get();
    }

    @PutMapping("/menus")
    public ApiDtos.ThemeMenusResponse updateMenus(
            @Valid @RequestBody ApiDtos.ThemeMenusRequest request,
            Authentication auth) {
        return menus.update(request, auth.getName());
    }

    @PostMapping("/{id}/starter-content/restore")
    public ApiDtos.ThemeStarterContentResponse restoreStarterContent(
            @PathVariable String id,
            Authentication auth) {
        return service.restoreStarterContent(id, auth.getName());
    }

    @GetMapping("/{id}/demo-bundle")
    public ThemeDemoBundleService.DemoBundleStatus demoBundle(@PathVariable String id) {
        return demoBundles.status(id);
    }

    @PostMapping("/{id}/demo-bundle/restore")
    public ThemeDemoBundleService.DemoRestoreResult restoreDemoBundle(
            @PathVariable String id,
            Authentication auth) {
        return demoBundles.restore(id, auth.getName());
    }

    @GetMapping(value = "/{id}/demo-bundle/export", produces = "application/zip")
    public ResponseEntity<byte[]> exportDemoBundle(
            @PathVariable String id,
            Authentication auth) {
        ThemeDemoBundleService.DemoExport exported = demoBundles.export(id, auth.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exported.filename() + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(exported.bytes().length)
                .header("X-TrastCMS-Demo-Files", String.valueOf(exported.files()))
                .body(exported.bytes());
    }

    @PutMapping("/active")
    public ApiDtos.ThemeResponse activate(
            @Valid @RequestBody ApiDtos.ActivateThemeRequest request,
            Authentication auth) {
        return service.activate(
                request.themeId(),
                auth.getName());
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.ThemeResponse install(
            @RequestPart("file") MultipartFile file,
            Authentication auth) {
        return service.install(file, auth.getName());
    }

    @GetMapping("/{id}/settings")
    public ApiDtos.ThemeSettingsResponse settings(
            @PathVariable String id) {
        return service.settings(id);
    }

    @PutMapping("/{id}/settings")
    public ApiDtos.ThemeSettingsResponse updateSettings(
            @PathVariable String id,
            @Valid @RequestBody ApiDtos.ThemeSettingsRequest request,
            Authentication auth) {
        return service.updateSettings(
                id,
                request.values(),
                auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String id,
            Authentication auth) {
        service.delete(id, auth.getName());
    }
}
