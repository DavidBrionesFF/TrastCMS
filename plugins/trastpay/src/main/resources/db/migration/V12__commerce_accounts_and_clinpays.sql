ALTER TABLE commerce_settings ADD COLUMN bank_transfer_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE commerce_settings ADD COLUMN cash_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE commerce_settings ADD COLUMN sandbox_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE commerce_settings ADD COLUMN paypal_receiver_email VARCHAR(190);
ALTER TABLE commerce_settings ADD COLUMN clinpays_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE commerce_settings ADD COLUMN clinpays_environment VARCHAR(20) NOT NULL DEFAULT 'STAGE';
ALTER TABLE commerce_settings ADD COLUMN clinpays_access_token_cipher VARCHAR(8000);
ALTER TABLE commerce_settings ADD COLUMN clinpays_account_id VARCHAR(190);
ALTER TABLE commerce_settings ADD COLUMN clinpays_webhook_secret_cipher VARCHAR(4000);
ALTER TABLE commerce_settings ADD COLUMN clinpays_previous_webhook_secret_cipher VARCHAR(4000);

CREATE TABLE commerce_clinpays_webhook_event (
    webhook_id VARCHAR(190) PRIMARY KEY,
    event_type VARCHAR(80),
    order_reference VARCHAR(100),
    payment_reference VARCHAR(190),
    processed_at TIMESTAMP NOT NULL
);
