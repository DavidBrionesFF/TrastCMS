package com.nattechnologies.trastcms.plugins.saas;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_product")
class SaasProduct {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id
    @Column(length = 36)
    String id;

    @Column(name = "product_key", nullable = false, length = 100)
    String productKey;

    @Column(nullable = false, length = 200)
    String name;

    @Lob
    String description;

    @Column(name = "logo_url", length = 1000)
    String logoUrl;

    @Column(nullable = false)
    boolean active = true;

    @Column(name = "demo_data", nullable = false)
    boolean demoData;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = Instant.now();
    }
}
