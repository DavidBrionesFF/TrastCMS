package com.nattechnologies.trastcms.multisite;

import java.util.Optional;
import java.util.function.Supplier;

public final class SiteContext {
    public static final String PRIMARY_SITE_ID = "11111111-1111-1111-1111-111111111111";
    public static final String ACADEMY_SITE_ID = "22222222-2222-2222-2222-222222222222";
    public static final String SAAS_SITE_ID = "33333333-3333-3333-3333-333333333333";
    public static final String DEFAULT_SITE_ID = PRIMARY_SITE_ID;

    private static final ThreadLocal<ResolvedSite> CURRENT = new ThreadLocal<>();

    private SiteContext() {}

    public static void set(ResolvedSite site) { CURRENT.set(site); }
    public static Optional<ResolvedSite> current() { return Optional.ofNullable(CURRENT.get()); }
    public static String siteId() { return current().map(ResolvedSite::id).orElse(DEFAULT_SITE_ID); }
    public static String siteKey() { return current().map(ResolvedSite::siteKey).orElse("primary-site"); }
    public static String siteName() { return current().map(ResolvedSite::name).orElse("Primary Site"); }
    public static void clear() { CURRENT.remove(); }

    public static <T> T withSite(ResolvedSite site, Supplier<T> action) {
        ResolvedSite previous = CURRENT.get();
        try {
            CURRENT.set(site);
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    public static void withSite(ResolvedSite site, Runnable action) {
        withSite(site, () -> { action.run(); return null; });
    }

    public record ResolvedSite(String id, String siteKey, String name) {}
}
