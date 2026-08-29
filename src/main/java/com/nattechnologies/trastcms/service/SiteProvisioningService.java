package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.site.CmsSite;
import com.nattechnologies.trastcms.multisite.SiteContext;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/** Coordinates provisioning so the tenant context is selected before a new
 * persistence transaction is opened by TenantProvisioningOperations. */
@Service
public class SiteProvisioningService {
    private final TenantProvisioningOperations operations;

    public SiteProvisioningService(TenantProvisioningOperations operations) {
        this.operations = operations;
    }

    public void provision(CmsSite site, String blueprint, String actor) {
        Blueprint selected = Blueprint.from(blueprint);
        SiteContext.withSite(resolved(site), () -> operations.provision(site, selected, actor));
    }

    public void provisionIfEmpty(CmsSite site, String blueprint, String actor) {
        SiteContext.withSite(resolved(site), () -> {
            if (!operations.isProvisioned()) operations.provision(site, Blueprint.from(blueprint), actor);
            else operations.initializePlugins();
        });
    }

    public void configurePlugins(CmsSite site, Set<String> pluginIds, String actor) {
        SiteContext.withSite(resolved(site), () -> operations.configurePlugins(pluginIds, actor));
    }

    private SiteContext.ResolvedSite resolved(CmsSite site) {
        return new SiteContext.ResolvedSite(site.getId(), site.getSiteKey(), site.getName());
    }

    public record Blueprint(String id, String theme, Set<String> plugins, String tagline, String description, String accent) {
        static Blueprint from(String raw) {
            String id = raw == null || raw.isBlank() ? "EMPTY" : raw.trim().toUpperCase(Locale.ROOT);
            return switch (id) {
                case "ACADEMY" -> new Blueprint("ACADEMY", "nexus",
                        Set.of("trastacademy", "trastpay", "trastaccount", "trasttranslate", "trastcrm"),
                        "Formación práctica que se convierte en resultados",
                        "Cursos, lecciones en video, evaluaciones y seguimiento de progreso.", "#3157d5");
                case "SAAS", "STORE_SAAS" -> new Blueprint("SAAS", "nexus",
                        Set.of("traststore", "trastsaas", "trastpay", "trastaccount", "trasttranslate", "trastnewsletter", "trastcrm"),
                        "Software, licencias y suscripciones en un solo lugar",
                        "Tienda digital para vender software, planes SaaS, licencias y renovaciones.", "#635bff");
                case "TRAVEL" -> new Blueprint("TRAVEL", "aurora",
                        Set.of("trasttravel", "trastpay", "trastaccount", "trasttranslate", "trastcrm"),
                        "Experiencias que comienzan antes del viaje",
                        "Catálogo de tours, reservaciones, pagos y atención al viajero.", "#087f7b");
                default -> new Blueprint("EMPTY", "nexus", Set.of("trastaccount", "trasttranslate"),
                        "Su nuevo sitio con TrastCMS", "Un sitio modular listo para crecer con temas y plugins.", "#5b3df5");
            };
        }
    }
}
