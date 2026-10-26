package com.example.MeterMind.ui;

import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.identity.CustomUserDetails;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.usage.UsageEvent;
import com.example.MeterMind.usage.UsageEventRepository;
import com.example.MeterMind.wallet.CreditLedgerEntry;
import com.example.MeterMind.wallet.CreditLedgerEntryRepository;
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer/dashboard")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerDashboardController {

    private final CustomerRepository customerRepository;
    private final CreditWalletRepository walletRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageEventRepository usageEventRepository;
    private final CreditLedgerEntryRepository ledgerRepository;
    private final com.example.MeterMind.insights.InsightsService insightsService;

    public CustomerDashboardController(CustomerRepository customerRepository,
                                       CreditWalletRepository walletRepository,
                                       SubscriptionRepository subscriptionRepository,
                                       UsageEventRepository usageEventRepository,
                                       CreditLedgerEntryRepository ledgerRepository,
                                       com.example.MeterMind.insights.InsightsService insightsService) {
        this.customerRepository = customerRepository;
        this.walletRepository = walletRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageEventRepository = usageEventRepository;
        this.ledgerRepository = ledgerRepository;
        this.insightsService = insightsService;
    }

    @GetMapping
    public String showDashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }

        Long customerId = user.getCustomerId();
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, user.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found: " + customerId));

        CreditWallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> new CreditWallet(customer, 0L));

        Subscription sub = subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        List<UsageEvent> allEvents = usageEventRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        Map<String, Long> featureUsageCredits = allEvents.stream()
                .collect(Collectors.groupingBy(
                        UsageEvent::getFeatureCode,
                        TreeMap::new,
                        Collectors.summingLong(UsageEvent::getCreditsCharged)
                ));

        List<UsageEvent> recentUsage = usageEventRepository.findTop10ByCustomerIdOrderByCreatedAtDesc(customerId);
        List<CreditLedgerEntry> recentLedger = wallet.getId() != null
                ? ledgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream().limit(10).toList()
                : List.of();

        com.example.MeterMind.insights.CustomerInsightsDto insights = insightsService.getCustomerInsights(
                user.getOrganizationId(),
                customerId,
                30
        );

        CustomerDashboardViewModel vm = new CustomerDashboardViewModel(
                customer.getDisplayName(),
                customer.getEmail(),
                customer.getOrganization().getName(),
                wallet.getBalance(),
                sub != null ? sub.getPlan().getName() : "None",
                sub != null ? sub.getPlan().getMonthlyPrice() : null,
                sub != null ? sub.getPlan().getMonthlyCredits() : null,
                sub != null ? sub.getCurrentPeriodEnd() : null,
                featureUsageCredits,
                recentUsage,
                recentLedger,
                insights
        );

        model.addAttribute("vm", vm);
        model.addAttribute("currentUser", user);
        return "customer/dashboard";
    }
}
