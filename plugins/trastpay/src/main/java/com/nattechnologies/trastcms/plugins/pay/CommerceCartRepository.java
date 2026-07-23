package com.nattechnologies.trastcms.plugins.pay;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
interface CommerceCartRepository extends JpaRepository<CommerceCart,String> { Optional<CommerceCart> findByCartToken(String cartToken); }
