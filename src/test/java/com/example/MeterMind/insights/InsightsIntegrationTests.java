package com.example.MeterMind.insights;

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
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InsightsIntegrationTests {

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
    private UsageEventRepository usageEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org1;
    private Organization org2;
    private Customer customer1;
    private AppUser owner1;
    private AppUser owner2;
    private AppUser custUser1;
    private Plan plan1;
    private Subscription subscription1;
    private CreditWallet wallet1;

    @BeforeEach
    void setUp() {
        usageEventRepository.deleteAll();
        walletRepository.deleteAll();
        subscriptionRepository.deleteAll();
        featurePriceRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());
        org1 = organizationRepository.save(new Organization("Insights Org 1 " + suffix));
        org2 = organizationRepository.save(new Organization("Insights Org 2 " + suffix));

        owner1 = userRepository.save(new AppUser(
                org1,
                "owner1_" + suffix + "@insights.test",
                passwordEncoder.encode("Password123!"),
                Role.OWNER
        ));

        owner2 = userRepository.save(new AppUser(
                org2,
                "owner2_" + suffix + "@insights.test",
                passwordEncoder.encode("Password123!"),
                Role.OWNER
        ));

        customer1 = customerRepository.save(new Customer(
                org1,
                "Acme Corp " + suffix,
                "acme_" + suffix + "@client.test"
        ));

        custUser1 = new AppUser(
                org1,
                "custuser1_" + suffix + "@insights.test",
                passwordEncoder.encode("Password123!"),
                Role.CUSTOMER
        );
        custUser1.setCustomerId(customer1.getId());
        custUser1 = userRepository.save(custUser1);

        plan1 = planRepository.save(new Plan(
                org1,
                "Pro Plan " + suffix,
                new BigDecimal("99.00"),
                1000L
        ));

        featurePriceRepository.save(new FeaturePrice(org1, plan1, "LLM_INFERENCE", 5L));
        featurePriceRepository.save(new FeaturePrice(org1, plan1, "EMBEDDINGS", 1L));

        subscription1 = subscriptionRepository.save(new Subscription(
                org1,
                customer1,
                plan1,
                SubscriptionStatus.ACTIVE,
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().plus(20, ChronoUnit.DAYS)
        ));

        wallet1 = walletRepository.save(new CreditWallet(customer1, 100L));
    }

    @Test
    @DisplayName("Edge Case: No usage history returns INSUFFICIENT_DATA and explicit assumption")
    void testNoUsageHistoryReturnsInsufficientData() throws Exception {
        mockMvc.perform(get("/customer/insights")
                        .with(httpBasic(custUser1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer1.getId()))
                .andExpect(jsonPath("$.forecast.status").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.forecast.availableCredits").value(100))
                .andExpect(jsonPath("$.forecast.daysRemaining").doesNotExist())
                .andExpect(jsonPath("$.forecast.measurementWindow").value("Last 30 days"))
                .andExpect(jsonPath("$.forecast.assumption").value(containsString("No usage recorded in the 30-day measurement window")));
    }

    @Test
    @DisplayName("Edge Case: Zero balance returns DEPLETED and CRITICAL ZERO_BALANCE recommendation")
    void testZeroBalanceReturnsDepletedAndCriticalRecommendation() throws Exception {
        wallet1.setBalance(0L);
        walletRepository.save(wallet1);

        mockMvc.perform(get("/customer/insights")
                        .with(httpBasic(custUser1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.status").value("DEPLETED"))
                .andExpect(jsonPath("$.forecast.availableCredits").value(0))
                .andExpect(jsonPath("$.forecast.daysRemaining").value(0))
                .andExpect(jsonPath("$.recommendations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.recommendations[0].ruleCode").value("ZERO_BALANCE"))
                .andExpect(jsonPath("$.recommendations[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.recommendations[0].observation").value(containsString("0 credits")));
    }

    @Test
    @DisplayName("Active forecast computes daily burn rate, projected runway, and feature concentration")
    void testActiveForecastAndFeatureConcentration() throws Exception {
        // Customer has 100 credits in wallet.
        // Create 2 usage events:
        // Event 1: LLM_INFERENCE -> 80 credits (80% concentration)
        // Event 2: EMBEDDINGS -> 20 credits (20%)
        // Total credits = 100 credits consumed.
        usageEventRepository.save(new UsageEvent(
                org1, customer1, subscription1, "LLM_INFERENCE",
                16L, 5L, 80L, "idemp-feat-1", "hash-1"
        ));
        usageEventRepository.save(new UsageEvent(
                org1, customer1, subscription1, "EMBEDDINGS",
                20L, 1L, 20L, "idemp-feat-2", "hash-2"
        ));

        mockMvc.perform(get("/customer/insights?windowDays=30")
                        .with(httpBasic(custUser1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.status").value("ACTIVE_FORECAST"))
                .andExpect(jsonPath("$.forecast.availableCredits").value(100))
                .andExpect(jsonPath("$.forecast.averageDailyBurn").value(100.00))
                .andExpect(jsonPath("$.forecast.daysRemaining").value(1))
                .andExpect(jsonPath("$.forecast.measurementWindow").value("Last 30 days"))
                .andExpect(jsonPath("$.forecast.assumption").value(containsString("100.00 credits/day")))
                // Feature breakdown:
                .andExpect(jsonPath("$.topFeatures[0].featureCode").value("LLM_INFERENCE"))
                .andExpect(jsonPath("$.topFeatures[0].totalCredits").value(80))
                .andExpect(jsonPath("$.topFeatures[0].percentageOfTotal").value(80.0))
                .andExpect(jsonPath("$.topFeatures[1].featureCode").value("EMBEDDINGS"))
                .andExpect(jsonPath("$.topFeatures[1].totalCredits").value(20))
                // Explainable recommendations:
                .andExpect(jsonPath("$.recommendations[*].ruleCode", hasItems("LOW_RUNWAY", "FEATURE_CONCENTRATION")));
    }

    @Test
    @DisplayName("Tenant Isolation: Owner of Org 2 cannot access Org 1's customer insights")
    void testTenantIsolationOnInsights() throws Exception {
        mockMvc.perform(get("/owner/customers/" + customer1.getId() + "/insights")
                        .with(httpBasic(owner2.getEmail(), "Password123!")))
                .andExpect(status().isNotFound());

        // Org 1 Owner can access Org 1's customer insights
        mockMvc.perform(get("/owner/customers/" + customer1.getId() + "/insights")
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer1.getId()));
    }
}
