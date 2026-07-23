ALTER TABLE plugin_registration ADD COLUMN description VARCHAR(1000);
ALTER TABLE plugin_registration ADD COLUMN author VARCHAR(160);
ALTER TABLE plugin_registration ADD COLUMN homepage VARCHAR(600);
ALTER TABLE plugin_registration ADD COLUMN permissions TEXT;
ALTER TABLE plugin_registration ADD COLUMN health_check_path VARCHAR(240) NOT NULL DEFAULT '/health';
ALTER TABLE plugin_registration ADD COLUMN last_test_status VARCHAR(30) NOT NULL DEFAULT 'NEVER';
ALTER TABLE plugin_registration ADD COLUMN last_test_message VARCHAR(1000);
ALTER TABLE plugin_registration ADD COLUMN last_test_at TIMESTAMP;
