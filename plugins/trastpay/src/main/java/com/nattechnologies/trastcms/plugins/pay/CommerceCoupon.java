package com.nattechnologies.trastcms.plugins.pay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commerce_coupon")
class CommerceCoupon {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id
    @Column(length = 36)
    String id;

    @Column(nullable = false, length = 80)
    String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    String discountType = "PERCENT";

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    BigDecimal discountValue;

    @Column(name = "minimum_amount", precision = 19, scale = 4)
    BigDecimal minimumAmount;

    @Column(name = "maximum_uses")
    Integer maximumUses;

    @Column(name = "used_count", nullable = false)
    int usedCount = 0;

    @Column(name = "starts_at")
    Instant startsAt;

    @Column(name = "ends_at")
    Instant endsAt;

    @Column(nullable = false)
    boolean active = true;

    @Column(name = "demo_data", nullable = false)
    boolean demoData = false;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        code = code.toUpperCase();
    }
}
