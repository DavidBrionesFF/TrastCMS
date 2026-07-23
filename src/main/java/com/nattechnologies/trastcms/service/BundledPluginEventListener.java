package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BundledPluginEventListener {
    private final BundledPluginService plugins;
    public BundledPluginEventListener(BundledPluginService plugins) { this.plugins = plugins; }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void dispatch(PluginEvent event) { plugins.dispatch(event); }
}
