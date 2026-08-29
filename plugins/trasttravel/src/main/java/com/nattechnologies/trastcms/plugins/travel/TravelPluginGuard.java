package com.nattechnologies.trastcms.plugins.travel;

import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Component;

@Component
class TravelPluginGuard {
    private final BundledPluginStateRepository states;
    TravelPluginGuard(BundledPluginStateRepository states) { this.states = states; }
    void requireEnabled() {
        if (!states.findCurrent("trasttravel").map(item -> item.isEnabled()).orElse(false)) {
            throw new NotFoundException("TrastTravel está desactivado");
        }
    }
}
