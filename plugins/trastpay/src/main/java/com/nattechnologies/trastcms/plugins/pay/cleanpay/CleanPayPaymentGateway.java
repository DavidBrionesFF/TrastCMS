package com.nattechnologies.trastcms.plugins.pay.cleanpay;

import com.nattechnologies.trastcms.plugin.api.PaymentGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CleanPayPaymentGateway implements PaymentGateway {
    private final CleanPayGatewaySupport support;

    public CleanPayPaymentGateway(CleanPayGatewaySupport support) {
        this.support = support;
    }

    @Override
    public String key() {
        return "cleanpay";
    }

    @Override
    public String name() {
        return "CleanPay / Promerica";
    }

    @Override
    public PaymentResult create(PaymentRequest request) {
        return (PaymentResult) support.invoke(
                PaymentGateway.class,
                "create",
                new Object[]{request},
                PaymentResult.class);
    }

    @Override
    public PaymentResult capture(String externalReference, BigDecimal amount) {
        throw new UnsupportedOperationException("Captura no soportada");
    }

    @Override
    public PaymentResult refund(
            String externalReference,
            BigDecimal amount,
            String reason) {
        throw new UnsupportedOperationException("Reembolso no soportado");
    }
}
