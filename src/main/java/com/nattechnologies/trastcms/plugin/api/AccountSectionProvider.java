package com.nattechnologies.trastcms.plugin.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contributes a section to the customer-facing "Mi Cuenta" portal without
 * coupling the account plugin to the database model of commerce, SaaS, travel
 * or future plugins.
 */
public interface AccountSectionProvider {
    String key();
    String pluginId();
    String label();
    default String icon() { return "plugin"; }
    default int order() { return 100; }
    AccountSection load(AccountContext context);

    record AccountContext(String userId, String email, String displayName) { }

    record AccountSection(
            String key,
            String pluginId,
            String label,
            String icon,
            int order,
            String status,
            String emptyMessage,
            List<Map<String, Object>> items,
            Map<String, Object> summary) {
        public AccountSection {
            items = items == null ? List.of() : List.copyOf(items);
            summary = immutable(summary);
        }

        private static Map<String, Object> immutable(Map<String, Object> source) {
            if (source == null || source.isEmpty()) return Map.of();
            Map<String, Object> normalized = new LinkedHashMap<>();
            source.forEach((key, value) -> {
                if (key != null && value != null) normalized.put(key, value);
            });
            return normalized.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(normalized);
        }
    }
}
