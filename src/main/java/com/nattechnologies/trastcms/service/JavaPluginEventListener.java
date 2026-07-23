package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.plugin.api.TrastCmsExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JavaPluginEventListener {
    private static final Logger log =
            LoggerFactory.getLogger(JavaPluginEventListener.class);

    private final JavaPluginService plugins;

    public JavaPluginEventListener(JavaPluginService plugins) {
        this.plugins = plugins;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void dispatch(PluginEvent event) {
        for (TrastCmsExtension extension : plugins.extensions()) {
            try {
                extension.onEvent(event);
            } catch (RuntimeException exception) {
                log.warn(
                        "La extensión Java {} falló procesando {}",
                        extension.name(),
                        event.type(),
                        exception);
            }
        }
    }
}
