package com.nattechnologies.trastcms.plugins.maintenance;

import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Component;

@Component
class MaintenancePluginGuard {
    private final BundledPluginStateRepository states;

    MaintenancePluginGuard(BundledPluginStateRepository states) {
        this.states = states;
    }

    void requireEnabled() {
        if (!states.findCurrent("trastmaintenance").map(s -> s.isEnabled()).orElse(false)) {
            throw new NotFoundException("TrastMaintenance está desactivado");
        }
    }
}
