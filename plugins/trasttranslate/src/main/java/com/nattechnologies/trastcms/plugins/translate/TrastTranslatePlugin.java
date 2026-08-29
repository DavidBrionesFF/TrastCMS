package com.nattechnologies.trastcms.plugins.translate;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TrastTranslatePlugin implements BundledPlugin {
    private final TranslationService service;

    TrastTranslatePlugin(TranslationService service) {
        this.service = service;
    }

    @Override public String id() { return "trasttranslate"; }
    @Override public String name() { return "TrastTranslate"; }
    @Override public String version() { return "1.0.0"; }
    @Override public boolean enabledByDefault() { return true; }
    @Override public String description() {
        return "Selector multilingüe para el sitio público con Google Translate, posiciones configurables y bloque reutilizable.";
    }
    @Override public List<String> capabilities() {
        return List.of("translation.website", "translation.google", "translation.selector", "builder.blocks", "rest.api");
    }
    @Override public void onEnabled() { service.initializeDefaults(); }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "translation",
                "label", "Traducción web",
                "icon", "language",
                "description", "Idiomas, proveedor y ubicación del selector público.",
                "routeName", "translation"));
    }

    @Override
    public List<Map<String, Object>> blocks() {
        return List.of(Map.of(
                "type", "plugin:trasttranslate:selector",
                "label", "Selector de idiomas",
                "description", "Inserta el selector de Google Translate dentro de cualquier página.",
                "category", "Traducción",
                "icon", "language",
                "schema", Map.of(
                        "style", Map.of(
                                "type", "select",
                                "label", "Estilo",
                                "default", "inherit",
                                "options", List.of(
                                        Map.of("value", "inherit", "label", "Usar configuración del plugin"),
                                        Map.of("value", "compact", "label", "Compacto"),
                                        Map.of("value", "dropdown", "label", "Desplegable"),
                                        Map.of("value", "buttons", "label", "Botones"))),
                        "label", Map.of("type", "text", "label", "Etiqueta opcional", "default", "Language"))));
    }
}
