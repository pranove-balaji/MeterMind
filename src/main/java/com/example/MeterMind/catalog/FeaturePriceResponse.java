package com.example.MeterMind.catalog;

import java.time.Instant;

public class FeaturePriceResponse {

    private Long id;
    private Long planId;
    private String featureCode;
    private long creditsPerUnit;
    private boolean active;
    private Instant createdAt;

    public FeaturePriceResponse() {
    }

    public FeaturePriceResponse(FeaturePrice featurePrice) {
        this.id = featurePrice.getId();
        this.planId = featurePrice.getPlan().getId();
        this.featureCode = featurePrice.getFeatureCode();
        this.creditsPerUnit = featurePrice.getCreditsPerUnit();
        this.active = featurePrice.isActive();
        this.createdAt = featurePrice.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public long getCreditsPerUnit() {
        return creditsPerUnit;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
