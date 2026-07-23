package com.nattechnologies.trastcms.plugins.saas;

import com.nattechnologies.trastcms.mcp.McpExtension;
import com.nattechnologies.trastcms.service.BundledPluginService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SaasMcpExtension implements McpExtension {
    private final SaasService service;
    private final BundledPluginService plugins;

    SaasMcpExtension(SaasService service, BundledPluginService plugins) {
        this.service = service;
        this.plugins = plugins;
    }

    @Override
    public boolean enabled() {
        return plugins.isEnabled("trastsaas");
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                tool(
                        "saas_subscription_summary",
                        "Resumen SaaS",
                        "Consulta suscripciones, licencias, activaciones y uso.",
                        Map.of("type", "object", "properties", Map.of())),
                tool(
                        "saas_list_licenses",
                        "Listar licencias",
                        "Lista licencias sin revelar las claves completas.",
                        Map.of("type", "object", "properties", Map.of())));
    }

    @Override
    public Object call(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "saas_subscription_summary" -> service.dashboard();
            case "saas_list_licenses" -> service.licenses();
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
