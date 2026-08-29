package com.nattechnologies.trastcms.plugins.pay;

import com.nattechnologies.trastcms.plugin.api.AccountSectionProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PayAccountSectionProvider implements AccountSectionProvider {
    private final CommerceOrderRepository orders;
    private final CommercePaymentRepository payments;

    PayAccountSectionProvider(
            CommerceOrderRepository orders,
            CommercePaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @Override public String key() { return "orders"; }
    @Override public String pluginId() { return "trastpay"; }
    @Override public String label() { return "Compras y pagos"; }
    @Override public String icon() { return "payment"; }
    @Override public int order() { return 10; }

    @Override
    public AccountSection load(AccountContext context) {
        List<Map<String, Object>> items = orders
                .findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(context.email())
                .stream()
                .map(order -> {
                    List<Map<String, Object>> lines = order.items.stream()
                            .map(line -> {
                                Map<String, Object> value = new LinkedHashMap<>();
                                value.put("id", line.id);
                                value.put("name", line.name);
                                value.put("provider", line.providerKey);
                                value.put("type", line.itemType);
                                value.put("quantity", line.quantity);
                                value.put("unitPrice", line.unitPrice);
                                value.put("total", line.lineTotal);
                                value.put("fulfilled", line.fulfilledAt != null);
                                return Collections.unmodifiableMap(value);
                            })
                            .toList();
                    List<Map<String, Object>> paymentItems = payments
                            .findByOrderIdOrderByCreatedAtDesc(order.id)
                            .stream()
                            .map(payment -> {
                                Map<String, Object> value = new LinkedHashMap<>();
                                value.put("id", payment.id);
                                value.put("gateway", payment.gatewayKey);
                                value.put("status", payment.status.name());
                                value.put("amount", payment.amount);
                                value.put("currency", payment.currency);
                                value.put("reference", payment.externalReference);
                                value.put("failureMessage", payment.failureMessage);
                                value.put("createdAt", payment.createdAt);
                                return Collections.unmodifiableMap(value);
                            })
                            .toList();

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", order.id);
                    item.put("orderNumber", order.orderNumber);
                    item.put("reference", order.orderNumber);
                    item.put("title", lines.isEmpty()
                            ? "Orden " + order.orderNumber
                            : lines.get(0).get("name") + (lines.size() > 1
                                    ? " y " + (lines.size() - 1) + " artículo(s) más"
                                    : ""));
                    item.put("description", "Orden " + order.orderNumber);
                    item.put("status", order.status.name());
                    item.put("subtotal", order.subtotal);
                    item.put("discountTotal", order.discountTotal);
                    item.put("taxTotal", order.taxTotal);
                    item.put("total", order.grandTotal);
                    item.put("currency", order.currency);
                    item.put("couponCode", order.couponCode);
                    item.put("paidAt", order.paidAt);
                    item.put("createdAt", order.createdAt);
                    item.put("lines", lines);
                    item.put("payments", paymentItems);
                    return Collections.unmodifiableMap(item);
                })
                .toList();
        long paid = items.stream()
                .filter(item -> Set.of("PAID", "COMPLETED").contains(item.get("status")))
                .count();
        long pending = items.size() - paid;
        return new AccountSection(
                key(), pluginId(), label(), icon(), order(), "READY",
                "Todavía no tiene órdenes o pagos registrados.",
                items, Map.of("total", items.size(), "paid", paid, "pending", pending));
    }
}
