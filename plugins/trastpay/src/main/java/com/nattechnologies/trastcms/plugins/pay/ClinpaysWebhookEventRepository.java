package com.nattechnologies.trastcms.plugins.pay;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClinpaysWebhookEventRepository extends JpaRepository<ClinpaysWebhookEvent, String> {
}
