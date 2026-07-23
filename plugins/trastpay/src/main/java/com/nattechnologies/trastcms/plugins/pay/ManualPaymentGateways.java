package com.nattechnologies.trastcms.plugins.pay;

import com.nattechnologies.trastcms.plugin.api.PaymentGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
class BankTransferGateway implements PaymentGateway {
    private final CommerceSettingsRepository settings;

    BankTransferGateway(CommerceSettingsRepository settings) {
        this.settings = settings;
    }

    @Override
    public String key() {
        return "bank_transfer";
    }

    @Override
    public String name() {
        return "Transferencia bancaria";
    }

    @Override
    public String description() {
        return "Registra la orden y muestra las instrucciones bancarias configuradas por el comercio.";
    }

    @Override
    public PaymentResult create(PaymentRequest request) {
        String instructions = settings.findById(CommerceSettings.DEFAULT_ID)
                .map(value -> value.bankInstructions)
                .filter(value -> value != null && !value.isBlank())
                .orElse("Configure sus datos bancarios en TrastPay.");
        return new PaymentResult(
                "PENDING",
                "BANK-" + request.orderNumber(),
                null,
                "Orden pendiente de confirmación bancaria",
                Map.of("instructions", instructions));
    }
}

@Component
class CashGateway implements PaymentGateway {
    @Override
    public String key() {
        return "cash";
    }

    @Override
    public String name() {
        return "Pago contra entrega";
    }

    @Override
    public String description() {
        return "Disponible para productos físicos y servicios locales.";
    }

    @Override
    public PaymentResult create(PaymentRequest request) {
        return new PaymentResult(
                "PENDING",
                "CASH-" + request.orderNumber(),
                null,
                "Pago pendiente al momento de la entrega",
                Map.of());
    }
}

@Component
class SandboxGateway implements PaymentGateway {
    @Override
    public String key() {
        return "sandbox";
    }

    @Override
    public String name() {
        return "Pago de prueba";
    }

    @Override
    public String description() {
        return "Confirma inmediatamente para probar el flujo completo sin dinero real.";
    }

    @Override
    public boolean supportsRefunds() {
        return true;
    }

    @Override
    public PaymentResult create(PaymentRequest request) {
        return new PaymentResult(
                "SUCCEEDED",
                "TEST-" + UUID.randomUUID(),
                null,
                "Pago de prueba confirmado",
                Map.of("sandbox", true));
    }

    @Override
    public PaymentResult refund(
            String externalReference,
            BigDecimal amount,
            String reason) {
        return new PaymentResult(
                "REFUNDED",
                externalReference,
                null,
                "Reembolso de prueba confirmado",
                Map.of("reason", reason == null ? "" : reason));
    }
}
