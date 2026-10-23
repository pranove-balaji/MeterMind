package com.example.MeterMind.ui;

import com.example.MeterMind.catalog.FeaturePrice;
import com.example.MeterMind.catalog.FeaturePriceRepository;
import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.catalog.PlanRepository;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.identity.AppUser;
import com.example.MeterMind.identity.AppUserRepository;
import com.example.MeterMind.identity.Role;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.usage.UsageEvent;
import com.example.MeterMind.usage.UsageEventRepository;
import com.example.MeterMind.wallet.CreditLedgerEntryRepository;
import com.example.MeterMind.wallet.CreditWalletRepository;
import com.example.MeterMind.wallet.GrantCreditsRequest;
import com.example.MeterMind.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private FeaturePriceRepository featurePriceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CreditWalletRepository walletRepository;

    @Autowired
    private CreditLedgerEntryRepository ledgerRepository;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org1;
    private Organization org2;
    private AppUser owner1;
    private AppUser owner2;
    private Customer customer1;
    private Customer customer2;
    private AppUser customerUser1;
    private Plan plan1;

    @BeforeEach
    void setup() {
        usageEventRepository.deleteAll();
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        subscriptionRepository.deleteAll();
        featurePriceRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();

        owner1 = userRepository.findByEmail("owner@acme.test").orElseThrow();
        org1 = owner1.getOrganization();

        if (!userRepository.existsByEmail("owner2@competitor.test")) {
            org2 = organizationRepository.save(new Organization("Beta Tech AI"));
            owner2 = userRepository.save(new AppUser(org2, "owner2@competitor.test", passwordEncoder.encode("secret123"), Role.OWNER));
        } else {
            owner2 = userRepository.findByEmail("owner2@competitor.test").orElseThrow();
            org2 = owner2.getOrganization();
        }

        // Create plan for Org 1
        plan1 = planRepository.save(new Plan(org1, "Startup Tier", new BigDecimal("29.00"), 500L));
        featurePriceRepository.save(new FeaturePrice(org1, plan1, "AI_SUMMARY", 5L));

        // Create Customer in Org 1
        customer1 = customerRepository.save(new Customer(org1, "Acme Client One", "client1@acme.test"));
        customerUser1 = userRepository.findByEmail("customerA@acme.test").orElseGet(() ->
                userRepository.save(new AppUser(org1, "customerA@acme.test", passwordEncoder.encode("customer123"), Role.CUSTOMER))
        );
        customerUser1.setCustomerId(customer1.getId());
        userRepository.save(customerUser1);

        // Create Customer in Org 2
        customer2 = customerRepository.save(new Customer(org2, "Beta Client Two", "client2@beta.test"));

        // Subscription for Customer 1
        Instant now = Instant.now();
        subscriptionRepository.save(new Subscription(org1, customer1, plan1, SubscriptionStatus.ACTIVE, now, now.plus(30, ChronoUnit.DAYS)));

        // Grant 250 credits to Customer 1
        walletService.grantCredits(org1.getId(), customer1.getId(), owner1.getId(),
                new GrantCreditsRequest(250L, "Initial onboarding grant", "SEED", "REF-UI"));

        // Insert a usage event for Customer 1
        UsageEvent event = new UsageEvent(
                org1, customer1, subscriptionRepository.findByCustomerIdAndStatus(customer1.getId(), SubscriptionStatus.ACTIVE).orElseThrow(),
                "AI_SUMMARY", 2L, 5L, 10L, "idemp-ui-1", "hash-1"
        );
        usageEventRepository.save(event);
    }

    @Test
    @DisplayName("Owner can access /owner/dashboard: rendered view model is strictly tenant-scoped to Org 1")
    void ownerCanAccessOwnerDashboard() throws Exception {
        mockMvc.perform(get("/owner/dashboard")
                        .with(httpBasic("owner@acme.test", "owner123")))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/dashboard"))
                .andExpect(model().attributeExists("vm"))
                .andExpect(model().attribute("vm", org.hamcrest.Matchers.hasProperty("organizationName", org.hamcrest.Matchers.equalTo(org1.getName()))))
                .andExpect(model().attribute("vm", org.hamcrest.Matchers.hasProperty("totalCustomers", org.hamcrest.Matchers.equalTo(1L))))
                .andExpect(model().attribute("vm", org.hamcrest.Matchers.hasProperty("totalCreditsInCirculation", org.hamcrest.Matchers.equalTo(250L))));
    }

    @Test
    @DisplayName("Customer is forbidden from /owner/dashboard with 403 Forbidden")
    void customerForbiddenFromOwnerDashboard() throws Exception {
        mockMvc.perform(get("/owner/dashboard")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer can access /customer/dashboard: view model contains their balance, plan, and usage")
    void customerCanAccessCustomerDashboard() throws Exception {
        mockMvc.perform(get("/customer/dashboard")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/dashboard"))
                .andExpect(model().attributeExists("vm"))
                .andExpect(model().attribute("vm", org.hamcrest.Matchers.hasProperty("displayName", org.hamcrest.Matchers.equalTo("Acme Client One"))))
                .andExpect(model().attribute("vm", org.hamcrest.Matchers.hasProperty("currentBalance", org.hamcrest.Matchers.equalTo(250L))))
                .andExpect(model().attribute("vm", org.hamcrest.Matchers.hasProperty("planName", org.hamcrest.Matchers.equalTo("Startup Tier"))));
    }

    @Test
    @DisplayName("Owner is forbidden from /customer/dashboard with 403 Forbidden")
    void ownerForbiddenFromCustomerDashboard() throws Exception {
        mockMvc.perform(get("/customer/dashboard")
                        .with(httpBasic("owner@acme.test", "owner123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Root route / redirects based on user role")
    void rootRouteRedirectsProperly() throws Exception {
        // Owner redirects to /owner/dashboard
        mockMvc.perform(get("/")
                        .with(httpBasic("owner@acme.test", "owner123")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/dashboard"));

        // Customer redirects to /customer/dashboard
        mockMvc.perform(get("/")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));
    }
}
