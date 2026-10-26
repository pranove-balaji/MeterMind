package com.example.MeterMind.insights;

public class FeatureConsumptionDto {

    private String featureCode;
    private long totalCredits;
    private long totalUnits;
    private double percentageOfTotal;

    public FeatureConsumptionDto() {
    }

    public FeatureConsumptionDto(String featureCode, long totalCredits, long totalUnits, double percentageOfTotal) {
        this.featureCode = featureCode;
        this.totalCredits = totalCredits;
        this.totalUnits = totalUnits;
        this.percentageOfTotal = percentageOfTotal;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public long getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(long totalCredits) {
        this.totalCredits = totalCredits;
    }

    public long getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(long totalUnits) {
        this.totalUnits = totalUnits;
    }

    public double getPercentageOfTotal() {
        return percentageOfTotal;
    }

    public void setPercentageOfTotal(double percentageOfTotal) {
        this.percentageOfTotal = percentageOfTotal;
    }
}
