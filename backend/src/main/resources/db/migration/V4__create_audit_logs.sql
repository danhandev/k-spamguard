CREATE TABLE audit_logs
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type    VARCHAR(50) NOT NULL,
    target_type   VARCHAR(50),
    target_id     BIGINT,
    metadata_json JSON,
    created_at    DATETIME(6) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
