ALTER TABLE commerce_settings ADD COLUMN prices_include_tax BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE commerce_settings ADD COLUMN tax_label VARCHAR(80) NOT NULL DEFAULT 'ISV';
ALTER TABLE commerce_settings ADD COLUMN tax_country VARCHAR(2) NOT NULL DEFAULT 'HN';
ALTER TABLE commerce_coupon ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE commerce_settings ADD COLUMN paypal_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE commerce_settings ADD COLUMN paypal_mode VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';
ALTER TABLE commerce_settings ADD COLUMN paypal_client_id VARCHAR(500);
ALTER TABLE commerce_settings ADD COLUMN paypal_client_secret_cipher VARCHAR(4000);
ALTER TABLE commerce_settings ADD COLUMN paypal_webhook_id VARCHAR(200);

ALTER TABLE store_product ADD COLUMN taxable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE store_product ADD COLUMN tax_class VARCHAR(40) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE store_product ADD COLUMN tax_rate_override DECIMAL(8,4);
ALTER TABLE store_product ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX ix_store_product_demo ON store_product(demo_data);

ALTER TABLE saas_product ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE saas_plan ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE saas_subscription ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE saas_license ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE saas_settings (
    id INT PRIMARY KEY,
    company_name VARCHAR(180) NOT NULL,
    support_email VARCHAR(190),
    billing_email VARCHAR(190),
    default_currency VARCHAR(3) NOT NULL,
    default_trial_days INT NOT NULL,
    default_grace_days INT NOT NULL,
    allow_self_service_cancel BOOLEAN NOT NULL,
    allow_plan_changes BOOLEAN NOT NULL,
    require_device_name BOOLEAN NOT NULL,
    show_annual_savings BOOLEAN NOT NULL,
    pricing_headline VARCHAR(250),
    pricing_description VARCHAR(1000),
    portal_message VARCHAR(1000),
    terms_url VARCHAR(1000),
    privacy_url VARCHAR(1000),
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE crm_company ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE crm_contact ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE crm_deal ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE crm_activity ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE crm_form ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
