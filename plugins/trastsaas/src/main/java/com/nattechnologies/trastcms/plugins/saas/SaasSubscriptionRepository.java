package com.nattechnologies.trastcms.plugins.saas;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SaasSubscriptionRepository
        extends JpaRepository<SaasSubscription, String> {

    @Override
    @EntityGraph(attributePaths = {
            "plan",
            "plan.product"
    })
    List<SaasSubscription> findAll();

    @EntityGraph(attributePaths = {
            "plan",
            "plan.product"
    })
    List<SaasSubscription>
            findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(
                    String email);

    @EntityGraph(attributePaths = {
            "plan",
            "plan.product"
    })
    List<SaasSubscription> findByOrderId(String orderId);

    @EntityGraph(attributePaths = {
            "plan",
            "plan.product"
    })
    List<SaasSubscription> findByDemoDataTrue();

    @EntityGraph(attributePaths = {
            "plan",
            "plan.product"
    })
    Optional<SaasSubscription>
            findFirstByPlanIdAndCustomerEmailIgnoreCaseAndDemoDataTrue(
                    String planId,
                    String customerEmail);

    long countByDemoDataTrue();
}
