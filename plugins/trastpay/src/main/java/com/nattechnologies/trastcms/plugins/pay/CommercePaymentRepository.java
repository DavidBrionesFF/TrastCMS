package com.nattechnologies.trastcms.plugins.pay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CommercePaymentRepository
        extends JpaRepository<CommercePayment, String> {
    Optional<CommercePayment> findByIdempotencyKey(String idempotencyKey);
    Optional<CommercePayment> findByExternalReference(String externalReference);
    List<CommercePayment> findByOrderIdOrderByCreatedAtDesc(String orderId);
}
