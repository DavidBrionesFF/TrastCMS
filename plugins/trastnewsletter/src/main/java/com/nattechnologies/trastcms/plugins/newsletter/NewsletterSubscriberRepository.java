package com.nattechnologies.trastcms.plugins.newsletter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, String> {
    Optional<NewsletterSubscriber> findByEmail(String email);
    Optional<NewsletterSubscriber> findByToken(String token);
    Page<NewsletterSubscriber> findByStatus(String status, Pageable p);
    long countByStatus(String status);
}
