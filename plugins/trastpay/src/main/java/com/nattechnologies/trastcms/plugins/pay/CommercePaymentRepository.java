package com.nattechnologies.trastcms.plugins.pay;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
interface CommercePaymentRepository extends JpaRepository<CommercePayment,String> { Optional<CommercePayment> findByIdempotencyKey(String idempotencyKey); List<CommercePayment> findByOrderIdOrderByCreatedAtDesc(String orderId); }
