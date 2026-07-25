package com.nattechnologies.trastcms.plugins.crm;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CrmActivityRepository extends JpaRepository<CrmActivity, String> {
    Page<CrmActivity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByCompletedAtIsNull();
    List<CrmActivity> findByDemoDataTrue();
}
