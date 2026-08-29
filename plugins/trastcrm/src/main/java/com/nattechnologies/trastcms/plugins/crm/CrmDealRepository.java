package com.nattechnologies.trastcms.plugins.crm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrmDealRepository extends JpaRepository<CrmDeal, String> {

    @Override
    @EntityGraph(attributePaths = {
            "stage",
            "contact",
            "contact.company",
            "company"
    })
    Optional<CrmDeal> findById(String id);

    @EntityGraph(attributePaths = {
            "stage",
            "contact",
            "contact.company",
            "company"
    })
    Page<CrmDeal> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    long countByStageClosedFalse();

    Optional<CrmDeal> findByTitleIgnoreCase(String title);

    @EntityGraph(attributePaths = {
            "stage",
            "contact",
            "contact.company",
            "company"
    })
    List<CrmDeal> findByDemoDataTrue();
}
