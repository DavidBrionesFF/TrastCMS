package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "travel_extra")
class TravelExtra {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(nullable = false, length = 180) String name;
    @Column(nullable = false, length = 180) String slug;
    @Column(length = 600) String description;
    @Column(nullable = false, precision = 19, scale = 4) BigDecimal price;
    @Column(nullable = false, length = 3) String currency = "USD";
    @Column(name = "pricing_unit", nullable = false, length = 30) String pricingUnit = "PER_BOOKING";
    @Column(name = "max_quantity", nullable = false) int maxQuantity = 10;
    @Column(name = "image_url", length = 1000) String imageUrl;
    @Column(nullable = false) boolean active = true;
    @Column(name = "demo_data", nullable = false) boolean demoData;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @PrePersist void create() {
        createdAt = updatedAt = Instant.now();
    }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
