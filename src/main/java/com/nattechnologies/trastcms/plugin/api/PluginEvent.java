package com.nattechnologies.trastcms.plugin.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PluginEvent(
        String id,
        String type,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static PluginEvent of(String type, Map<String, Object> payload) {
        return new PluginEvent(UUID.randomUUID().toString(), type, Instant.now(), payload);
    }
}
