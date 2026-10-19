package com.example.MeterMind.wallet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DebitCreditsRequest {

    @Min(value = 1, message = "Debit amount must be at least 1 credit")
    private long amount;

    @Size(max = 100, message = "Reference type must not exceed 100 characters")
    private String referenceType;

    @Size(max = 255, message = "Reference ID must not exceed 255 characters")
    private String referenceId;

    @NotBlank(message = "Debit reason is required for auditability")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public DebitCreditsRequest() {
    }

    public DebitCreditsRequest(long amount, String referenceType, String referenceId, String reason) {
        this.amount = amount;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
