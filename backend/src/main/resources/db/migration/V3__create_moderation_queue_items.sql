CREATE TABLE moderation_queue_items
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id         BIGINT      NOT NULL,
    recommended_action VARCHAR(20),
    status             VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_mqi_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
