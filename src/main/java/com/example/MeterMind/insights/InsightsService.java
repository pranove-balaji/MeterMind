package com.example.MeterMind.insights;

import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.usage.UsageEvent;
import com.example.MeterMind.usage.UsageEventRepository;
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InsightsService {

    private final CustomerRepository customerRepository;
    private final CreditWalletRepository walletRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageEventRepository usageEventRepository;

    public InsightsService(CustomerRepository customerRepository,
                           CreditWalletRepository walletRepository,
                           SubscriptionRepository subscriptionRepository,
                           UsageEventRepository usageEventRepository) {
        this.customerRepository = customerRepository;
        this.walletRepository = walletRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageEventRepository = usageEventRepository;
    }

    @Transactional(readOnly = true)
    public CustomerInsightsDto getCustomerInsights(Long organizationId, Long customerId, int windowDays) {
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        int effectiveWindowDays = (windowDays <= 0) ? 30 : Math.min(365, windowDays);
        Instant windowStart = Instant.now().minus(effectiveWindowDays, ChronoUnit.DAYS);

        CreditWallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> new CreditWallet(customer, 0L));

        Subscription subscription = subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        List<UsageEvent> windowEvents = usageEventRepository.findByCustomerIdAndCreatedAtAfterOrderByCreatedAtDesc(customerId, windowStart);

        RunwayForecastDto forecast = computeRunwayForecast(wallet.getBalance(), windowEvents, effectiveWindowDays);
        List<FeatureConsumptionDto> topFeatures = computeFeatureBreakdown(windowEvents);
        List<RecommendationDto> recommendations = evaluateRecommendations(wallet.getBalance(), forecast, topFeatures, subscription, effectiveWindowDays);

        return new CustomerInsightsDto(
                customer.getId(),
                customer.getDisplayName(),
                forecast,
                recommendations,
                topFeatures
        );
    }

    private RunwayForecastDto computeRunwayForecast(long balance, List<UsageEvent> events, int windowDays) {
        String measurementWindow = "Last " + windowDays + " days";

        if (balance <= 0) {
            long totalCredits = events.stream().mapToLong(UsageEvent::getCreditsCharged).sum();
            BigDecimal dailyBurn = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (!events.isEmpty()) {
                long effectiveDays = calculateEffectiveSpanDays(events, windowDays);
                dailyBurn = BigDecimal.valueOf(totalCredits).divide(BigDecimal.valueOf(effectiveDays), 2, RoundingMode.HALF_UP);
            }
            return new RunwayForecastDto(
                    ForecastStatus.DEPLETED,
                    balance,
                    dailyBurn,
                    0L,
                    Instant.now(),
                    windowDays,
                    measurementWindow,
                    "Wallet balance is 0 credits. Runout is immediate. Billable operations are blocked until more credits are added."
            );
        }

        if (events.isEmpty()) {
            return new RunwayForecastDto(
                    ForecastStatus.INSUFFICIENT_DATA,
                    balance,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    null,
                    null,
                    windowDays,
                    measurementWindow,
                    "No usage recorded in the " + windowDays + "-day measurement window. Minimum 1 usage event required to establish empirical burn rate."
            );
        }

        long totalCredits = events.stream().mapToLong(UsageEvent::getCreditsCharged).sum();
        if (totalCredits == 0) {
            return new RunwayForecastDto(
                    ForecastStatus.INDEFINITE,
                    balance,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    null,
                    null,
                    windowDays,
                    measurementWindow,
                    "Total credit consumption in the " + windowDays + "-day measurement window is 0 credits. Balance will not deplete at current zero-consumption rate."
            );
        }

        long effectiveDays = calculateEffectiveSpanDays(events, windowDays);
        BigDecimal dailyBurn = BigDecimal.valueOf(totalCredits).divide(BigDecimal.valueOf(effectiveDays), 2, RoundingMode.HALF_UP);

        if (dailyBurn.compareTo(BigDecimal.ZERO) == 0) {
            return new RunwayForecastDto(
                    ForecastStatus.INDEFINITE,
                    balance,
                    dailyBurn,
                    null,
                    null,
                    windowDays,
                    measurementWindow,
                    "Average daily burn rate is below 0.01 credits/day. Runway is effectively indefinite at current consumption."
            );
        }

        long daysRemaining = BigDecimal.valueOf(balance).divide(dailyBurn, 0, RoundingMode.FLOOR).longValue();
        Instant projectedDepletionDate = Instant.now().plus(daysRemaining, ChronoUnit.DAYS);

        String assumption = "Assumes future consumption continues at the empirical daily burn rate of "
                + dailyBurn + " credits/day observed over " + effectiveDays + " active days in the past " + windowDays + " days.";

        return new RunwayForecastDto(
                ForecastStatus.ACTIVE_FORECAST,
                balance,
                dailyBurn,
                daysRemaining,
                projectedDepletionDate,
                windowDays,
                measurementWindow,
                assumption
        );
    }

    private long calculateEffectiveSpanDays(List<UsageEvent> events, int windowDays) {
        if (events.isEmpty()) {
            return 1L;
        }
        Instant earliest = events.get(events.size() - 1).getCreatedAt();
        long daysBetween = ChronoUnit.DAYS.between(earliest, Instant.now());
        return Math.min(windowDays, Math.max(1L, daysBetween));
    }

    private List<FeatureConsumptionDto> computeFeatureBreakdown(List<UsageEvent> events) {
        long totalCredits = events.stream().mapToLong(UsageEvent::getCreditsCharged).sum();
        Map<String, List<UsageEvent>> byFeature = events.stream()
                .collect(Collectors.groupingBy(UsageEvent::getFeatureCode));

        List<FeatureConsumptionDto> list = new ArrayList<>();
        for (Map.Entry<String, List<UsageEvent>> entry : byFeature.entrySet()) {
            String featureCode = entry.getKey();
            long credits = entry.getValue().stream().mapToLong(UsageEvent::getCreditsCharged).sum();
            long units = entry.getValue().stream().mapToLong(UsageEvent::getUnits).sum();
            double pct = totalCredits > 0 ? ((double) credits / totalCredits) * 100.0 : 0.0;
            pct = Math.round(pct * 10.0) / 10.0; // round to 1 decimal
            list.add(new FeatureConsumptionDto(featureCode, credits, units, pct));
        }

        list.sort(Comparator.comparingLong(FeatureConsumptionDto::getTotalCredits).reversed());
        return list;
    }

    private List<RecommendationDto> evaluateRecommendations(long balance,
                                                            RunwayForecastDto forecast,
                                                            List<FeatureConsumptionDto> topFeatures,
                                                            Subscription subscription,
                                                            int windowDays) {
        List<RecommendationDto> recommendations = new ArrayList<>();

        // Rule 1: Zero Balance
        if (balance <= 0) {
            recommendations.add(new RecommendationDto(
                    "ZERO_BALANCE",
                    "Wallet Balance Exhausted",
                    RecommendationSeverity.CRITICAL,
                    "Current wallet balance is " + balance + " credits. All billable feature operations are currently blocked.",
                    "Grant additional credits or upgrade subscription to resume service access immediately."
            ));
        }

        // Rule 2: Low Runway (< 7 days)
        if (forecast.getStatus() == ForecastStatus.ACTIVE_FORECAST
                && forecast.getDaysRemaining() != null
                && forecast.getDaysRemaining() < 7
                && balance > 0) {
            recommendations.add(new RecommendationDto(
                    "LOW_RUNWAY",
                    "Critical Runway Warning: < 7 Days Remaining",
                    RecommendationSeverity.WARNING,
                    "At the empirical burn rate of " + forecast.getAverageDailyBurn() + " credits/day, your balance of "
                            + balance + " credits is projected to deplete in " + forecast.getDaysRemaining() + " days.",
                    "Purchase an additional credit grant or upgrade to a higher-capacity plan to avoid automated service cut-off."
            ));
        }

        // Rule 3: Feature Concentration (>= 60% of total consumption)
        if (!topFeatures.isEmpty()) {
            FeatureConsumptionDto top = topFeatures.get(0);
            if (top.getPercentageOfTotal() >= 60.0 && top.getTotalCredits() >= 10) {
                RecommendationSeverity severity = top.getPercentageOfTotal() >= 80.0
                        ? RecommendationSeverity.WARNING
                        : RecommendationSeverity.INFO;

                recommendations.add(new RecommendationDto(
                        "FEATURE_CONCENTRATION",
                        "High Feature Concentration: " + top.getFeatureCode(),
                        severity,
                        "Feature '" + top.getFeatureCode() + "' accounted for " + top.getPercentageOfTotal()
                                + "% (" + top.getTotalCredits() + " credits across " + top.getTotalUnits()
                                + " units) of total consumption in the last " + windowDays + " days.",
                        "Evaluate opportunities to cache repetitive results or optimize batch payloads for '"
                                + top.getFeatureCode() + "' to extend overall credit runway."
                ));
            }
        }

        // Rule 4: Plan Underutilization (< 10% used)
        if (subscription != null && subscription.getPlan() != null && subscription.getPlan().getMonthlyCredits() > 0) {
            long includedCredits = subscription.getPlan().getMonthlyCredits();
            long totalConsumed = topFeatures.stream().mapToLong(FeatureConsumptionDto::getTotalCredits).sum();
            double utilization = ((double) totalConsumed / includedCredits) * 100.0;
            utilization = Math.round(utilization * 10.0) / 10.0;

            long subDays = ChronoUnit.DAYS.between(subscription.getCreatedAt(), Instant.now());
            if (subDays >= 7 && utilization < 10.0) {
                recommendations.add(new RecommendationDto(
                        "LOW_UTILIZATION",
                        "Low Plan Quota Utilization (" + utilization + "%)",
                        RecommendationSeverity.INFO,
                        "Only " + totalConsumed + " of " + includedCredits + " included monthly plan credits ("
                                + utilization + "%) have been consumed over the last " + windowDays + " days.",
                        "Consider right-sizing to a lower subscription tier to optimize recurring software spend."
                ));
            }
        }

        return recommendations;
    }
}
