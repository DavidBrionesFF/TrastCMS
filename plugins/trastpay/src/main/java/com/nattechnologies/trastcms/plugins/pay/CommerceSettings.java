package com.nattechnologies.trastcms.plugins.pay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commerce_settings")
class CommerceSettings {
    static final String DEFAULT_ID = "default";

    @Id
    @Column(length = 40)
    String id = DEFAULT_ID;

    @Column(name = "default_currency", nullable = false, length = 3)
    String defaultCurrency = "USD";

    @Column(name = "tax_rate", nullable = false, precision = 8, scale = 4)
    BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "cart_expiration_days", nullable = false)
    int cartExpirationDays = 14;

    @Column(name = "guest_checkout", nullable = false)
    boolean guestCheckout = true;

    @Column(name = "collect_shipping", nullable = false)
    boolean collectShipping = false;

    @Column(name = "company_name", length = 190)
    String companyName;

    @Column(name = "support_email", length = 190)
    String supportEmail;

    @Column(name = "terms_url", length = 1000)
    String termsUrl;

    @Column(name = "bank_instructions", length = 4000)
    String bankInstructions;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
