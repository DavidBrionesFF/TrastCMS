CREATE TABLE maint_backup_job (
    id VARCHAR(36) PRIMARY KEY,
    backup_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    file_path VARCHAR(1000),
    file_size_bytes BIGINT,
    notes TEXT,
    triggered_by VARCHAR(190),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE maint_schedule (
    id VARCHAR(36) PRIMARY KEY,
    schedule_key VARCHAR(120) NOT NULL UNIQUE,
    cron_expression VARCHAR(120),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_run_at TIMESTAMP,
    settings_json TEXT,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_maint_backup_status ON maint_backup_job(status, created_at);
