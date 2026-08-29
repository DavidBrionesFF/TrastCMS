CREATE TABLE academy_settings (
    id VARCHAR(36) PRIMARY KEY,
    catalog_title VARCHAR(220) NOT NULL,
    catalog_description VARCHAR(1000),
    default_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    default_import_url VARCHAR(1000),
    allow_free_enrollment BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE academy_course (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(240) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    excerpt VARCHAR(1000),
    description TEXT,
    featured_image_url VARCHAR(1000),
    instructor_name VARCHAR(180),
    level VARCHAR(30) NOT NULL DEFAULT 'BEGINNER',
    access_type VARCHAR(30) NOT NULL DEFAULT 'FREE',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    price DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    estimated_minutes INTEGER NOT NULL DEFAULT 0,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    external_source_id VARCHAR(190),
    external_source_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE academy_section (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(240) NOT NULL,
    description VARCHAR(1000),
    position INTEGER NOT NULL DEFAULT 0,
    external_source_id VARCHAR(190),
    CONSTRAINT fk_academy_section_course FOREIGN KEY (course_id) REFERENCES academy_course(id) ON DELETE CASCADE
);

CREATE TABLE academy_lesson (
    id VARCHAR(36) PRIMARY KEY,
    section_id VARCHAR(36) NOT NULL,
    title VARCHAR(240) NOT NULL,
    content TEXT,
    position INTEGER NOT NULL DEFAULT 0,
    lesson_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    video_provider VARCHAR(30),
    video_url VARCHAR(1200),
    media_id VARCHAR(36),
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    preview BOOLEAN NOT NULL DEFAULT FALSE,
    external_source_id VARCHAR(190),
    CONSTRAINT fk_academy_lesson_section FOREIGN KEY (section_id) REFERENCES academy_section(id) ON DELETE CASCADE
);

CREATE TABLE academy_quiz (
    id VARCHAR(36) PRIMARY KEY,
    section_id VARCHAR(36) NOT NULL,
    title VARCHAR(240) NOT NULL,
    description TEXT,
    position INTEGER NOT NULL DEFAULT 0,
    passing_score DECIMAL(7,2) NOT NULL DEFAULT 70,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    duration_minutes INTEGER NOT NULL DEFAULT 0,
    randomize_questions BOOLEAN NOT NULL DEFAULT FALSE,
    preview BOOLEAN NOT NULL DEFAULT FALSE,
    external_source_id VARCHAR(190),
    CONSTRAINT fk_academy_quiz_section FOREIGN KEY (section_id) REFERENCES academy_section(id) ON DELETE CASCADE
);

CREATE TABLE academy_question (
    id VARCHAR(36) PRIMARY KEY,
    quiz_id VARCHAR(36) NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(40) NOT NULL DEFAULT 'SINGLE_CHOICE',
    position INTEGER NOT NULL DEFAULT 0,
    points DECIMAL(10,2) NOT NULL DEFAULT 1,
    explanation TEXT,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_academy_question_quiz FOREIGN KEY (quiz_id) REFERENCES academy_quiz(id) ON DELETE CASCADE
);

CREATE TABLE academy_answer_option (
    id VARCHAR(36) PRIMARY KEY,
    question_id VARCHAR(36) NOT NULL,
    option_text TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_academy_option_question FOREIGN KEY (question_id) REFERENCES academy_question(id) ON DELETE CASCADE
);

CREATE TABLE academy_enrollment (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36),
    email VARCHAR(190) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(30) NOT NULL DEFAULT 'FREE',
    order_id VARCHAR(36),
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_academy_enrollment_course FOREIGN KEY (course_id) REFERENCES academy_course(id),
    CONSTRAINT fk_academy_enrollment_user FOREIGN KEY (user_id) REFERENCES cms_user(id) ON DELETE SET NULL,
    CONSTRAINT uq_academy_enrollment UNIQUE (course_id, email)
);

CREATE TABLE academy_lesson_progress (
    id VARCHAR(36) PRIMARY KEY,
    enrollment_id VARCHAR(36) NOT NULL,
    lesson_id VARCHAR(36) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    watched_seconds INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_academy_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES academy_enrollment(id) ON DELETE CASCADE,
    CONSTRAINT fk_academy_progress_lesson FOREIGN KEY (lesson_id) REFERENCES academy_lesson(id),
    CONSTRAINT uq_academy_progress UNIQUE (enrollment_id, lesson_id)
);

CREATE TABLE academy_quiz_attempt (
    id VARCHAR(36) PRIMARY KEY,
    enrollment_id VARCHAR(36) NOT NULL,
    quiz_id VARCHAR(36) NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    score DECIMAL(7,2),
    passed BOOLEAN,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    CONSTRAINT fk_academy_attempt_enrollment FOREIGN KEY (enrollment_id) REFERENCES academy_enrollment(id) ON DELETE CASCADE,
    CONSTRAINT fk_academy_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES academy_quiz(id),
    CONSTRAINT uq_academy_attempt UNIQUE (enrollment_id, quiz_id, attempt_number)
);

CREATE TABLE academy_question_response (
    id VARCHAR(36) PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    selected_option_ids TEXT,
    text_answer TEXT,
    correct BOOLEAN,
    awarded_points DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_academy_response_attempt FOREIGN KEY (attempt_id) REFERENCES academy_quiz_attempt(id) ON DELETE CASCADE,
    CONSTRAINT fk_academy_response_question FOREIGN KEY (question_id) REFERENCES academy_question(id),
    CONSTRAINT uq_academy_response UNIQUE (attempt_id, question_id)
);

CREATE TABLE academy_api_token (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    email VARCHAR(190) NOT NULL,
    access_token_hash VARCHAR(64) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(64) NOT NULL UNIQUE,
    device_name VARCHAR(180),
    expires_at TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_academy_token_user FOREIGN KEY (user_id) REFERENCES cms_user(id) ON DELETE CASCADE
);

CREATE TABLE academy_import_job (
    id VARCHAR(36) PRIMARY KEY,
    source_url VARCHAR(1200) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    detected_title VARCHAR(240),
    result_course_id VARCHAR(36),
    warnings_json TEXT,
    error_message TEXT,
    requested_by VARCHAR(190) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    CONSTRAINT fk_academy_import_course FOREIGN KEY (result_course_id) REFERENCES academy_course(id) ON DELETE SET NULL
);

CREATE INDEX idx_academy_course_status ON academy_course(status, featured, updated_at);
CREATE INDEX idx_academy_section_course ON academy_section(course_id, position);
CREATE INDEX idx_academy_lesson_section ON academy_lesson(section_id, position);
CREATE INDEX idx_academy_quiz_section ON academy_quiz(section_id, position);
CREATE INDEX idx_academy_question_quiz ON academy_question(quiz_id, position);
CREATE INDEX idx_academy_enrollment_email ON academy_enrollment(email, status);
CREATE INDEX idx_academy_progress_enrollment ON academy_lesson_progress(enrollment_id, completed);
CREATE INDEX idx_academy_attempt_enrollment ON academy_quiz_attempt(enrollment_id, quiz_id);
CREATE INDEX idx_academy_token_access ON academy_api_token(access_token_hash, revoked_at, expires_at);
CREATE INDEX idx_academy_import_status ON academy_import_job(status, started_at);
