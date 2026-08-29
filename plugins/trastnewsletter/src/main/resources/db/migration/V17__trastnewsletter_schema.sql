CREATE TABLE newsletter_subscriber (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    name VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    push_subscription_id VARCHAR(36),
    source VARCHAR(120),
    token VARCHAR(100) NOT NULL,
    subscribed_at TIMESTAMP NOT NULL,
    unsubscribed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE newsletter_campaign (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(220) NOT NULL,
    subject VARCHAR(220) NOT NULL,
    body_html TEXT NOT NULL,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_title VARCHAR(100),
    push_body VARCHAR(300),
    push_url VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    recipient_count INTEGER,
    open_count INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(190) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE newsletter_send_log (
    id VARCHAR(36) PRIMARY KEY,
    campaign_id VARCHAR(36) NOT NULL,
    subscriber_id VARCHAR(36) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    CONSTRAINT fk_send_log_campaign FOREIGN KEY (campaign_id) REFERENCES newsletter_campaign(id),
    CONSTRAINT fk_send_log_subscriber FOREIGN KEY (subscriber_id) REFERENCES newsletter_subscriber(id)
);

CREATE INDEX idx_newsletter_sub_status ON newsletter_subscriber(status, subscribed_at);
CREATE INDEX idx_newsletter_campaign_status ON newsletter_campaign(status, scheduled_at);
