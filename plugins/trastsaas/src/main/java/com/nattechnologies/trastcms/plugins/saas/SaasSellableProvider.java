package com.nattechnologies.trastcms.plugins.saas;

import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                .map(plan -> new SellableItem(
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
                        Map.of(
                                "productKey", plan.product.productKey,
                                "planKey", plan.planKey,
                                "trialDays", plan.trialDays,
                                "maxActivations", plan.maxActivations,
                                "entitlements", service.entitlements(plan))));
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
