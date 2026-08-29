package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.site.CmsSite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Executes tenant-owned provisioning only after SiteContext has been switched
 * to the target site. REQUIRES_NEW is intentional: Hibernate resolves the
 * discriminator tenant when the persistence context is created, therefore a
 * transaction opened for another site must never be reused here.
 */
@Service
public class TenantProvisioningOperations {
    private final SiteSettingService settings;
    private final BundledPluginService plugins;
    private final ThemeService themes;

    public TenantProvisioningOperations(SiteSettingService settings,
                                        BundledPluginService plugins,
                                        ThemeService themes) {
        this.settings = settings;
        this.plugins = plugins;
        this.themes = themes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provision(CmsSite site, SiteProvisioningService.Blueprint blueprint, String actor) {
        applySiteDefaults(site, blueprint);
        plugins.configureCurrentSite(blueprint.plugins(), actor);
        themes.activate(blueprint.theme(), actor);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isProvisioned() {
        // V21 seeds a handful of safe defaults for every site, so "has any
        // setting" is not a valid provisioning marker. theme.active is only
        // written when a blueprint/theme has actually been provisioned.
        return !settings.get("theme.active", "").isBlank();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializePlugins() {
        plugins.initializeCurrentSite();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void configurePlugins(Set<String> pluginIds, String actor) {
        plugins.configureCurrentSite(pluginIds, actor);
    }

    private void applySiteDefaults(CmsSite site, SiteProvisioningService.Blueprint blueprint) {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("site.name", site.getName());
        defaults.put("site.tagline", blueprint.tagline());
        defaults.put("site.description", blueprint.description());
        defaults.put("site.locale", site.getLocale());
        defaults.put("site.timezone", site.getTimezone());
        defaults.put("site.suspended", String.valueOf(site.getStatus() == com.nattechnologies.trastcms.domain.site.SiteStatus.SUSPENDED));
        defaults.put("theme.active", blueprint.theme());
        defaults.put("branding.logoUrl", "");
        defaults.put("branding.faviconUrl", "");
        defaults.put("branding.accentColor", blueprint.accent());
        defaults.put("reading.homePageSlug", "inicio");
        defaults.put("reading.postsPerPage", "12");
        defaults.put("reading.showAdminBar", "true");
        defaults.put("reading.dateFormat", "dd/MM/yyyy");
        defaults.put("seo.titleTemplate", "%s · " + site.getName());
        defaults.put("seo.defaultDescription", blueprint.description());
        defaults.put("seo.robotsIndex", "true");
        defaults.put("contact.email", "");
        defaults.put("contact.phone", "");
        defaults.put("contact.address", "");
        defaults.put("contact.city", "");
        defaults.put("contact.country", "Honduras");
        defaults.put("performance.publicCacheSeconds", "300");
        defaults.put("media.maxUploadMb", "500");
        defaults.put("comments.enabled", "false");
        defaults.put("maintenance.enabled", "false");
        defaults.forEach(settings::put);
    }
}
