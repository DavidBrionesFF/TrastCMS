package com.nattechnologies.trastcms.plugins.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

interface StoreInventoryMovementRepository extends JpaRepository<StoreInventoryMovement, String> {
    Page<StoreInventoryMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
    void deleteByProductIdIn(Collection<String> productIds);
}
