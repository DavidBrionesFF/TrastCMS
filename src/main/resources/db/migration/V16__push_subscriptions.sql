CREATE TABLE push_subscription (
    id VARCHAR(36) PRIMARY KEY,
    endpoint VARCHAR(2000) NOT NULL UNIQUE,
    p256dh_key VARCHAR(500) NOT NULL,
    auth_key VARCHAR(200) NOT NULL,
    user_agent VARCHAR(500),
    ip_hash VARCHAR(128),
    subscriber_email VARCHAR(190),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_push_sub_active ON push_subscription(active, created_at);
CREATE INDEX idx_push_sub_email ON push_subscription(subscriber_email);
