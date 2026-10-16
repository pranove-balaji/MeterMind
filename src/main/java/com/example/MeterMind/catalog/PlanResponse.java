package com.example.MeterMind.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class PlanResponse {

    private Long id;
    private Long organizationId;
    private String name;
    private BigDecimal monthlyPrice;
    private long monthlyCredits;
    private boolean active;
    private List<FeaturePriceResponse> featurePrices;
    private Instant createdAt;

    public PlanResponse() {
    }

    public PlanResponse(Plan plan) {
        this.id = plan.getId();
        this.organizationId = plan.getOrganization().getId();
        this.name = plan.getName();
        this.monthlyPrice = plan.getMonthlyPrice();
        this.monthlyCredits = plan.getMonthlyCredits();
        this.active = plan.isActive();
        this.createdAt = plan.getCreatedAt();
        if (plan.getFeaturePrices() != null) {
            this.featurePrices = plan.getFeaturePrices().stream()
                    .map(FeaturePriceResponse::new)
                    .collect(Collectors.toList());
        }
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public long getMonthlyCredits() {
        return monthlyCredits;
    }

    public boolean isActive() {
        return active;
    }

    public List<FeaturePriceResponse> getFeaturePrices() {
        return featurePrices;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
