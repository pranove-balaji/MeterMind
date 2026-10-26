package com.example.MeterMind.alerts;

import java.time.Instant;

public class AlertRuleResponse {

    private Long id;
    private Long organizationId;
    private Long customerId;
    private String customerDisplayName;
    private AlertRuleType ruleType;
    private long thresholdValue;
    private boolean enabled;
    private Instant createdAt;

    public AlertRuleResponse() {
    }

    public AlertRuleResponse(AlertRule rule) {
        this.id = rule.getId();
        this.organizationId = rule.getOrganization().getId();
        if (rule.getCustomer() != null) {
            this.customerId = rule.getCustomer().getId();
            this.customerDisplayName = rule.getCustomer().getDisplayName();
        } else {
            this.customerId = null;
            this.customerDisplayName = "All Organization Customers (Org-Wide)";
        }
        this.ruleType = rule.getRuleType();
        this.thresholdValue = rule.getThresholdValue();
        this.enabled = rule.isEnabled();
        this.createdAt = rule.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public AlertRuleType getRuleType() {
        return ruleType;
    }

    public long getThresholdValue() {
        return thresholdValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
