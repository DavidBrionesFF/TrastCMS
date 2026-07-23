package com.nattechnologies.trastcms.plugins.pay;

enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    PAID,
    ON_HOLD,
    PARTIALLY_REFUNDED,
    REFUNDED,
    CANCELLED,
    FAILED,
    COMPLETED
}
