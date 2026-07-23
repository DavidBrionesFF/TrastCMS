package com.nattechnologies.trastcms.plugins.store;

import com.nattechnologies.trastcms.mcp.McpExtension;
import com.nattechnologies.trastcms.service.BundledPluginService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class StoreMcpExtension implements McpExtension {
    private final StoreService service;
    private final BundledPluginService plugins;

    StoreMcpExtension(StoreService service, BundledPluginService plugins) {
        this.service = service;
        this.plugins = plugins;
    }

    @Override
    public boolean enabled() {
        return plugins.isEnabled("traststore");
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                tool(
                        "store_inventory_alerts",
                        "Alertas de inventario",
                        "Consulta productos agotados o con existencia baja.",
                        Map.of("type", "object", "properties", Map.of())),
                tool(
                        "store_product_lookup",
                        "Buscar productos",
                        "Busca productos publicados en el catálogo.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "search", Map.of(
                                                "type", "string",
                                                "description", "Texto libre para buscar por nombre, SKU o categoría.")))));
    }

    @Override
    public Object call(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "store_inventory_alerts" -> service.dashboard();
            case "store_product_lookup" -> service.catalog(
                    Objects.toString(arguments.get("search"), ""),
                    0,
                    25);
            default -> throw new IllegalArgumentException("Herramienta desconocida: " + toolName);
        };
    }

    private McpToolDefinition tool(
            String name,
            String title,
            String description,
            Map<String, Object> schema) {
        return new McpToolDefinition(
                name,
                title,
                description,
                schema,
                true,
                false,
                true);
    }
}
