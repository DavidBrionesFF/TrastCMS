package com.nattechnologies.trastcms.plugins.pay;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*; import org.springframework.data.domain.*;
interface CommerceOrderRepository extends JpaRepository<CommerceOrder,String> { Optional<CommerceOrder> findByOrderNumber(String orderNumber); Page<CommerceOrder> findAllByOrderByCreatedAtDesc(Pageable pageable); }
