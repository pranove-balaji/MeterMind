package com.example.MeterMind.insights;

public class RecommendationDto {

    private String ruleCode;
    private String title;
    private RecommendationSeverity severity;
    private String observation;
    private String actionableAdvice;

    public RecommendationDto() {
    }

    public RecommendationDto(String ruleCode, String title, RecommendationSeverity severity, String observation, String actionableAdvice) {
        this.ruleCode = ruleCode;
        this.title = title;
        this.severity = severity;
        this.observation = observation;
        this.actionableAdvice = actionableAdvice;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public RecommendationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RecommendationSeverity severity) {
        this.severity = severity;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getActionableAdvice() {
        return actionableAdvice;
    }

    public void setActionableAdvice(String actionableAdvice) {
        this.actionableAdvice = actionableAdvice;
    }
}
