CREATE TABLE translation_settings (
    id VARCHAR(30) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    provider VARCHAR(40) NOT NULL DEFAULT 'GOOGLE_ELEMENT',
    source_language VARCHAR(10) NOT NULL DEFAULT 'en',
    enabled_languages VARCHAR(1000) NOT NULL DEFAULT 'en,es,fr,de,it,pt,nl',
    placement VARCHAR(40) NOT NULL DEFAULT 'TOPBAR',
    display_style VARCHAR(40) NOT NULL DEFAULT 'COMPACT',
    show_flags BOOLEAN NOT NULL DEFAULT TRUE,
    show_native_names BOOLEAN NOT NULL DEFAULT TRUE,
    remember_selection BOOLEAN NOT NULL DEFAULT TRUE,
    button_label VARCHAR(80) NOT NULL DEFAULT 'Language',
    floating_offset_x INT NOT NULL DEFAULT 24,
    floating_offset_y INT NOT NULL DEFAULT 88,
    excluded_paths VARCHAR(2000) NOT NULL DEFAULT '/admin,/api',
    updated_at TIMESTAMP NOT NULL
);
