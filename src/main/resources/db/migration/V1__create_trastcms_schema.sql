CREATE TABLE cms_user (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE category (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    slug VARCHAR(180) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE post (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(250) NOT NULL,
    slug VARCHAR(280) NOT NULL UNIQUE,
    excerpt VARCHAR(600),
    body TEXT NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    featured_image_url VARCHAR(500),
    category_id VARCHAR(36),
    author_id VARCHAR(36) NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES cms_user(id)
);

CREATE TABLE post_revision (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    revision_number BIGINT NOT NULL,
    title VARCHAR(250) NOT NULL,
    excerpt VARCHAR(600),
    body TEXT NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(190) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_revision_post_number UNIQUE (post_id, revision_number),
    CONSTRAINT fk_revision_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE TABLE media_asset (
    id VARCHAR(36) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(600) NOT NULL,
    public_url VARCHAR(600) NOT NULL,
    uploaded_by VARCHAR(190) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE site_setting (
    setting_key VARCHAR(190) PRIMARY KEY,
    setting_value TEXT,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE plugin_registration (
    id VARCHAR(36) PRIMARY KEY,
    plugin_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    version VARCHAR(50) NOT NULL,
    base_url VARCHAR(600) NOT NULL,
    encrypted_secret VARCHAR(1000) NOT NULL,
    subscriptions TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE audit_log (
    id VARCHAR(36) PRIMARY KEY,
    actor VARCHAR(190) NOT NULL,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_post_status_published ON post(status, content_type, published_at);
CREATE INDEX idx_post_updated ON post(updated_at);
CREATE INDEX idx_revision_post ON post_revision(post_id, revision_number);
CREATE INDEX idx_media_created ON media_asset(created_at);
CREATE INDEX idx_audit_created ON audit_log(created_at);
