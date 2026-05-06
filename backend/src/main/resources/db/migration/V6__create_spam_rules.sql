CREATE TABLE spam_rules
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code  VARCHAR(60)   NOT NULL UNIQUE,
    rule_type  VARCHAR(20)   NOT NULL,
    pattern    VARCHAR(500)  NOT NULL,
    threshold  DECIMAL(4, 2) NOT NULL,
    enabled    TINYINT(1)    NOT NULL DEFAULT 1,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
