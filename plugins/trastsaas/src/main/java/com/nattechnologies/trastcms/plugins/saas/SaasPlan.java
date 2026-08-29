package com.nattechnologies.trastcms.plugins.saas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_plan")
class SaasPlan {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id
    @Column(length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    SaasProduct product;

    @Column(name = "plan_key", nullable = false, length = 100)
    String planKey;

    @Column(nullable = false, length = 200)
    String name;

    @Lob
    String description;

    @Column(nullable = false, precision = 19, scale = 4)
    BigDecimal price;

    @Column(nullable = false, length = 3)
    String currency = "USD";

    @Column(name = "billing_interval", nullable = false, length = 20)
    String billingInterval = "MONTH";

    @Column(name = "interval_count", nullable = false)
    int intervalCount = 1;

    @Column(name = "trial_days", nullable = false)
    int trialDays;

    @Column(name = "max_activations", nullable = false)
    int maxActivations = 1;

    @Lob
    @Column(name = "entitlements_json")
    String entitlementsJson;

    @Column(nullable = false)
    boolean active = true;

    @Column(nullable = false)
    boolean featured;

    @Column(name = "demo_data", nullable = false)
    boolean demoData;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }
}
