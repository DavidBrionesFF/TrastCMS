package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.setting.SiteSetting;
import com.nattechnologies.trastcms.domain.setting.SiteSettingRepository;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class SiteSettingService {
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Map<String, SettingRule> RULES = rules();

    private final SiteSettingRepository repository;
    private final AuditService audit;

    public SiteSettingService(SiteSettingRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Map<String, String> all() {
        Map<String, String> result = new TreeMap<>();
        repository.findAll().forEach(setting -> result.put(setting.getKey(), setting.getValue()));
        return result;
    }

    @Transactional(readOnly = true)
    public String get(String key, String fallback) {
        return repository.findById(key).map(SiteSetting::getValue).orElse(fallback);
    }

    public boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, Boolean.toString(fallback)));
    }

    @Transactional
    public ApiDtos.SettingsResponse update(Map<String, String> values, String actor) {
        if (values == null || values.isEmpty()) throw new BadRequestException("No se recibieron configuraciones");
        values.forEach((key, raw) -> {
            SettingRule rule = RULES.get(key);
            if (rule == null) throw new BadRequestException("Configuración no permitida: " + key);
            String value = rule.normalize(key, raw);
            SiteSetting setting = repository.findById(key).orElseGet(() -> new SiteSetting(key, ""));
            setting.setValue(value);
            repository.save(setting);
        });
        audit.record(actor, "settings.updated", "settings", null, String.join(",", values.keySet()));
        return new ApiDtos.SettingsResponse(all());
    }

    @Transactional
    public void put(String key, String value) {
        SiteSetting setting = repository.findById(key).orElseGet(() -> new SiteSetting(key, value));
        setting.setValue(value == null ? "" : value);
        repository.save(setting);
    }

    @Transactional(readOnly = true)
    public ApiDtos.SiteInfo siteInfo() {
        return new ApiDtos.SiteInfo(
                get("site.name", "TrastCMS"),
                get("site.tagline", ""),
                get("site.description", ""),
                get("site.locale", "es-HN"),
                get("theme.active", "aurora"),
                get("branding.logoUrl", ""),
                get("reading.homePageSlug", "inicio"),
                getBoolean("reading.showAdminBar", true),
                get("contact.email", ""),
                get("contact.phone", ""));
    }

    private static Map<String, SettingRule> rules() {
        Map<String, SettingRule> result = new LinkedHashMap<>();
        text(result, 160, "site.name", "site.tagline", "contact.phone", "contact.address",
                "contact.city", "contact.country", "seo.titleTemplate", "reading.dateFormat");
        text(result, 2000, "site.description", "seo.defaultDescription", "contact.schedule");
        text(result, 600, "branding.logoUrl", "branding.faviconUrl", "contact.email",
                "contact.facebook", "contact.instagram", "contact.linkedin", "contact.youtube");
        result.put("site.locale", (key, value) -> match(key, value, "[a-z]{2}(?:-(?:[A-Z]{2}|[0-9]{3}))?", 20));
        result.put("site.timezone", (key, value) -> match(key, value, "(?:UTC|[A-Za-z_]+(?:/[A-Za-z_+-]+)+)", 80));
        result.put("branding.accentColor", (key, value) -> {
            String normalized = trim(value, 20);
            if (!COLOR.matcher(normalized).matches()) throw new BadRequestException("Color inválido para " + key);
            return normalized.toLowerCase(Locale.ROOT);
        });
        result.put("reading.homePageSlug", (key, value) -> match(key, value, "[a-z0-9][a-z0-9-]{1,279}", 280));
        integer(result, 1, 100, "reading.postsPerPage");
        integer(result, 0, 86400, "performance.publicCacheSeconds");
        integer(result, 1, 500, "media.maxUploadMb");
        bool(result, "reading.showAdminBar", "seo.robotsIndex", "comments.enabled", "maintenance.enabled");
        return Collections.unmodifiableMap(result);
    }

    private static void text(Map<String, SettingRule> map, int max, String... keys) {
        for (String key : keys) map.put(key, (name, value) -> trim(value, max));
    }
    private static void integer(Map<String, SettingRule> map, int min, int max, String... keys) {
        for (String key : keys) map.put(key, (name, value) -> {
            try {
                int number = Integer.parseInt(trim(value, 20));
                if (number < min || number > max) throw new NumberFormatException();
                return Integer.toString(number);
            } catch (NumberFormatException exception) {
                throw new BadRequestException("Valor numérico inválido para " + name);
            }
        });
    }
    private static void bool(Map<String, SettingRule> map, String... keys) {
        for (String key : keys) map.put(key, (name, value) -> {
            String normalized = trim(value, 10);
            if (!normalized.equals("true") && !normalized.equals("false"))
                throw new BadRequestException("Valor booleano inválido para " + name);
            return normalized;
        });
    }
    private static String match(String key, String raw, String regex, int max) {
        String value = trim(raw, max);
        if (!value.matches(regex)) throw new BadRequestException("Valor inválido para " + key);
        return value;
    }
    private static String trim(String value, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > max) throw new BadRequestException("La configuración supera " + max + " caracteres");
        return normalized;
    }
    @FunctionalInterface private interface SettingRule { String normalize(String key, String value); }
}
