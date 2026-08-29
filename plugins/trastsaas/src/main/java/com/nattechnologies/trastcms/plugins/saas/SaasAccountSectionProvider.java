package com.nattechnologies.trastcms.plugins.saas;

import com.nattechnologies.trastcms.plugin.api.AccountSectionProvider;
import com.nattechnologies.trastcms.service.PluginSecretCipher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SaasAccountSectionProvider implements AccountSectionProvider {
    private final SaasLicenseRepository licenses;
    private final SaasSubscriptionRepository subscriptions;
    private final SaasActivationRepository activations;
    private final PluginSecretCipher cipher;

    SaasAccountSectionProvider(
            SaasLicenseRepository licenses,
            SaasSubscriptionRepository subscriptions,
            SaasActivationRepository activations,
            PluginSecretCipher cipher) {
        this.licenses = licenses;
        this.subscriptions = subscriptions;
        this.activations = activations;
        this.cipher = cipher;
    }

    @Override public String key() { return "licenses"; }
    @Override public String pluginId() { return "trastsaas"; }
    @Override public String label() { return "Licencias y suscripciones"; }
    @Override public String icon() { return "key"; }
    @Override public int order() { return 30; }

    @Override
    public AccountSection load(AccountContext context) {
        List<Map<String, Object>> items = new ArrayList<>();
        licenses.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(context.email())
                .forEach(license -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", license.id);
                    item.put("type", "LICENSE");
                    item.put("title", license.product.name + " · " + license.plan.name);
                    item.put("product", license.product.name);
                    item.put("productKey", license.product.productKey);
                    item.put("plan", license.plan.name);
                    item.put("planKey", license.plan.planKey);
                    item.put("licenseKey", cipher.decrypt(license.keyCipher));
                    item.put("keyPrefix", license.keyPrefix);
                    item.put("status", license.status.name());
                    item.put("issuedAt", license.issuedAt);
                    item.put("expiresAt", license.expiresAt);
                    item.put("graceEndsAt", license.graceEndsAt);
                    item.put("lastValidatedAt", license.lastValidatedAt);
                    item.put("maxActivations", license.maxActivations);
                    item.put("activeActivations", activations.countByLicenseIdAndStatus(
                            license.id, ActivationStatus.ACTIVE));
                    item.put("orderId", license.orderId);
                    items.add(Collections.unmodifiableMap(item));
                });
        subscriptions.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(context.email())
                .forEach(subscription -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", subscription.id);
                    item.put("type", "SUBSCRIPTION");
                    item.put("title", subscription.plan.product.name + " · " + subscription.plan.name);
                    item.put("product", subscription.plan.product.name);
                    item.put("productKey", subscription.plan.product.productKey);
                    item.put("plan", subscription.plan.name);
                    item.put("planKey", subscription.plan.planKey);
                    item.put("status", subscription.status.name());
                    item.put("startedAt", subscription.startedAt);
                    item.put("trialEndsAt", subscription.trialEndsAt);
                    item.put("periodStart", subscription.currentPeriodStart);
                    item.put("periodEnd", subscription.currentPeriodEnd);
                    item.put("cancelAtPeriodEnd", subscription.cancelAtPeriodEnd);
                    item.put("cancelledAt", subscription.cancelledAt);
                    item.put("orderId", subscription.orderId);
                    items.add(Collections.unmodifiableMap(item));
                });
        items.sort(Comparator.comparing(
                item -> String.valueOf(item.get("title")),
                String.CASE_INSENSITIVE_ORDER));
        long active = items.stream()
                .filter(item -> Set.of("ACTIVE", "TRIALING").contains(item.get("status")))
                .count();
        return new AccountSection(
                key(), pluginId(), label(), icon(), order(), "READY",
                "Todavía no tiene licencias o suscripciones.",
                List.copyOf(items),
                Map.of("total", items.size(), "active", active));
    }
}
