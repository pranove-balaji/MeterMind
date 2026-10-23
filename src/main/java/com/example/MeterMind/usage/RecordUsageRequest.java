package com.example.MeterMind.usage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RecordUsageRequest {

    @NotBlank(message = "Feature code is required")
    @Size(max = 100, message = "Feature code must not exceed 100 characters")
    private String featureCode;

    @NotNull(message = "Units are required")
    @Min(value = 1, message = "Usage units must be strictly positive (at least 1)")
    private Long units;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    private String idempotencyKey;

    public RecordUsageRequest() {
    }

    public RecordUsageRequest(String featureCode, Long units, String idempotencyKey) {
        this.featureCode = featureCode;
        this.units = units;
        this.idempotencyKey = idempotencyKey;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public Long getUnits() {
        return units;
    }

    public void setUnits(Long units) {
        this.units = units;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
