package com.nattechnologies.trastcms.plugins.crm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmReputationRepository extends JpaRepository<CrmReputationRequest, String> {
    Optional<CrmReputationRequest> findByPublicToken(String publicToken);
    Optional<CrmReputationRequest> findBySourceTypeAndSourceReference(CrmReputationSource sourceType, String sourceReference);
    Page<CrmReputationRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
