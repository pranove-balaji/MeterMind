CREATE TABLE alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    customer_id BIGINT NULL,
    rule_type VARCHAR(50) NOT NULL,
    threshold_value BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rules_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rules_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    INDEX idx_alert_rules_org (organization_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    rule_id BIGINT NULL,
    event_key VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_rule FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE SET NULL,
    CONSTRAINT uk_notifications_customer_event UNIQUE (customer_id, event_key),
    INDEX idx_notifications_customer_created (customer_id, created_at),
    INDEX idx_notifications_org_created (organization_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
