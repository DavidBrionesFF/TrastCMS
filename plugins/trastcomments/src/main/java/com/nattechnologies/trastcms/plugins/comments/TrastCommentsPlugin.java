package com.nattechnologies.trastcms.plugins.comments;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TrastCommentsPlugin implements BundledPlugin {

    @Override
    public String id() {
        return "trastcomments";
    }

    @Override
    public String name() {
        return "TrastComments";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Sistema de comentarios configurable para publicaciones y páginas.";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public List<String> capabilities() {
        return List.of("comments.post", "comments.moderation", "builder.blocks");
    }

    @Override
    public List<Map<String, Object>> blocks() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("showTitle", Map.of("type", "boolean", "label", "Mostrar título", "default", true));
        schema.put("title", Map.of("type", "text", "label", "Título", "default", "Comentarios"));
        schema.put("style", Map.of("type", "select", "label", "Estilo", "default", "default",
                "options", List.of(Map.of("label", "Por defecto", "value", "default"), Map.of("label", "Compacto", "value", "compact"))));

        return List.of(Map.of(
                "type", "comments",
                "label", "Sección de Comentarios",
                "description", "Muestra e integra la caja de comentarios del post.",
                "category", "Contenido",
                "icon", "comment",
                "template", "<div class=\"rounded-xl border border-slate-200 p-6 bg-slate-50\">Sección de Comentarios</div>",
                "schema", schema
        ));
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "comments",
                "label", "Comentarios",
                "icon", "comment",
                "description", "Moderación y configuración de comentarios.",
                "routeName", "comments"
        ));
    }
}
