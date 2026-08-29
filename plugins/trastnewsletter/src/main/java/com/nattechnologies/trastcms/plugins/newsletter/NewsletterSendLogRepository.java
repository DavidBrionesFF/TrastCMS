package com.nattechnologies.trastcms.plugins.newsletter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSendLogRepository extends JpaRepository<NewsletterSendLog, String> {
}
