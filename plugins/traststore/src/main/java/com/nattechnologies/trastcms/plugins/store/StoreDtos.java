package com.nattechnologies.trastcms.plugins.store;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class StoreDtos {
    private StoreDtos() { }

    public record VariantRequest(
            String id,
            @NotBlank String name,
            @NotBlank String sku,
            BigDecimal price,
            @Min(0) int stockQuantity,
            String optionValues,
            String imageUrl,
            boolean active) { }

    public record ProductRequest(
            @NotBlank String name,
            String slug,
            @NotBlank String sku,
            String description,
            String shortDescription,
            ProductType type,
            ProductStatus status,
            @DecimalMin("0.00") BigDecimal price,
            BigDecimal compareAtPrice,
            @Pattern(regexp = "[A-Z]{3}") String currency,
            String category,
            String brand,
            String featuredImageUrl,
            List<String> gallery,
            Map<String, Object> attributes,
            Map<String, Object> seo,
            boolean featured,
            boolean trackInventory,
            boolean allowBackorder,
            @Min(0) int stockQuantity,
            @Min(0) int lowStockThreshold,
            Integer weightGrams,
            String digitalUrl,
            boolean taxable,
            String taxClass,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRateOverride,
            List<VariantRequest> variants) { }

    public record VariantResponse(
            String id,
            String name,
            String sku,
            BigDecimal price,
            int stockQuantity,
            String optionValues,
            String imageUrl,
            boolean active) { }

    public record ProductResponse(
            String id,
            String name,
            String slug,
            String sku,
            String description,
            String shortDescription,
            String type,
            String status,
            BigDecimal price,
            BigDecimal compareAtPrice,
            String currency,
            String category,
            String brand,
            String featuredImageUrl,
            List<String> gallery,
            Map<String, Object> attributes,
            Map<String, Object> seo,
            boolean featured,
            boolean trackInventory,
            boolean allowBackorder,
            int stockQuantity,
            int lowStockThreshold,
            Integer weightGrams,
            String digitalUrl,
            boolean taxable,
            String taxClass,
            BigDecimal taxRateOverride,
            BigDecimal effectiveTaxRate,
            boolean demoData,
            List<VariantResponse> variants,
            boolean inStock,
            Instant createdAt,
            Instant updatedAt) { }

    public record InventoryAdjustmentRequest(
            @NotBlank String type,
            @NotNull Integer quantity,
            @NotBlank String reason,
            String variantId,
            String referenceType,
            String referenceId) { }

    public record InventoryMovementResponse(
            String id,
            String productId,
            String productName,
            String variantId,
            String variantName,
            String type,
            int quantity,
            String reason,
            String referenceType,
            String referenceId,
            Instant createdAt) { }

    public record Dashboard(
            long products,
            long active,
            long lowStock,
            long outOfStock,
            long variants,
            long taxableProducts,
            long demoProducts,
            List<ProductResponse> featured,
            List<ProductResponse> alerts) { }

    public record DemoDataResponse(
            int created,
            int existing,
            int deleted,
            String message) { }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) { }
}
