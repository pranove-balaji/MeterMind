package com.example.MeterMind.ui;

import com.example.MeterMind.usage.UsageEvent;

import java.util.List;
import java.util.Map;

public class OwnerDashboardViewModel {

    private String organizationName;
    private long totalCustomers;
    private long totalPlans;
    private long totalCreditsInCirculation;
    private long totalUsageEvents;
    private List<CustomerSummaryItem> customers;
    private Map<String, Long> featureUsageCredits;
    private List<UsageEvent> recentActivity;

    public OwnerDashboardViewModel() {
    }

    public OwnerDashboardViewModel(String organizationName, long totalCustomers, long totalPlans,
                                   long totalCreditsInCirculation, long totalUsageEvents,
                                   List<CustomerSummaryItem> customers, Map<String, Long> featureUsageCredits,
                                   List<UsageEvent> recentActivity) {
        this.organizationName = organizationName;
        this.totalCustomers = totalCustomers;
        this.totalPlans = totalPlans;
        this.totalCreditsInCirculation = totalCreditsInCirculation;
        this.totalUsageEvents = totalUsageEvents;
        this.customers = customers;
        this.featureUsageCredits = featureUsageCredits;
        this.recentActivity = recentActivity;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public long getTotalPlans() {
        return totalPlans;
    }

    public long getTotalCreditsInCirculation() {
        return totalCreditsInCirculation;
    }

    public long getTotalUsageEvents() {
        return totalUsageEvents;
    }

    public List<CustomerSummaryItem> getCustomers() {
        return customers;
    }

    public Map<String, Long> getFeatureUsageCredits() {
        return featureUsageCredits;
    }

    public List<UsageEvent> getRecentActivity() {
        return recentActivity;
    }

    public static class CustomerSummaryItem {
        private Long id;
        private String displayName;
        private String email;
        private String planName;
        private long walletBalance;

        public CustomerSummaryItem(Long id, String displayName, String email, String planName, long walletBalance) {
            this.id = id;
            this.displayName = displayName;
            this.email = email;
            this.planName = planName;
            this.walletBalance = walletBalance;
        }

        public Long getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmail() {
            return email;
        }

        public String getPlanName() {
            return planName;
        }

        public long getWalletBalance() {
            return walletBalance;
        }
    }
}
