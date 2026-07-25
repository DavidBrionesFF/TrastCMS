package com.nattechnologies.trastcms.plugins.store;

import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StoreSellableProvider implements SellableProvider {
    private final StoreProductRepository products;
    private final StoreService service;

    StoreSellableProvider(
            StoreProductRepository products,
            StoreService service) {
        this.products = products;
        this.service = service;
    }

    @Override
    public String providerKey() {
        return "store";
    }

    @Override
    public String pluginId() {
        return "traststore";
    }

    @Override
    public Optional<SellableItem> resolve(String reference) {
        return products.findById(reference)
                .filter(product -> product.status == ProductStatus.ACTIVE)
                .map(product -> {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("slug", product.slug);
                    metadata.put("sku", product.sku);
                    metadata.put("productType", product.type.name());
                    metadata.put("taxable", product.taxable);
                    metadata.put("taxClass", product.taxClass);
                    if (product.taxRateOverride != null) {
                        metadata.put("taxRate", product.taxRateOverride);
                    }
                    if (product.digitalUrl != null) {
                        metadata.put("digitalUrl", product.digitalUrl);
                    }

                    return new SellableItem(
                            providerKey(),
                            product.id,
                            "STORE_PRODUCT",
                            product.name,
                            product.shortDescription,
                            product.price,
                            product.currency,
                            false,
                            null,
                            product.type == ProductType.PHYSICAL,
                            product.featuredImageUrl,
                            !product.trackInventory
                                    || product.allowBackorder
                                    || product.stockQuantity > 0,
                            metadata);
                });
    }

    @Override
    public List<SellableItem> featured(int limit) {
        return products
                .findTop12ByStatusAndFeaturedTrueOrderByUpdatedAtDesc(
                        ProductStatus.ACTIVE)
                .stream()
                .limit(limit)
                .map(product -> resolve(product.id).orElseThrow())
                .toList();
    }

    @Override
    public void fulfill(FulfillmentRequest request) {
        service.reserve(
                request.itemReference(),
                request.quantity(),
                request.orderId());
    }
}
