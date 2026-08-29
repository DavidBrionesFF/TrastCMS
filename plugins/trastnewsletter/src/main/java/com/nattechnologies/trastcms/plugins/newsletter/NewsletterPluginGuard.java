package com.nattechnologies.trastcms.plugins.newsletter;

import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Component;

@Component
class NewsletterPluginGuard {

    private final BundledPluginStateRepository states;

    NewsletterPluginGuard(BundledPluginStateRepository states) {
        this.states = states;
    }

    void requireEnabled() {
        if (!states.findCurrent("trastnewsletter").map(s -> s.isEnabled()).orElse(false)) {
            throw new NotFoundException("TrastNewsletter está desactivado");
        }
    }
}
