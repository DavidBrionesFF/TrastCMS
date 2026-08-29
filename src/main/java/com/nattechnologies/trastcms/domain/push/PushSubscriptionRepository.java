package com.nattechnologies.trastcms.domain.push;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {
    List<PushSubscription> findByActiveTrue();
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findBySubscriberEmailAndActiveTrue(String email);
    Page<PushSubscription> findAll(Pageable pageable);
}
