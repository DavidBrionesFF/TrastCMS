package com.nattechnologies.trastcms.plugins.pay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CommerceCouponRepository extends JpaRepository<CommerceCoupon, String> {
    Optional<CommerceCoupon> findByCodeIgnoreCase(String code);
    List<CommerceCoupon> findByDemoDataTrue();
    long countByDemoDataTrue();
}
