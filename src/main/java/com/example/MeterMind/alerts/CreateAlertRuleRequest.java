package com.example.MeterMind.alerts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateAlertRuleRequest {

    private Long customerId;

    @NotNull(message = "Rule type is required (e.g. LOW_BALANCE, ZERO_BALANCE, USAGE_SPIKE)")
    private AlertRuleType ruleType;

    @NotNull(message = "Threshold value is required")
    @Min(value = 0, message = "Threshold value must be greater than or equal to 0")
    private Long thresholdValue;

    private Boolean enabled;

    public CreateAlertRuleRequest() {
    }

    public CreateAlertRuleRequest(Long customerId, AlertRuleType ruleType, Long thresholdValue) {
        this.customerId = customerId;
        this.ruleType = ruleType;
        this.thresholdValue = thresholdValue;
        this.enabled = true;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AlertRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(AlertRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Long getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Long thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
