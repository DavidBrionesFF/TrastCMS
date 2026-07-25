package com.nattechnologies.trastcms.plugins.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PaymentGateway;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.PluginSecretCipher;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class PayPalGateway implements PaymentGateway {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final CommerceSettingsRepository settingsRepository;
    private final PluginSecretCipher cipher;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;
    private volatile String cachedCredentialKey;

    PayPalGateway(
            CommerceSettingsRepository settingsRepository,
            PluginSecretCipher cipher,
            ObjectMapper mapper) {
        this.settingsRepository = settingsRepository;
        this.cipher = cipher;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String key() {
        return "paypal";
    }

    @Override
    public String name() {
        return "PayPal";
    }

    @Override
    public String description() {
        return "Checkout online mediante PayPal Orders v2, con redirección y captura segura en el servidor.";
    }

    @Override
    public boolean supportsRefunds() {
        return false;
    }

    @Override
    public boolean enabled() {
        return settingsRepository.findById(CommerceSettings.DEFAULT_ID)
                .map(this::configured)
                .orElse(false);
    }

    @Override
    public PaymentResult create(PaymentRequest request) {
        CommerceSettings settings = requireSettings();
        String accessToken = accessToken(settings);
        String origin = origin(request.returnUrl());
        String returnUrl = origin
                + "/api/public/commerce/paypal/return?orderId="
                + encode(request.orderId());
        String cancelUrl = origin
                + "/cart?paypal=cancelled&order="
                + encode(request.orderNumber());

        Map<String, Object> amount = Map.of(
                "currency_code", request.currency().toUpperCase(Locale.ROOT),
                "value", money(request.amount()));
        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("reference_id", request.orderId());
        purchaseUnit.put("custom_id", request.orderId());
        purchaseUnit.put("invoice_id", request.orderNumber());
        purchaseUnit.put("description", "Orden " + request.orderNumber());
        purchaseUnit.put("amount", amount);

        Map<String, Object> experience = new LinkedHashMap<>();
        experience.put("return_url", returnUrl);
        experience.put("cancel_url", cancelUrl);
        experience.put("user_action", "PAY_NOW");
        experience.put("shipping_preference", "NO_SHIPPING");
        experience.put("landing_page", "LOGIN");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intent", "CAPTURE");
        body.put("purchase_units", List.of(purchaseUnit));
        body.put("payment_source", Map.of(
                "paypal", Map.of("experience_context", experience)));

        JsonNode response = sendJson(
                settings,
                "POST",
                "/v2/checkout/orders",
                accessToken,
                request.idempotencyKey(),
                body);
        String paypalOrderId = text(response, "id");
        String approveUrl = findLink(response, "payer-action");
        if (approveUrl == null) approveUrl = findLink(response, "approve");
        if (paypalOrderId == null || approveUrl == null) {
            throw new BadRequestException(
                    "PayPal no devolvió una URL válida para aprobar la compra");
        }

        return new PaymentResult(
                "PENDING",
                paypalOrderId,
                approveUrl,
                "Continúe en PayPal para aprobar el pago",
                Map.of(
                        "paypalOrderId", paypalOrderId,
                        "paypalMode", normalizeMode(settings.paypalMode)));
    }

    @Override
    public PaymentResult capture(
            String externalReference,
            BigDecimal amount) {
        CommerceSettings settings = requireSettings();
        JsonNode response = sendJson(
                settings,
                "POST",
                "/v2/checkout/orders/" + encodePath(externalReference) + "/capture",
                accessToken(settings),
                "capture-" + externalReference,
                Map.of());
        String status = text(response, "status");
        JsonNode capture = response.path("purchase_units")
                .path(0)
                .path("payments")
                .path("captures")
                .path(0);
        String captureId = text(capture, "id");
        String captureStatus = text(capture, "status");
        boolean succeeded = "COMPLETED".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(captureStatus);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("paypalOrderId", externalReference);
        if (captureId != null) metadata.put("paypalCaptureId", captureId);
        if (captureStatus != null) metadata.put("paypalCaptureStatus", captureStatus);

        return new PaymentResult(
                succeeded ? "SUCCEEDED" : "PENDING",
                externalReference,
                null,
                succeeded
                        ? "Pago PayPal capturado correctamente"
                        : "PayPal todavía está procesando la captura",
                metadata);
    }

    boolean verifyWebhook(
            HttpHeaders headers,
            JsonNode event) {
        CommerceSettings settings = requireSettings();
        if (settings.paypalWebhookId == null
                || settings.paypalWebhookId.isBlank()) {
            return false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("auth_algo", header(headers, "paypal-auth-algo"));
        body.put("cert_url", header(headers, "paypal-cert-url"));
        body.put("transmission_id", header(headers, "paypal-transmission-id"));
        body.put("transmission_sig", header(headers, "paypal-transmission-sig"));
        body.put("transmission_time", header(headers, "paypal-transmission-time"));
        body.put("webhook_id", settings.paypalWebhookId);
        body.put("webhook_event", mapper.convertValue(event, Map.class));

        if (body.values().stream().anyMatch(value -> value == null)) {
            return false;
        }

        JsonNode response = sendJson(
                settings,
                "POST",
                "/v1/notifications/verify-webhook-signature",
                accessToken(settings),
                null,
                body);
        return "SUCCESS".equalsIgnoreCase(
                text(response, "verification_status"));
    }

    private CommerceSettings requireSettings() {
        CommerceSettings settings = settingsRepository
                .findById(CommerceSettings.DEFAULT_ID)
                .orElseThrow(() -> new BadRequestException(
                        "TrastPay todavía no tiene configuración"));
        if (!configured(settings)) {
            throw new BadRequestException(
                    "PayPal no está configurado o permanece desactivado");
        }
        return settings;
    }

    private boolean configured(CommerceSettings settings) {
        return settings.paypalEnabled
                && settings.paypalClientId != null
                && !settings.paypalClientId.isBlank()
                && settings.paypalClientSecretCipher != null
                && !settings.paypalClientSecretCipher.isBlank();
    }

    private synchronized String accessToken(CommerceSettings settings) {
        String credentialKey = normalizeMode(settings.paypalMode)
                + ":" + settings.paypalClientId;
        if (cachedAccessToken != null
                && credentialKey.equals(cachedCredentialKey)
                && tokenExpiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return cachedAccessToken;
        }

        String secret = cipher.decrypt(settings.paypalClientSecretCipher);
        String basic = Base64.getEncoder().encodeToString(
                (settings.paypalClientId + ":" + secret)
                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl(settings) + "/v1/oauth2/token"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Accept-Language", "es_ES")
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=client_credentials"))
                .build();
        JsonNode response = send(request);
        String token = text(response, "access_token");
        long expiresIn = response.path("expires_in").asLong(300);
        if (token == null) {
            throw new BadRequestException(
                    "PayPal rechazó las credenciales configuradas");
        }
        cachedAccessToken = token;
        cachedCredentialKey = credentialKey;
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn));
        return token;
    }

    private JsonNode sendJson(
            CommerceSettings settings,
            String method,
            String path,
            String accessToken,
            String requestId,
            Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl(settings) + path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken);
            if (requestId != null && !requestId.isBlank()) {
                builder.header("PayPal-Request-Id", requestId);
            }
            String payload = mapper.writeValueAsString(body);
            HttpRequest request = switch (method) {
                case "POST" -> builder.POST(
                        HttpRequest.BodyPublishers.ofString(payload)).build();
                default -> throw new IllegalArgumentException(
                        "Método PayPal no soportado: " + method);
            };
            return send(request);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException(
                    "No se pudo preparar la solicitud para PayPal");
        }
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = response.body() == null || response.body().isBlank()
                    ? mapper.createObjectNode()
                    : mapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = text(json, "message");
                if (message == null) message = text(json, "error_description");
                throw new BadRequestException(
                        message == null
                                ? "PayPal respondió con estado " + response.statusCode()
                                : "PayPal: " + message);
            }
            return json;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BadRequestException(
                    "La comunicación con PayPal fue interrumpida");
        } catch (Exception exception) {
            throw new BadRequestException(
                    "No fue posible comunicarse con PayPal");
        }
    }

    private String baseUrl(CommerceSettings settings) {
        return "LIVE".equals(normalizeMode(settings.paypalMode))
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    private String normalizeMode(String value) {
        return "LIVE".equalsIgnoreCase(value) ? "LIVE" : "SANDBOX";
    }

    private String origin(String returnUrl) {
        try {
            URI uri = URI.create(returnUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getRawAuthority() == null) {
                throw new IllegalArgumentException();
            }
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (Exception exception) {
            throw new BadRequestException(
                    "El checkout debe indicar una URL de retorno absoluta");
        }
    }

    private String findLink(JsonNode response, String relation) {
        for (JsonNode link : response.path("links")) {
            if (relation.equalsIgnoreCase(link.path("rel").asText())) {
                String href = link.path("href").asText();
                return href.isBlank() ? null : href;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(field).asText("");
        return value.isBlank() ? null : value;
    }

    private String header(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        return value == null || value.isBlank() ? null : value;
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return encode(value).replace("+", "%20");
    }
}
