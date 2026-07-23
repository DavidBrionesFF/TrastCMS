package com.nattechnologies.trastcms.plugins.pay;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TrastPayPlugin implements BundledPlugin {
    private final PayService service;

    TrastPayPlugin(PayService service) {
        this.service = service;
    }

    @Override
    public String id() {
        return "trastpay";
    }

    @Override
    public String name() {
        return "TrastPay";
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
    public String description() {
        return "Carrito, checkout, clientes, órdenes, cupones y pasarelas reutilizables para todos los plugins.";
    }

    @Override
    public List<String> capabilities() {
        return List.of(
                "commerce.cart",
                "commerce.checkout",
                "commerce.orders",
                "commerce.payments",
                "commerce.coupons",
                "commerce.settings",
                "payment.gateways",
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
                "id", "commerce",
                "label", "Comercio y pagos",
                "icon", "cart",
                "description", "Órdenes, pagos, cupones y checkout.",
                "routeName", "commerce"));
    }

    @Override
    public List<Map<String, Object>> blocks() {
        return List.of(
                Map.of(
                        "type", "plugin:trastpay:cart",
                        "label", "Carrito",
                        "description", "Resumen interactivo del carrito.",
                        "category", "Comercio",
                        "icon", "cart",
                        "schema", Map.of(
                                "style", Map.of(
                                        "type", "select",
                                        "label", "Estilo",
                                        "default", "card",
                                        "options", List.of("card", "drawer", "minimal")))),
                Map.of(
                        "type", "plugin:trastpay:checkout",
                        "label", "Checkout",
                        "description", "Formulario seguro de compra.",
                        "category", "Comercio",
                        "icon", "payment",
                        "schema", Map.of(
                                "showCoupon", Map.of(
                                        "type", "boolean",
                                        "label", "Mostrar cupón",
                                        "default", true))));
    }
}
