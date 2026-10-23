CREATE TABLE usage_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    feature_code VARCHAR(100) NOT NULL,
    units BIGINT NOT NULL,
    unit_credit_cost BIGINT NOT NULL,
    credits_charged BIGINT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usage_customer_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT fk_usage_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_usage_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_usage_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE RESTRICT,
    INDEX idx_usage_customer_created (customer_id, created_at),
    INDEX idx_usage_org_created (organization_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
