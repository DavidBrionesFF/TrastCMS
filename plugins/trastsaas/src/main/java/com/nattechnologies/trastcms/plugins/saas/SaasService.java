package com.nattechnologies.trastcms.plugins.saas;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.plugins.pay.OrderAccessService;
import com.nattechnologies.trastcms.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class SaasService {
    private static final String DEMO_DISMISSED_KEY = "plugins.trastsaas.demo.dismissed";
    private final SaasProductRepository products;
    private final SaasPlanRepository plans;
    private final SaasSubscriptionRepository subscriptions;
    private final SaasLicenseRepository licenses;
    private final SaasActivationRepository activations;
    private final SaasUsageEventRepository usage;
    private final SaasReleaseRepository releases;
    private final SaasSettingsRepository settingsRepository;
    private final ObjectMapper mapper;
    private final PluginSecretCipher cipher;
    private final PluginEventDispatcher events;
    private final SaasPluginGuard guard;
    private final OrderAccessService orderAccess;
    private final SiteSettingService siteSettings;
    private final SecureRandom random = new SecureRandom();

    public SaasService(
            SaasProductRepository products,
            SaasPlanRepository plans,
            SaasSubscriptionRepository subscriptions,
            SaasLicenseRepository licenses,
            SaasActivationRepository activations,
            SaasUsageEventRepository usage,
            SaasReleaseRepository releases,
            SaasSettingsRepository settingsRepository,
            ObjectMapper mapper,
            PluginSecretCipher cipher,
            PluginEventDispatcher events,
            SaasPluginGuard guard,
            OrderAccessService orderAccess,
            SiteSettingService siteSettings) {
        this.products = products;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.licenses = licenses;
        this.activations = activations;
        this.usage = usage;
        this.releases = releases;
        this.settingsRepository = settingsRepository;
        this.mapper = mapper;
        this.cipher = cipher;
        this.events = events;
        this.guard = guard;
        this.orderAccess = orderAccess;
        this.siteSettings = siteSettings;
    }

    @Transactional
    public void initializeDefaults() {
        settings();
        if (products.count() == 0
                && !siteSettings.getBoolean(DEMO_DISMISSED_KEY, false)) {
            generateDemoData();
        }
    }

    @Transactional
    public synchronized SaasDtos.DemoDataResponse generateDemoData() {
        guard.requireEnabled();
        siteSettings.put(DEMO_DISMISSED_KEY, "false");
        SaasProduct cloud = upsertDemoProduct(
                "trast-cloud",
                "Trast Cloud",
                "Plataforma colaborativa para demostrar suscripciones, límites por plan, API y licencias por dispositivo.",
                "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=640&q=80");
        SaasPlan cloudStarter = upsertDemoPlan(cloud, "starter", "Starter",
                "Para profesionales que comienzan a vender una solución digital.",
                "19.00", "MONTH", 1, 14, 2,
                mapOf("users.max", 3, "projects.max", 5, "api.enabled", false,
                        "storage.gb", 10, "support.level", "community"), false);
        SaasPlan cloudPro = upsertDemoPlan(cloud, "pro", "Pro",
                "Automatización, reportes avanzados y API para equipos en crecimiento.",
                "49.00", "MONTH", 1, 14, 10,
                mapOf("users.max", 20, "projects.max", 100, "api.enabled", true,
                        "reports.advanced", true, "storage.gb", 100,
                        "support.level", "priority"), true);
        SaasPlan cloudBusiness = upsertDemoPlan(cloud, "business", "Business",
                "Marca blanca, mayor capacidad y soporte prioritario para organizaciones.",
                "129.00", "MONTH", 1, 30, 100,
                mapOf("users.max", 100, "projects.max", 1000, "api.enabled", true,
                        "reports.advanced", true, "white_label.enabled", true,
                        "support.priority", true, "storage.gb", 500), false);
        upsertDemoPlan(cloud, "pro-annual", "Pro anual",
                "El plan Pro con facturación anual y ahorro frente al pago mensual.",
                "490.00", "YEAR", 1, 14, 10,
                entitlements(cloudPro), true);

        SaasProduct desktop = upsertDemoProduct(
                "trast-desktop",
                "Trast Desktop",
                "Aplicación instalable con licencia perpetua, activaciones por equipo y actualizaciones firmadas.",
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=640&q=80");
        SaasPlan desktopStandard = upsertDemoPlan(desktop, "standard", "Standard",
                "Licencia perpetua para un equipo con actualizaciones durante un año.",
                "149.00", "ONE_TIME", 1, 0, 1,
                mapOf("devices.max", 1, "updates.months", 12,
                        "exports.enabled", true, "offline.enabled", true), false);
        SaasPlan desktopTeam = upsertDemoPlan(desktop, "team", "Team",
                "Licencia perpetua para cinco equipos y funciones colaborativas.",
                "399.00", "ONE_TIME", 1, 0, 5,
                mapOf("devices.max", 5, "updates.months", 24,
                        "exports.enabled", true, "offline.enabled", true,
                        "team.features", true), true);

        ensureDemoSubscription(cloudPro, "ana@empresa-demo.com", SubscriptionStatus.ACTIVE, false);
        ensureDemoSubscription(cloudStarter, "carlos@startup-demo.com", SubscriptionStatus.TRIALING, false);
        ensureDemoSubscription(cloudBusiness, "facturacion@grupo-demo.com", SubscriptionStatus.PAST_DUE, false);
        ensureDemoLicense(
                desktopStandard,
                "licencias@consultora-demo.com",
                null,
                1);
        ensureDemoLicense(
                desktopTeam,
                "tecnologia@agencia-demo.com",
                null,
                5);

        ensureDemoRelease(cloud, "2.6.0", "web", "stable",
                "https://example.invalid/releases/trast-cloud-2.6.0",
                "pro", true);
        ensureDemoRelease(desktop, "1.4.0", "windows", "stable",
                "https://example.invalid/releases/trast-desktop-1.4.0.exe",
                "standard", true);
        ensureDemoRelease(desktop, "1.4.0", "linux", "stable",
                "https://example.invalid/releases/trast-desktop-1.4.0",
                "standard", true);

        events.publish(PluginEvent.of("saas.demo.generated", Map.of(
                "products", products.countByDemoDataTrue(),
                "plans", plans.countByDemoDataTrue())));
        return demoResponse("Datos demo de TrastSaaS generados correctamente.");
    }

    @Transactional
    public SaasDtos.DemoDataResponse deleteDemoData() {
        guard.requireEnabled();
        siteSettings.put(DEMO_DISMISSED_KEY, "true");
        List<SaasLicense> demoLicenses = licenses.findByDemoDataTrue();
        List<SaasSubscription> demoSubscriptions = subscriptions.findByDemoDataTrue();
        List<SaasPlan> demoPlans = plans.findByDemoDataTrue();
        List<SaasProduct> demoProducts = products.findByDemoDataTrue();

        List<String> licenseIds = demoLicenses.stream().map(item -> item.id).toList();
        List<String> subscriptionIds = demoSubscriptions.stream().map(item -> item.id).toList();
        List<String> productIds = demoProducts.stream().map(item -> item.id).toList();

        if (!licenseIds.isEmpty()) activations.deleteByLicenseIdIn(licenseIds);
        if (!subscriptionIds.isEmpty()) usage.deleteBySubscriptionIdIn(subscriptionIds);
        licenses.deleteAll(demoLicenses);
        subscriptions.deleteAll(demoSubscriptions);
        if (!productIds.isEmpty()) releases.deleteByProductIdIn(productIds);
        plans.deleteAll(demoPlans);
        plans.flush();

        // A demo plan may have been attached to a real product with the same
        // productKey. Delete only demo products that are no longer referenced.
        List<SaasProduct> removableProducts = demoProducts.stream()
                .filter(product -> plans.countByProductId(product.id) == 0)
                .toList();
        products.deleteAll(removableProducts);

        events.publish(PluginEvent.of("saas.demo.deleted", Map.of(
                "licenses", demoLicenses.size(),
                "subscriptions", demoSubscriptions.size(),
                "plans", demoPlans.size(),
                "products", removableProducts.size())));
        return new SaasDtos.DemoDataResponse(
                "Datos demo de TrastSaaS eliminados.", 0, 0, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<SaasDtos.ProductResponse> products() {
        guard.requireEnabled();
        return products.findAll().stream().map(this::product).toList();
    }

    @Transactional
    public SaasDtos.ProductResponse saveProduct(String id, SaasDtos.ProductRequest request) {
        guard.requireEnabled();
        SaasProduct product = id == null ? new SaasProduct() : requireProduct(id);
        String normalizedProductKey = request.productKey()
                .trim()
                .toLowerCase(Locale.ROOT);
        products.findByProductKey(normalizedProductKey)
                .filter(existing -> !Objects.equals(existing.id, id))
                .ifPresent(existing -> {
                    throw new BadRequestException("La clave del producto ya existe");
                });
        product.productKey = normalizedProductKey;
        product.name = request.name().trim();
        product.description = blank(request.description());
        product.logoUrl = blank(request.logoUrl());
        product.active = request.active();
        if (id == null) product.demoData = false;
        return product(products.save(product));
    }

    @Transactional(readOnly = true)
    public List<SaasDtos.PlanResponse> plans() {
        guard.requireEnabled();
        return plans.findAll().stream()
                .sorted(Comparator.comparing((SaasPlan item) -> item.product.name)
                        .thenComparing(item -> item.price))
                .map(this::plan)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaasDtos.PlanResponse> publicPlans(String productKey) {
        guard.requireEnabled();
        SaasProduct product = products.findByProductKey(productKey)
                .filter(item -> item.active)
                .orElseThrow(() -> new NotFoundException("Producto SaaS no encontrado"));
        return plans.findByProductIdAndActiveTrueOrderByPriceAsc(product.id)
                .stream().map(this::plan).toList();
    }

    @Transactional
    public SaasDtos.CatalogResponse publicCatalog() {
        guard.requireEnabled();
        List<SaasDtos.ProductResponse> visibleProducts = products.findAll().stream()
                .filter(item -> item.active)
                .map(this::product)
                .toList();
        List<SaasDtos.PlanResponse> visiblePlans = plans.findByActiveTrueOrderByPriceAsc()
                .stream().filter(item -> item.product.active)
                .map(this::plan)
                .toList();
        return new SaasDtos.CatalogResponse(settings(), visibleProducts, visiblePlans);
    }

    @Transactional
    public SaasDtos.PlanResponse savePlan(String id, SaasDtos.PlanRequest request) {
        guard.requireEnabled();
        SaasPlan plan = id == null ? new SaasPlan() : requirePlan(id);
        if (request.productId() == null || request.productId().isBlank()) {
            throw new BadRequestException("Seleccione un producto SaaS");
        }
        if (request.planKey() == null || request.planKey().isBlank()) {
            throw new BadRequestException("La clave del plan es obligatoria");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("El nombre del plan es obligatorio");
        }
        SaasProduct product = requireProduct(request.productId());
        String normalizedPlanKey = request.planKey().trim().toLowerCase(Locale.ROOT);
        plans.findByProductIdAndPlanKey(product.id, normalizedPlanKey)
                .filter(existing -> !Objects.equals(existing.id, id))
                .ifPresent(existing -> {
                    throw new BadRequestException("La clave del plan ya existe");
                });
        plan.product = product;
        plan.planKey = normalizedPlanKey;
        plan.name = request.name().trim();
        plan.description = blank(request.description());
        plan.price = money(request.price());
        plan.currency = request.currency() == null || request.currency().isBlank()
                ? settingsEntity().defaultCurrency
                : request.currency().trim().toUpperCase(Locale.ROOT);
        if (request.billingInterval() == null || request.billingInterval().isBlank()) {
            throw new BadRequestException("Seleccione un intervalo de facturación");
        }
        plan.billingInterval = request.billingInterval().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DAY", "WEEK", "MONTH", "YEAR", "ONE_TIME").contains(plan.billingInterval)) {
            throw new BadRequestException("Intervalo inválido");
        }
        plan.intervalCount = Math.max(request.intervalCount(), 1);
        plan.trialDays = Math.max(request.trialDays(), 0);
        plan.maxActivations = Math.max(request.maxActivations(), 1);
        plan.entitlementsJson = write(request.entitlements() == null ? Map.of() : request.entitlements());
        plan.active = request.active();
        plan.featured = request.featured();
        if (id == null) plan.demoData = false;
        return plan(plans.save(plan));
    }

    @Transactional
    public SaasDtos.LicenseResponse issue(SaasDtos.IssueLicenseRequest request) {
        guard.requireEnabled();
        SaasPlan plan = requirePlan(request.planId());
        Issued issued = issueLicense(plan, request.customerEmail(), null, request.orderId(),
                request.expiresAt(), request.maxActivations(), false);
        return license(issued.license(), issued.rawKey());
    }

    @Transactional
    public void fulfillPlan(
            String planId,
            String customerId,
            String email,
            String orderId,
            int quantity) {
        SaasPlan plan = requirePlan(planId);
        for (int index = 0; index < quantity; index++) {
            SaasSubscription subscription = createSubscription(
                    plan, customerId, email, orderId, false);
            issueLicense(plan, email, subscription, orderId,
                    subscription.currentPeriodEnd, plan.maxActivations, false);
        }
    }

    @Transactional
    public SaasDtos.LicenseResult validate(SaasDtos.LicenseRequest request, boolean activate) {
        guard.requireEnabled();
        SaasLicense license = requireByKey(request.licenseKey(), request.productKey());
        refresh(license);
        if (license.status != LicenseStatus.ACTIVE) {
            return result(false, license, null, "Licencia " + license.status.name());
        }
        SaasActivation activation = null;
        if (activate) {
            SaasSettings settings = settingsEntity();
            if (request.fingerprint() == null || request.fingerprint().isBlank()) {
                throw new BadRequestException("fingerprint es obligatorio");
            }
            if (settings.requireDeviceName
                    && (request.deviceName() == null || request.deviceName().isBlank())) {
                throw new BadRequestException("deviceName es obligatorio");
            }
            String fingerprint = hash(request.fingerprint());
            activation = activations.findByLicenseIdAndFingerprintHash(license.id, fingerprint)
                    .orElse(null);
            if (activation == null) {
                long active = activations.countByLicenseIdAndStatus(
                        license.id, ActivationStatus.ACTIVE);
                if (active >= license.maxActivations) {
                    throw new BadRequestException("Se alcanzó el máximo de activaciones");
                }
                activation = new SaasActivation();
                activation.license = license;
                activation.fingerprintHash = fingerprint;
                activation.deviceName = blank(request.deviceName());
                activation.platform = blank(request.platform());
                activation.applicationVersion = blank(request.applicationVersion());
                activation.status = ActivationStatus.ACTIVE;
                activation.activatedAt = Instant.now();
            } else if (activation.status != ActivationStatus.ACTIVE) {
                activation.status = ActivationStatus.ACTIVE;
                activation.deactivatedAt = null;
            }
            activation.lastHeartbeatAt = Instant.now();
            activation = activations.save(activation);
            events.publish(PluginEvent.of("saas.license.activated", Map.of(
                    "licenseId", license.id,
                    "activationId", activation.id)));
        }
        license.lastValidatedAt = Instant.now();
        licenses.save(license);
        return result(true, license, activation, "Licencia válida");
    }

    @Transactional
    public SaasDtos.LicenseResult heartbeat(SaasDtos.LicenseRequest request) {
        return validate(request, true);
    }

    @Transactional
    public void deactivate(SaasDtos.DeactivateRequest request) {
        guard.requireEnabled();
        SaasLicense license = requireByKey(request.licenseKey(), request.productKey());
        String fingerprint = hash(request.fingerprint());
        SaasActivation activation = activations
                .findByLicenseIdAndFingerprintHash(license.id, fingerprint)
                .orElseThrow(() -> new NotFoundException("Activación no encontrada"));
        activation.status = ActivationStatus.DEACTIVATED;
        activation.deactivatedAt = Instant.now();
        activations.save(activation);
        events.publish(PluginEvent.of("saas.license.deactivated", Map.of(
                "licenseId", license.id,
                "activationId", activation.id)));
    }

    @Transactional
    public Map<String, Object> usage(SaasDtos.UsageRequest request) {
        guard.requireEnabled();
        SaasLicense license = requireByKey(request.licenseKey(), request.productKey());
        if (license.subscription == null) {
            throw new BadRequestException("La licencia no está asociada a una suscripción");
        }
        Optional<SaasUsageEvent> existing = usage.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return Map.of("accepted", true, "duplicate", true, "eventId", existing.get().id);
        }
        SaasUsageEvent event = new SaasUsageEvent();
        event.subscription = license.subscription;
        event.meter = request.meter();
        event.quantity = request.quantity();
        event.idempotencyKey = request.idempotencyKey();
        event.occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
        event.metadataJson = write(request.metadata());
        event = usage.save(event);
        events.publish(PluginEvent.of("saas.usage.recorded", Map.of(
                "subscriptionId", license.subscription.id,
                "meter", event.meter,
                "quantity", event.quantity)));
        return Map.of("accepted", true, "duplicate", false, "eventId", event.id);
    }

    @Transactional(readOnly = true)
    public SaasDtos.ClaimResponse claim(String orderNumber, String token) {
        guard.requireEnabled();
        OrderAccessService.OrderAccess access = orderAccess.require(orderNumber, token);
        List<SaasDtos.LicenseResponse> orderLicenses = licenses.findByOrderId(access.id())
                .stream().map(item -> license(item, cipher.decrypt(item.keyCipher))).toList();
        List<SaasDtos.SubscriptionResponse> orderSubscriptions = subscriptions
                .findByOrderId(access.id()).stream().map(this::subscription).toList();
        return new SaasDtos.ClaimResponse(
                orderNumber, access.customerEmail(), orderLicenses, orderSubscriptions);
    }

    @Transactional(readOnly = true)
    public List<SaasDtos.LicenseResponse> licenses() {
        guard.requireEnabled();
        return licenses.findAll().stream()
                .sorted(Comparator.comparing((SaasLicense item) -> item.createdAt).reversed())
                .map(item -> license(item, null)).toList();
    }

    @Transactional
    public SaasDtos.LicenseResponse updateLicenseStatus(
            String id,
            SaasDtos.LicenseStatusRequest request) {
        guard.requireEnabled();
        SaasLicense license = licenses.findById(id)
                .orElseThrow(() -> new NotFoundException("Licencia no encontrada"));
        license.status = request.status();
        return license(licenses.save(license), null);
    }

    @Transactional(readOnly = true)
    public List<SaasDtos.SubscriptionResponse> subscriptions() {
        guard.requireEnabled();
        return subscriptions.findAll().stream()
                .sorted(Comparator.comparing((SaasSubscription item) -> item.createdAt).reversed())
                .map(this::subscription).toList();
    }

    @Transactional
    public SaasDtos.SubscriptionResponse updateSubscription(
            String id,
            SaasDtos.SubscriptionStatusRequest request) {
        guard.requireEnabled();
        SaasSubscription subscription = subscriptions.findById(id)
                .orElseThrow(() -> new NotFoundException("Suscripción no encontrada"));
        subscription.status = request.status();
        if (request.cancelAtPeriodEnd() != null) {
            subscription.cancelAtPeriodEnd = request.cancelAtPeriodEnd();
        }
        if (request.status() == SubscriptionStatus.CANCELLED) {
            subscription.cancelledAt = Instant.now();
            subscription.cancelAtPeriodEnd = false;
            suspendSubscriptionLicenses(subscription.id);
        } else if (Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING)
                .contains(request.status())) {
            subscription.cancelledAt = null;
            reactivateSubscriptionLicenses(subscription.id);
        }
        return subscription(subscriptions.save(subscription));
    }

    @Transactional
    public SaasDtos.ReleaseResponse saveRelease(String id, SaasDtos.ReleaseRequest request) {
        guard.requireEnabled();
        SaasRelease release = id == null ? new SaasRelease() : releases.findById(id)
                .orElseThrow(() -> new NotFoundException("Versión no encontrada"));
        release.product = requireProduct(request.productId());
        release.version = request.version();
        release.platform = request.platform().toLowerCase(Locale.ROOT);
        release.channel = request.channel() == null
                ? "stable" : request.channel().toLowerCase(Locale.ROOT);
        release.downloadUrl = request.downloadUrl();
        release.checksum = blank(request.checksum());
        release.minimumPlan = blank(request.minimumPlan());
        release.active = request.active();
        release.publishedAt = request.publishedAt() == null
                ? Instant.now() : request.publishedAt();
        return release(releases.save(release));
    }

    @Transactional(readOnly = true)
    public SaasDtos.ReleaseResponse latest(
            String productKey,
            String platform,
            String channel,
            SaasDtos.LicenseRequest request) {
        SaasLicense license = requireByKey(request.licenseKey(), productKey);
        if (license.status != LicenseStatus.ACTIVE) {
            throw new BadRequestException("Licencia inactiva");
        }
        return releases.findFirstByProductIdAndPlatformAndChannelAndActiveTrueOrderByPublishedAtDesc(
                        license.product.id,
                        platform.toLowerCase(Locale.ROOT),
                        channel == null ? "stable" : channel.toLowerCase(Locale.ROOT))
                .map(this::release)
                .orElseThrow(() -> new NotFoundException("No hay versiones disponibles"));
    }

    @Transactional
    public SaasDtos.SettingsResponse settings() {
        guard.requireEnabled();
        return settings(settingsEntity());
    }

    @Transactional
    public SaasDtos.SettingsResponse saveSettings(SaasDtos.SettingsRequest request) {
        guard.requireEnabled();
        SaasSettings settings = settingsEntity();
        settings.companyName = request.companyName().trim();
        settings.supportEmail = lower(request.supportEmail());
        settings.billingEmail = lower(request.billingEmail());
        settings.defaultCurrency = request.defaultCurrency();
        settings.defaultTrialDays = request.defaultTrialDays();
        settings.defaultGraceDays = request.defaultGraceDays();
        settings.allowSelfServiceCancel = request.allowSelfServiceCancel();
        settings.allowPlanChanges = request.allowPlanChanges();
        settings.requireDeviceName = request.requireDeviceName();
        settings.showAnnualSavings = request.showAnnualSavings();
        settings.pricingHeadline = blank(request.pricingHeadline());
        settings.pricingDescription = blank(request.pricingDescription());
        settings.portalMessage = blank(request.portalMessage());
        settings.termsUrl = blank(request.termsUrl());
        settings.privacyUrl = blank(request.privacyUrl());
        return settings(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public SaasDtos.Dashboard dashboard() {
        guard.requireEnabled();
        List<SaasSubscription> allSubscriptions = subscriptions.findAll();
        List<SaasLicense> allLicenses = licenses.findAll();
        List<SaasDtos.LicenseResponse> recent = allLicenses.stream()
                .sorted(Comparator.comparing((SaasLicense item) -> item.createdAt).reversed())
                .limit(8).map(item -> license(item, null)).toList();
        return new SaasDtos.Dashboard(
                products.count(),
                plans.count(),
                allSubscriptions.size(),
                allSubscriptions.stream().filter(item -> item.status == SubscriptionStatus.ACTIVE).count(),
                allSubscriptions.stream().filter(item -> item.status == SubscriptionStatus.TRIALING).count(),
                allSubscriptions.stream().filter(item -> item.status == SubscriptionStatus.PAST_DUE).count(),
                allLicenses.size(),
                allLicenses.stream().filter(item -> item.status == LicenseStatus.ACTIVE).count(),
                activations.count(),
                usage.count(),
                products.countByDemoDataTrue(),
                plans.countByDemoDataTrue(),
                recent,
                plans.findByActiveTrueOrderByPriceAsc().stream()
                        .filter(item -> item.featured).map(this::plan).toList());
    }

    Map<String, Object> entitlements(SaasPlan plan) {
        return read(plan.entitlementsJson);
    }

    private SaasProduct upsertDemoProduct(
            String key,
            String name,
            String description,
            String logoUrl) {
        Optional<SaasProduct> existing = products.findByProductKey(key);
        if (existing.isPresent()) {
            SaasProduct product = existing.get();
            if (product.demoData) {
                product.name = name;
                product.description = description;
                product.logoUrl = logoUrl;
                product.active = true;
                return products.save(product);
            }
            return product;
        }

        SaasProduct product = new SaasProduct();
        product.productKey = key;
        product.name = name;
        product.description = description;
        product.logoUrl = logoUrl;
        product.active = true;
        product.demoData = true;
        return products.save(product);
    }

    private SaasPlan upsertDemoPlan(
            SaasProduct product,
            String key,
            String name,
            String description,
            String price,
            String interval,
            int intervalCount,
            int trialDays,
            int maxActivations,
            Map<String, Object> entitlements,
            boolean featured) {
        Optional<SaasPlan> existing =
                plans.findByProductIdAndPlanKey(product.id, key);
        if (existing.isPresent() && !existing.get().demoData) {
            return existing.get();
        }

        SaasPlan plan = existing.orElseGet(SaasPlan::new);
        plan.product = product;
        plan.planKey = key;
        plan.name = name;
        plan.description = description;
        plan.price = new BigDecimal(price);
        plan.currency = settingsEntity().defaultCurrency;
        plan.billingInterval = interval;
        plan.intervalCount = intervalCount;
        plan.trialDays = trialDays;
        plan.maxActivations = maxActivations;
        plan.entitlementsJson = write(entitlements);
        plan.active = true;
        plan.featured = featured;
        plan.demoData = true;
        return plans.save(plan);
    }

    private SaasSubscription ensureDemoSubscription(
            SaasPlan plan,
            String email,
            SubscriptionStatus status,
            boolean cancelAtPeriodEnd) {
        Optional<SaasSubscription> existing = subscriptions
                .findFirstByPlanIdAndCustomerEmailIgnoreCaseAndDemoDataTrue(
                        plan.id, email);
        SaasSubscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.status = status;
            subscription.cancelAtPeriodEnd = cancelAtPeriodEnd;
            subscriptions.save(subscription);
        } else {
            subscription = createSubscription(
                    plan, null, email, null, true);
            subscription.status = status;
            subscription.cancelAtPeriodEnd = cancelAtPeriodEnd;
            subscriptions.save(subscription);
        }

        boolean hasLicense = licenses
                .findFirstByPlanIdAndCustomerEmailIgnoreCaseAndDemoDataTrue(
                        plan.id, email)
                .isPresent();
        if (!hasLicense) {
            issueLicense(
                    plan,
                    email,
                    subscription,
                    null,
                    subscription.currentPeriodEnd,
                    Math.max(plan.maxActivations, 1),
                    true);
        }
        return subscription;
    }

    private void ensureDemoLicense(
            SaasPlan plan,
            String email,
            Instant expiresAt,
            int maxActivations) {
        if (licenses
                .findFirstByPlanIdAndCustomerEmailIgnoreCaseAndDemoDataTrue(
                        plan.id, email)
                .isEmpty()) {
            issueLicense(
                    plan,
                    email,
                    null,
                    null,
                    expiresAt,
                    Math.max(maxActivations, 1),
                    true);
        }
    }

    private SaasSubscription createSubscription(
            SaasPlan plan,
            String customerId,
            String email,
            String orderId,
            boolean demoData) {
        Instant now = Instant.now();
        SaasSubscription subscription = new SaasSubscription();
        subscription.plan = plan;
        subscription.customerId = customerId;
        subscription.customerEmail = email.trim().toLowerCase(Locale.ROOT);
        subscription.orderId = orderId;
        subscription.startedAt = now;
        subscription.currentPeriodStart = now;
        subscription.trialEndsAt = plan.trialDays > 0
                ? now.plus(Duration.ofDays(plan.trialDays)) : null;
        subscription.status = plan.trialDays > 0
                ? SubscriptionStatus.TRIALING : SubscriptionStatus.ACTIVE;
        subscription.currentPeriodEnd = periodEnd(now, plan.billingInterval, plan.intervalCount);
        subscription.demoData = demoData;
        return subscriptions.save(subscription);
    }

    private Issued issueLicense(
            SaasPlan plan,
            String email,
            SaasSubscription subscription,
            String orderId,
            Instant expiresAt,
            Integer maxActivations,
            boolean demoData) {
        String raw = key(plan.product.productKey);
        SaasLicense license = new SaasLicense();
        license.product = plan.product;
        license.plan = plan;
        license.subscription = subscription;
        license.customerEmail = email.trim().toLowerCase(Locale.ROOT);
        license.orderId = orderId;
        license.keyHash = hash(raw);
        license.keyCipher = cipher.encrypt(raw);
        license.keyPrefix = raw.substring(0, Math.min(14, raw.length()));
        license.status = LicenseStatus.ACTIVE;
        license.maxActivations = maxActivations == null ? plan.maxActivations : maxActivations;
        license.issuedAt = Instant.now();
        license.expiresAt = expiresAt;
        int graceDays = settingsEntity().defaultGraceDays;
        license.graceEndsAt = expiresAt == null ? null
                : expiresAt.plus(Duration.ofDays(graceDays));
        license.demoData = demoData;
        return new Issued(licenses.save(license), raw);
    }

    private void ensureDemoRelease(
            SaasProduct product,
            String version,
            String platform,
            String channel,
            String downloadUrl,
            String minimumPlan,
            boolean active) {
        if (!product.demoData) {
            return;
        }
        Optional<SaasRelease> existing = releases
                .findFirstByProductIdAndVersionAndPlatformAndChannel(
                        product.id, version, platform, channel);
        SaasRelease release = existing.orElseGet(SaasRelease::new);
        release.product = product;
        release.version = version;
        release.platform = platform;
        release.channel = channel;
        release.downloadUrl = downloadUrl;
        release.minimumPlan = minimumPlan;
        release.active = active;
        if (release.publishedAt == null) {
            release.publishedAt = Instant.now();
        }
        releases.save(release);
    }

    private void suspendSubscriptionLicenses(String subscriptionId) {
        licenses.findAll().stream()
                .filter(item -> item.subscription != null
                        && Objects.equals(item.subscription.id, subscriptionId))
                .forEach(item -> {
                    item.status = LicenseStatus.SUSPENDED;
                    licenses.save(item);
                });
    }

    private void reactivateSubscriptionLicenses(String subscriptionId) {
        licenses.findAll().stream()
                .filter(item -> item.subscription != null
                        && Objects.equals(item.subscription.id, subscriptionId)
                        && item.status == LicenseStatus.SUSPENDED)
                .forEach(item -> {
                    item.status = LicenseStatus.ACTIVE;
                    licenses.save(item);
                });
    }

    private SaasDtos.DemoDataResponse demoResponse(String message) {
        return new SaasDtos.DemoDataResponse(
                message,
                products.countByDemoDataTrue(),
                plans.countByDemoDataTrue(),
                subscriptions.countByDemoDataTrue(),
                licenses.countByDemoDataTrue());
    }

    private SaasSettings settingsEntity() {
        return settingsRepository.findById(1).orElseGet(() -> {
            SaasSettings settings = new SaasSettings();
            settings.id = 1;
            return settingsRepository.save(settings);
        });
    }

    private SaasLicense requireByKey(String raw, String productKey) {
        SaasLicense license = licenses.findByKeyHash(hash(raw))
                .orElseThrow(() -> new NotFoundException("Licencia inválida"));
        if (!license.product.productKey.equals(productKey)) {
            throw new NotFoundException("Licencia inválida");
        }
        return license;
    }

    private SaasProduct requireProduct(String id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto SaaS no encontrado"));
    }

    private SaasPlan requirePlan(String id) {
        return plans.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
    }

    private void refresh(SaasLicense license) {
        Instant end = license.graceEndsAt == null ? license.expiresAt : license.graceEndsAt;
        if (license.status == LicenseStatus.ACTIVE
                && end != null
                && Instant.now().isAfter(end)) {
            license.status = LicenseStatus.EXPIRED;
        }
    }

    private SaasDtos.LicenseResult result(
            boolean valid,
            SaasLicense license,
            SaasActivation activation,
            String message) {
        Map<String, Object> entitlements = read(license.plan.entitlementsJson);
        String payload = license.id + "|" + license.status + "|"
                + Objects.toString(license.expiresAt, "") + "|" + entitlements;
        return new SaasDtos.LicenseResult(
                valid,
                license.status.name(),
                license.id,
                activation == null ? null : activation.id,
                license.expiresAt,
                license.graceEndsAt,
                entitlements,
                message,
                hash(payload));
    }

    private SaasDtos.ProductResponse product(SaasProduct product) {
        return new SaasDtos.ProductResponse(
                product.id, product.productKey, product.name, product.description,
                product.logoUrl, product.active, product.demoData, product.createdAt);
    }

    private SaasDtos.PlanResponse plan(SaasPlan plan) {
        return new SaasDtos.PlanResponse(
                plan.id,
                plan.product.id,
                plan.product.productKey,
                plan.product.name,
                plan.planKey,
                plan.name,
                plan.description,
                plan.price,
                plan.currency,
                plan.billingInterval,
                plan.intervalCount,
                plan.trialDays,
                plan.maxActivations,
                read(plan.entitlementsJson),
                plan.active,
                plan.featured,
                plan.demoData,
                plan.createdAt,
                plan.updatedAt);
    }

    private SaasDtos.SubscriptionResponse subscription(SaasSubscription subscription) {
        return new SaasDtos.SubscriptionResponse(
                subscription.id,
                subscription.plan.product.productKey,
                subscription.plan.product.name,
                subscription.plan.planKey,
                subscription.plan.name,
                subscription.customerEmail,
                subscription.status.name(),
                subscription.startedAt,
                subscription.trialEndsAt,
                subscription.currentPeriodStart,
                subscription.currentPeriodEnd,
                subscription.cancelAtPeriodEnd,
                subscription.cancelledAt,
                subscription.orderId,
                subscription.demoData);
    }

    private SaasDtos.LicenseResponse license(SaasLicense license, String raw) {
        long active = activations.countByLicenseIdAndStatus(
                license.id, ActivationStatus.ACTIVE);
        return new SaasDtos.LicenseResponse(
                license.id,
                license.product.productKey,
                license.product.name,
                license.plan.planKey,
                license.plan.name,
                license.customerEmail,
                license.keyPrefix + "…",
                raw,
                license.status.name(),
                license.maxActivations,
                active,
                license.issuedAt,
                license.expiresAt,
                license.graceEndsAt,
                license.lastValidatedAt,
                license.orderId,
                license.demoData);
    }

    private SaasDtos.ReleaseResponse release(SaasRelease release) {
        return new SaasDtos.ReleaseResponse(
                release.id,
                release.product.productKey,
                release.version,
                release.platform,
                release.channel,
                release.downloadUrl,
                release.checksum,
                release.minimumPlan,
                release.active,
                release.publishedAt);
    }

    private SaasDtos.SettingsResponse settings(SaasSettings settings) {
        return new SaasDtos.SettingsResponse(
                settings.companyName,
                settings.supportEmail,
                settings.billingEmail,
                settings.defaultCurrency,
                settings.defaultTrialDays,
                settings.defaultGraceDays,
                settings.allowSelfServiceCancel,
                settings.allowPlanChanges,
                settings.requireDeviceName,
                settings.showAnnualSavings,
                settings.pricingHeadline,
                settings.pricingDescription,
                settings.portalMessage,
                settings.termsUrl,
                settings.privacyUrl,
                settings.updatedAt);
    }

    private Instant periodEnd(Instant start, String interval, int count) {
        return switch (interval) {
            case "DAY" -> start.plus(Duration.ofDays(count));
            case "WEEK" -> start.plus(Duration.ofDays(7L * count));
            case "YEAR" -> start.atZone(ZoneOffset.UTC).plusYears(count).toInstant();
            case "ONE_TIME" -> null;
            default -> start.atZone(ZoneOffset.UTC).plusMonths(count).toInstant();
        };
    }

    private String key(String product) {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bytes).toUpperCase(Locale.ROOT);
        return ("TRAST-" + product.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "") + "-" + token)
                .replaceAll("(.{8})(?=.)", "$1-");
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
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
            return value == null ? Map.of()
                    : mapper.readValue(value, new TypeReference<>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private Map<String, Object> mapOf(Object... entries) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String lower(String value) {
        return value == null || value.isBlank()
                ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record Issued(SaasLicense license, String rawKey) {}
}
