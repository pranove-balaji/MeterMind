package com.example.MeterMind.ui;

import com.example.MeterMind.usage.UsageEvent;
import com.example.MeterMind.wallet.CreditLedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CustomerDashboardViewModel {

    private String displayName;
    private String email;
    private String organizationName;
    private long currentBalance;
    private String planName;
    private BigDecimal planPrice;
    private Long planMonthlyCredits;
    private Instant periodEnd;
    private Map<String, Long> featureUsageCredits;
    private List<UsageEvent> recentUsageEvents;
    private List<CreditLedgerEntry> recentLedgerEntries;

    public CustomerDashboardViewModel() {
    }

    public CustomerDashboardViewModel(String displayName, String email, String organizationName,
                                      long currentBalance, String planName, BigDecimal planPrice,
                                      Long planMonthlyCredits, Instant periodEnd,
                                      Map<String, Long> featureUsageCredits,
                                      List<UsageEvent> recentUsageEvents,
                                      List<CreditLedgerEntry> recentLedgerEntries) {
        this.displayName = displayName;
        this.email = email;
        this.organizationName = organizationName;
        this.currentBalance = currentBalance;
        this.planName = planName;
        this.planPrice = planPrice;
        this.planMonthlyCredits = planMonthlyCredits;
        this.periodEnd = periodEnd;
        this.featureUsageCredits = featureUsageCredits;
        this.recentUsageEvents = recentUsageEvents;
        this.recentLedgerEntries = recentLedgerEntries;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public long getCurrentBalance() {
        return currentBalance;
    }

    public String getPlanName() {
        return planName;
    }

    public BigDecimal getPlanPrice() {
        return planPrice;
    }

    public Long getPlanMonthlyCredits() {
        return planMonthlyCredits;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public Map<String, Long> getFeatureUsageCredits() {
        return featureUsageCredits;
    }

    public List<UsageEvent> getRecentUsageEvents() {
        return recentUsageEvents;
    }

    public List<CreditLedgerEntry> getRecentLedgerEntries() {
        return recentLedgerEntries;
    }
}
