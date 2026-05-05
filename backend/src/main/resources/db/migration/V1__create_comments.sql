CREATE TABLE comments
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_comment_id VARCHAR(255) NOT NULL,
    username            VARCHAR(255) NOT NULL,
    original_text       TEXT         NOT NULL,
    normalized_text     TEXT,
    received_at         DATETIME(6)  NOT NULL,
    UNIQUE KEY uq_comments_external_id (external_comment_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
