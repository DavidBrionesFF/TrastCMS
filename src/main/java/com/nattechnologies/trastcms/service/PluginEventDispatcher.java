package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class PluginEventDispatcher {
    private final ApplicationEventPublisher publisher;

    public PluginEventDispatcher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Publica el evento en el bus interno. Los webhooks se envían después del commit
     * para que los plugins nunca observen datos que luego sean revertidos.
     */
    public void publish(PluginEvent event) {
        publisher.publishEvent(event);
    }
}
