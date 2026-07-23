package com.nattechnologies.trastcms.example;

import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.plugin.api.TrastCmsExtension;
import org.pf4j.Extension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Extension
public final class HelloExtension implements TrastCmsExtension {
    @Override
    public String name() {
        return "Hello Java Plugin";
    }

    @Override
    public String description() {
        return "Ejemplo de bloques, menús, formularios y listeners para el SDK de TrastCMS.";
    }

    @Override
    public List<String> capabilities() {
        return List.of(
                "content.read",
                "builder.blocks.register",
                "admin.menu.register",
                "admin.actions.execute");
    }

    @Override
    public List<Map<String, Object>> blocks() {
        return List.of(Map.of(
                "type", "hello-card",
                "label", "Tarjeta Hello",
                "category", "Plugins",
                "icon", "plugin",
                "template", "<article class=\"plugin-hello-card\"><h3>{{title}}</h3><p>{{message}}</p></article>",
                "schema", Map.of(
                        "title", Map.of(
                                "type", "text",
                                "label", "Título",
                                "default", "Hola TrastCMS"),
                        "message", Map.of(
                                "type", "textarea",
                                "label", "Mensaje",
                                "default", "Bloque registrado por un plugin Java"))));
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "hello-java",
                "label", "Hello Plugin",
                "icon", "plugin",
                "description", "Panel declarativo renderizado dentro de la administración de TrastCMS.",
                "sections", List.of(
                        Map.of(
                                "type", "notice",
                                "tone", "success",
                                "title", "Plugin iniciado correctamente",
                                "description", "PF4J cargó la extensión y TrastCMS generó esta pantalla sin acoplar el plugin al frontend principal."),
                        Map.of(
                                "type", "stats",
                                "items", List.of(
                                        Map.of("label", "Estado", "value", "Activo"),
                                        Map.of("label", "Runtime", "value", "JVM"),
                                        Map.of("label", "Bloques", "value", "1"),
                                        Map.of("label", "Acciones", "value", "1"))),
                        Map.of(
                                "id", "hello-settings",
                                "type", "form",
                                "title", "Configuración de ejemplo",
                                "description", "El formulario invoca una acción Java autenticada del plugin.",
                                "action", "hello.save",
                                "submitLabel", "Guardar en el plugin",
                                "fields", List.of(
                                        Map.of(
                                                "name", "greeting",
                                                "label", "Saludo",
                                                "type", "text",
                                                "required", true,
                                                "default", "Hola desde PF4J"),
                                        Map.of(
                                                "name", "accent",
                                                "label", "Color",
                                                "type", "color",
                                                "default", "#6d4aff"),
                                        Map.of(
                                                "name", "enabled",
                                                "label", "Estado",
                                                "type", "boolean",
                                                "default", true))))));
    }

    @Override
    public List<String> adminActions() {
        return List.of("hello.save");
    }

    @Override
    public Map<String, Object> handleAdminAction(
            String action,
            Map<String, Object> input) {
        if (!"hello.save".equals(action)) {
            throw new IllegalArgumentException("Acción no reconocida");
        }
        return Map.of(
                "message", "La configuración fue recibida por el plugin",
                "savedAt", Instant.now().toString(),
                "values", input);
    }

    @Override
    public void onEvent(PluginEvent event) {
        System.out.printf("[hello-java] %s %s%n", event.type(), event.payload());
    }
}
