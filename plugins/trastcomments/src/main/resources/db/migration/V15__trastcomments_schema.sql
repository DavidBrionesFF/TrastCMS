CREATE TABLE comment (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    parent_id VARCHAR(36),
    author_name VARCHAR(120) NOT NULL,
    author_email VARCHAR(190) NOT NULL,
    author_url VARCHAR(500),
    author_user_id VARCHAR(36),
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ip_hash VARCHAR(128),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment(id)
);

CREATE INDEX idx_comment_post_status ON comment(post_id, status, created_at);
CREATE INDEX idx_comment_status_date ON comment(status, created_at);
