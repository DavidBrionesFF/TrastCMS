package com.nattechnologies.trastcms.plugins.saas;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class SaasDtos {
    private SaasDtos() {}

    public record ProductRequest(
            @NotBlank String productKey,
            @NotBlank String name,
            String description,
            String logoUrl,
            boolean active) {}

    public record ProductResponse(
            String id,
            String productKey,
            String name,
            String description,
            String logoUrl,
            boolean active,
            boolean demoData,
            Instant createdAt) {}

    public record PlanRequest(
            @NotBlank String productId,
            @NotBlank String planKey,
            @NotBlank String name,
            String description,
            @DecimalMin("0.00") BigDecimal price,
            @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank String billingInterval,
            @Min(1) int intervalCount,
            @Min(0) int trialDays,
            @Min(1) int maxActivations,
            Map<String, Object> entitlements,
            boolean active,
            boolean featured) {}

    public record PlanResponse(
            String id,
            String productId,
            String productKey,
            String productName,
            String planKey,
            String name,
            String description,
            BigDecimal price,
            String currency,
            String billingInterval,
            int intervalCount,
            int trialDays,
            int maxActivations,
            Map<String, Object> entitlements,
            boolean active,
            boolean featured,
            boolean demoData,
            Instant createdAt,
            Instant updatedAt) {}

    public record IssueLicenseRequest(
            @NotBlank String planId,
            @Email @NotBlank String customerEmail,
            Instant expiresAt,
            Integer maxActivations,
            String orderId) {}

    public record LicenseResponse(
            String id,
            String productKey,
            String productName,
            String planKey,
            String planName,
            String customerEmail,
            String maskedKey,
            String licenseKey,
            String status,
            int maxActivations,
            long activeActivations,
            Instant issuedAt,
            Instant expiresAt,
            Instant graceEndsAt,
            Instant lastValidatedAt,
            String orderId,
            boolean demoData) {}

    public record SubscriptionResponse(
            String id,
            String productKey,
            String productName,
            String planKey,
            String planName,
            String customerEmail,
            String status,
            Instant startedAt,
            Instant trialEndsAt,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            boolean cancelAtPeriodEnd,
            Instant cancelledAt,
            String orderId,
            boolean demoData) {}

    public record SubscriptionStatusRequest(
            @NotNull SubscriptionStatus status,
            Boolean cancelAtPeriodEnd) {}

    public record LicenseStatusRequest(@NotNull LicenseStatus status) {}

    public record LicenseRequest(
            @NotBlank String licenseKey,
            @NotBlank String productKey,
            String fingerprint,
            String deviceName,
            String platform,
            String applicationVersion) {}

    public record LicenseResult(
            boolean valid,
            String status,
            String licenseId,
            String activationId,
            Instant expiresAt,
            Instant graceEndsAt,
            Map<String, Object> entitlements,
            String message,
            String signature) {}

    public record DeactivateRequest(
            @NotBlank String licenseKey,
            @NotBlank String productKey,
            @NotBlank String fingerprint) {}

    public record UsageRequest(
            @NotBlank String licenseKey,
            @NotBlank String productKey,
            @NotBlank String meter,
            @DecimalMin("0.0001") BigDecimal quantity,
            @NotBlank String idempotencyKey,
            Instant occurredAt,
            Map<String, Object> metadata) {}

    public record ReleaseRequest(
            @NotBlank String productId,
            @NotBlank String version,
            @NotBlank String platform,
            String channel,
            @NotBlank String downloadUrl,
            String checksum,
            String minimumPlan,
            boolean active,
            Instant publishedAt) {}

    public record ReleaseResponse(
            String id,
            String productKey,
            String version,
            String platform,
            String channel,
            String downloadUrl,
            String checksum,
            String minimumPlan,
            boolean active,
            Instant publishedAt) {}

    public record SettingsRequest(
            @NotBlank @Size(max = 180) String companyName,
            @Email @Size(max = 190) String supportEmail,
            @Email @Size(max = 190) String billingEmail,
            @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
            @Min(0) @Max(365) int defaultTrialDays,
            @Min(0) @Max(90) int defaultGraceDays,
            boolean allowSelfServiceCancel,
            boolean allowPlanChanges,
            boolean requireDeviceName,
            boolean showAnnualSavings,
            @Size(max = 250) String pricingHeadline,
            @Size(max = 1000) String pricingDescription,
            @Size(max = 1000) String portalMessage,
            @Size(max = 1000) String termsUrl,
            @Size(max = 1000) String privacyUrl) {}

    public record SettingsResponse(
            String companyName,
            String supportEmail,
            String billingEmail,
            String defaultCurrency,
            int defaultTrialDays,
            int defaultGraceDays,
            boolean allowSelfServiceCancel,
            boolean allowPlanChanges,
            boolean requireDeviceName,
            boolean showAnnualSavings,
            String pricingHeadline,
            String pricingDescription,
            String portalMessage,
            String termsUrl,
            String privacyUrl,
            Instant updatedAt) {}

    public record CatalogResponse(
            SettingsResponse settings,
            List<ProductResponse> products,
            List<PlanResponse> plans) {}

    public record ClaimResponse(
            String orderNumber,
            String email,
            List<LicenseResponse> licenses,
            List<SubscriptionResponse> subscriptions) {}

    public record Dashboard(
            long products,
            long plans,
            long subscriptions,
            long activeSubscriptions,
            long trialingSubscriptions,
            long pastDueSubscriptions,
            long licences,
            long activeLicenses,
            long activations,
            long usageEvents,
            long demoProducts,
            long demoPlans,
            List<LicenseResponse> recentLicenses,
            List<PlanResponse> featuredPlans) {}

    public record DemoDataResponse(
            String message,
            long products,
            long plans,
            long subscriptions,
            long licenses) {}
}
