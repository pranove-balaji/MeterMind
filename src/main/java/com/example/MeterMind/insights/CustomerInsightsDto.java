package com.example.MeterMind.insights;

import java.time.Instant;
import java.util.List;

public class CustomerInsightsDto {

    private Long customerId;
    private String customerName;
    private RunwayForecastDto forecast;
    private List<RecommendationDto> recommendations;
    private List<FeatureConsumptionDto> topFeatures;
    private Instant generatedAt;

    public CustomerInsightsDto() {
    }

    public CustomerInsightsDto(Long customerId,
                               String customerName,
                               RunwayForecastDto forecast,
                               List<RecommendationDto> recommendations,
                               List<FeatureConsumptionDto> topFeatures) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.forecast = forecast;
        this.recommendations = recommendations;
        this.topFeatures = topFeatures;
        this.generatedAt = Instant.now();
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public RunwayForecastDto getForecast() {
        return forecast;
    }

    public void setForecast(RunwayForecastDto forecast) {
        this.forecast = forecast;
    }

    public List<RecommendationDto> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationDto> recommendations) {
        this.recommendations = recommendations;
    }

    public List<FeatureConsumptionDto> getTopFeatures() {
        return topFeatures;
    }

    public void setTopFeatures(List<FeatureConsumptionDto> topFeatures) {
        this.topFeatures = topFeatures;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
