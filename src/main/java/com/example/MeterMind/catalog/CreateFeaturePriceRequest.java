package com.example.MeterMind.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateFeaturePriceRequest {

    @NotBlank(message = "Feature code is required")
    private String featureCode;

    @NotNull(message = "Credits per unit is required")
    @Min(value = 0, message = "Credits per unit cannot be negative")
    private Long creditsPerUnit;

    public CreateFeaturePriceRequest() {
    }

    public CreateFeaturePriceRequest(String featureCode, Long creditsPerUnit) {
        this.featureCode = featureCode;
        this.creditsPerUnit = creditsPerUnit;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public Long getCreditsPerUnit() {
        return creditsPerUnit;
    }

    public void setCreditsPerUnit(Long creditsPerUnit) {
        this.creditsPerUnit = creditsPerUnit;
    }
}
