package com.example.MeterMind.alerts;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.organization.Organization;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "alert_rules", indexes = {
        @Index(name = "idx_alert_rules_org", columnList = "organization_id, enabled")
})
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private AlertRuleType ruleType;

    @Column(name = "threshold_value", nullable = false)
    private long thresholdValue;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AlertRule() {
    }

    public AlertRule(Organization organization, Customer customer, AlertRuleType ruleType, long thresholdValue) {
        this.organization = organization;
        this.customer = customer;
        this.ruleType = ruleType;
        this.thresholdValue = thresholdValue;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Customer getCustomer() {
        return customer;
    }

    public AlertRuleType getRuleType() {
        return ruleType;
    }

    public long getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(long thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
