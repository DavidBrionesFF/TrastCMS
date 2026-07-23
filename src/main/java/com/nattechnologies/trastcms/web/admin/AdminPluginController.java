package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.JavaPluginService;
import com.nattechnologies.trastcms.service.BundledPluginService;
import com.nattechnologies.trastcms.service.PluginService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/plugins")
public class AdminPluginController {
    private final PluginService webhooks;
    private final JavaPluginService javaPlugins;
    private final BundledPluginService bundledPlugins;

    public AdminPluginController(
            PluginService webhooks,
            JavaPluginService javaPlugins,
            BundledPluginService bundledPlugins) {
        this.webhooks = webhooks;
        this.javaPlugins = javaPlugins;
        this.bundledPlugins = bundledPlugins;
    }

    @GetMapping
    public List<ApiDtos.PluginResponse> list() {
        return webhooks.list();
    }

    @GetMapping("/catalog")
    public ApiDtos.PluginCatalogResponse catalog() {
        return webhooks.catalog();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.PluginResponse save(
            @Valid @RequestBody ApiDtos.PluginRequest request,
            Authentication auth) {
        return webhooks.save(request, auth.getName());
    }

    @PostMapping("/{id}/test")
    public ApiDtos.PluginResponse test(
            @PathVariable String id,
            Authentication auth) {
        return webhooks.test(id, auth.getName());
    }

    @PutMapping("/{id}/enabled")
    public ApiDtos.PluginResponse toggle(
            @PathVariable String id,
            @RequestParam boolean value,
            Authentication auth) {
        return webhooks.toggle(id, value, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String id,
            Authentication auth) {
        webhooks.delete(id, auth.getName());
    }

    @GetMapping("/bundled")
    public List<ApiDtos.BundledPluginResponse> bundledList() {
        return bundledPlugins.list();
    }

    @PutMapping("/bundled/{id}/enabled")
    public ApiDtos.BundledPluginResponse toggleBundled(
            @PathVariable String id,
            @RequestParam boolean value,
            Authentication auth) {
        return bundledPlugins.toggle(id, value, auth.getName());
    }

    @GetMapping("/contributions")
    public ApiDtos.PluginContributionsResponse contributions() {
        ApiDtos.PluginContributionsResponse bundledContributions =
                bundledPlugins.contributions();
        ApiDtos.PluginContributionsResponse javaContributions =
                javaPlugins.contributions();

        List<Map<String, Object>> blocks =
                new java.util.ArrayList<>(bundledContributions.blocks());
        blocks.addAll(javaContributions.blocks());

        List<Map<String, Object>> menus =
                new java.util.ArrayList<>(
                        bundledContributions.adminMenuItems());
        menus.addAll(javaContributions.adminMenuItems());

        return new ApiDtos.PluginContributionsResponse(
                List.copyOf(blocks),
                List.copyOf(menus));
    }

    @GetMapping("/{pluginId}/pages/{pageId}")
    public Map<String, Object> adminPage(
            @PathVariable String pluginId,
            @PathVariable String pageId) {
        try { return bundledPlugins.adminPage(pluginId, pageId); }
        catch (com.nattechnologies.trastcms.service.NotFoundException ignored) {
            return javaPlugins.adminPage(pluginId, pageId);
        }
    }

    @PostMapping("/{pluginId}/actions/{action}")
    public Map<String, Object> executeAction(
            @PathVariable String pluginId,
            @PathVariable String action,
            @RequestBody(required = false) Map<String, Object> input,
            Authentication auth) {
        try { return bundledPlugins.execute(pluginId, action, input, auth.getName()); }
        catch (com.nattechnologies.trastcms.service.NotFoundException ignored) {
            return javaPlugins.executeAdminAction(pluginId, action, input, auth.getName());
        }
    }

    @GetMapping("/java")
    public List<ApiDtos.JavaPluginResponse> javaList() {
        return javaPlugins.list();
    }

    @GetMapping("/java/contributions")
    public ApiDtos.PluginContributionsResponse javaContributions() {
        return javaPlugins.contributions();
    }


    @GetMapping("/java/{pluginId}/pages/{pageId}")
    public Map<String, Object> javaAdminPage(
            @PathVariable String pluginId,
            @PathVariable String pageId) {
        return javaPlugins.adminPage(pluginId, pageId);
    }

    @PostMapping("/java/{pluginId}/actions/{action}")
    public Map<String, Object> executeJavaAction(
            @PathVariable String pluginId,
            @PathVariable String action,
            @RequestBody(required = false) Map<String, Object> input,
            Authentication auth) {
        return javaPlugins.executeAdminAction(
                pluginId,
                action,
                input,
                auth.getName());
    }

    @PostMapping(value = "/java", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.JavaPluginResponse installJava(
            @RequestPart("file") MultipartFile file,
            Authentication auth) {
        return javaPlugins.install(file, auth.getName());
    }

    @PutMapping("/java/{id}/start")
    public ApiDtos.JavaPluginResponse startJava(
            @PathVariable String id,
            Authentication auth) {
        return javaPlugins.start(id, auth.getName());
    }

    @PutMapping("/java/{id}/stop")
    public ApiDtos.JavaPluginResponse stopJava(
            @PathVariable String id,
            Authentication auth) {
        return javaPlugins.stop(id, auth.getName());
    }

    @DeleteMapping("/java/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJava(
            @PathVariable String id,
            Authentication auth) {
        javaPlugins.delete(id, auth.getName());
    }
}
