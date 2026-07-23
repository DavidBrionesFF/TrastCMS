ALTER TABLE post ADD COLUMN editor_mode VARCHAR(30) NOT NULL DEFAULT 'RICH_TEXT';
ALTER TABLE post ADD COLUMN builder_data TEXT;
ALTER TABLE post ADD COLUMN custom_css TEXT;
ALTER TABLE post ADD COLUMN seo_title VARCHAR(250);
ALTER TABLE post ADD COLUMN seo_description VARCHAR(500);
ALTER TABLE post ADD COLUMN page_template VARCHAR(120) NOT NULL DEFAULT 'default';

ALTER TABLE post_revision ADD COLUMN editor_mode VARCHAR(30) NOT NULL DEFAULT 'RICH_TEXT';
ALTER TABLE post_revision ADD COLUMN builder_data TEXT;
ALTER TABLE post_revision ADD COLUMN custom_css TEXT;

ALTER TABLE media_asset ADD COLUMN title VARCHAR(255);
ALTER TABLE media_asset ADD COLUMN alt_text VARCHAR(500);
ALTER TABLE media_asset ADD COLUMN caption VARCHAR(1000);
ALTER TABLE media_asset ADD COLUMN description TEXT;
ALTER TABLE media_asset ADD COLUMN kind VARCHAR(30) NOT NULL DEFAULT 'OTHER';
ALTER TABLE media_asset ADD COLUMN folder VARCHAR(160) NOT NULL DEFAULT 'General';
ALTER TABLE media_asset ADD COLUMN width INTEGER;
ALTER TABLE media_asset ADD COLUMN height INTEGER;
ALTER TABLE media_asset ADD COLUMN duration_seconds DOUBLE PRECISION;
ALTER TABLE media_asset ADD COLUMN updated_at TIMESTAMP;

UPDATE media_asset
SET title = original_filename,
    kind = CASE
        WHEN content_type LIKE 'image/%' THEN 'IMAGE'
        WHEN content_type LIKE 'video/%' THEN 'VIDEO'
        WHEN content_type LIKE 'audio/%' THEN 'AUDIO'
        WHEN content_type = 'application/pdf' THEN 'DOCUMENT'
        ELSE 'OTHER'
    END,
    updated_at = created_at
WHERE updated_at IS NULL;

CREATE INDEX idx_media_kind_created ON media_asset(kind, created_at);
CREATE INDEX idx_post_content_type_updated ON post(content_type, updated_at);
