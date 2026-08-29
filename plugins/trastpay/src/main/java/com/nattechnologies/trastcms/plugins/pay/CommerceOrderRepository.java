package com.nattechnologies.trastcms.plugins.pay;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface CommerceOrderRepository extends JpaRepository<CommerceOrder,String> {
    Optional<CommerceOrder> findByOrderNumber(String orderNumber);
    Page<CommerceOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<CommerceOrder> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
