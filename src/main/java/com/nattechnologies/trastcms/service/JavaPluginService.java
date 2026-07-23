package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.plugin.api.TrastCmsExtension;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NativeDetector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class JavaPluginService {
    private static final Logger log = LoggerFactory.getLogger(JavaPluginService.class);
    private static final long MAX_PLUGIN_BYTES = 50L * 1024L * 1024L;

    private final Path pluginsDirectory;
    private final boolean enabled;
    private final AuditService audit;
    private PluginManager manager;

    public JavaPluginService(
            TrastCmsProperties properties,
            AuditService audit) {
        Path configured = Paths.get(
                properties.getDataDir(),
                properties.getPlugins().getDirectory());
        this.pluginsDirectory = configured.toAbsolutePath().normalize();
        this.enabled = properties.getPlugins().isJavaEnabled()
                && !NativeDetector.inNativeImage();
        this.audit = audit;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(pluginsDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de plugins Java",
                    exception);
        }

        if (!enabled) {
            log.info("Los plugins Java dinámicos están deshabilitados. "
                    + "Los webhooks continúan disponibles.");
            return;
        }

        try {
            manager = new DefaultPluginManager(pluginsDirectory);
            manager.loadPlugins();
            manager.startPlugins();
            log.info("PF4J inició {} plugins desde {}",
                    manager.getPlugins().size(),
                    pluginsDirectory);
        } catch (RuntimeException exception) {
            log.error("No se pudo inicializar completamente PF4J", exception);
        }
    }

    @PreDestroy
    void shutdown() {
        if (manager == null) return;
        try {
            manager.stopPlugins();
            manager.unloadPlugins();
        } catch (RuntimeException exception) {
            log.warn("No se pudieron detener todos los plugins Java", exception);
        }
    }

    public synchronized List<ApiDtos.JavaPluginResponse> list() {
        if (manager == null) return List.of();
        return manager.getPlugins().stream()
                .sorted(Comparator.comparing(
                        wrapper -> wrapper.getDescriptor().getPluginId()))
                .map(this::toResponse)
                .toList();
    }

    public synchronized ApiDtos.JavaPluginResponse install(
            MultipartFile archive,
            String actor) {
        requireEnabled();

        if (archive == null || archive.isEmpty()) {
            throw new BadRequestException(
                    "Debe seleccionar un plugin JAR o ZIP");
        }
        if (archive.getSize() > MAX_PLUGIN_BYTES) {
            throw new BadRequestException(
                    "El plugin supera el límite de 50 MB");
        }

        String original = Optional.ofNullable(
                        archive.getOriginalFilename())
                .orElse("plugin.jar");
        String lower = original.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".jar") && !lower.endsWith(".zip")) {
            throw new BadRequestException(
                    "El plugin debe estar empaquetado como JAR o ZIP PF4J");
        }

        String safeName = Paths.get(original)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = pluginsDirectory.resolve(safeName).normalize();

        if (!target.startsWith(pluginsDirectory)) {
            throw new BadRequestException("Nombre de plugin inválido");
        }

        try {
            Files.deleteIfExists(target);
            archive.transferTo(target);
            String pluginId = manager.loadPlugin(target);
            manager.startPlugin(pluginId);
            PluginWrapper wrapper = requireWrapper(pluginId);
            audit.record(
                    actor,
                    "plugin.java.installed",
                    "plugin",
                    pluginId,
                    safeName);
            return toResponse(wrapper);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo guardar el plugin", exception);
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw new BadRequestException(
                    "PF4J rechazó el paquete: " + exception.getMessage());
        }
    }

    public synchronized ApiDtos.JavaPluginResponse start(
            String pluginId,
            String actor) {
        requireEnabled();
        manager.startPlugin(pluginId);
        PluginWrapper wrapper = requireWrapper(pluginId);
        audit.record(
                actor,
                "plugin.java.started",
                "plugin",
                pluginId,
                wrapper.getDescriptor().getPluginId());
        return toResponse(wrapper);
    }

    public synchronized ApiDtos.JavaPluginResponse stop(
            String pluginId,
            String actor) {
        requireEnabled();
        PluginWrapper wrapper = requireWrapper(pluginId);
        manager.stopPlugin(pluginId);
        audit.record(
                actor,
                "plugin.java.stopped",
                "plugin",
                pluginId,
                wrapper.getDescriptor().getPluginId());
        return toResponse(requireWrapper(pluginId));
    }

    public synchronized void delete(String pluginId, String actor) {
        requireEnabled();
        PluginWrapper wrapper = requireWrapper(pluginId);
        Path pluginPath = wrapper.getPluginPath();

        manager.stopPlugin(pluginId);
        manager.unloadPlugin(pluginId);
        deletePath(pluginPath);

        audit.record(
                actor,
                "plugin.java.deleted",
                "plugin",
                pluginId,
                pluginPath.toString());
    }

    public synchronized List<TrastCmsExtension> extensions() {
        if (manager == null) return List.of();
        return manager.getExtensions(TrastCmsExtension.class);
    }

    public synchronized ApiDtos.PluginContributionsResponse contributions() {
        if (manager == null) {
            return new ApiDtos.PluginContributionsResponse(List.of(), List.of());
        }
        List<Map<String, Object>> blocks = new ArrayList<>();
        List<Map<String, Object>> menuItems = new ArrayList<>();

        manager.getPlugins().stream()
                .filter(wrapper -> "STARTED".equals(wrapper.getPluginState().toString()))
                .forEach(wrapper -> manager.getExtensions(
                                TrastCmsExtension.class,
                                wrapper.getDescriptor().getPluginId())
                        .forEach(extension -> {
                            extension.blocks().forEach(block -> blocks.add(
                                    contribution(block, wrapper, extension)));
                            extension.adminMenuItems().forEach(item -> menuItems.add(
                                    contribution(item, wrapper, extension)));
                        }));

        return new ApiDtos.PluginContributionsResponse(
                List.copyOf(blocks),
                List.copyOf(menuItems));
    }


    public synchronized Map<String, Object> adminPage(
            String pluginId,
            String pageId) {
        PluginWrapper wrapper = requireStartedWrapper(pluginId);
        return manager.getExtensions(TrastCmsExtension.class, pluginId)
                .stream()
                .flatMap(extension -> extension.adminMenuItems().stream()
                        .filter(item -> pageId.equals(String.valueOf(item.get("id"))))
                        .map(item -> contribution(item, wrapper, extension)))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Página administrativa del plugin no encontrada"));
    }

    public synchronized Map<String, Object> executeAdminAction(
            String pluginId,
            String action,
            Map<String, Object> input,
            String actor) {
        if (action == null || !action.matches("[a-zA-Z0-9._-]{2,120}")) {
            throw new BadRequestException("Identificador de acción inválido");
        }
        PluginWrapper wrapper = requireStartedWrapper(pluginId);
        TrastCmsExtension extension = manager
                .getExtensions(TrastCmsExtension.class, pluginId)
                .stream()
                .filter(candidate -> candidate.adminActions().contains(action))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "El plugin no declara la acción solicitada"));

        try {
            Map<String, Object> response = extension.handleAdminAction(
                    action,
                    input == null ? Map.of() : Map.copyOf(input));
            audit.record(
                    actor,
                    "plugin.java.action",
                    "plugin",
                    pluginId,
                    action);
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("success", true);
            normalized.put("pluginId", wrapper.getDescriptor().getPluginId());
            normalized.put("action", action);
            if (response != null) normalized.putAll(response);
            return Collections.unmodifiableMap(normalized);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BadRequestException(
                    "El plugin no pudo ejecutar la acción: " + exception.getMessage());
        }
    }

    private Map<String, Object> contribution(
            Map<String, Object> source,
            PluginWrapper wrapper,
            TrastCmsExtension extension) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source != null) result.putAll(source);
        result.put("pluginId", wrapper.getDescriptor().getPluginId());
        result.put("pluginVersion", wrapper.getDescriptor().getVersion());
        result.put("extensionName", extension.name());
        return Collections.unmodifiableMap(result);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Path getPluginsDirectory() {
        return pluginsDirectory;
    }

    private ApiDtos.JavaPluginResponse toResponse(PluginWrapper wrapper) {
        List<String> dependencies = wrapper.getDescriptor()
                .getDependencies()
                .stream()
                .map(dependency -> dependency.getPluginId())
                .toList();

        List<String> capabilities;
        try {
            capabilities = manager
                    .getExtensions(
                            TrastCmsExtension.class,
                            wrapper.getDescriptor().getPluginId())
                    .stream()
                    .flatMap(extension -> extension.capabilities().stream())
                    .distinct()
                    .sorted()
                    .toList();
        } catch (RuntimeException exception) {
            capabilities = List.of();
        }

        return new ApiDtos.JavaPluginResponse(
                wrapper.getDescriptor().getPluginId(),
                wrapper.getDescriptor().getPluginId(),
                wrapper.getDescriptor().getVersion(),
                wrapper.getDescriptor().getProvider(),
                wrapper.getDescriptor().getPluginDescription(),
                wrapper.getPluginState().toString(),
                wrapper.getPluginPath().toString(),
                dependencies,
                capabilities,
                true,
                enabled);
    }

    private PluginWrapper requireWrapper(String pluginId) {
        if (manager == null) {
            throw new NotFoundException("Plugin Java no encontrado");
        }
        PluginWrapper wrapper = manager.getPlugin(pluginId);
        if (wrapper == null) {
            throw new NotFoundException("Plugin Java no encontrado");
        }
        return wrapper;
    }


    private PluginWrapper requireStartedWrapper(String pluginId) {
        PluginWrapper wrapper = requireWrapper(pluginId);
        if (!"STARTED".equals(wrapper.getPluginState().toString())) {
            throw new ConflictException(
                    "El plugin debe estar iniciado para usar sus extensiones");
        }
        return wrapper;
    }

    private void requireEnabled() {
        if (!enabled || manager == null) {
            throw new BadRequestException(
                    "Los plugins Java dinámicos están deshabilitados "
                            + "o TrastCMS se ejecuta como Native Image");
        }
    }

    private void deletePath(Path path) {
        if (path == null || !Files.exists(path)) return;
        try {
            if (Files.isDirectory(path)) {
                try (var walk = Files.walk(path)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(item -> {
                                try {
                                    Files.deleteIfExists(item);
                                } catch (IOException exception) {
                                    throw new java.io.UncheckedIOException(exception);
                                }
                            });
                }
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException | java.io.UncheckedIOException exception) {
            throw new IllegalStateException(
                    "No se pudo eliminar el paquete del plugin",
                    exception);
        }
    }
}
