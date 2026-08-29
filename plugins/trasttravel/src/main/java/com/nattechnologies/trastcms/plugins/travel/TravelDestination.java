package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "travel_destination")
class TravelDestination {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(nullable = false, length = 180) String name;
    @Column(nullable = false, length = 180) String slug;
    @Column(length = 500) String summary;
    @Lob String description;
    @Column(name = "image_url", length = 1000) String imageUrl;
    @Column(length = 120) String country;
    @Column(length = 120) String region;
    @Column(length = 120) String city;
    @Column(precision = 10, scale = 7) BigDecimal latitude;
    @Column(precision = 10, scale = 7) BigDecimal longitude;
    @Column(nullable = false) boolean featured;
    @Column(nullable = false) boolean active = true;
    @Column(name = "demo_data", nullable = false) boolean demoData;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @PrePersist void create() {
        createdAt = updatedAt = Instant.now();
    }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
