package com.nattechnologies.trastcms.plugins.pay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PaymentGateway;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.PluginSecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class ClinpaysGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(ClinpaysGateway.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(40);
    private static final long WEBHOOK_TOLERANCE_SECONDS = 5 * 60;

    private final CommerceSettingsRepository settingsRepository;
    private final PluginSecretCipher cipher;
    private final ObjectMapper mapper;
    private final HttpClient client;

    ClinpaysGateway(
            CommerceSettingsRepository settingsRepository,
            PluginSecretCipher cipher,
            ObjectMapper mapper) {
        this.settingsRepository = settingsRepository;
        this.cipher = cipher;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String key() {
        return "clinpays";
    }

    @Override
    public String name() {
        return "Clinpays · Tarjeta 3DS";
    }

    @Override
    public String description() {
        return "Pago con tarjeta mediante Clinpays y autenticación 3D Secure de Promerica.";
    }

    @Override
    public boolean enabled() {
        return settingsRepository.findById(CommerceSettings.currentId())
                .map(this::configured)
                .orElse(false);
    }

    PayDtos.ClinpaysSetupResponse setup3ds(PayDtos.ClinpaysSetupRequest request) {
        CommerceSettings settings = requireSettings();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("safeIdentifier", digits(request.safeIdentifier()));
        payload.put("validThru", normalizeExpiry(request.validThru()));
        payload.put("currency", normalizeCurrency(request.currency()));

        JsonNode response = post(settings, "/payments/setup3ds", payload);
        return new PayDtos.ClinpaysSetupResponse(
                response.path("success").asBoolean(false),
                text(response, "threeDSecureAuthenticationId"),
                text(response, "accessToken"),
                text(response, "deviceDataCollectionUrl"));
    }

    @Override
    public PaymentResult create(PaymentRequest request) {
        CommerceSettings settings = requireSettings();
        Map<String, Object> input = request.metadata() == null
                ? Map.of()
                : request.metadata();
        Map<String, Object> paymentData = map(input.get("paymentData"));
        Map<String, Object> billing = map(input.get("billing"));

        String cardNumber = required(paymentData, "safeIdentifier", "Ingrese el número de tarjeta");
        String validThru = required(paymentData, "validThru", "Ingrese la fecha de vencimiento");
        String cvv = required(paymentData, "cvv", "Ingrese el código de seguridad");
        String cardholderName = required(paymentData, "cardholderName", "Ingrese el nombre del titular");
        String authenticationId = required(
                paymentData,
                "threeDSecureAuthenticationId",
                "No se pudo inicializar la autenticación 3DS");

        String customerName = string(input.get("customerName"));
        String[] names = splitName(customerName);
        BigDecimal subtotal = decimal(input.get("subtotal"), request.amount());
        BigDecimal tax = decimal(input.get("tax"), BigDecimal.ZERO);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstName", names[0]);
        payload.put("lastName", names[1]);
        payload.put("validThru", normalizeExpiry(validThru));
        payload.put("safeIdentifier", digits(cardNumber));
        payload.put("cvv", digits(cvv));
        payload.put("amount", money(request.amount()));
        payload.put("subtotal", money(subtotal));
        payload.put("tax", money(tax));
        payload.put("description", "Orden " + request.orderNumber());
        payload.put("currency", normalizeCurrency(request.currency()));
        payload.put("orderReference", request.orderNumber());
        payload.put("email", request.customerEmail());
        putIfPresent(payload, "mobileNumber", string(input.get("customerPhone")));
        payload.put("redirectUrl", validRedirect(request.returnUrl()));
        payload.put("cardholderName", cardholderName.trim());
        payload.put("clientIp", defaultString(string(input.get("clientIp")), "127.0.0.1"));
        payload.put("country", defaultString(string(billing.get("country")), "HN").toUpperCase(Locale.ROOT));
        payload.put("state", defaultString(string(billing.get("state")), "Francisco Morazán"));
        payload.put("city", defaultString(string(billing.get("city")), "Tegucigalpa"));
        payload.put("address", defaultString(string(billing.get("address")), "No especificada"));
        payload.put("postalCode", defaultString(string(billing.get("postalCode")), "11101"));
        payload.put("threeDSecureAuthenticationId", authenticationId);
        putIfPresent(payload, "sess", string(paymentData.get("sess")));
        putIfPresent(payload, "bankSess", string(paymentData.get("bankSess")));

        JsonNode response = post(settings, "/payments", payload);
        String externalReference = text(response, "_id");
        String initialStatus = defaultString(text(response, "status"), "PENDING").toUpperCase(Locale.ROOT);
        String message = firstNonBlank(
                text(response, "message"),
                text(response, "Message"),
                switch (initialStatus) {
                    case "APPROVED" -> "Pago autorizado. Esperando confirmación final de Clinpays.";
                    case "PENDING_AUTHENTICATION" -> "Complete la autenticación 3D Secure.";
                    case "DENIED" -> "Clinpays rechazó la transacción.";
                    default -> "Clinpays está procesando la transacción.";
                });

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("initialStatus", initialStatus);
        metadata.put("orderReference", request.orderNumber());
        putIfPresent(metadata, "transactionID", text(response, "transactionID"));
        putIfPresent(metadata, "extendedCode", text(response, "extendedCode"));
        putIfPresent(metadata, "paymentType", text(response, "paymentType"));
        putIfPresent(metadata, "safeIdentifier", text(response, "safeIdentifier"));
        JsonNode threeDSecureSession = response.path("threeDSecureSession");
        if (!threeDSecureSession.isMissingNode() && !threeDSecureSession.isNull()) {
            metadata.put("threeDSecureSession", mapper.convertValue(
                    threeDSecureSession,
                    new TypeReference<Map<String, Object>>() { }));
        }

        if (externalReference == null || externalReference.isBlank()) {
            externalReference = request.orderNumber();
        }

        // Clinpays specifies that the definitive result must be accepted from the
        // signed webhook. Even a frictionless APPROVED response remains PENDING
        // until that notification is verified.
        String internalStatus = "DENIED".equals(initialStatus) ? "FAILED" : "PENDING";
        return new PaymentResult(
                internalStatus,
                externalReference,
                null,
                message,
                metadata);
    }

    boolean verifyWebhook(String rawBody, String signatureHeader) {
        CommerceSettings settings = webhookSettings();
        if (signatureHeader == null || signatureHeader.isBlank()) return false;

        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String element : signatureHeader.split(",")) {
            String part = element.trim();
            if (part.startsWith("t=")) timestamp = part.substring(2);
            else if (part.startsWith("v1=")) signatures.add(part.substring(3));
        }
        if (timestamp == null || signatures.isEmpty()) return false;

        long receivedAt;
        try {
            receivedAt = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }
        if (Math.abs(Instant.now().getEpochSecond() - receivedAt) > WEBHOOK_TOLERANCE_SECONDS) {
            return false;
        }

        String signedPayload = timestamp + "." + rawBody;
        List<String> secrets = new ArrayList<>();
        decryptIfPresent(settings.clinpaysWebhookSecretCipher, secrets);
        decryptIfPresent(settings.clinpaysPreviousWebhookSecretCipher, secrets);
        for (String secret : secrets) {
            byte[] expected = hmac(secret, signedPayload);
            for (String signature : signatures) {
                try {
                    byte[] actual = HexFormat.of().parseHex(signature);
                    if (MessageDigest.isEqual(expected, actual)) return true;
                } catch (IllegalArgumentException ignored) {
                    // Continue with the next signature.
                }
            }
        }
        return false;
    }

    private void decryptIfPresent(String encrypted, List<String> values) {
        if (encrypted != null && !encrypted.isBlank()) {
            values.add(cipher.decrypt(encrypted));
        }
    }

    private byte[] hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo validar la firma de Clinpays", exception);
        }
    }

    private CommerceSettings requireSettings() {
        CommerceSettings settings = settingsRepository.findById(CommerceSettings.currentId())
                .orElseThrow(() -> new BadRequestException("TrastPay todavía no tiene configuración"));
        if (!configured(settings)) {
            throw new BadRequestException("Clinpays no está configurado o permanece desactivado");
        }
        return settings;
    }

    private CommerceSettings webhookSettings() {
        CommerceSettings settings = settingsRepository.findById(CommerceSettings.currentId())
                .orElseThrow(() -> new BadRequestException("TrastPay todavía no tiene configuración"));
        if (settings.clinpaysWebhookSecretCipher == null
                || settings.clinpaysWebhookSecretCipher.isBlank()) {
            throw new BadRequestException("El webhook de Clinpays no tiene un secreto configurado");
        }
        return settings;
    }

    private boolean configured(CommerceSettings settings) {
        return settings.clinpaysEnabled
                && settings.clinpaysAccessTokenCipher != null
                && !settings.clinpaysAccessTokenCipher.isBlank()
                && settings.clinpaysWebhookSecretCipher != null
                && !settings.clinpaysWebhookSecretCipher.isBlank();
    }

    private JsonNode post(CommerceSettings settings, String path, Map<String, Object> body) {
        String targetUri = baseUrl(settings) + path + "?lng=es";
        try {
            String rawToken = cipher.decrypt(settings.clinpaysAccessTokenCipher);
            String cleanToken = rawToken == null ? "" : rawToken.replaceFirst("(?i)^Bearer\\s+", "").trim();
            String tokenMasked = tokenPreview(cleanToken);
            String payload = mapper.writeValueAsString(body);

            log.info("[Clinpays API Request] POST {} | Entorno: {} | Token Sent: {} | Payload: {}",
                    targetUri, settings.clinpaysEnvironment, cleanToken, payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cleanToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            log.info("[Clinpays API Response] Status: {} | Body: {}", response.statusCode(), response.body());

            JsonNode json = response.body() == null || response.body().isBlank()
                    ? mapper.createObjectNode()
                    : mapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = firstNonBlank(
                        text(json, "message"),
                        text(json, "error"),
                        response.body());
                throw new BadRequestException("Clinpays HTTP " + response.statusCode() + " en " + targetUri + " | Token: " + tokenMasked + " | Respuesta: " + message);
            }
            return json;
        } catch (BadRequestException exception) {
            log.error("[Clinpays Error] {}", exception.getMessage());
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("La solicitud a Clinpays fue interrumpida");
        } catch (Exception exception) {
            log.error("[Clinpays Exception] Error de conexion con {}: {}", targetUri, exception.getMessage(), exception);
            throw new BadRequestException("No se pudo comunicar con Clinpays (" + targetUri + "): " + exception.getMessage());
        }
    }

    private String tokenPreview(String token) {
        if (token == null || token.isBlank()) return "[VACÍO]";
        if (token.length() <= 12) return token;
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6) + " (largo: " + token.length() + " chars)";
    }

    private String baseUrl(CommerceSettings settings) {
        return "PRODUCTION".equalsIgnoreCase(settings.clinpaysEnvironment)
                ? "https://api.clinpays.com"
                : "https://stage.paygatehn.com";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank()
                ? null
                : value.asText();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> source
                ? (Map<String, Object>) source
                : Map.of();
    }

    private String required(Map<String, Object> source, String key, String message) {
        String value = string(source.get(key));
        if (value == null || value.isBlank()) throw new BadRequestException(message);
        return value;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null) return fallback;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalizeExpiry(String value) {
        String normalized = digits(value);
        if (normalized.length() != 4) {
            throw new BadRequestException("La fecha de vencimiento debe usar el formato MM/AA");
        }
        return normalized;
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) return "HNL";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("LPS".equals(normalized) || "LEMPIRAS".equals(normalized) || "LEM".equals(normalized) || "$".equals(normalized)) {
            return "HNL";
        }
        if (!normalized.matches("[A-Z]{3}")) {
            return "HNL";
        }
        return normalized;
    }

    private String[] splitName(String fullName) {
        String normalized = defaultString(fullName, "Cliente").trim();
        int split = normalized.indexOf(' ');
        return split < 0
                ? new String[] { normalized, "" }
                : new String[] { normalized.substring(0, split), normalized.substring(split + 1).trim() };
    }

    private String validRedirect(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Configure la URL de retorno del pago");
        }
        try {
            URI uri = URI.create(value);
            boolean localDevelopment = "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(uri.getHost())
                        || "127.0.0.1".equals(uri.getHost()));
            if (!("https".equalsIgnoreCase(uri.getScheme()) || localDevelopment)
                    || uri.getRawAuthority() == null) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (Exception exception) {
            throw new BadRequestException("La URL de retorno de Clinpays debe usar HTTPS");
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) target.put(key, value);
    }
}
