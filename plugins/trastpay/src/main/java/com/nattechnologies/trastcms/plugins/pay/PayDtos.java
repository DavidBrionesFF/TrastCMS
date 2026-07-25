package com.nattechnologies.trastcms.plugins.pay;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PayDtos {
    private PayDtos() { }

    public record AddItemRequest(
            @NotBlank String providerKey,
            @NotBlank String reference,
            @Min(1) @Max(999) int quantity,
            Map<String, Object> metadata) { }

    public record UpdateItemRequest(@Min(0) @Max(999) int quantity) { }

    public record CartItemResponse(
            String id,
            String providerKey,
            String reference,
            String type,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String currency,
            String imageUrl,
            boolean taxable,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            Map<String, Object> metadata) { }

    public record CartResponse(
            String token,
            String status,
            String currency,
            List<CartItemResponse> items,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal total,
            String taxLabel,
            boolean pricesIncludeTax,
            Instant expiresAt) { }

    public record CheckoutRequest(
            @Email @NotBlank String email,
            @NotBlank String name,
            String phone,
            String couponCode,
            @NotBlank String gatewayKey,
            Map<String, Object> billing,
            Map<String, Object> shipping,
            String notes,
            String returnUrl,
            String cancelUrl,
            String idempotencyKey) { }

    public record CheckoutResponse(
            String orderId,
            String orderNumber,
            String orderToken,
            String orderStatus,
            String paymentId,
            String paymentStatus,
            String redirectUrl,
            String message) { }

    public record OrderLine(
            String id,
            String providerKey,
            String reference,
            String type,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal total,
            boolean fulfilled) { }

    public record OrderResponse(
            String id,
            String orderNumber,
            String status,
            String currency,
            String customerEmail,
            String customerName,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal total,
            String couponCode,
            List<OrderLine> items,
            Instant paidAt,
            Instant createdAt,
            Instant updatedAt) { }

    public record PaymentResponse(
            String id,
            String orderId,
            String gateway,
            String status,
            BigDecimal amount,
            String currency,
            String externalReference,
            String redirectUrl,
            String failureMessage,
            Instant createdAt,
            Instant updatedAt) { }

    public record CouponRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,40}") String code,
            @NotBlank String discountType,
            @DecimalMin("0.01") BigDecimal discountValue,
            BigDecimal minimumAmount,
            Integer maximumUses,
            Instant startsAt,
            Instant endsAt,
            boolean active) { }

    public record CouponResponse(
            String id,
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minimumAmount,
            Integer maximumUses,
            int usedCount,
            Instant startsAt,
            Instant endsAt,
            boolean active,
            boolean demoData) { }

    public record Dashboard(
            long orders,
            long paidOrders,
            long pendingOrders,
            BigDecimal revenue,
            long payments,
            long coupons,
            long demoCoupons,
            List<OrderResponse> recentOrders,
            List<Map<String, Object>> gateways) { }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) { }

    public record SettingsRequest(
            @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
            boolean pricesIncludeTax,
            String taxLabel,
            @Pattern(regexp = "[A-Z]{2}") String taxCountry,
            @Min(1) @Max(90) int cartExpirationDays,
            boolean guestCheckout,
            boolean collectShipping,
            String companyName,
            @Email String supportEmail,
            String termsUrl,
            String bankInstructions,
            boolean paypalEnabled,
            @Pattern(regexp = "SANDBOX|LIVE") String paypalMode,
            String paypalClientId,
            String paypalClientSecret,
            String paypalWebhookId) { }

    public record SettingsResponse(
            String defaultCurrency,
            BigDecimal taxRate,
            boolean pricesIncludeTax,
            String taxLabel,
            String taxCountry,
            int cartExpirationDays,
            boolean guestCheckout,
            boolean collectShipping,
            String companyName,
            String supportEmail,
            String termsUrl,
            String bankInstructions,
            boolean paypalEnabled,
            String paypalMode,
            String paypalClientId,
            boolean paypalConfigured,
            String paypalWebhookId,
            Instant updatedAt) { }

    public record DemoDataResponse(
            int created,
            int existing,
            int deleted,
            String message) { }

    public record StatusRequest(@NotBlank String status) { }
}
