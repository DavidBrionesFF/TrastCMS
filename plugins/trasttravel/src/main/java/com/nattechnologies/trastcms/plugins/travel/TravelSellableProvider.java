package com.nattechnologies.trastcms.plugins.travel;

import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class TravelSellableProvider implements SellableProvider {
    private final TravelService service;

    TravelSellableProvider(TravelService service) {
        this.service = service;
    }

    @Override public String providerKey() { return "travel"; }
    @Override public String pluginId() { return "trasttravel"; }

    @Override
    public Optional<SellableItem> resolve(String reference) {
        return service.sellable(reference);
    }

    @Override
    public void fulfill(FulfillmentRequest request) {
        service.fulfill(request.itemReference(), request);
    }
}
