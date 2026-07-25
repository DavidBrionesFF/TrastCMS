package com.nattechnologies.trastcms.plugins.saas;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saas_settings")
class SaasSettings {
    @Id
    int id = 1;

    @Column(name = "company_name", nullable = false, length = 180)
    String companyName = "TrastCMS Cloud";

    @Column(name = "support_email", length = 190)
    String supportEmail = "soporte@trastcms.local";

    @Column(name = "billing_email", length = 190)
    String billingEmail = "facturacion@trastcms.local";

    @Column(name = "default_currency", nullable = false, length = 3)
    String defaultCurrency = "USD";

    @Column(name = "default_trial_days", nullable = false)
    int defaultTrialDays = 14;

    @Column(name = "default_grace_days", nullable = false)
    int defaultGraceDays = 7;

    @Column(name = "allow_self_service_cancel", nullable = false)
    boolean allowSelfServiceCancel = true;

    @Column(name = "allow_plan_changes", nullable = false)
    boolean allowPlanChanges = true;

    @Column(name = "require_device_name", nullable = false)
    boolean requireDeviceName = true;

    @Column(name = "show_annual_savings", nullable = false)
    boolean showAnnualSavings = true;

    @Column(name = "pricing_headline", length = 250)
    String pricingHeadline = "Planes flexibles para convertir su software en un negocio recurrente";

    @Column(name = "pricing_description", length = 1000)
    String pricingDescription = "Venda suscripciones, emita licencias y controle activaciones desde una sola plataforma.";

    @Column(name = "portal_message", length = 1000)
    String portalMessage = "Administre sus licencias, dispositivos y renovaciones desde su cuenta.";

    @Column(name = "terms_url", length = 1000)
    String termsUrl;

    @Column(name = "privacy_url", length = 1000)
    String privacyUrl;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
