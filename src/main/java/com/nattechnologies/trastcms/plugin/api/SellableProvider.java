package com.nattechnologies.trastcms.plugin.api;

import java.math.BigDecimal;
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
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record FulfillmentRequest(
            String orderId,
            String orderNumber,
            String customerId,
            String customerEmail,
            String itemReference,
            int quantity,
            Map<String, Object> metadata) {
        public FulfillmentRequest {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
