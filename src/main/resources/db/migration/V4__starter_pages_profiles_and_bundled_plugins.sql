ALTER TABLE cms_user ADD COLUMN first_name VARCHAR(80);
ALTER TABLE cms_user ADD COLUMN last_name VARCHAR(80);
ALTER TABLE cms_user ADD COLUMN phone VARCHAR(60);
ALTER TABLE cms_user ADD COLUMN avatar_url VARCHAR(600);
ALTER TABLE cms_user ADD COLUMN bio VARCHAR(1000);
ALTER TABLE cms_user ADD COLUMN locale VARCHAR(20) NOT NULL DEFAULT 'es-HN';
ALTER TABLE cms_user ADD COLUMN timezone VARCHAR(80) NOT NULL DEFAULT 'America/Tegucigalpa';
ALTER TABLE cms_user ADD COLUMN last_login_at TIMESTAMP;

ALTER TABLE post ADD COLUMN show_in_menu BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE post ADD COLUMN menu_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE post ADD COLUMN page_role VARCHAR(40);
ALTER TABLE post ADD COLUMN theme_origin VARCHAR(64);

CREATE TABLE bundled_plugin_state (
    plugin_id VARCHAR(120) PRIMARY KEY,
    installed_version VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_post_public_menu ON post(content_type, status, show_in_menu, menu_order);
