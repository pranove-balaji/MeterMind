package com.example.MeterMind.usage;

import java.time.Instant;

public class UsageEventResponse {

    private Long id;
    private Long customerId;
    private Long subscriptionId;
    private String featureCode;
    private long units;
    private long unitCreditCost;
    private long creditsCharged;
    private long balanceRemaining;
    private String idempotencyKey;
    private Instant createdAt;
    private boolean replayed;

    public UsageEventResponse() {
    }

    public UsageEventResponse(UsageEvent event, long balanceRemaining, boolean replayed) {
        this.id = event.getId();
        this.customerId = event.getCustomer().getId();
        this.subscriptionId = event.getSubscription().getId();
        this.featureCode = event.getFeatureCode();
        this.units = event.getUnits();
        this.unitCreditCost = event.getUnitCreditCost();
        this.creditsCharged = event.getCreditsCharged();
        this.balanceRemaining = balanceRemaining;
        this.idempotencyKey = event.getIdempotencyKey();
        this.createdAt = event.getCreatedAt();
        this.replayed = replayed;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public long getUnits() {
        return units;
    }

    public long getUnitCreditCost() {
        return unitCreditCost;
    }

    public long getCreditsCharged() {
        return creditsCharged;
    }

    public long getBalanceRemaining() {
        return balanceRemaining;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isReplayed() {
        return replayed;
    }
}
