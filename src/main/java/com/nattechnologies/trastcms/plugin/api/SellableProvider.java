package com.nattechnologies.trastcms.plugin.api;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registers products that can be sold by TrastPay without coupling commerce to a catalog plugin. */
public interface SellableProvider {
    String providerKey();
    String pluginId();
    Optional<SellableItem> resolve(String reference);
    default List<SellableItem> featured(int limit) { return List.of(); }
    default void fulfill(FulfillmentRequest request) { }

    record SellableItem(
            String providerKey,
            String reference,
            String type,
            String name,
            String description,
            BigDecimal unitPrice,
            String currency,
            boolean recurring,
            String billingInterval,
            boolean shippable,
            String imageUrl,
            boolean available,
            Map<String, Object> metadata) {
        public SellableItem {
            metadata = immutableMetadata(metadata);
        }
    }

    record FulfillmentRequest(
            String orderId,
            String orderNumber,
            String customerId,
            String customerEmail,
            String itemReference,
            int quantity,
            BigDecimal lineSubtotal,
            BigDecimal discountAmount,
            BigDecimal lineTotal,
            String couponCode,
            Map<String, Object> metadata) {
        public FulfillmentRequest {
            metadata = immutableMetadata(metadata);
        }
    }

    /**
     * Map.copyOf rejects null keys and values. Plugin metadata frequently
     * contains optional values such as trial days, billing intervals or
     * image URLs, so normalize those values at the SPI boundary instead of
     * letting a NullPointerException escape through the checkout endpoint.
     */
    private static Map<String, Object> immutableMetadata(
            Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                normalized.put(key, value);
            }
        });
        return normalized.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(normalized);
    }
}
