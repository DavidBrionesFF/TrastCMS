package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.plugin.BundledPluginState;
import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Order(20)
public class BundledPluginService implements ApplicationRunner {
    private final Map<String, BundledPlugin> plugins;
    private final BundledPluginStateRepository states;
    private final AuditService audit;

    public BundledPluginService(List<BundledPlugin> plugins, BundledPluginStateRepository states, AuditService audit) {
        Map<String, BundledPlugin> indexed = new TreeMap<>();
        for (BundledPlugin plugin : plugins) {
            if (indexed.put(plugin.id(), plugin) != null) {
                throw new IllegalStateException("Plugin incorporado duplicado: " + plugin.id());
            }
        }
        this.plugins = Collections.unmodifiableMap(indexed);
        this.states = states;
        this.audit = audit;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (BundledPlugin plugin : plugins.values()) {
            BundledPluginState state = states.findById(plugin.id()).orElseGet(() -> {
                BundledPluginState created = new BundledPluginState();
                created.setPluginId(plugin.id());
                created.setEnabled(plugin.enabledByDefault());
                created.setInstalledVersion(plugin.version());
                return created;
            });
            state.setInstalledVersion(plugin.version());
            states.save(state);
            if (state.isEnabled()) plugin.onEnabled();
        }
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.BundledPluginResponse> list() {
        return plugins.values().stream().map(this::response).toList();
    }

    @Transactional
    public ApiDtos.BundledPluginResponse toggle(String pluginId, boolean enabled, String actor) {
        BundledPlugin plugin = require(pluginId);
        BundledPluginState state = requireState(pluginId);
        if (enabled) {
            for (String dependency : plugin.requiredPlugins()) {
                if (!isEnabled(dependency)) {
                    throw new BadRequestException("Active primero el plugin requerido: " + dependency);
                }
            }
        }
        if (!enabled) {
            plugins.values().stream()
                    .filter(candidate -> isEnabled(candidate.id()) && candidate.requiredPlugins().contains(pluginId))
                    .findFirst()
                    .ifPresent(candidate -> { throw new BadRequestException("Desactive primero el plugin dependiente: " + candidate.name()); });
        }
        if (state.isEnabled() != enabled) {
            state.setEnabled(enabled);
            state.setInstalledVersion(plugin.version());
            states.save(state);
            if (enabled) plugin.onEnabled(); else plugin.onDisabled();
            audit.record(actor, enabled ? "plugin.bundled.enabled" : "plugin.bundled.disabled",
                    "plugin", pluginId, plugin.name());
        }
        return response(plugin);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String pluginId) {
        return states.findById(pluginId).map(BundledPluginState::isEnabled).orElse(false);
    }

    @Transactional(readOnly = true)
    public ApiDtos.PluginContributionsResponse contributions() {
        List<Map<String, Object>> blocks = new ArrayList<>();
        List<Map<String, Object>> menus = new ArrayList<>();
        for (BundledPlugin plugin : enabledPlugins()) {
            plugin.blocks().forEach(item -> blocks.add(contribution(item, plugin)));
            plugin.adminMenuItems().forEach(item -> menus.add(contribution(item, plugin)));
        }
        return new ApiDtos.PluginContributionsResponse(List.copyOf(blocks), List.copyOf(menus));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminPage(String pluginId, String pageId) {
        BundledPlugin plugin = requireEnabled(pluginId);
        return plugin.adminMenuItems().stream()
                .filter(item -> pageId.equals(String.valueOf(item.get("id"))))
                .map(item -> contribution(item, plugin))
                .findFirst().orElseThrow(() -> new NotFoundException("Página del plugin no encontrada"));
    }

    @Transactional
    public Map<String, Object> execute(String pluginId, String action, Map<String, Object> input, String actor) {
        BundledPlugin plugin = requireEnabled(pluginId);
        if (!plugin.adminActions().contains(action)) {
            throw new NotFoundException("El plugin no declara la acción solicitada");
        }
        Map<String, Object> result = plugin.handleAdminAction(action, input == null ? Map.of() : Map.copyOf(input));
        audit.record(actor, "plugin.bundled.action", "plugin", pluginId, action);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("success", true);
        normalized.put("pluginId", pluginId);
        normalized.put("action", action);
        if (result != null) normalized.putAll(result);
        return Collections.unmodifiableMap(normalized);
    }

    public void dispatch(PluginEvent event) {
        for (BundledPlugin plugin : enabledPlugins()) {
            try { plugin.onEvent(event); } catch (RuntimeException ignored) { }
        }
    }

    private List<BundledPlugin> enabledPlugins() {
        return plugins.values().stream().filter(plugin -> isEnabled(plugin.id())).toList();
    }
    private BundledPlugin require(String id) {
        BundledPlugin plugin = plugins.get(id);
        if (plugin == null) throw new NotFoundException("Plugin incorporado no encontrado");
        return plugin;
    }
    private BundledPlugin requireEnabled(String id) {
        BundledPlugin plugin = require(id);
        if (!isEnabled(id)) throw new NotFoundException("El plugin está desactivado");
        return plugin;
    }
    private BundledPluginState requireState(String id) {
        return states.findById(id).orElseThrow(() -> new NotFoundException("Estado del plugin no encontrado"));
    }
    private ApiDtos.BundledPluginResponse response(BundledPlugin plugin) {
        BundledPluginState state = states.findById(plugin.id()).orElse(null);
        return new ApiDtos.BundledPluginResponse(plugin.id(), plugin.name(), plugin.version(),
                plugin.description(), plugin.author(), state != null && state.isEnabled(),
                plugin.enabledByDefault(), plugin.capabilities(),
                state == null ? null : state.getUpdatedAt());
    }
    private Map<String, Object> contribution(Map<String, Object> source, BundledPlugin plugin) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source != null) result.putAll(source);
        result.put("pluginId", plugin.id());
        result.put("pluginVersion", plugin.version());
        result.put("extensionName", plugin.name());
        result.put("bundled", true);
        return Collections.unmodifiableMap(result);
    }
}
