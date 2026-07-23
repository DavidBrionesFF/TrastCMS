package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.plugin.PluginRegistration;
import com.nattechnologies.trastcms.domain.plugin.PluginRegistrationRepository;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@Service
public class PluginService {
    public static final List<String> EVENTS = List.of(
            "*",
            "content.saved",
            "content.published",
            "content.deleted",
            "media.uploaded",
            "user.created",
            "theme.activated");

    public static final List<String> PERMISSIONS = List.of(
            "content:read",
            "content:write",
            "media:read",
            "media:write",
            "settings:read",
            "settings:write",
            "users:read",
            "webhooks:receive");

    private final PluginRegistrationRepository repository;
    private final PluginSecretCipher cipher;
    private final AuditService audit;
    private final RestClient restClient;
    private final JavaPluginService javaPlugins;

    public PluginService(
            PluginRegistrationRepository repository,
            PluginSecretCipher cipher,
            AuditService audit,
            RestClient restClient,
            JavaPluginService javaPlugins) {
        this.repository = repository;
        this.cipher = cipher;
        this.audit = audit;
        this.restClient = restClient;
        this.javaPlugins = javaPlugins;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.PluginResponse> list() {
        return repository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        PluginRegistration::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApiDtos.PluginResponse save(
            ApiDtos.PluginRequest request,
            String actor) {
        validateUrl(request.baseUrl());
        validateOptionalUrl(request.homepage());

        PluginRegistration plugin = repository
                .findByPluginKey(request.pluginKey())
                .orElseGet(PluginRegistration::new);

        boolean creating = plugin.getId() == null;
        if (creating && (request.secret() == null
                || request.secret().isBlank())) {
            throw new BadRequestException(
                    "El secreto HMAC es obligatorio al crear el plugin");
        }

        plugin.setPluginKey(request.pluginKey());
        plugin.setName(request.name().trim());
        plugin.setVersion(request.version().trim());
        plugin.setDescription(blankToNull(request.description()));
        plugin.setAuthor(blankToNull(request.author()));
        plugin.setHomepage(blankToNull(request.homepage()));
        plugin.setBaseUrl(request.baseUrl().replaceAll("/+$", ""));
        plugin.setHealthCheckPath(normalizePath(request.healthCheckPath()));
        plugin.setSubscriptions(join(request.subscriptions()));
        plugin.setPermissions(join(request.permissions()));
        plugin.setEnabled(request.enabled());

        if (request.secret() != null && !request.secret().isBlank()) {
            plugin.setEncryptedSecret(cipher.encrypt(request.secret()));
        }

        PluginRegistration saved = repository.save(plugin);
        audit.record(
                actor,
                creating ? "plugin.webhook.created" : "plugin.webhook.updated",
                "plugin",
                saved.getId(),
                saved.getPluginKey());
        return toResponse(saved);
    }

    @Transactional
    public ApiDtos.PluginResponse test(String id, String actor) {
        PluginRegistration plugin = require(id);
        String message;
        String status;

        try {
            HttpStatusCode code = restClient.get()
                    .uri(plugin.getBaseUrl() + plugin.getHealthCheckPath())
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            status = code.is2xxSuccessful() ? "HEALTHY" : "UNHEALTHY";
            message = "HTTP " + code.value();
        } catch (RuntimeException exception) {
            status = "UNHEALTHY";
            message = Optional.ofNullable(exception.getMessage())
                    .orElse(exception.getClass().getSimpleName());
            if (message.length() > 950) {
                message = message.substring(0, 950);
            }
        }

        plugin.setLastTestStatus(status);
        plugin.setLastTestMessage(message);
        plugin.setLastTestAt(Instant.now());
        PluginRegistration saved = repository.save(plugin);

        audit.record(
                actor,
                "plugin.webhook.tested",
                "plugin",
                id,
                status + ": " + message);
        return toResponse(saved);
    }

    @Transactional
    public ApiDtos.PluginResponse toggle(
            String id,
            boolean enabled,
            String actor) {
        PluginRegistration plugin = require(id);
        plugin.setEnabled(enabled);
        PluginRegistration saved = repository.save(plugin);
        audit.record(
                actor,
                enabled
                        ? "plugin.webhook.enabled"
                        : "plugin.webhook.disabled",
                "plugin",
                id,
                plugin.getPluginKey());
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id, String actor) {
        PluginRegistration plugin = require(id);
        repository.delete(plugin);
        audit.record(
                actor,
                "plugin.webhook.deleted",
                "plugin",
                id,
                plugin.getPluginKey());
    }

    public ApiDtos.PluginCatalogResponse catalog() {
        return new ApiDtos.PluginCatalogResponse(
                EVENTS,
                PERMISSIONS,
                javaPlugins.isEnabled(),
                org.springframework.core.NativeDetector.inNativeImage());
    }

    private PluginRegistration require(String id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Plugin no encontrado"));
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new BadRequestException(
                        "La URL del plugin debe usar HTTP o HTTPS");
            }
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("URL del plugin inválida");
        }
    }

    private void validateOptionalUrl(String value) {
        if (value != null && !value.isBlank()) validateUrl(value);
    }

    private String normalizePath(String value) {
        if (value == null || value.isBlank()) return "/health";
        String result = value.trim();
        if (!result.startsWith("/")) result = "/" + result;
        if (result.contains("..")) {
            throw new BadRequestException(
                    "La ruta de salud del plugin es inválida");
        }
        return result;
    }

    private String join(List<String> values) {
        if (values == null) return "";
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<String> split(String values) {
        if (values == null || values.isBlank()) return List.of();
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    ApiDtos.PluginResponse toResponse(PluginRegistration plugin) {
        return new ApiDtos.PluginResponse(
                plugin.getId(),
                plugin.getPluginKey(),
                plugin.getName(),
                plugin.getVersion(),
                plugin.getDescription(),
                plugin.getAuthor(),
                plugin.getHomepage(),
                plugin.getBaseUrl(),
                split(plugin.getSubscriptions()),
                split(plugin.getPermissions()),
                plugin.getHealthCheckPath(),
                plugin.isEnabled(),
                plugin.getLastTestStatus(),
                plugin.getLastTestMessage(),
                plugin.getLastTestAt(),
                plugin.getCreatedAt(),
                plugin.getUpdatedAt());
    }
}
