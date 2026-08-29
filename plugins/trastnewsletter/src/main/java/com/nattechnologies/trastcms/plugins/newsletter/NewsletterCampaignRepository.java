package com.nattechnologies.trastcms.plugins.newsletter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterCampaignRepository extends JpaRepository<NewsletterCampaign, String> {
    Page<NewsletterCampaign> findAll(Pageable p);
    long countByStatus(String status);
}
