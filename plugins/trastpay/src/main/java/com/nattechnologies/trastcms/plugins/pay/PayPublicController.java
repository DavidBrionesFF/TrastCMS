package com.nattechnologies.trastcms.plugins.pay;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/commerce")
public class PayPublicController {
    private final PayService service;
    private final PayPalGateway payPal;

    PayPublicController(
            PayService service,
            PayPalGateway payPal) {
        this.service = service;
        this.payPal = payPal;
    }

    @GetMapping("/config")
    PayDtos.SettingsResponse config() {
        return service.publicSettings();
    }

    @PostMapping("/carts")
    @ResponseStatus(HttpStatus.CREATED)
    PayDtos.CartResponse create(
            @RequestParam(required = false) String currency) {
        return service.createCart(currency);
    }

    @GetMapping("/carts/{token}")
    PayDtos.CartResponse cart(
            @PathVariable String token,
            @RequestParam(required = false) String coupon) {
        return service.getCart(token, coupon);
    }

    @PostMapping("/carts/{token}/items")
    PayDtos.CartResponse add(
            @PathVariable String token,
            @Valid @RequestBody PayDtos.AddItemRequest request,
            @RequestParam(required = false) String coupon) {
        return service.add(token, request, coupon);
    }

    @PutMapping("/carts/{token}/items/{itemId}")
    PayDtos.CartResponse update(
            @PathVariable String token,
            @PathVariable String itemId,
            @Valid @RequestBody PayDtos.UpdateItemRequest request,
            @RequestParam(required = false) String coupon) {
        return service.update(token, itemId, request, coupon);
    }

    @DeleteMapping("/carts/{token}/items")
    PayDtos.CartResponse clear(@PathVariable String token) {
        return service.clear(token);
    }

    @PostMapping("/carts/{token}/checkout")
    PayDtos.CheckoutResponse checkout(
            @PathVariable String token,
            @Valid @RequestBody PayDtos.CheckoutRequest request) {
        return service.checkout(token, request);
    }

    @GetMapping("/gateways")
    List<Map<String, Object>> gateways() {
        return service.gateways();
    }

    @GetMapping("/paypal/return")
    RedirectView paypalReturn(
            @RequestParam String orderId,
            @RequestParam("token") String paypalOrderId) {
        try {
            PayDtos.OrderResponse order = service.capturePayPal(
                    orderId,
                    paypalOrderId);
            return cartRedirect(
                    "success",
                    order.orderNumber(),
                    "Pago confirmado correctamente");
        } catch (RuntimeException exception) {
            return cartRedirect(
                    "failed",
                    null,
                    exception.getMessage() == null
                            ? "No se pudo confirmar el pago PayPal"
                            : exception.getMessage());
        }
    }

    @PostMapping("/paypal/webhook")
    ResponseEntity<Void> paypalWebhook(
            @RequestHeader HttpHeaders headers,
            @RequestBody JsonNode event) {
        if (!payPal.verifyWebhook(headers, event)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String eventType = event.path("event_type").asText();
        JsonNode resource = event.path("resource");
        if ("CHECKOUT.ORDER.APPROVED".equals(eventType)) {
            String paypalOrderId = resource.path("id").asText();
            String orderId = resource.path("purchase_units")
                    .path(0)
                    .path("reference_id")
                    .asText();
            if (!paypalOrderId.isBlank() && !orderId.isBlank()) {
                service.capturePayPal(orderId, paypalOrderId);
            }
        } else if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
            String captureId = resource.path("id").asText();
            String paypalOrderId = resource.path("supplementary_data")
                    .path("related_ids")
                    .path("order_id")
                    .asText();
            if (!paypalOrderId.isBlank()) {
                service.confirmPayPalWebhook(paypalOrderId, captureId);
            }
        }
        return ResponseEntity.noContent().build();
    }

    private RedirectView cartRedirect(
            String status,
            String order,
            String message) {
        StringBuilder target = new StringBuilder("/cart?paypal=")
                .append(encode(status));
        if (order != null && !order.isBlank()) {
            target.append("&order=").append(encode(order));
        }
        if (message != null && !message.isBlank()) {
            target.append("&message=").append(encode(message));
        }
        RedirectView redirect = new RedirectView(target.toString());
        redirect.setContextRelative(true);
        return redirect;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
