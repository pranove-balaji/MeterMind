package com.example.MeterMind.subscription;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class CreateSubscriptionRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Current period start is required")
    private Instant currentPeriodStart;

    @NotNull(message = "Current period end is required")
    private Instant currentPeriodEnd;

    public CreateSubscriptionRequest() {
    }

    public CreateSubscriptionRequest(Long planId, Instant currentPeriodStart, Instant currentPeriodEnd) {
        this.planId = planId;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(Instant currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }
}
