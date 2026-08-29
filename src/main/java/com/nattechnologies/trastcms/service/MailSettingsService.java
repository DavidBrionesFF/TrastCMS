package com.nattechnologies.trastcms.service;

import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailSettingsService {
    private static final String PREFIX = "mail.";
    private final SiteSettingService settings;
    private final PluginSecretCipher cipher;

    public MailSettingsService(SiteSettingService settings, PluginSecretCipher cipher) {
        this.settings = settings;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public MailSettingsResponse get() {
        String encrypted = settings.get(PREFIX + "passwordCipher", "");
        return new MailSettingsResponse(
                Boolean.parseBoolean(settings.get(PREFIX + "enabled", "false")),
                settings.get(PREFIX + "host", ""),
                integer(settings.get(PREFIX + "port", "587"), 587),
                settings.get(PREFIX + "username", ""),
                !encrypted.isBlank(),
                settings.get(PREFIX + "fromEmail", ""),
                settings.get(PREFIX + "fromName", "TrastCMS"),
                Boolean.parseBoolean(settings.get(PREFIX + "auth", "true")),
                Boolean.parseBoolean(settings.get(PREFIX + "startTls", "true")),
                Boolean.parseBoolean(settings.get(PREFIX + "ssl", "false")),
                settings.get(PREFIX + "replyTo", ""));
    }

    @Transactional
    public MailSettingsResponse update(MailSettingsRequest request) {
        settings.put(PREFIX + "enabled", Boolean.toString(request.enabled()));
        settings.put(PREFIX + "host", trim(request.host()));
        settings.put(PREFIX + "port", Integer.toString(request.port()));
        settings.put(PREFIX + "username", trim(request.username()));
        settings.put(PREFIX + "fromEmail", trim(request.fromEmail()));
        settings.put(PREFIX + "fromName", trim(request.fromName()));
        settings.put(PREFIX + "auth", Boolean.toString(request.auth()));
        settings.put(PREFIX + "startTls", Boolean.toString(request.startTls()));
        settings.put(PREFIX + "ssl", Boolean.toString(request.ssl()));
        settings.put(PREFIX + "replyTo", trim(request.replyTo()));
        if (request.password() != null && !request.password().isBlank()) {
            settings.put(PREFIX + "passwordCipher", cipher.encrypt(request.password()));
        }
        if (request.enabled()
                && (trim(request.host()).isBlank() || trim(request.fromEmail()).isBlank())) {
            throw new BadRequestException("Configure servidor SMTP y correo remitente antes de habilitar los envíos");
        }
        return get();
    }

    @Transactional(readOnly = true)
    public RuntimeMailSettings runtime() {
        MailSettingsResponse value = get();
        String encrypted = settings.get(PREFIX + "passwordCipher", "");
        String password = encrypted.isBlank() ? "" : cipher.decrypt(encrypted);
        return new RuntimeMailSettings(
                value.enabled(), value.host(), value.port(), value.username(),
                password, value.fromEmail(), value.fromName(), value.auth(),
                value.startTls(), value.ssl(), value.replyTo());
    }

    private int integer(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { return fallback; }
    }
    private String trim(String value) { return value == null ? "" : value.trim(); }

    public record MailSettingsRequest(
            boolean enabled,
            @Size(max = 240) String host,
            @Min(1) @Max(65535) int port,
            @Size(max = 240) String username,
            @Size(max = 500) String password,
            @Email @Size(max = 240) String fromEmail,
            @Size(max = 160) String fromName,
            boolean auth,
            boolean startTls,
            boolean ssl,
            @Email @Size(max = 240) String replyTo) { }

    public record MailSettingsResponse(
            boolean enabled, String host, int port, String username,
            boolean passwordConfigured, String fromEmail, String fromName,
            boolean auth, boolean startTls, boolean ssl, String replyTo) { }

    public record RuntimeMailSettings(
            boolean enabled, String host, int port, String username,
            String password, String fromEmail, String fromName,
            boolean auth, boolean startTls, boolean ssl, String replyTo) { }
}
