CREATE TABLE crm_company (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    domain VARCHAR(190),
    email VARCHAR(190),
    phone VARCHAR(60),
    address VARCHAR(500),
    city VARCHAR(120),
    country VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'PROSPECT',
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE crm_contact (
    id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(190),
    phone VARCHAR(60),
    job_title VARCHAR(140),
    status VARCHAR(30) NOT NULL DEFAULT 'LEAD',
    source VARCHAR(120),
    owner_email VARCHAR(190),
    tags TEXT,
    notes TEXT,
    company_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_crm_contact_company FOREIGN KEY (company_id) REFERENCES crm_company(id)
);

CREATE TABLE crm_pipeline_stage (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL,
    probability INTEGER NOT NULL DEFAULT 0,
    color VARCHAR(20) NOT NULL DEFAULT '#6d4aff',
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    won BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE crm_deal (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(220) NOT NULL,
    value_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'HNL',
    expected_close_date DATE,
    owner_email VARCHAR(190),
    description TEXT,
    contact_id VARCHAR(36),
    company_id VARCHAR(36),
    stage_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_crm_deal_contact FOREIGN KEY (contact_id) REFERENCES crm_contact(id),
    CONSTRAINT fk_crm_deal_company FOREIGN KEY (company_id) REFERENCES crm_company(id),
    CONSTRAINT fk_crm_deal_stage FOREIGN KEY (stage_id) REFERENCES crm_pipeline_stage(id)
);

CREATE TABLE crm_activity (
    id VARCHAR(36) PRIMARY KEY,
    activity_type VARCHAR(30) NOT NULL,
    subject VARCHAR(220) NOT NULL,
    description TEXT,
    due_at TIMESTAMP,
    completed_at TIMESTAMP,
    assigned_to VARCHAR(190),
    contact_id VARCHAR(36),
    company_id VARCHAR(36),
    deal_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_crm_activity_contact FOREIGN KEY (contact_id) REFERENCES crm_contact(id),
    CONSTRAINT fk_crm_activity_company FOREIGN KEY (company_id) REFERENCES crm_company(id),
    CONSTRAINT fk_crm_activity_deal FOREIGN KEY (deal_id) REFERENCES crm_deal(id)
);

CREATE TABLE crm_form (
    id VARCHAR(36) PRIMARY KEY,
    form_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(1000),
    success_message VARCHAR(500) NOT NULL,
    notify_emails VARCHAR(1000),
    fields_json TEXT NOT NULL,
    settings_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE crm_submission (
    id VARCHAR(36) PRIMARY KEY,
    form_id VARCHAR(36) NOT NULL,
    contact_id VARCHAR(36),
    payload_json TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    source_url VARCHAR(1000),
    ip_hash VARCHAR(128),
    user_agent VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_crm_submission_form FOREIGN KEY (form_id) REFERENCES crm_form(id),
    CONSTRAINT fk_crm_submission_contact FOREIGN KEY (contact_id) REFERENCES crm_contact(id)
);

CREATE INDEX idx_crm_contact_email ON crm_contact(email);
CREATE INDEX idx_crm_contact_status ON crm_contact(status, updated_at);
CREATE INDEX idx_crm_company_name ON crm_company(name);
CREATE INDEX idx_crm_deal_stage ON crm_deal(stage_id, updated_at);
CREATE INDEX idx_crm_activity_due ON crm_activity(completed_at, due_at);
CREATE INDEX idx_crm_submission_form_created ON crm_submission(form_id, created_at);
