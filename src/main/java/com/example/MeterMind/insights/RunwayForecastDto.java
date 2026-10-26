package com.example.MeterMind.insights;

import java.math.BigDecimal;
import java.time.Instant;

public class RunwayForecastDto {

    private ForecastStatus status;
    private Long availableCredits;
    private BigDecimal averageDailyBurn;
    private Long daysRemaining;
    private Instant projectedDepletionDate;
    private int windowDays;
    private String measurementWindow;
    private String assumption;

    public RunwayForecastDto() {
    }

    public RunwayForecastDto(ForecastStatus status,
                             Long availableCredits,
                             BigDecimal averageDailyBurn,
                             Long daysRemaining,
                             Instant projectedDepletionDate,
                             int windowDays,
                             String measurementWindow,
                             String assumption) {
        this.status = status;
        this.availableCredits = availableCredits;
        this.averageDailyBurn = averageDailyBurn;
        this.daysRemaining = daysRemaining;
        this.projectedDepletionDate = projectedDepletionDate;
        this.windowDays = windowDays;
        this.measurementWindow = measurementWindow;
        this.assumption = assumption;
    }

    public ForecastStatus getStatus() {
        return status;
    }

    public void setStatus(ForecastStatus status) {
        this.status = status;
    }

    public Long getAvailableCredits() {
        return availableCredits;
    }

    public void setAvailableCredits(Long availableCredits) {
        this.availableCredits = availableCredits;
    }

    public BigDecimal getAverageDailyBurn() {
        return averageDailyBurn;
    }

    public void setAverageDailyBurn(BigDecimal averageDailyBurn) {
        this.averageDailyBurn = averageDailyBurn;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public Instant getProjectedDepletionDate() {
        return projectedDepletionDate;
    }

    public void setProjectedDepletionDate(Instant projectedDepletionDate) {
        this.projectedDepletionDate = projectedDepletionDate;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public String getMeasurementWindow() {
        return measurementWindow;
    }

    public void setMeasurementWindow(String measurementWindow) {
        this.measurementWindow = measurementWindow;
    }

    public String getAssumption() {
        return assumption;
    }

    public void setAssumption(String assumption) {
        this.assumption = assumption;
    }
}
