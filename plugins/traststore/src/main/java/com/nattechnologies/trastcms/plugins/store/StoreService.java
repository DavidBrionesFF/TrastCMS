package com.nattechnologies.trastcms.plugins.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PluginEventDispatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoreService {
    private final StoreProductRepository products;
    private final StoreVariantRepository variants;
    private final StoreInventoryMovementRepository movements;
    private final ObjectMapper mapper;
    private final PluginEventDispatcher events;
    private final StorePluginGuard guard;

    StoreService(
            StoreProductRepository products,
            StoreVariantRepository variants,
            StoreInventoryMovementRepository movements,
            ObjectMapper mapper,
            PluginEventDispatcher events,
            StorePluginGuard guard) {
        this.products = products;
        this.variants = variants;
        this.movements = movements;
        this.mapper = mapper;
        this.events = events;
        this.guard = guard;
    }

    @Transactional
    public void initializeDefaults() {
        if (products.count() != 0) return;

        save(null, new StoreDtos.ProductRequest(
                "Plantilla corporativa premium",
                "plantilla-corporativa",
                "TRAST-THEME-PRO",
                "Tema profesional para empresas, agencias y consultores.",
                "Tema responsive con secciones editables.",
                ProductType.DIGITAL,
                ProductStatus.ACTIVE,
                new BigDecimal("79.00"),
                new BigDecimal("99.00"),
                "USD",
                "Temas",
                "TrastCMS",
                null,
                List.of(),
                Map.of("delivery", "download"),
                Map.of(),
                true,
                false,
                true,
                999,
                5,
                null,
                "/downloads/theme.zip",
                List.of()));

        save(null, new StoreDtos.ProductRequest(
                "Implementación de sitio web",
                "implementacion-web",
                "TRAST-SERVICE-WEB",
                "Servicio profesional para configurar, diseñar y publicar un sitio con TrastCMS.",
                "Configuración y lanzamiento guiado.",
                ProductType.SERVICE,
                ProductStatus.ACTIVE,
                new BigDecimal("499.00"),
                null,
                "USD",
                "Servicios",
                "NaT Technologies",
                null,
                List.of(),
                Map.of("duration", "10 días"),
                Map.of(),
                true,
                false,
                true,
                999,
                5,
                null,
                null,
                List.of()));
    }

    @Transactional(readOnly = true)
    public StoreDtos.PageResponse<StoreDtos.ProductResponse> catalog(
            String search,
            int page,
            int size) {
        guard.requireEnabled();
        Page<StoreProduct> result = products.search(
                ProductStatus.ACTIVE,
                clean(search),
                PageRequest.of(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100)));
        return page(result.map(this::response));
    }

    @Transactional(readOnly = true)
    public StoreDtos.ProductResponse publicProduct(String slug) {
        guard.requireEnabled();
        StoreProduct product = products.findBySlug(slug)
                .filter(item -> item.status == ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        return response(product);
    }

    @Transactional(readOnly = true)
    public StoreDtos.PageResponse<StoreDtos.ProductResponse> adminProducts(
            String search,
            int page,
            int size) {
        guard.requireEnabled();
        Page<StoreProduct> result = products.findAll(PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")));

        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase(Locale.ROOT);
            List<StoreDtos.ProductResponse> filtered = result.getContent().stream()
                    .filter(product -> (product.name + " " + product.sku + " "
                            + Objects.toString(product.category, ""))
                            .toLowerCase(Locale.ROOT)
                            .contains(term))
                    .map(this::response)
                    .toList();
            return new StoreDtos.PageResponse<>(
                    filtered,
                    page,
                    size,
                    filtered.size(),
                    filtered.isEmpty() ? 0 : 1);
        }
        return page(result.map(this::response));
    }

    @Transactional
    public StoreDtos.ProductResponse save(
            String id,
            StoreDtos.ProductRequest request) {
        guard.requireEnabled();
        StoreProduct product = id == null ? new StoreProduct() : require(id);
        String slug = slug(request.slug(), request.name());
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);

        products.findBySlug(slug)
                .filter(existing -> !Objects.equals(existing.id, id))
                .ifPresent(existing -> {
                    throw new BadRequestException("El slug ya existe");
                });
        products.findBySku(sku)
                .filter(existing -> !Objects.equals(existing.id, id))
                .ifPresent(existing -> {
                    throw new BadRequestException("El SKU ya existe");
                });

        product.name = request.name().trim();
        product.slug = slug;
        product.sku = sku;
        product.description = blank(request.description());
        product.shortDescription = blank(request.shortDescription());
        product.type = request.type() == null
                ? ProductType.PHYSICAL
                : request.type();
        product.status = request.status() == null
                ? ProductStatus.DRAFT
                : request.status();
        product.price = money(request.price());
        product.compareAtPrice = request.compareAtPrice() == null
                ? null
                : money(request.compareAtPrice());
        product.currency = request.currency() == null
                ? "USD"
                : request.currency().toUpperCase(Locale.ROOT);
        product.category = blank(request.category());
        product.brand = blank(request.brand());
        product.featuredImageUrl = blank(request.featuredImageUrl());
        product.galleryJson = write(request.gallery());
        product.attributesJson = write(request.attributes());
        product.seoJson = write(request.seo());
        product.featured = request.featured();
        product.trackInventory = request.trackInventory();
        product.allowBackorder = request.allowBackorder();
        product.stockQuantity = request.stockQuantity();
        product.lowStockThreshold = request.lowStockThreshold();
        product.weightGrams = request.weightGrams();
        product.digitalUrl = blank(request.digitalUrl());

        mergeVariants(product, request.variants());
        product = products.save(product);

        events.publish(PluginEvent.of(
                id == null ? "store.product.created" : "store.product.updated",
                Map.of(
                        "id", product.id,
                        "sku", product.sku,
                        "status", product.status.name())));
        return response(product);
    }

    @Transactional
    public void delete(String id) {
        guard.requireEnabled();
        products.delete(require(id));
    }

    @Transactional
    public StoreDtos.ProductResponse adjust(
            String id,
            StoreDtos.InventoryAdjustmentRequest request) {
        guard.requireEnabled();
        StoreProduct product = require(id);
        StoreVariant variant = request.variantId() == null
                ? null
                : variants.findById(request.variantId())
                        .filter(value -> value.product.id.equals(product.id))
                        .orElseThrow(() -> new NotFoundException(
                                "Variante no encontrada"));

        int current = variant == null
                ? product.stockQuantity
                : variant.stockQuantity;
        int delta = request.type().equalsIgnoreCase("SET")
                ? request.quantity() - current
                : request.quantity();
        int next = Math.max(0, current + delta);

        if (variant == null) product.stockQuantity = next;
        else variant.stockQuantity = next;

        StoreInventoryMovement movement = new StoreInventoryMovement();
        movement.product = product;
        movement.variant = variant;
        movement.type = request.type().toUpperCase(Locale.ROOT);
        movement.quantity = delta;
        movement.reason = request.reason();
        movement.referenceType = blank(request.referenceType());
        movement.referenceId = blank(request.referenceId());
        movements.save(movement);
        products.save(product);

        events.publish(PluginEvent.of(
                "store.inventory.adjusted",
                Map.of("productId", product.id, "quantity", delta)));
        return response(product);
    }

    @Transactional(readOnly = true)
    public StoreDtos.PageResponse<StoreDtos.InventoryMovementResponse> movements(
            int page,
            int size) {
        guard.requireEnabled();
        Page<StoreInventoryMovement> result = movements
                .findAllByOrderByCreatedAtDesc(PageRequest.of(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100)));
        return page(result.map(this::movement));
    }

    @Transactional
    public void reserve(String reference, int quantity, String orderId) {
        StoreProduct product = products.findById(reference)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        if (!product.trackInventory) return;
        if (!product.allowBackorder && product.stockQuantity < quantity) {
            throw new BadRequestException(
                    "Inventario insuficiente para " + product.name);
        }

        product.stockQuantity = Math.max(0, product.stockQuantity - quantity);
        products.save(product);

        StoreInventoryMovement movement = new StoreInventoryMovement();
        movement.product = product;
        movement.type = "SALE";
        movement.quantity = -quantity;
        movement.reason = "Orden pagada " + orderId;
        movement.referenceType = "ORDER";
        movement.referenceId = orderId;
        movements.save(movement);
    }

    @Transactional(readOnly = true)
    public StoreDtos.Dashboard dashboard() {
        guard.requireEnabled();
        List<StoreProduct> all = products.findAll();
        List<StoreProduct> alerts = all.stream()
                .filter(product -> product.trackInventory
                        && product.stockQuantity <= product.lowStockThreshold)
                .sorted(Comparator.comparingInt(product -> product.stockQuantity))
                .limit(8)
                .toList();

        return new StoreDtos.Dashboard(
                all.size(),
                all.stream().filter(product ->
                        product.status == ProductStatus.ACTIVE).count(),
                all.stream().filter(product -> product.trackInventory
                        && product.stockQuantity > 0
                        && product.stockQuantity <= product.lowStockThreshold)
                        .count(),
                all.stream().filter(product -> product.trackInventory
                        && product.stockQuantity == 0).count(),
                variants.count(),
                products.findTop12ByStatusAndFeaturedTrueOrderByUpdatedAtDesc(
                                ProductStatus.ACTIVE)
                        .stream()
                        .map(this::response)
                        .toList(),
                alerts.stream().map(this::response).toList());
    }

    StoreProduct require(String id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    StoreDtos.ProductResponse response(StoreProduct product) {
        return new StoreDtos.ProductResponse(
                product.id,
                product.name,
                product.slug,
                product.sku,
                product.description,
                product.shortDescription,
                product.type.name(),
                product.status.name(),
                product.price,
                product.compareAtPrice,
                product.currency,
                product.category,
                product.brand,
                product.featuredImageUrl,
                readList(product.galleryJson),
                readMap(product.attributesJson),
                readMap(product.seoJson),
                product.featured,
                product.trackInventory,
                product.allowBackorder,
                product.stockQuantity,
                product.lowStockThreshold,
                product.weightGrams,
                product.digitalUrl,
                product.variants.stream()
                        .map(variant -> new StoreDtos.VariantResponse(
                                variant.id,
                                variant.name,
                                variant.sku,
                                variant.price == null
                                        ? product.price
                                        : variant.price,
                                variant.stockQuantity,
                                variant.optionValues,
                                variant.imageUrl,
                                variant.active))
                        .toList(),
                !product.trackInventory
                        || product.allowBackorder
                        || product.stockQuantity > 0,
                product.createdAt,
                product.updatedAt);
    }

    private void mergeVariants(
            StoreProduct product,
            List<StoreDtos.VariantRequest> requests) {
        Map<String, StoreVariant> existing = product.variants.stream()
                .filter(variant -> variant.id != null)
                .collect(Collectors.toMap(
                        variant -> variant.id,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<StoreVariant> next = new ArrayList<>();

        if (requests != null) {
            for (StoreDtos.VariantRequest request : requests) {
                String sku = request.sku().trim().toUpperCase(Locale.ROOT);
                variants.findBySku(sku)
                        .filter(found -> !Objects.equals(found.id, request.id()))
                        .ifPresent(found -> {
                            throw new BadRequestException(
                                    "El SKU de variante ya existe: " + sku);
                        });

                StoreVariant variant;
                if (request.id() == null || request.id().isBlank()) {
                    variant = new StoreVariant();
                } else {
                    variant = existing.remove(request.id());
                    if (variant == null) {
                        throw new BadRequestException(
                                "La variante no pertenece al producto");
                    }
                }

                variant.product = product;
                variant.name = request.name().trim();
                variant.sku = sku;
                variant.price = request.price() == null
                        ? null
                        : money(request.price());
                variant.stockQuantity = request.stockQuantity();
                variant.optionValues = blank(request.optionValues());
                variant.imageUrl = blank(request.imageUrl());
                variant.active = request.active();
                next.add(variant);
            }
        }

        product.variants.clear();
        product.variants.addAll(next);
    }

    private StoreDtos.InventoryMovementResponse movement(
            StoreInventoryMovement movement) {
        return new StoreDtos.InventoryMovementResponse(
                movement.id,
                movement.product.id,
                movement.product.name,
                movement.variant == null ? null : movement.variant.id,
                movement.variant == null ? null : movement.variant.name,
                movement.type,
                movement.quantity,
                movement.reason,
                movement.referenceType,
                movement.referenceId,
                movement.createdAt);
    }

    private <T> StoreDtos.PageResponse<T> page(Page<T> page) {
        return new StoreDtos.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private String slug(String value, String fallback) {
        return (value == null || value.isBlank() ? fallback : value)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new BadRequestException("Datos JSON inválidos");
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return value == null
                    ? Map.of()
                    : mapper.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private List<String> readList(String value) {
        try {
            return value == null
                    ? List.of()
                    : mapper.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
