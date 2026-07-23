package com.nattechnologies.trastcms.plugins.store;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TrastStorePlugin implements BundledPlugin {
    private final StoreService service;

    TrastStorePlugin(StoreService service) {
        this.service = service;
    }

    @Override
    public String id() {
        return "traststore";
    }

    @Override
    public String name() {
        return "TrastStore";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public List<String> requiredPlugins() {
        return List.of("trastpay");
    }

    @Override
    public String description() {
        return "Tienda online con catálogo, variantes, productos físicos y digitales, inventario y fulfillment.";
    }

    @Override
    public List<String> capabilities() {
        return List.of(
                "store.catalog",
                "store.products",
                "store.variants",
                "store.inventory",
                "store.digital",
                "commerce.sellables",
                "builder.blocks",
                "mcp.tools");
    }

    @Override
    public void onEnabled() {
        service.initializeDefaults();
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "store",
                "label", "Tienda online",
                "icon", "store",
                "description", "Catálogo, productos e inventario.",
                "routeName", "store"));
    }

    @Override
    public List<Map<String, Object>> blocks() {
        return List.of(
                Map.of(
                        "type", "plugin:traststore:grid",
                        "label", "Productos de la tienda",
                        "description", "Cuadrícula responsive del catálogo.",
                        "category", "Tienda",
                        "icon", "store",
                        "schema", Map.of(
                                "limit", Map.of(
                                        "type", "number",
                                        "label", "Cantidad",
                                        "default", 8),
                                "columns", Map.of(
                                        "type", "select",
                                        "label", "Columnas",
                                        "default", "4",
                                        "options", List.of("2", "3", "4")),
                                "category", Map.of(
                                        "type", "text",
                                        "label", "Categoría"))),
                Map.of(
                        "type", "plugin:traststore:product",
                        "label", "Producto",
                        "description", "Ficha y botón para agregar al carrito.",
                        "category", "Tienda",
                        "icon", "package",
                        "schema", Map.of(
                                "productSlug", Map.of(
                                        "type", "select",
                                        "label", "Producto",
                                        "optionsEndpoint", "/api/admin/store/products?size=100",
                                        "optionsLabel", "name",
                                        "optionsValue", "slug"))));
    }
}
