package com.nattechnologies.trastcms.plugins.travel;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class TrastTravelPlugin implements BundledPlugin {
    private final TravelService service;

    TrastTravelPlugin(TravelService service) {
        this.service = service;
    }

    @Override public String id() { return "trasttravel"; }
    @Override public String name() { return "TrastTravel"; }
    @Override public String version() { return "1.0.2"; }
    @Override public String description() {
        return "Catálogo turístico, destinos, actividades, salidas, cupos, reservaciones, viajeros y calendario integrado.";
    }
    @Override public boolean enabledByDefault() { return true; }
    @Override public List<String> requiredPlugins() { return List.of("trastpay"); }
    @Override public List<String> optionalPlugins() { return List.of("trastcrm", "trastaccount"); }
    @Override public void onEnabled() { service.initializeDefaults(); }

    @Override
    public List<String> capabilities() {
        return List.of(
                "travel.catalog", "travel.destinations", "travel.activities",
                "travel.departures", "travel.availability", "travel.bookings",
                "travel.travellers", "travel.extras", "travel.calendar",
                "commerce.sellables", "account.sections", "builder.blocks",
                "rest.api", "plugin.events");
    }

    @Override
    public List<String> adminActions() {
        return List.of("demo.restore", "source.restore");
    }

    @Override
    public Map<String, Object> handleAdminAction(String action, Map<String, Object> input) {
        if (!List.of("demo.restore", "source.restore").contains(action)) {
            return BundledPlugin.super.handleAdminAction(action, input);
        }
        TravelDtos.DemoDataResponse result = "source.restore".equals(action)
                ? service.restoreSourceSnapshot()
                : service.generateDemoData();
        return Map.of(
                "destinations", result.destinations(),
                "activities", result.activities(),
                "tours", result.tours(),
                "departures", result.departures(),
                "extras", result.extras(),
                "message", result.message());
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "travel",
                "label", "Tours y reservaciones",
                "icon", "calendar",
                "description", "Catálogo, salidas, viajeros y calendario operativo.",
                "routeName", "travel"));
    }

    @Override
    public List<Map<String, Object>> blocks() {
        return List.of(
                Map.of(
                        "type", "plugin:trasttravel:search",
                        "label", "Buscador de tours",
                        "description", "Buscador por destino, fecha y viajeros.",
                        "category", "Turismo",
                        "icon", "search",
                        "schema", Map.of(
                                "title", Map.of("type", "text", "label", "Título", "default", "Where would you like to go?"),
                                "layout", Map.of("type", "select", "label", "Diseño", "default", "bar", "options", List.of(
                                        Map.of("value", "bar", "label", "Barra horizontal"),
                                        Map.of("value", "columns", "label", "Formulario por columnas"),
                                        Map.of("value", "compact", "label", "Compacto"))),
                                "showDate", Map.of("type", "boolean", "label", "Solicitar fecha", "default", true),
                                "showTravellers", Map.of("type", "boolean", "label", "Solicitar viajeros", "default", true),
                                "showActivity", Map.of("type", "boolean", "label", "Filtrar actividad", "default", true))),
                Map.of(
                        "type", "plugin:trasttravel:tour-grid",
                        "label", "Catálogo de tours",
                        "description", "Cuadrícula responsive de experiencias publicadas.",
                        "category", "Turismo",
                        "icon", "grid",
                        "schema", Map.of(
                                "limit", Map.of("type", "number", "label", "Cantidad", "default", 6),
                                "destination", Map.of("type", "text", "label", "Slug del destino"),
                                "activity", Map.of("type", "text", "label", "Slug de la actividad"))),
                Map.of(
                        "type", "plugin:trasttravel:activities",
                        "label", "Actividades turísticas",
                        "description", "Tarjetas para filtrar el catálogo por actividad.",
                        "category", "Turismo",
                        "icon", "grid",
                        "schema", Map.of(
                                "limit", Map.of("type", "number", "label", "Cantidad", "default", 8))),
                Map.of(
                        "type", "plugin:trasttravel:destinations",
                        "label", "Destinos destacados",
                        "description", "Tarjetas para navegar el catálogo por destino.",
                        "category", "Turismo",
                        "icon", "map",
                        "schema", Map.of(
                                "limit", Map.of("type", "number", "label", "Cantidad", "default", 6))),
                Map.of(
                        "type", "plugin:trasttravel:booking",
                        "label", "Panel de reservación",
                        "description", "Calendario, disponibilidad, viajeros y enlace al checkout.",
                        "category", "Turismo",
                        "icon", "calendar",
                        "schema", Map.of(
                                "tourSlug", Map.of("type", "text", "label", "Slug del tour"))));
    }
}
