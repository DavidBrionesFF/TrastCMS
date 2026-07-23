package com.nattechnologies.trastcms.plugins.pay;

import com.nattechnologies.trastcms.mcp.McpExtension;
import com.nattechnologies.trastcms.service.BundledPluginService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PayMcpExtension implements McpExtension {
    private final PayService service;
    private final BundledPluginService plugins;

    PayMcpExtension(PayService service, BundledPluginService plugins) {
        this.service = service;
        this.plugins = plugins;
    }

    @Override
    public boolean enabled() {
        return plugins.isEnabled("trastpay");
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                tool(
                        "commerce_order_summary",
                        "Resumen de comercio",
                        "Consulta ingresos, pagos y estados de órdenes.",
                        Map.of("type", "object", "properties", Map.of())),
                tool(
                        "commerce_list_orders",
                        "Listar órdenes",
                        "Lista las órdenes recientes con paginación.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "page", Map.of("type", "integer", "minimum", 0),
                                        "size", Map.of("type", "integer", "minimum", 1, "maximum", 100)))));
    }

    @Override
    public Object call(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "commerce_order_summary" -> service.dashboard();
            case "commerce_list_orders" -> service.orders(
                    integer(arguments.get("page"), 0),
                    integer(arguments.get("size"), 25));
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

    private int integer(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
