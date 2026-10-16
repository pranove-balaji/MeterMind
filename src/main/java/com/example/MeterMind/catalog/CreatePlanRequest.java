package com.example.MeterMind.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreatePlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "0.00", message = "Monthly price cannot be negative")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Monthly credits are required")
    @Min(value = 0, message = "Monthly credits cannot be negative")
    private Long monthlyCredits;

    public CreatePlanRequest() {
    }

    public CreatePlanRequest(String name, BigDecimal monthlyPrice, Long monthlyCredits) {
        this.name = name;
        this.monthlyPrice = monthlyPrice;
        this.monthlyCredits = monthlyCredits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public Long getMonthlyCredits() {
        return monthlyCredits;
    }

    public void setMonthlyCredits(Long monthlyCredits) {
        this.monthlyCredits = monthlyCredits;
    }
}
