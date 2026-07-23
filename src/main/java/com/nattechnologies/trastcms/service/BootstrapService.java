package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.category.Category;
import com.nattechnologies.trastcms.domain.category.CategoryRepository;
import com.nattechnologies.trastcms.domain.setting.SiteSetting;
import com.nattechnologies.trastcms.domain.setting.SiteSettingRepository;
import com.nattechnologies.trastcms.domain.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Order(10)
public class BootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);
    private final TrastCmsProperties properties;
    private final UserAccountRepository users;
    private final CategoryRepository categories;
    private final SiteSettingRepository settings;
    private final PasswordEncoder passwordEncoder;
    private final SlugService slugService;
    private final ThemeStarterContentService starterContent;

    public BootstrapService(TrastCmsProperties properties, UserAccountRepository users,
                            CategoryRepository categories, SiteSettingRepository settings,
                            PasswordEncoder passwordEncoder, SlugService slugService,
                            ThemeStarterContentService starterContent) {
        this.properties = properties;
        this.users = users;
        this.categories = categories;
        this.settings = settings;
        this.passwordEncoder = passwordEncoder;
        this.slugService = slugService;
        this.starterContent = starterContent;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if ("change-this-plugin-master-key-before-production".equals(properties.getPlugins().getMasterKey())) {
            log.warn("TRASTCMS_PLUGIN_MASTER_KEY conserva el valor de desarrollo. Cámbielo antes de registrar plugins en producción.");
        }
        if (!users.existsByEmailIgnoreCase(properties.getAdmin().getEmail())) {
            UserAccount admin = new UserAccount();
            admin.setEmail(properties.getAdmin().getEmail().trim().toLowerCase());
            admin.setDisplayName(properties.getAdmin().getName());
            admin.setPasswordHash(passwordEncoder.encode(properties.getAdmin().getPassword()));
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            users.save(admin);
            log.warn("Se creó el administrador inicial {}. Cambie TRASTCMS_ADMIN_PASSWORD en producción.", admin.getEmail());
        }

        if (categories.count() == 0) {
            Category general = new Category();
            general.setName("General");
            general.setSlug(slugService.slugify("General"));
            general.setDescription("Categoría general de TrastCMS");
            categories.save(general);
        }

        Map<String, String> defaults = Map.ofEntries(
            Map.entry("site.name", "TrastCMS"),
            Map.entry("site.tagline", "Contenido rápido, moderno y bajo su control"),
            Map.entry("site.description", "Un CMS open source construido con Spring Boot y Vue."),
            Map.entry("site.locale", "es-HN"),
            Map.entry("site.timezone", "America/Tegucigalpa"),
            Map.entry("theme.active", "aurora"),
            Map.entry("branding.logoUrl", ""),
            Map.entry("branding.faviconUrl", ""),
            Map.entry("branding.accentColor", "#6d4aff"),
            Map.entry("reading.homePageSlug", "inicio"),
            Map.entry("reading.postsPerPage", "12"),
            Map.entry("reading.showAdminBar", "true"),
            Map.entry("reading.dateFormat", "dd/MM/yyyy"),
            Map.entry("seo.titleTemplate", "%s · TrastCMS"),
            Map.entry("seo.defaultDescription", "Construya y gestione sitios rápidos con TrastCMS."),
            Map.entry("seo.robotsIndex", "true"),
            Map.entry("contact.email", "hola@trastcms.local"),
            Map.entry("contact.phone", "+504 0000-0000"),
            Map.entry("contact.address", "Honduras"),
            Map.entry("contact.city", "Tegucigalpa"),
            Map.entry("contact.country", "Honduras"),
            Map.entry("performance.publicCacheSeconds", "300"),
            Map.entry("media.maxUploadMb", "100"),
            Map.entry("comments.enabled", "false"),
            Map.entry("maintenance.enabled", "false")
        );
        defaults.forEach((key, value) -> {
            if (!settings.existsById(key)) settings.save(new SiteSetting(key, value));
        });
        starterContent.apply(
                defaults.get("theme.active"),
                null,
                properties.getAdmin().getEmail().trim().toLowerCase());
    }
}
