package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.category.Category;
import com.nattechnologies.trastcms.domain.category.CategoryRepository;
import com.nattechnologies.trastcms.domain.site.CmsSite;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembership;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembershipRepository;
import com.nattechnologies.trastcms.domain.site.CmsSiteRepository;
import com.nattechnologies.trastcms.domain.user.*;
import com.nattechnologies.trastcms.multisite.SiteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Order(10)
public class BootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);
    private final TrastCmsProperties properties;
    private final UserAccountRepository users;
    private final CategoryRepository categories;
    private final PasswordEncoder passwordEncoder;
    private final SlugService slugService;
    private final ThemeStarterContentService starterContent;
    private final SiteSettingService settings;
    private final CmsSiteRepository sites;
    private final CmsSiteMembershipRepository memberships;

    public BootstrapService(
            TrastCmsProperties properties,
            UserAccountRepository users,
            CategoryRepository categories,
            PasswordEncoder passwordEncoder,
            SlugService slugService,
            ThemeStarterContentService starterContent,
            SiteSettingService settings,
            CmsSiteRepository sites,
            CmsSiteMembershipRepository memberships) {
        this.properties = properties;
        this.users = users;
        this.categories = categories;
        this.passwordEncoder = passwordEncoder;
        this.slugService = slugService;
        this.starterContent = starterContent;
        this.settings = settings;
        this.sites = sites;
        this.memberships = memberships;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if ("change-this-plugin-master-key-before-production".equals(properties.getPlugins().getMasterKey())) {
            log.warn("TRASTCMS_PLUGIN_MASTER_KEY conserva el valor de desarrollo. Cámbielo antes de registrar plugins en producción.");
        }

        UserAccount admin = users.findByEmailIgnoreCase(properties.getAdmin().getEmail()).orElseGet(() -> {
            UserAccount created = new UserAccount();
            created.setEmail(properties.getAdmin().getEmail().trim().toLowerCase());
            created.setDisplayName(properties.getAdmin().getName());
            created.setPasswordHash(passwordEncoder.encode(properties.getAdmin().getPassword()));
            created.setRole(UserRole.ADMIN);
            created.setEnabled(true);
            created.setNetworkAdmin(true);
            UserAccount saved = users.save(created);
            log.warn("Se creó el administrador inicial {}. Cambie TRASTCMS_ADMIN_PASSWORD en producción.", saved.getEmail());
            return saved;
        });
        if (!admin.isNetworkAdmin()) {
            admin.setNetworkAdmin(true);
            users.save(admin);
        }

        CmsSite primarySite = sites.findById(SiteContext.DEFAULT_SITE_ID)
                .orElseThrow(() -> new IllegalStateException("No existe el sitio inicial de TrastCMS"));
        memberships.findBySiteIdAndUserId(primarySite.getId(), admin.getId()).orElseGet(() -> {
            CmsSiteMembership membership = new CmsSiteMembership();
            membership.setSite(primarySite);
            membership.setUser(admin);
            membership.setRole(UserRole.ADMIN);
            membership.setEnabled(true);
            return memberships.save(membership);
        });

        if (categories.count() == 0) {
            Category general = new Category();
            general.setName("General");
            general.setSlug(slugService.slugify("General"));
            general.setDescription("Categoría general de TrastCMS");
            categories.save(general);
        }

        Map<String, String> defaults = Map.ofEntries(
            Map.entry("site.name", "TrastCMS"),
            Map.entry("site.tagline", "Contenido y experiencias digitales en un solo lugar"),
            Map.entry("site.description", "Sitio creado con TrastCMS."),
            Map.entry("site.locale", "es"),
            Map.entry("site.timezone", "UTC"),
            Map.entry("theme.active", "nexus"),
            Map.entry("branding.logoUrl", ""),
            Map.entry("branding.faviconUrl", ""),
            Map.entry("branding.accentColor", "#5142d8"),
            Map.entry("reading.homePageSlug", "inicio"),
            Map.entry("reading.postsPerPage", "12"),
            Map.entry("reading.showAdminBar", "true"),
            Map.entry("reading.dateFormat", "dd/MM/yyyy"),
            Map.entry("seo.titleTemplate", "%s · TrastCMS"),
            Map.entry("seo.defaultDescription", "Sitio creado con TrastCMS."),
            Map.entry("seo.robotsIndex", "true"),
            Map.entry("contact.email", ""),
            Map.entry("contact.phone", ""),
            Map.entry("contact.address", ""),
            Map.entry("contact.city", ""),
            Map.entry("contact.country", ""),
            Map.entry("performance.publicCacheSeconds", "300"),
            Map.entry("media.maxUploadMb", "500"),
            Map.entry("comments.enabled", "false"),
            Map.entry("maintenance.enabled", "false")
        );
        Map<String, String> current = settings.all();
        boolean legacyAcademyWasGlobalSite = "nexus".equalsIgnoreCase(current.getOrDefault("theme.active", ""));
        if (legacyAcademyWasGlobalSite) {
            // Before multisite, activating Academy Lab replaced the one global site.
            // Academy now has its own tenant, so restore the original Primary Site identity
            // while MultisiteBootstrapService provisions Academy independently.
            defaults.forEach(settings::put);
            current = settings.all();
        } else {
            Map<String, String> existing = current;
            defaults.forEach((key, value) -> { if (!existing.containsKey(key)) settings.put(key, value); });
        }
        String activeTheme = settings.get("theme.active", defaults.get("theme.active"));
        starterContent.apply(activeTheme, null, admin.getEmail());
    }
}
