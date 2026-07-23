package com.nattechnologies.trastcms.plugins.pay;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
public final class PayDtos { private PayDtos(){}
 public record AddItemRequest(@NotBlank String providerKey,@NotBlank String reference,@Min(1)@Max(999)int quantity,Map<String,Object> metadata){}
 public record UpdateItemRequest(@Min(0)@Max(999)int quantity){}
 public record CartItemResponse(String id,String providerKey,String reference,String type,String name,int quantity,BigDecimal unitPrice,BigDecimal lineTotal,String currency,String imageUrl,Map<String,Object> metadata){}
 public record CartResponse(String token,String status,String currency,List<CartItemResponse> items,BigDecimal subtotal,BigDecimal discountTotal,BigDecimal taxTotal,BigDecimal total,Instant expiresAt){}
 public record CheckoutRequest(@Email@NotBlank String email,@NotBlank String name,String phone,String couponCode,@NotBlank String gatewayKey,Map<String,Object> billing,Map<String,Object> shipping,String notes,String returnUrl,String cancelUrl,String idempotencyKey){}
 public record CheckoutResponse(String orderId,String orderNumber,String orderToken,String orderStatus,String paymentId,String paymentStatus,String redirectUrl,String message){}
 public record OrderLine(String id,String providerKey,String reference,String type,String name,int quantity,BigDecimal unitPrice,BigDecimal total,boolean fulfilled){}
 public record OrderResponse(String id,String orderNumber,String status,String currency,String customerEmail,String customerName,BigDecimal subtotal,BigDecimal discountTotal,BigDecimal taxTotal,BigDecimal total,String couponCode,List<OrderLine> items,Instant paidAt,Instant createdAt,Instant updatedAt){}
 public record PaymentResponse(String id,String orderId,String gateway,String status,BigDecimal amount,String currency,String externalReference,String redirectUrl,String failureMessage,Instant createdAt,Instant updatedAt){}
 public record CouponRequest(@NotBlank@Pattern(regexp="[A-Za-z0-9_-]{3,40}")String code,@NotBlank String discountType,@DecimalMin("0.01")BigDecimal discountValue,BigDecimal minimumAmount,Integer maximumUses,Instant startsAt,Instant endsAt,boolean active){}
 public record CouponResponse(String id,String code,String discountType,BigDecimal discountValue,BigDecimal minimumAmount,Integer maximumUses,int usedCount,Instant startsAt,Instant endsAt,boolean active){}
 public record Dashboard(long orders,long paidOrders,long pendingOrders,BigDecimal revenue,long payments,long coupons,List<OrderResponse> recentOrders,List<Map<String,Object>> gateways){}
 public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages){}
 public record SettingsRequest(@Pattern(regexp="[A-Z]{3}") String defaultCurrency,@DecimalMin("0.00") BigDecimal taxRate,@Min(1)@Max(90) int cartExpirationDays,boolean guestCheckout,boolean collectShipping,String companyName,@Email String supportEmail,String termsUrl,String bankInstructions){}
 public record SettingsResponse(String defaultCurrency,BigDecimal taxRate,int cartExpirationDays,boolean guestCheckout,boolean collectShipping,String companyName,String supportEmail,String termsUrl,String bankInstructions,Instant updatedAt){}
 public record StatusRequest(@NotBlank String status){}
}
