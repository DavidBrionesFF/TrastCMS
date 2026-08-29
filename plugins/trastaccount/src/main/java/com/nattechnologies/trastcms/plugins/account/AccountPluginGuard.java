package com.nattechnologies.trastcms.plugins.account;

import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Component;

@Component
class AccountPluginGuard {
    private final BundledPluginStateRepository states;
    AccountPluginGuard(BundledPluginStateRepository states) { this.states = states; }
    void requireEnabled() {
        if (!states.findCurrent("trastaccount").map(item -> item.isEnabled()).orElse(false)) {
            throw new NotFoundException("TrastAccount está desactivado");
        }
    }
}
