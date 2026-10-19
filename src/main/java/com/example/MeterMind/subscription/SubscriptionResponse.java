package com.example.MeterMind.subscription;

import java.time.Instant;

public class SubscriptionResponse {

    private Long id;
    private Long organizationId;
    private Long customerId;
    private Long planId;
    private String planName;
    private SubscriptionStatus status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant createdAt;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(Subscription subscription) {
        this.id = subscription.getId();
        this.organizationId = subscription.getOrganization().getId();
        this.customerId = subscription.getCustomer().getId();
        this.planId = subscription.getPlan().getId();
        this.planName = subscription.getPlan().getName();
        this.status = subscription.getStatus();
        this.currentPeriodStart = subscription.getCurrentPeriodStart();
        this.currentPeriodEnd = subscription.getCurrentPeriodEnd();
        this.createdAt = subscription.getCreatedAt();
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

    public Long getPlanId() {
        return planId;
    }

    public String getPlanName() {
        return planName;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
