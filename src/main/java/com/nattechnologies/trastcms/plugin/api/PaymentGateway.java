package com.nattechnologies.trastcms.plugin.api;

import java.math.BigDecimal;
import java.util.Map;

/** Payment adapter contract used by TrastPay. Providers never receive raw card details. */
public interface PaymentGateway {
    String key();
    String name();
    default String description() { return ""; }
    default boolean supportsRecurring() { return false; }
    default boolean supportsRefunds() { return false; }
    default boolean enabled() { return true; }
    PaymentResult create(PaymentRequest request);
    default PaymentResult capture(String externalReference, BigDecimal amount) {
        throw new UnsupportedOperationException("Captura no soportada");
    }
    default PaymentResult refund(String externalReference, BigDecimal amount, String reason) {
        throw new UnsupportedOperationException("Reembolso no soportado");
    }

    record PaymentRequest(String orderId, String orderNumber, BigDecimal amount,
                          String currency, String customerEmail, String returnUrl,
                          String cancelUrl, String idempotencyKey,
                          Map<String,Object> metadata) { }
    record PaymentResult(String status, String externalReference, String redirectUrl,
                         String message, Map<String,Object> metadata) {
        public PaymentResult {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
