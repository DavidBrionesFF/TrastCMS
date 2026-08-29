ALTER TABLE academy_course ADD COLUMN category VARCHAR(120) DEFAULT 'General' NOT NULL;
CREATE INDEX idx_academy_course_category ON academy_course(category);
