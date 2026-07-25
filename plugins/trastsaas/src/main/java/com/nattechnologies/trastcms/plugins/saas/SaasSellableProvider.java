package com.nattechnologies.trastcms.plugins.saas;

import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SaasSellableProvider implements SellableProvider {
    private final SaasPlanRepository plans;
    private final SaasService service;

    SaasSellableProvider(SaasPlanRepository plans, SaasService service) {
        this.plans = plans;
        this.service = service;
    }

    @Override
    public String providerKey() {
        return "saas";
    }

    @Override
    public String pluginId() {
        return "trastsaas";
    }

    @Override
    public Optional<SellableItem> resolve(String reference) {
        return plans.findById(reference)
                .filter(plan -> plan.active && plan.product.active)
                .map(plan -> {
                    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("productKey", plan.product.productKey);
                    metadata.put("productName", plan.product.name);
                    metadata.put("planKey", plan.planKey);
                    metadata.put("trialDays", Math.max(plan.trialDays, 0));
                    metadata.put("maxActivations", Math.max(plan.maxActivations, 1));
                    metadata.put("billingInterval", plan.billingInterval);
                    metadata.put("intervalCount", Math.max(plan.intervalCount, 1));
                    metadata.put("licenseType", "ONE_TIME".equals(plan.billingInterval)
                            ? "PERPETUAL"
                            : "SUBSCRIPTION");
                    metadata.put("entitlements", service.entitlements(plan));
                    metadata.put("demoData", plan.demoData);
                    return new SellableItem(
                            providerKey(),
                            plan.id,
                            "SAAS_PLAN",
                            plan.product.name + " · " + plan.name,
                            plan.description,
                            plan.price,
                            plan.currency,
                            !"ONE_TIME".equals(plan.billingInterval),
                            plan.billingInterval,
                            false,
                            plan.product.logoUrl,
                            true,
                            Collections.unmodifiableMap(metadata));
                });
    }

    @Override
    public List<SellableItem> featured(int limit) {
        return plans.findByActiveTrueOrderByPriceAsc().stream()
                .filter(plan -> plan.featured)
                .limit(limit)
                .map(plan -> resolve(plan.id).orElseThrow())
                .toList();
    }

    @Override
    public void fulfill(FulfillmentRequest request) {
        service.fulfillPlan(
                request.itemReference(),
                request.customerId(),
                request.customerEmail(),
                request.orderId(),
                request.quantity());
    }
}
