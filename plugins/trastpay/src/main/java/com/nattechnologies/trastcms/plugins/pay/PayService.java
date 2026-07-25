package com.nattechnologies.trastcms.plugins.pay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PaymentGateway;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.CommerceRegistry;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PluginEventDispatcher;
import com.nattechnologies.trastcms.service.PluginSecretCipher;
import com.nattechnologies.trastcms.service.SiteSettingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PayService {
    private static final String DEMO_DISMISSED_KEY = "plugins.trastpay.demo.dismissed";
    private final CommerceCartRepository carts;
    private final CommerceCustomerRepository customers;
    private final CommerceOrderRepository orders;
    private final CommercePaymentRepository payments;
    private final CommerceCouponRepository coupons;
    private final CommerceSettingsRepository configuration;
    private final CommerceRegistry registry;
    private final ObjectMapper mapper;
    private final PluginEventDispatcher events;
    private final PayPluginGuard guard;
    private final SiteSettingService siteSettings;
    private final PluginSecretCipher cipher;
    private final SecureRandom random = new SecureRandom();

    public PayService(
            CommerceCartRepository carts,
            CommerceCustomerRepository customers,
            CommerceOrderRepository orders,
            CommercePaymentRepository payments,
            CommerceCouponRepository coupons,
            CommerceSettingsRepository configuration,
            CommerceRegistry registry,
            ObjectMapper mapper,
            PluginEventDispatcher events,
            PayPluginGuard guard,
            SiteSettingService siteSettings,
            PluginSecretCipher cipher) {
        this.carts = carts;
        this.customers = customers;
        this.orders = orders;
        this.payments = payments;
        this.coupons = coupons;
        this.configuration = configuration;
        this.registry = registry;
        this.mapper = mapper;
        this.events = events;
        this.guard = guard;
        this.siteSettings = siteSettings;
        this.cipher = cipher;
    }

    @Transactional
    public void initializeDefaults() {
        if (configuration.findById(CommerceSettings.DEFAULT_ID).isEmpty()) {
            configuration.save(new CommerceSettings());
        }
        if (coupons.count() == 0
                && !siteSettings.getBoolean(DEMO_DISMISSED_KEY, false)) {
            generateDemoDataInternal();
        }
    }

    @Transactional
    public synchronized PayDtos.DemoDataResponse generateDemoData() {
        guard.requireEnabled();
        siteSettings.put(DEMO_DISMISSED_KEY, "false");
        return generateDemoDataInternal();
    }

    private PayDtos.DemoDataResponse generateDemoDataInternal() {
        List<DemoCoupon> demoCoupons = List.of(
                new DemoCoupon("DEMO10", "PERCENT", "10.00", "0.00", 100),
                new DemoCoupon("BIENVENIDA15", "PERCENT", "15.00", "50.00", 50),
                new DemoCoupon("TRASTSAAS20", "FIXED", "20.00", "100.00", 25));
        int created = 0;
        int existing = 0;
        for (DemoCoupon demo : demoCoupons) {
            if (coupons.findByCodeIgnoreCase(demo.code()).isPresent()) {
                existing++;
                continue;
            }
            CommerceCoupon coupon = new CommerceCoupon();
            coupon.code = demo.code();
            coupon.discountType = demo.type();
            coupon.discountValue = new BigDecimal(demo.value());
            coupon.minimumAmount = new BigDecimal(demo.minimum());
            coupon.maximumUses = demo.maximumUses();
            coupon.active = true;
            coupon.demoData = true;
            coupons.save(coupon);
            created++;
        }
        return new PayDtos.DemoDataResponse(
                created,
                existing,
                0,
                created == 0
                        ? "Los cupones demostrativos ya estaban disponibles."
                        : "Se crearon cupones de prueba para validar descuentos y checkout.");
    }

    @Transactional
    public PayDtos.DemoDataResponse deleteDemoData() {
        guard.requireEnabled();
        List<CommerceCoupon> demo = coupons.findByDemoDataTrue();
        coupons.deleteAll(demo);
        siteSettings.put(DEMO_DISMISSED_KEY, "true");
        return new PayDtos.DemoDataResponse(
                0,
                0,
                demo.size(),
                demo.isEmpty()
                        ? "No había cupones demostrativos."
                        : "Se eliminaron únicamente los cupones demostrativos.");
    }

    @Transactional
    public PayDtos.CartResponse createCart(String currency) {
        guard.requireEnabled();
        CommerceSettings settings = currentSettings();
        CommerceCart cart = new CommerceCart();
        cart.currency = normalizeCurrency(
                currency == null || currency.isBlank()
                        ? settings.defaultCurrency
                        : currency);
        cart.expiresAt = Instant.now().plusSeconds(
                86400L * settings.cartExpirationDays);
        return cart(carts.save(cart), null);
    }

    @Transactional(readOnly = true)
    public PayDtos.CartResponse getCart(String token, String coupon) {
        guard.requireEnabled();
        return cart(requireCart(token), coupon);
    }

    @Transactional
    public PayDtos.CartResponse add(
            String token,
            PayDtos.AddItemRequest request,
            String coupon) {
        guard.requireEnabled();
        CommerceCart cart = requireCart(token);
        ensureActive(cart);
        SellableProvider.SellableItem item = registry.requireItem(
                request.providerKey(), request.reference());
        if (!item.available()) {
            throw new BadRequestException("El producto no está disponible");
        }
        if (!cart.currency.equalsIgnoreCase(item.currency())) {
            throw new BadRequestException("El carrito no puede mezclar monedas");
        }

        Map<String, Object> resolvedMetadata = merge(
                item.metadata(), request.metadata());
        String metadataJson = write(resolvedMetadata);
        CommerceCartItem line = cart.items.stream()
                .filter(existing -> existing.providerKey.equals(request.providerKey())
                        && existing.itemReference.equals(request.reference())
                        && Objects.equals(existing.metadataJson, metadataJson))
                .findFirst()
                .orElseGet(() -> {
                    CommerceCartItem created = new CommerceCartItem();
                    // The response is built before Hibernate flushes the managed
                    // cart. Assign the UUID now so tax maps and DTOs never receive
                    // a null line identifier.
                    created.id = UUID.randomUUID().toString();
                    created.cart = cart;
                    created.providerKey = item.providerKey();
                    created.itemReference = item.reference();
                    created.itemType = item.type();
                    created.name = item.name();
                    created.unitPrice = money(item.unitPrice());
                    created.currency = item.currency();
                    created.metadataJson = metadataJson;
                    created.quantity = 0;
                    cart.items.add(created);
                    return created;
                });
        line.quantity = Math.min(999, line.quantity + request.quantity());
        carts.save(cart);
        events.publish(PluginEvent.of(
                "commerce.cart.updated",
                Map.of("cartToken", cart.cartToken, "items", cart.items.size())));
        return cart(cart, coupon);
    }

    @Transactional
    public PayDtos.CartResponse update(
            String token,
            String itemId,
            PayDtos.UpdateItemRequest request,
            String coupon) {
        guard.requireEnabled();
        CommerceCart cart = requireCart(token);
        CommerceCartItem line = cart.items.stream()
                .filter(item -> item.id.equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Línea no encontrada"));
        if (request.quantity() == 0) cart.items.remove(line);
        else line.quantity = request.quantity();
        carts.save(cart);
        return cart(cart, coupon);
    }

    @Transactional
    public PayDtos.CartResponse clear(String token) {
        CommerceCart cart = requireCart(token);
        cart.items.clear();
        return cart(carts.save(cart), null);
    }

    @Transactional
    public PayDtos.CheckoutResponse checkout(
            String token,
            PayDtos.CheckoutRequest request) {
        guard.requireEnabled();
        CommerceCart cart = requireCart(token);
        ensureActive(cart);
        if (cart.items.isEmpty()) {
            throw new BadRequestException("El carrito está vacío");
        }

        if (request.idempotencyKey() != null
                && !request.idempotencyKey().isBlank()) {
            var existing = payments.findByIdempotencyKey(
                    request.idempotencyKey());
            if (existing.isPresent()) {
                CommercePayment payment = existing.get();
                return new PayDtos.CheckoutResponse(
                        payment.order.id,
                        payment.order.orderNumber,
                        payment.order.publicToken,
                        payment.order.status.name(),
                        payment.id,
                        payment.status.name(),
                        payment.redirectUrl,
                        "Solicitud procesada previamente");
            }
        }

        CommerceCustomer customer = customers
                .findByEmailIgnoreCase(request.email())
                .orElseGet(CommerceCustomer::new);
        customer.email = request.email().trim().toLowerCase(Locale.ROOT);
        customer.displayName = request.name().trim();
        customer.phone = blank(request.phone());
        customer = customers.save(customer);

        Totals totals = totals(cart, request.couponCode());
        CommerceOrder order = new CommerceOrder();
        order.orderNumber = orderNumber();
        order.publicToken = UUID.randomUUID().toString().replace("-", "");
        order.currency = cart.currency;
        order.customer = customer;
        order.customerEmail = customer.email;
        order.customerName = customer.displayName;
        order.subtotal = totals.subtotal();
        order.discountTotal = totals.discount();
        order.taxTotal = totals.tax();
        order.grandTotal = totals.total();
        order.couponCode = totals.couponCode();
        order.billingJson = write(request.billing());
        order.shippingJson = write(request.shipping());
        order.notes = blank(request.notes());

        for (CommerceCartItem cartItem : cart.items) {
            CommerceOrderItem orderItem = new CommerceOrderItem();
            orderItem.order = order;
            orderItem.providerKey = cartItem.providerKey;
            orderItem.itemReference = cartItem.itemReference;
            orderItem.itemType = cartItem.itemType;
            orderItem.name = cartItem.name;
            orderItem.quantity = cartItem.quantity;
            orderItem.unitPrice = cartItem.unitPrice;
            orderItem.lineTotal = money(cartItem.unitPrice.multiply(
                    BigDecimal.valueOf(cartItem.quantity)));
            orderItem.metadataJson = cartItem.metadataJson;
            order.items.add(orderItem);
        }
        order = orders.save(order);

        PaymentGateway gateway = registry.requireGateway(request.gatewayKey());
        String idempotencyKey = request.idempotencyKey() == null
                || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey();
        PaymentGateway.PaymentResult result = gateway.create(
                new PaymentGateway.PaymentRequest(
                        order.id,
                        order.orderNumber,
                        order.grandTotal,
                        order.currency,
                        order.customerEmail,
                        request.returnUrl(),
                        request.cancelUrl(),
                        idempotencyKey,
                        Map.of()));

        CommercePayment payment = new CommercePayment();
        payment.order = order;
        payment.gatewayKey = gateway.key();
        payment.amount = order.grandTotal;
        payment.currency = order.currency;
        payment.externalReference = result.externalReference();
        payment.redirectUrl = result.redirectUrl();
        payment.idempotencyKey = idempotencyKey;
        payment.metadataJson = write(result.metadata());
        payment.status = parsePaymentStatus(result.status());
        payment = payments.save(payment);

        if (payment.status == PaymentStatus.SUCCEEDED) {
            markPaid(order, payment);
        } else {
            order.status = OrderStatus.PENDING_PAYMENT;
        }
        orders.save(order);
        cart.status = CartStatus.CONVERTED;
        carts.save(cart);

        events.publish(PluginEvent.of(
                "commerce.order.created",
                Map.of(
                        "orderId", order.id,
                        "orderNumber", order.orderNumber,
                        "total", order.grandTotal)));
        return new PayDtos.CheckoutResponse(
                order.id,
                order.orderNumber,
                order.publicToken,
                order.status.name(),
                payment.id,
                payment.status.name(),
                payment.redirectUrl,
                result.message());
    }

    @Transactional
    public PayDtos.OrderResponse capturePayPal(
            String orderId,
            String paypalOrderId) {
        guard.requireEnabled();
        CommerceOrder order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada"));
        CommercePayment payment = payments.findByExternalReference(paypalOrderId)
                .filter(item -> item.order.id.equals(order.id))
                .orElseThrow(() -> new NotFoundException(
                        "Pago PayPal no encontrado para la orden"));
        if (!"paypal".equals(payment.gatewayKey)) {
            throw new BadRequestException("La orden no utiliza PayPal");
        }
        if (payment.status == PaymentStatus.SUCCEEDED) {
            return order(order);
        }

        PaymentGateway.PaymentResult result = registry
                .requireGateway("paypal")
                .capture(paypalOrderId, payment.amount);
        payment.status = parsePaymentStatus(result.status());
        payment.metadataJson = write(merge(
                read(payment.metadataJson),
                result.metadata()));
        payment.redirectUrl = null;
        payments.save(payment);
        if (payment.status == PaymentStatus.SUCCEEDED) {
            markPaid(order, payment);
        }
        return order(order);
    }

    @Transactional
    public void confirmPayPalWebhook(
            String paypalOrderId,
            String captureId) {
        guard.requireEnabled();
        CommercePayment payment = payments
                .findByExternalReference(paypalOrderId)
                .orElseThrow(() -> new NotFoundException(
                        "Pago PayPal no encontrado"));
        if (payment.status == PaymentStatus.SUCCEEDED) return;

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("paypalOrderId", paypalOrderId);
        if (captureId != null && !captureId.isBlank()) {
            update.put("paypalCaptureId", captureId);
        }
        update.put("paypalCaptureStatus", "COMPLETED");
        payment.metadataJson = write(merge(
                read(payment.metadataJson), update));
        payment.status = PaymentStatus.SUCCEEDED;
        payments.save(payment);
        markPaid(payment.order, payment);
    }

    @Transactional
    public PayDtos.OrderResponse markPayment(String paymentId, String status) {
        guard.requireEnabled();
        CommercePayment payment = payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        payment.status = parsePaymentStatus(status);
        if (payment.status == PaymentStatus.SUCCEEDED) {
            markPaid(payment.order, payment);
        } else if (payment.status == PaymentStatus.FAILED) {
            payment.order.status = OrderStatus.FAILED;
            orders.save(payment.order);
        }
        payments.save(payment);
        return order(payment.order);
    }

    @Transactional
    public PayDtos.OrderResponse updateOrderStatus(String id, String status) {
        guard.requireEnabled();
        CommerceOrder order = orders.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada"));
        try {
            order.status = OrderStatus.valueOf(status);
        } catch (Exception exception) {
            throw new BadRequestException("Estado inválido");
        }
        return order(orders.save(order));
    }

    @Transactional(readOnly = true)
    public PayDtos.PageResponse<PayDtos.OrderResponse> orders(int page, int size) {
        guard.requireEnabled();
        Page<CommerceOrder> result = orders.findAllByOrderByCreatedAtDesc(
                PageRequest.of(
                        Math.max(0, page),
                        Math.min(Math.max(size, 1), 100)));
        return new PayDtos.PageResponse<>(
                result.map(this::order).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PayDtos.OrderResponse order(String id) {
        guard.requireEnabled();
        return order(orders.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada")));
    }

    @Transactional(readOnly = true)
    public PayDtos.Dashboard dashboard() {
        guard.requireEnabled();
        List<CommerceOrder> all = orders.findAll();
        BigDecimal revenue = all.stream()
                .filter(order -> Set.of(
                                OrderStatus.PAID,
                                OrderStatus.COMPLETED)
                        .contains(order.status))
                .map(order -> order.grandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PayDtos.OrderResponse> recent = orders
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8))
                .map(this::order)
                .getContent();
        return new PayDtos.Dashboard(
                all.size(),
                all.stream().filter(order ->
                        order.status == OrderStatus.PAID
                                || order.status == OrderStatus.COMPLETED).count(),
                all.stream().filter(order ->
                        order.status == OrderStatus.PENDING_PAYMENT).count(),
                money(revenue),
                payments.count(),
                coupons.count(),
                coupons.countByDemoDataTrue(),
                recent,
                registry.gateways());
    }

    @Transactional(readOnly = true)
    public List<PayDtos.CouponResponse> coupons() {
        guard.requireEnabled();
        return coupons.findAll().stream().map(this::coupon).toList();
    }

    @Transactional
    public PayDtos.CouponResponse saveCoupon(
            String id,
            PayDtos.CouponRequest request) {
        guard.requireEnabled();
        CommerceCoupon coupon = id == null
                ? new CommerceCoupon()
                : coupons.findById(id)
                        .orElseThrow(() -> new NotFoundException("Cupón no encontrado"));
        coupons.findByCodeIgnoreCase(request.code())
                .filter(existing -> !Objects.equals(existing.id, id))
                .ifPresent(existing -> {
                    throw new BadRequestException("El código ya existe");
                });
        coupon.code = request.code().trim().toUpperCase(Locale.ROOT);
        coupon.discountType = request.discountType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PERCENT", "FIXED").contains(coupon.discountType)) {
            throw new BadRequestException("Tipo de descuento inválido");
        }
        coupon.discountValue = request.discountValue();
        coupon.minimumAmount = request.minimumAmount();
        coupon.maximumUses = request.maximumUses();
        coupon.startsAt = request.startsAt();
        coupon.endsAt = request.endsAt();
        coupon.active = request.active();
        if (id == null) coupon.demoData = false;
        return coupon(coupons.save(coupon));
    }

    @Transactional
    public void deleteCoupon(String id) {
        guard.requireEnabled();
        coupons.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PayDtos.SettingsResponse settings() {
        guard.requireEnabled();
        return settings(currentSettings());
    }

    @Transactional(readOnly = true)
    public PayDtos.SettingsResponse publicSettings() {
        guard.requireEnabled();
        return settings(currentSettings());
    }

    @Transactional
    public PayDtos.SettingsResponse saveSettings(PayDtos.SettingsRequest request) {
        guard.requireEnabled();
        CommerceSettings settings = currentSettings();
        settings.defaultCurrency = normalizeCurrency(request.defaultCurrency());
        settings.taxRate = percent(request.taxRate());
        settings.pricesIncludeTax = request.pricesIncludeTax();
        settings.taxLabel = blank(request.taxLabel()) == null
                ? "Impuesto"
                : request.taxLabel().trim();
        settings.taxCountry = request.taxCountry() == null
                ? "HN"
                : request.taxCountry().toUpperCase(Locale.ROOT);
        settings.cartExpirationDays = request.cartExpirationDays();
        settings.guestCheckout = request.guestCheckout();
        settings.collectShipping = request.collectShipping();
        settings.companyName = blank(request.companyName());
        settings.supportEmail = blank(request.supportEmail());
        settings.termsUrl = blank(request.termsUrl());
        settings.bankInstructions = blank(request.bankInstructions());
        settings.paypalEnabled = request.paypalEnabled();
        settings.paypalMode = request.paypalMode() == null
                ? "SANDBOX"
                : request.paypalMode().toUpperCase(Locale.ROOT);
        settings.paypalClientId = blank(request.paypalClientId());
        settings.paypalWebhookId = blank(request.paypalWebhookId());
        String paypalSecret = blank(request.paypalClientSecret());
        if (paypalSecret != null) {
            settings.paypalClientSecretCipher = cipher.encrypt(paypalSecret);
        }
        if (settings.paypalEnabled
                && (settings.paypalClientId == null
                    || settings.paypalClientSecretCipher == null)) {
            throw new BadRequestException(
                    "Configure Client ID y Client Secret antes de activar PayPal");
        }
        return settings(configuration.save(settings));
    }

    public List<Map<String, Object>> gateways() {
        guard.requireEnabled();
        return registry.gateways();
    }

    private void markPaid(CommerceOrder order, CommercePayment payment) {
        if (order.paidAt != null) return;
        order.status = OrderStatus.PAID;
        order.paidAt = Instant.now();
        orders.save(order);
        for (CommerceOrderItem item : order.items) {
            if (item.fulfilledAt != null) continue;
            registry.requireProvider(item.providerKey).fulfill(
                    new SellableProvider.FulfillmentRequest(
                            order.id,
                            order.orderNumber,
                            order.customer.id,
                            order.customerEmail,
                            item.itemReference,
                            item.quantity,
                            read(item.metadataJson)));
            item.fulfilledAt = Instant.now();
        }
        orders.save(order);
        if (order.couponCode != null) {
            coupons.findByCodeIgnoreCase(order.couponCode).ifPresent(coupon -> {
                coupon.usedCount++;
                coupons.save(coupon);
            });
        }
        events.publish(PluginEvent.of(
                "commerce.payment.succeeded",
                Map.of(
                        "orderId", order.id,
                        "paymentId", payment.id,
                        "amount", payment.amount)));
    }

    private CommerceCart requireCart(String token) {
        return carts.findByCartToken(token)
                .orElseThrow(() -> new NotFoundException("Carrito no encontrado"));
    }

    private void ensureActive(CommerceCart cart) {
        if (cart.status != CartStatus.ACTIVE
                || cart.expiresAt.isBefore(Instant.now())) {
            throw new BadRequestException("El carrito ya no está activo");
        }
    }

    private PayDtos.CartResponse cart(CommerceCart cart, String couponCode) {
        CommerceSettings settings = currentSettings();
        Totals totals = totals(cart, couponCode);
        List<PayDtos.CartItemResponse> lines = cart.items.stream()
                .map(item -> {
                    SellableProvider.SellableItem resolved = registry.requireItem(
                            item.providerKey, item.itemReference);
                    LineTax lineTax = totals.lines().getOrDefault(
                            item.id,
                            new LineTax(false, BigDecimal.ZERO, BigDecimal.ZERO));
                    return new PayDtos.CartItemResponse(
                            item.id,
                            item.providerKey,
                            item.itemReference,
                            item.itemType,
                            item.name,
                            item.quantity,
                            item.unitPrice,
                            money(item.unitPrice.multiply(
                                    BigDecimal.valueOf(item.quantity))),
                            item.currency,
                            resolved.imageUrl(),
                            lineTax.taxable(),
                            lineTax.rate(),
                            lineTax.amount(),
                            read(item.metadataJson));
                })
                .toList();
        return new PayDtos.CartResponse(
                cart.cartToken,
                cart.status.name(),
                cart.currency,
                lines,
                totals.subtotal(),
                totals.discount(),
                totals.tax(),
                totals.total(),
                settings.taxLabel,
                settings.pricesIncludeTax,
                cart.expiresAt);
    }

    private Totals totals(CommerceCart cart, String couponCode) {
        CommerceSettings settings = currentSettings();
        BigDecimal subtotal = cart.items.stream()
                .map(item -> item.unitPrice.multiply(
                        BigDecimal.valueOf(item.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO;
        String normalizedCoupon = null;

        if (couponCode != null && !couponCode.isBlank()) {
            CommerceCoupon coupon = coupons.findByCodeIgnoreCase(couponCode)
                    .orElseThrow(() -> new BadRequestException("Cupón inválido"));
            Instant now = Instant.now();
            if (!coupon.active
                    || (coupon.startsAt != null && coupon.startsAt.isAfter(now))
                    || (coupon.endsAt != null && coupon.endsAt.isBefore(now))
                    || (coupon.maximumUses != null
                        && coupon.usedCount >= coupon.maximumUses)
                    || (coupon.minimumAmount != null
                        && subtotal.compareTo(coupon.minimumAmount) < 0)) {
                throw new BadRequestException("El cupón no está disponible");
            }
            discount = coupon.discountType.equals("PERCENT")
                    ? subtotal.multiply(coupon.discountValue)
                            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    : coupon.discountValue;
            discount = discount.min(subtotal);
            normalizedCoupon = coupon.code;
        }

        BigDecimal discountedSubtotal = subtotal.subtract(discount);
        BigDecimal ratio = subtotal.signum() == 0
                ? BigDecimal.ONE
                : discountedSubtotal.divide(subtotal, 8, RoundingMode.HALF_UP);
        BigDecimal taxTotal = BigDecimal.ZERO;
        Map<String, LineTax> lineTaxes = new HashMap<>();

        for (CommerceCartItem item : cart.items) {
            Map<String, Object> metadata = read(item.metadataJson);
            boolean taxable = booleanValue(metadata.get("taxable"), true);
            BigDecimal rate = taxable
                    ? decimalValue(metadata.get("taxRate"), settings.taxRate)
                    : BigDecimal.ZERO;
            BigDecimal lineSubtotal = item.unitPrice.multiply(
                    BigDecimal.valueOf(item.quantity));
            BigDecimal discountedLine = lineSubtotal.multiply(ratio);
            BigDecimal lineTax = BigDecimal.ZERO;
            if (taxable && rate.signum() > 0) {
                if (settings.pricesIncludeTax) {
                    BigDecimal divisor = BigDecimal.ONE.add(
                            rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
                    lineTax = discountedLine.subtract(
                            discountedLine.divide(divisor, 8, RoundingMode.HALF_UP));
                } else {
                    lineTax = discountedLine.multiply(rate)
                            .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
                }
            }
            lineTax = money(lineTax);
            taxTotal = taxTotal.add(lineTax);
            lineTaxes.put(item.id, new LineTax(taxable, rate, lineTax));
        }

        BigDecimal total = settings.pricesIncludeTax
                ? discountedSubtotal
                : discountedSubtotal.add(taxTotal);
        return new Totals(
                money(subtotal),
                money(discount),
                money(taxTotal),
                money(total),
                normalizedCoupon,
                Map.copyOf(lineTaxes));
    }

    private PayDtos.OrderResponse order(CommerceOrder order) {
        return new PayDtos.OrderResponse(
                order.id,
                order.orderNumber,
                order.status.name(),
                order.currency,
                order.customerEmail,
                order.customerName,
                order.subtotal,
                order.discountTotal,
                order.taxTotal,
                order.grandTotal,
                order.couponCode,
                order.items.stream()
                        .map(item -> new PayDtos.OrderLine(
                                item.id,
                                item.providerKey,
                                item.itemReference,
                                item.itemType,
                                item.name,
                                item.quantity,
                                item.unitPrice,
                                item.lineTotal,
                                item.fulfilledAt != null))
                        .toList(),
                order.paidAt,
                order.createdAt,
                order.updatedAt);
    }

    private PayDtos.CouponResponse coupon(CommerceCoupon coupon) {
        return new PayDtos.CouponResponse(
                coupon.id,
                coupon.code,
                coupon.discountType,
                coupon.discountValue,
                coupon.minimumAmount,
                coupon.maximumUses,
                coupon.usedCount,
                coupon.startsAt,
                coupon.endsAt,
                coupon.active,
                coupon.demoData);
    }

    private CommerceSettings currentSettings() {
        return configuration.findById(CommerceSettings.DEFAULT_ID)
                .orElseGet(CommerceSettings::new);
    }

    private PayDtos.SettingsResponse settings(CommerceSettings settings) {
        return new PayDtos.SettingsResponse(
                settings.defaultCurrency,
                settings.taxRate,
                settings.pricesIncludeTax,
                settings.taxLabel,
                settings.taxCountry,
                settings.cartExpirationDays,
                settings.guestCheckout,
                settings.collectShipping,
                settings.companyName,
                settings.supportEmail,
                settings.termsUrl,
                settings.bankInstructions,
                settings.paypalEnabled,
                settings.paypalMode,
                settings.paypalClientId,
                settings.paypalClientId != null
                        && settings.paypalClientSecretCipher != null,
                settings.paypalWebhookId,
                settings.updatedAt);
    }

    private PaymentStatus parsePaymentStatus(String value) {
        try {
            return PaymentStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            return PaymentStatus.PENDING;
        }
    }

    private String orderNumber() {
        return "TR-"
                + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                        .withZone(java.time.ZoneOffset.UTC)
                        .format(Instant.now())
                + "-"
                + (100000 + random.nextInt(900000));
    }

    private String normalizeCurrency(String value) {
        String normalized = value == null || value.isBlank()
                ? "USD"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new BadRequestException("Moneda inválida");
        }
        return normalized;
    }

    private BigDecimal percent(BigDecimal value) {
        BigDecimal normalized = value == null
                ? BigDecimal.ZERO
                : value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0
                || normalized.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("La tasa debe estar entre 0 y 100");
        }
        return normalized;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new BadRequestException("Datos inválidos");
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return value == null || value.isBlank()
                    ? Map.of()
                    : mapper.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private Map<String, Object> merge(
            Map<String, Object> first,
            Map<String, Object> second) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (first != null) result.putAll(first);
        if (second != null) result.putAll(second);
        return result;
    }

    private BigDecimal decimalValue(Object value, BigDecimal fallback) {
        if (value == null) return fallback;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record LineTax(
            boolean taxable,
            BigDecimal rate,
            BigDecimal amount) { }

    private record Totals(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal tax,
            BigDecimal total,
            String couponCode,
            Map<String, LineTax> lines) { }

    private record DemoCoupon(
            String code,
            String type,
            String value,
            String minimum,
            Integer maximumUses) { }
}
