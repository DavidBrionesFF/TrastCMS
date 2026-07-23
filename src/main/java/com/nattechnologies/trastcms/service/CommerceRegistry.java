package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.plugin.api.PaymentGateway;
import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CommerceRegistry {
    private final Map<String, SellableProvider> sellables;
    private final Map<String, PaymentGateway> gateways;

    public CommerceRegistry(List<SellableProvider> sellables, List<PaymentGateway> gateways) {
        this.sellables = index(sellables, SellableProvider::providerKey, "Proveedor vendible");
        this.gateways = index(gateways, PaymentGateway::key, "Pasarela");
    }
    private static <T> Map<String,T> index(List<T> values, java.util.function.Function<T,String> key,
                                            String type) {
        Map<String,T> result = new TreeMap<>();
        for (T value : values) {
            String id = key.apply(value);
            if (result.put(id, value) != null) throw new IllegalStateException(type + " duplicado: " + id);
        }
        return Collections.unmodifiableMap(result);
    }
    public SellableProvider.SellableItem requireItem(String provider, String reference) {
        SellableProvider source = sellables.get(provider);
        if (source == null) throw new NotFoundException("Proveedor de producto no encontrado: " + provider);
        return source.resolve(reference).orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }
    public SellableProvider requireProvider(String provider) {
        SellableProvider source = sellables.get(provider);
        if (source == null) throw new NotFoundException("Proveedor de producto no encontrado");
        return source;
    }
    public PaymentGateway requireGateway(String key) {
        PaymentGateway gateway = gateways.get(key);
        if (gateway == null || !gateway.enabled()) throw new NotFoundException("Método de pago no disponible");
        return gateway;
    }
    public List<Map<String,Object>> gateways() {
        return gateways.values().stream().filter(PaymentGateway::enabled).map(item -> Map.<String,Object>of(
                "key", item.key(), "name", item.name(), "description", item.description(),
                "recurring", item.supportsRecurring(), "refunds", item.supportsRefunds())).toList();
    }
    public List<SellableProvider.SellableItem> featured(int limit) {
        return sellables.values().stream().flatMap(p -> p.featured(limit).stream()).limit(limit).toList();
    }
}
