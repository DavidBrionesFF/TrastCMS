package com.nattechnologies.trastcms.plugins.saas;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SaasLicenseRepository extends JpaRepository<SaasLicense, String> {

    @Override
    @EntityGraph(attributePaths = {
            "product",
            "plan",
            "plan.product",
            "subscription"
    })
    List<SaasLicense> findAll();

    @EntityGraph(attributePaths = {
            "product",
            "plan",
            "plan.product",
            "subscription"
    })
    Optional<SaasLicense> findByKeyHash(String keyHash);

    @EntityGraph(attributePaths = {
            "product",
            "plan",
            "plan.product",
            "subscription"
    })
    List<SaasLicense> findByOrderId(String orderId);

    @EntityGraph(attributePaths = {
            "product",
            "plan",
            "plan.product",
            "subscription"
    })
    List<SaasLicense> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(
            String email);

    @EntityGraph(attributePaths = {
            "product",
            "plan",
            "plan.product",
            "subscription"
    })
    List<SaasLicense> findByDemoDataTrue();

    @EntityGraph(attributePaths = {
            "product",
            "plan",
            "plan.product",
            "subscription"
    })
    Optional<SaasLicense>
            findFirstByPlanIdAndCustomerEmailIgnoreCaseAndDemoDataTrue(
                    String planId,
                    String customerEmail);

    long countByDemoDataTrue();
}
