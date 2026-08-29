package com.nattechnologies.trastcms.domain.site;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CmsSiteRepository extends JpaRepository<CmsSite, String> {
    Optional<CmsSite> findBySiteKeyIgnoreCase(String siteKey);
    List<CmsSite> findAllByOrderByNameAsc();
    boolean existsBySiteKeyIgnoreCase(String siteKey);
}
