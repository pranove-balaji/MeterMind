package com.example.MeterMind.wallet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GrantCreditsRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1 credit")
    private Long amount;

    @NotBlank(message = "Reason is required for auditability")
    private String reason;

    private String referenceType;
    private String referenceId;

    public GrantCreditsRequest() {
    }

    public GrantCreditsRequest(Long amount, String reason) {
        this.amount = amount;
        this.reason = reason;
        this.referenceType = "MANUAL_GRANT";
    }

    public GrantCreditsRequest(Long amount, String reason, String referenceType, String referenceId) {
        this.amount = amount;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
