CREATE TABLE detection_results
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id  BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL,
    score       INT         NOT NULL,
    reason_codes JSON,
    detected_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_detection_results_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
