CREATE TABLE crm_reputation_request (
    id VARCHAR(36) PRIMARY KEY,
    site_id VARCHAR(36) NOT NULL,
    contact_id VARCHAR(36),
    source_type VARCHAR(30) NOT NULL,
    source_reference VARCHAR(190) NOT NULL,
    source_label VARCHAR(240) NOT NULL,
    recipient_email VARCHAR(190) NOT NULL,
    customer_name VARCHAR(180),
    trigger_event VARCHAR(120) NOT NULL,
    public_token VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rating INTEGER,
    private_feedback TEXT,
    channel_id VARCHAR(80),
    follow_up_activity_id VARCHAR(36),
    sent_at TIMESTAMP,
    rated_at TIMESTAMP,
    channel_clicked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_crm_reputation_contact FOREIGN KEY (contact_id) REFERENCES crm_contact(id) ON DELETE SET NULL,
    CONSTRAINT fk_crm_reputation_activity FOREIGN KEY (follow_up_activity_id) REFERENCES crm_activity(id) ON DELETE SET NULL,
    CONSTRAINT uq_crm_reputation_source UNIQUE(site_id, source_type, source_reference),
    CONSTRAINT uq_crm_reputation_token UNIQUE(public_token)
);

CREATE INDEX idx_crm_reputation_status ON crm_reputation_request(site_id, status, created_at);
CREATE INDEX idx_crm_reputation_contact ON crm_reputation_request(site_id, contact_id, created_at);

-- Academy and Store + SaaS now use CRM reputation automation as part of their
-- default stack. V21 already created these site-specific plugin-state rows.
UPDATE bundled_plugin_state
SET enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE plugin_id IN (
    '22222222-2222-2222-2222-222222222222::trastcrm',
    '33333333-3333-3333-3333-333333333333::trastcrm'
);
