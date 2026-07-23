package com.nattechnologies.trastcms.plugins.pay;

import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class OrderAccessService {
    private final CommerceOrderRepository orders;
    public OrderAccessService(CommerceOrderRepository orders) { this.orders = orders; }
    @Transactional(readOnly = true)
    public OrderAccess require(String orderNumber, String token) {
        CommerceOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada"));
        byte[] expected = order.publicToken.getBytes(StandardCharsets.UTF_8);
        byte[] supplied = (token == null ? "" : token).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new NotFoundException("Orden no encontrada");
        }
        return new OrderAccess(order.id, order.orderNumber, order.customerEmail);
    }
    public record OrderAccess(String id, String orderNumber, String customerEmail) { }
}
