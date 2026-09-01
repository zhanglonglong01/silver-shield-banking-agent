CREATE TABLE app_user (
    id CHAR(36) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username),
    CONSTRAINT chk_app_user_role CHECK (role_code IN ('CUSTOMER', 'DEMO_ADMIN')),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE sandbox_dataset_metadata (
    dataset_id VARCHAR(80) NOT NULL,
    dataset_version VARCHAR(80) NOT NULL,
    random_seed BIGINT NOT NULL,
    baseline_date DATE NOT NULL,
    dataset_digest CHAR(64) NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (dataset_id)
) ENGINE=InnoDB;
