package com.nattechnologies.trastcms.plugins.pay;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
interface CommerceCustomerRepository extends JpaRepository<CommerceCustomer,String> { Optional<CommerceCustomer> findByEmailIgnoreCase(String email); }
