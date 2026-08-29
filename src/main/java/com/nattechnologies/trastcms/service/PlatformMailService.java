package com.nattechnologies.trastcms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

@Service
public class PlatformMailService {
    private static final Logger log = LoggerFactory.getLogger(PlatformMailService.class);
    private final MailSettingsService settings;

    public PlatformMailService(MailSettingsService settings) {
        this.settings = settings;
    }

    @Async
    public void send(String to, String subject, String body) {
        try {
            sendNow(to, subject, body, false);
        } catch (RuntimeException exception) {
            log.warn("No se pudo enviar el correo a {}: {}", to, exception.getMessage());
        }
    }

    @Async
    public void sendHtml(String to, String subject, String html) {
        try {
            sendNow(to, subject, html, true);
        } catch (RuntimeException exception) {
            log.warn("No se pudo enviar el correo HTML a {}: {}", to, exception.getMessage());
        }
    }

    /**
     * Envía HTML de forma síncrona y devuelve el resultado real del intento SMTP.
     * Útil para automatizaciones ejecutadas fuera del request principal (por ejemplo,
     * eventos de plugins) que necesitan persistir si el mensaje realmente salió o falló.
     */
    public MailResult sendHtmlNow(String to, String subject, String html) {
        try {
            sendNow(to, subject, html, true);
            return new MailResult(true, "Correo enviado");
        } catch (RuntimeException exception) {
            return new MailResult(false, exception.getMessage() == null
                    ? "No se pudo enviar el correo HTML"
                    : exception.getMessage());
        }
    }

    public MailResult test(String recipient) {
        try {
            sendNow(
                    recipient,
                    "Prueba SMTP de TrastCMS",
                    "La configuración SMTP funciona correctamente. Los plugins ya pueden reutilizar el servicio de correo.",
                    false);
            return new MailResult(true, "Correo de prueba enviado");
        } catch (RuntimeException exception) {
            return new MailResult(false, exception.getMessage() == null
                    ? "No se pudo enviar el correo de prueba"
                    : exception.getMessage());
        }
    }

    private void sendNow(String to, String subject, String body, boolean html) {
        MailSettingsService.RuntimeMailSettings configuration = settings.runtime();
        if (!configuration.enabled()) {
            throw new BadRequestException("El servicio SMTP está deshabilitado");
        }
        if (to == null || to.isBlank()) {
            throw new BadRequestException("El destinatario es obligatorio");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(configuration.host());
        sender.setPort(configuration.port());
        sender.setUsername(configuration.username());
        sender.setPassword(configuration.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(configuration.auth()));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(configuration.startTls()));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(configuration.ssl()));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "15000");
        properties.put("mail.smtp.writetimeout", "15000");

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, false, StandardCharsets.UTF_8.name());
            if (configuration.fromName() == null || configuration.fromName().isBlank()) {
                helper.setFrom(configuration.fromEmail());
            } else {
                helper.setFrom(configuration.fromEmail(), configuration.fromName());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);
            if (configuration.replyTo() != null && !configuration.replyTo().isBlank()) {
                helper.setReplyTo(configuration.replyTo());
            }
            sender.send(message);
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo construir o enviar el correo SMTP: "
                    + Optional.ofNullable(exception.getMessage())
                    .orElse(exception.getClass().getSimpleName()));
        }
    }

    public record MailResult(boolean delivered, String message) { }
}
