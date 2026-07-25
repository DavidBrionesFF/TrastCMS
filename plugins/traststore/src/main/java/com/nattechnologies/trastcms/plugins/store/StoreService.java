package com.nattechnologies.trastcms.plugins.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PluginEventDispatcher;
import com.nattechnologies.trastcms.service.SiteSettingService;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoreService {
    private static final String DEMO_DISMISSED_KEY = "plugins.traststore.demo.dismissed";
    private final StoreProductRepository products;
    private final StoreVariantRepository variants;
    private final StoreInventoryMovementRepository movements;
    private final ObjectMapper mapper;
    private final PluginEventDispatcher events;
    private final StorePluginGuard guard;
    private final SiteSettingService siteSettings;

    StoreService(
            StoreProductRepository products,
            StoreVariantRepository variants,
            StoreInventoryMovementRepository movements,
            ObjectMapper mapper,
            PluginEventDispatcher events,
            StorePluginGuard guard,
            SiteSettingService siteSettings) {
        this.products = products;
        this.variants = variants;
        this.movements = movements;
        this.mapper = mapper;
        this.events = events;
        this.guard = guard;
        this.siteSettings = siteSettings;
    }

    @Transactional
    public void initializeDefaults() {
        if (products.count() == 0
                && !siteSettings.getBoolean(DEMO_DISMISSED_KEY, false)) {
            generateDemoDataInternal();
        }
    }

    @Transactional
    public synchronized StoreDtos.DemoDataResponse generateDemoData() {
        guard.requireEnabled();
        siteSettings.put(DEMO_DISMISSED_KEY, "false");
        return generateDemoDataInternal();
    }

    private StoreDtos.DemoDataResponse generateDemoDataInternal() {
        int created = 0;
        int existing = 0;

        for (DemoProduct demo : demoCatalog()) {
            Optional<StoreProduct> bySku = products.findBySku(demo.sku());
            Optional<StoreProduct> bySlug =
                    products.findBySlug(demo.request().slug());

            if (bySku.isPresent() || bySlug.isPresent()) {
                existing++;
                continue;
            }

            StoreDtos.ProductResponse response = save(null, demo.request());
            StoreProduct product = require(response.id());
            product.demoData = true;
            products.save(product);
            created++;
        }

        events.publish(PluginEvent.of(
                "store.demo.generated",
                Map.of("created", created, "existing", existing)));
        return new StoreDtos.DemoDataResponse(
                created,
                existing,
                0,
                created == 0
                        ? "Los datos demostrativos ya estaban cargados."
                        : "Se cargaron productos demostrativos para explorar la tienda.");
    }

    @Transactional
    public StoreDtos.DemoDataResponse deleteDemoData() {
        guard.requireEnabled();
        List<StoreProduct> demoProducts = products.findByDemoDataTrue();
        siteSettings.put(DEMO_DISMISSED_KEY, "true");
        if (demoProducts.isEmpty()) {
            return new StoreDtos.DemoDataResponse(
                    0, 0, 0, "No hay productos demostrativos para eliminar.");
        }

        List<String> ids = demoProducts.stream().map(product -> product.id).toList();
        movements.deleteByProductIdIn(ids);
        products.deleteAll(demoProducts);
        events.publish(PluginEvent.of(
                "store.demo.deleted",
                Map.of("deleted", demoProducts.size())));
        return new StoreDtos.DemoDataResponse(
                0,
                0,
                demoProducts.size(),
                "Se eliminaron únicamente los productos demostrativos.");
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
        product.taxable = request.taxable();
        product.taxClass = normalizeTaxClass(request.taxClass());
        product.taxRateOverride = request.taxRateOverride() == null
                ? null
                : percent(request.taxRateOverride());

        mergeVariants(product, request.variants());
        product = products.save(product);

        events.publish(PluginEvent.of(
                id == null ? "store.product.created" : "store.product.updated",
                Map.of(
                        "id", product.id,
                        "sku", product.sku,
                        "status", product.status.name(),
                        "taxable", product.taxable)));
        return response(product);
    }

    @Transactional
    public void delete(String id) {
        guard.requireEnabled();
        StoreProduct product = require(id);
        movements.deleteByProductIdIn(List.of(product.id));
        products.delete(product);
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
        String movementType = request.type() == null
                ? "SET"
                : request.type().trim().toUpperCase(Locale.ROOT);
        int requestedQuantity = Math.max(0, request.quantity());
        int delta = switch (movementType) {
            case "SET" -> requestedQuantity - current;
            case "REMOVE", "DAMAGE", "SALE", "TRANSFER_OUT" ->
                    -requestedQuantity;
            case "ADD", "RETURN", "PURCHASE", "TRANSFER_IN" ->
                    requestedQuantity;
            default -> throw new BadRequestException(
                    "Tipo de movimiento de inventario no válido");
        };
        int next = Math.max(0, current + delta);

        if (variant == null) product.stockQuantity = next;
        else variant.stockQuantity = next;

        StoreInventoryMovement movement = new StoreInventoryMovement();
        movement.product = product;
        movement.variant = variant;
        movement.type = movementType;
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
                all.stream().filter(product -> product.taxable).count(),
                all.stream().filter(product -> product.demoData).count(),
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
        BigDecimal effectiveTaxRate = product.taxable
                ? Objects.requireNonNullElse(product.taxRateOverride, BigDecimal.ZERO)
                : BigDecimal.ZERO;
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
                product.taxable,
                product.taxClass,
                product.taxRateOverride,
                effectiveTaxRate,
                product.demoData,
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

    private List<DemoProduct> demoCatalog() {
        return List.of(
                demo("Plantilla corporativa premium", "plantilla-corporativa", "DEMO-THEME-CORP",
                        "Tema profesional para empresas, agencias y consultores.", "Diseño responsive con páginas y secciones editables.",
                        ProductType.DIGITAL, "Temas", "TrastCMS", "79.00", "99.00", 999,
                        "https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Entrega", "Descarga inmediata", "Compatibilidad", "Constructor visual"), true, "15.00"),
                demo("Implementación de sitio web", "implementacion-web", "DEMO-SERVICE-WEB",
                        "Servicio profesional para configurar, diseñar y publicar un sitio con TrastCMS.", "Configuración, personalización y lanzamiento guiado.",
                        ProductType.SERVICE, "Servicios", "NaT Technologies", "499.00", null, 999,
                        "https://images.unsplash.com/photo-1559028012-481c04fa702d?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Duración", "10 días", "Reuniones", "3 sesiones"), true, "15.00"),
                demo("Auditoría UX y rendimiento", "auditoria-ux", "DEMO-SERVICE-UX",
                        "Revisión técnica y visual con oportunidades priorizadas para mejorar la conversión.", "Informe de rendimiento, accesibilidad, navegación y contenido.",
                        ProductType.SERVICE, "Consultoría", "NaT Technologies", "149.00", "189.00", 999,
                        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Entregable", "Informe PDF", "Sesión", "60 minutos"), false, "15.00"),
                demo("Kit de marca digital", "kit-marca-digital", "DEMO-DIGITAL-BRAND",
                        "Recursos visuales editables para lanzar una marca con coherencia.", "Paleta, tipografías, portadas y plantillas para redes.",
                        ProductType.DIGITAL, "Recursos", "TrastCMS Studio", "49.00", "69.00", 999,
                        "https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Formatos", "SVG, PNG y Figma", "Licencia", "Comercial"), false, "15.00"),
                demo("Plugin de analítica editorial", "plugin-analitica", "DEMO-PLUGIN-ANALYTICS",
                        "Panel de métricas para páginas, campañas y publicaciones.", "Indicadores editoriales, conversiones y fuentes de tráfico.",
                        ProductType.DIGITAL, "Plugins", "TrastCMS Labs", "59.00", null, 999,
                        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Actualizaciones", "12 meses", "Sitios", "1"), true, "15.00"),
                demo("Soporte técnico prioritario", "soporte-prioritario", "DEMO-SERVICE-SUPPORT",
                        "Bolsa de acompañamiento para configuraciones, despliegues y extensiones.", "Cinco horas de soporte especializado.",
                        ProductType.SERVICE, "Soporte", "NaT Technologies", "199.00", null, 999,
                        "https://images.unsplash.com/photo-1521737711867-e3b97375f902?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Horas", "5", "Vigencia", "90 días"), false, "15.00"),
                demo("Camiseta TrastCMS", "camiseta-trastcms", "DEMO-PHYSICAL-SHIRT",
                        "Camiseta de algodón para la comunidad TrastCMS.", "Disponible en varias tallas con inventario controlado.",
                        ProductType.PHYSICAL, "Merchandising", "TrastCMS", "24.90", "29.90", 34,
                        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Material", "Algodón", "Color", "Negro"), true, "15.00"),
                demo("Taza para desarrolladores", "taza-desarrolladores", "DEMO-PHYSICAL-MUG",
                        "Taza cerámica con identidad TrastCMS.", "Ideal para oficinas, equipos y comunidades tecnológicas.",
                        ProductType.PHYSICAL, "Merchandising", "TrastCMS", "16.00", null, 18,
                        "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Capacidad", "350 ml", "Material", "Cerámica"), false, "15.00"),
                demo("Paquete de iconos para dashboard", "iconos-dashboard", "DEMO-DIGITAL-ICONS",
                        "Colección de iconos SVG optimizados para aplicaciones administrativas.", "Más de 180 iconos organizados y listos para personalizar.",
                        ProductType.DIGITAL, "Recursos", "TrastCMS Studio", "29.00", null, 999,
                        "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Iconos", "180+", "Formato", "SVG"), false, "0.00"),
                demo("Mantenimiento mensual", "mantenimiento-mensual", "DEMO-SERVICE-MAINTENANCE",
                        "Revisión recurrente de seguridad, respaldos y actualizaciones.", "Acompañamiento operativo para mantener el sitio estable.",
                        ProductType.SERVICE, "Soporte", "NaT Technologies", "89.00", null, 999,
                        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1200&q=80",
                        Map.of("Frecuencia", "Mensual", "Reportes", "Incluidos"), true, "15.00"));
    }

    private DemoProduct demo(
            String name,
            String slug,
            String sku,
            String description,
            String shortDescription,
            ProductType type,
            String category,
            String brand,
            String price,
            String compareAt,
            int stock,
            String image,
            Map<String, Object> attributes,
            boolean featured,
            String taxRate) {
        return new DemoProduct(
                sku,
                new StoreDtos.ProductRequest(
                        name,
                        slug,
                        sku,
                        description,
                        shortDescription,
                        type,
                        ProductStatus.ACTIVE,
                        new BigDecimal(price),
                        compareAt == null ? null : new BigDecimal(compareAt),
                        "USD",
                        category,
                        brand,
                        image,
                        List.of(image),
                        attributes,
                        Map.of(
                                "title", name,
                                "description", shortDescription),
                        featured,
                        type == ProductType.PHYSICAL,
                        false,
                        stock,
                        5,
                        type == ProductType.PHYSICAL ? 350 : null,
                        type == ProductType.DIGITAL ? "/downloads/demo" : null,
                        !"0.00".equals(taxRate),
                        "0.00".equals(taxRate) ? "EXEMPT" : "STANDARD",
                        new BigDecimal(taxRate),
                        List.of()));
    }

    private String normalizeTaxClass(String value) {
        String normalized = value == null || value.isBlank()
                ? "STANDARD"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_-]{2,40}")) {
            throw new BadRequestException("Clase de impuesto inválida");
        }
        return normalized;
    }

    private BigDecimal percent(BigDecimal value) {
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0
                || normalized.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("La tasa debe estar entre 0 y 100");
        }
        return normalized;
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

    private record DemoProduct(
            String sku,
            StoreDtos.ProductRequest request) { }
}
