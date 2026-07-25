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
    BigDecimal taxRate = new BigDecimal("15.00");

    @Column(name = "prices_include_tax", nullable = false)
    boolean pricesIncludeTax = false;

    @Column(name = "tax_label", nullable = false, length = 80)
    String taxLabel = "ISV";

    @Column(name = "tax_country", nullable = false, length = 2)
    String taxCountry = "HN";

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

    @Column(name = "paypal_enabled", nullable = false)
    boolean paypalEnabled = false;

    @Column(name = "paypal_mode", nullable = false, length = 20)
    String paypalMode = "SANDBOX";

    @Column(name = "paypal_client_id", length = 500)
    String paypalClientId;

    @Column(name = "paypal_client_secret_cipher", length = 4000)
    String paypalClientSecretCipher;

    @Column(name = "paypal_webhook_id", length = 200)
    String paypalWebhookId;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
