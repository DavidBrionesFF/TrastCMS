package com.nattechnologies.trastcms.plugins.pay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "commerce_clinpays_webhook_event")
class ClinpaysWebhookEvent {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id
    @Column(name = "webhook_id", length = 190)
    String webhookId;

    @Column(name = "event_type", length = 120)
    String eventType;

    @Column(name = "order_reference", length = 80)
    String orderReference;

    @Column(name = "payment_reference", length = 190)
    String paymentReference;

    @Column(name = "processed_at", nullable = false)
    Instant processedAt;

    @PrePersist
    void create() {
        processedAt = Instant.now();
    }
}
