package com.nattechnologies.trastcms.plugins.crm;

import com.nattechnologies.trastcms.mcp.McpExtension;
import com.nattechnologies.trastcms.service.BundledPluginService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CrmMcpExtension implements McpExtension {
    private final CrmService crm;
    private final BundledPluginService bundledPlugins;

    public CrmMcpExtension(
            CrmService crm,
            BundledPluginService bundledPlugins) {
        this.crm = crm;
        this.bundledPlugins = bundledPlugins;
    }

    @Override
    public int order() { return 50; }

    @Override
    public boolean enabled() {
        return bundledPlugins.isEnabled("trastcrm");
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                tool("crm_pipeline_summary", "Resumen del pipeline",
                        "Devuelve métricas, etapas, valor abierto y actividad comercial de TrastCRM.",
                        objectSchema(Map.of(), List.of())),
                tool("crm_list_leads", "Listar leads",
                        "Lista contactos del CRM con filtros de búsqueda y estado.",
                        objectSchema(Map.of(
                                "search", stringProperty("Texto para buscar por nombre, correo o empresa"),
                                "status", Map.of(
                                        "type", "string",
                                        "enum", List.of("LEAD", "PROSPECT", "CUSTOMER", "INACTIVE")),
                                "page", integerProperty(0, 100000),
                                "size", integerProperty(1, 100)), List.of())),
                tool("crm_list_submissions", "Listar envíos de formularios",
                        "Devuelve los envíos recientes recibidos desde formularios públicos.",
                        objectSchema(Map.of(
                                "page", integerProperty(0, 100000),
                                "size", integerProperty(1, 100)), List.of())));
    }

    @Override
    public Object call(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "crm_pipeline_summary" -> crm.dashboard();
            case "crm_list_leads" -> crm.contacts(
                    text(arguments.get("search")),
                    status(arguments.get("status")),
                    integer(arguments, "page", 0),
                    integer(arguments, "size", 25));
            case "crm_list_submissions" -> crm.submissions(
                    integer(arguments, "page", 0),
                    integer(arguments, "size", 25));
            default -> throw new IllegalArgumentException(
                    "Herramienta MCP de TrastCRM desconocida: " + toolName);
        };
    }

    private CrmContactStatus status(Object value) {
        String normalized = text(value);
        if (normalized == null || normalized.isBlank()) return null;
        try {
            return CrmContactStatus.valueOf(
                    normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Estado CRM inválido");
        }
    }

    private int integer(
            Map<String, Object> arguments,
            String key,
            int fallback) {
        Object raw = arguments.get(key);
        if (raw == null) return fallback;
        if (raw instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(raw)); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " debe ser numérico");
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
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

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, Object> stringProperty(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> integerProperty(int minimum, int maximum) {
        return Map.of(
                "type", "integer",
                "minimum", minimum,
                "maximum", maximum);
    }
}
