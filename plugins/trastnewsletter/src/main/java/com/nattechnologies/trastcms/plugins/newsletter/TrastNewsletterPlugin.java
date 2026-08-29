package com.nattechnologies.trastcms.plugins.newsletter;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.WebPushService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TrastNewsletterPlugin implements BundledPlugin {

    private final WebPushService pushService;

    public TrastNewsletterPlugin(WebPushService pushService) {
        this.pushService = pushService;
    }

    @Override
    public String id() {
        return "trastnewsletter";
    }

    @Override
    public String name() {
        return "TrastNewsletter";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Gestión de boletines por correo electrónico y notificaciones push web integradas.";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public List<String> capabilities() {
        return List.of("newsletter.email", "newsletter.push", "builder.blocks", "public.subscribe");
    }

    @Override
    public void onEvent(PluginEvent event) {
        if (event == null || event.type() == null) return;

        // Auto Push en nuevo contenido o nuevo tour
        if ("travel.tour.published".equals(event.type()) || "post.published".equals(event.type())) {
            Map<String, Object> data = event.payload();
            String title = data != null && data.containsKey("title") ? String.valueOf(data.get("title")) : "Nuevo contenido disponible";
            String url = data != null && data.containsKey("url") ? String.valueOf(data.get("url")) : "/";

            pushService.sendToAll(
                    "📢 " + title,
                    "Visita TrastCMS para ver los últimos detalles.",
                    url,
                    null
            );
        }
    }

    @Override
    public List<Map<String, Object>> blocks() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("title", Map.of("type", "text", "label", "Título", "default", "Suscríbete a nuestro Boletín"));
        schema.put("subtitle", Map.of("type", "text", "label", "Subtítulo", "default", "Recibe las mejores ofertas y noticias en tu correo."));
        schema.put("buttonText", Map.of("type", "text", "label", "Texto Botón", "default", "Suscribirme"));
        schema.put("showPushConsent", Map.of("type", "boolean", "label", "Pedir notificaciones Push", "default", true));

        return List.of(Map.of(
                "type", "newsletter-widget",
                "label", "Formulario Newsletter",
                "description", "Caja de suscripción a boletín y alertas Push.",
                "category", "Marketing",
                "icon", "newsletter",
                "template", "<div class=\"rounded-xl border border-indigo-200 bg-indigo-50 p-6 text-center\">Formulario Newsletter</div>",
                "schema", schema
        ));
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "newsletter",
                "label", "Newsletter & Push",
                "icon", "newsletter",
                "description", "Suscriptores, campañas de correo y notificaciones push.",
                "routeName", "newsletter"
        ));
    }
}
