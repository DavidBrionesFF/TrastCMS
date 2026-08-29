ALTER TABLE academy_course ADD COLUMN demo_data BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_academy_course_demo ON academy_course(demo_data, created_at);
