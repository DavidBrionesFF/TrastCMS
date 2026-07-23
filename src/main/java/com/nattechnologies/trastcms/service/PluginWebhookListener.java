package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.plugin.PluginRegistration;
import com.nattechnologies.trastcms.domain.plugin.PluginRegistrationRepository;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

@Component
public class PluginWebhookListener {
    private static final Logger log = LoggerFactory.getLogger(PluginWebhookListener.class);

    private final PluginRegistrationRepository repository;
    private final PluginSecretCipher cipher;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PluginWebhookListener(PluginRegistrationRepository repository, PluginSecretCipher cipher,
                                 RestClient restClient, ObjectMapper objectMapper) {
        this.repository = repository;
        this.cipher = cipher;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void dispatch(PluginEvent event) {
        String body;
        try {
            body = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            log.error("No se pudo serializar el evento {}", event.type(), exception);
            return;
        }

        for (PluginRegistration plugin : repository.findByEnabledTrue()) {
            if (!subscribed(plugin, event.type())) continue;
            try {
                String secret = cipher.decrypt(plugin.getEncryptedSecret());
                String signature = hmac(body, secret);
                restClient.post()
                        .uri(plugin.getBaseUrl() + "/trastcms/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-TrastCMS-Event", event.type())
                        .header("X-TrastCMS-Signature", "sha256=" + signature)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RuntimeException exception) {
                log.warn("El plugin {} no pudo procesar {}: {}", plugin.getPluginKey(), event.type(), exception.getMessage());
            }
        }
    }

    private boolean subscribed(PluginRegistration plugin, String type) {
        if (plugin.getSubscriptions() == null || plugin.getSubscriptions().isBlank()) return false;
        return Arrays.stream(plugin.getSubscriptions().split(","))
                .map(String::trim)
                .anyMatch(value -> value.equals("*") || value.equals(type));
    }

    private String hmac(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar el webhook", exception);
        }
    }
}
