package com.example.MeterMind.ui;

import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.catalog.PlanRepository;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.identity.CustomUserDetails;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.usage.UsageEvent;
import com.example.MeterMind.usage.UsageEventRepository;
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/owner/dashboard")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerDashboardController {

    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CreditWalletRepository walletRepository;
    private final UsageEventRepository usageEventRepository;

    public OwnerDashboardController(OrganizationRepository organizationRepository,
                                    CustomerRepository customerRepository,
                                    PlanRepository planRepository,
                                    SubscriptionRepository subscriptionRepository,
                                    CreditWalletRepository walletRepository,
                                    UsageEventRepository usageEventRepository) {
        this.organizationRepository = organizationRepository;
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.walletRepository = walletRepository;
        this.usageEventRepository = usageEventRepository;
    }

    @GetMapping
    public String showDashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long orgId = user.getOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

        List<Customer> customerList = customerRepository.findByOrganizationId(orgId);
        List<Plan> planList = planRepository.findByOrganizationId(orgId);
        Long totalCirculation = walletRepository.sumBalanceByOrganizationId(orgId);
        long totalEvents = usageEventRepository.countByOrganizationId(orgId);

        List<OwnerDashboardViewModel.CustomerSummaryItem> customerSummaries = new ArrayList<>();
        for (Customer c : customerList) {
            String planName = subscriptionRepository.findByCustomerIdAndStatus(c.getId(), SubscriptionStatus.ACTIVE)
                    .map(sub -> sub.getPlan().getName())
                    .orElse("No Active Plan");

            long balance = walletRepository.findByCustomerId(c.getId())
                    .map(CreditWallet::getBalance)
                    .orElse(0L);

            customerSummaries.add(new OwnerDashboardViewModel.CustomerSummaryItem(
                    c.getId(),
                    c.getDisplayName(),
                    c.getEmail(),
                    planName,
                    balance
            ));
        }

        List<UsageEvent> allOrgEvents = usageEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        Map<String, Long> featureUsageCredits = allOrgEvents.stream()
                .collect(Collectors.groupingBy(
                        UsageEvent::getFeatureCode,
                        TreeMap::new,
                        Collectors.summingLong(UsageEvent::getCreditsCharged)
                ));

        List<UsageEvent> recentActivity = usageEventRepository.findTop10ByOrganizationIdOrderByCreatedAtDesc(orgId);

        OwnerDashboardViewModel vm = new OwnerDashboardViewModel(
                org.getName(),
                customerList.size(),
                planList.size(),
                totalCirculation != null ? totalCirculation : 0L,
                totalEvents,
                customerSummaries,
                featureUsageCredits,
                recentActivity
        );

        model.addAttribute("vm", vm);
        model.addAttribute("currentUser", user);
        return "owner/dashboard";
    }
}
