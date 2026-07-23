package com.nattechnologies.trastcms.plugins.pay;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
interface CommerceCouponRepository extends JpaRepository<CommerceCoupon,String> { Optional<CommerceCoupon> findByCodeIgnoreCase(String code); }
