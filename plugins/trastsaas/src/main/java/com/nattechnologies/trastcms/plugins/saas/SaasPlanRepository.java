package com.nattechnologies.trastcms.plugins.saas;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SaasPlanRepository extends JpaRepository<SaasPlan, String> {

    @Override
    @EntityGraph(attributePaths = "product")
    Optional<SaasPlan> findById(String id);

    @Override
    @EntityGraph(attributePaths = "product")
    List<SaasPlan> findAll();

    @EntityGraph(attributePaths = "product")
    Optional<SaasPlan> findByProductIdAndPlanKey(
            String productId,
            String planKey);

    @EntityGraph(attributePaths = "product")
    List<SaasPlan> findByProductIdAndActiveTrueOrderByPriceAsc(
            String productId);

    @EntityGraph(attributePaths = "product")
    List<SaasPlan> findByActiveTrueOrderByPriceAsc();

    @EntityGraph(attributePaths = "product")
    List<SaasPlan> findByDemoDataTrue();

    long countByDemoDataTrue();

    long countByProductId(String productId);
}
