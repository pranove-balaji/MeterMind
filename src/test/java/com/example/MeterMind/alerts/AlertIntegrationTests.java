package com.example.MeterMind.alerts;

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
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlertIntegrationTests {

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
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org1;
    private Organization org2;
    private Customer customer1;
    private AppUser owner1;
    private AppUser owner2;
    private AppUser custUser1;
    private Plan plan1;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        alertRuleRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());
        org1 = organizationRepository.save(new Organization("Alert Org 1 " + suffix));
        org2 = organizationRepository.save(new Organization("Alert Org 2 " + suffix));

        owner1 = userRepository.save(new AppUser(
                org1,
                "owner1_" + suffix + "@metermind.test",
                passwordEncoder.encode("Password123!"),
                Role.OWNER
        ));

        owner2 = userRepository.save(new AppUser(
                org2,
                "owner2_" + suffix + "@metermind.test",
                passwordEncoder.encode("Password123!"),
                Role.OWNER
        ));

        customer1 = customerRepository.save(new Customer(
                org1,
                "Cust 1 " + suffix,
                "cust1_" + suffix + "@client.test"
        ));

        custUser1 = new AppUser(
                org1,
                "custuser1_" + suffix + "@metermind.test",
                passwordEncoder.encode("Password123!"),
                Role.CUSTOMER
        );
        custUser1.setCustomerId(customer1.getId());
        custUser1 = userRepository.save(custUser1);

        plan1 = planRepository.save(new Plan(
                org1,
                "Pro Plan " + suffix,
                new BigDecimal("99.00"),
                5000L
        ));

        featurePriceRepository.save(new FeaturePrice(
                org1,
                plan1,
                "API_CALL",
                10L
        ));

        subscriptionRepository.save(new Subscription(
                org1,
                customer1,
                plan1,
                SubscriptionStatus.ACTIVE,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS)
        ));

        walletRepository.save(new CreditWallet(customer1, 100L));
    }

    @Test
    @DisplayName("Owner can create, list, and delete alert rules")
    void testOwnerRuleLifecycle() throws Exception {
        String createJson = """
                {
                    "ruleType": "LOW_BALANCE",
                    "thresholdValue": 50,
                    "enabled": true
                }
                """;

        mockMvc.perform(post("/owner/alerts/rules")
                        .with(httpBasic(owner1.getEmail(), "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ruleType").value("LOW_BALANCE"))
                .andExpect(jsonPath("$.thresholdValue").value(50))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/owner/alerts/rules")
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        List<AlertRule> rules = alertRuleRepository.findByOrganizationId(org1.getId());
        assertThat(rules).hasSize(1);
        Long ruleId = rules.get(0).getId();

        mockMvc.perform(delete("/owner/alerts/rules/" + ruleId)
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/owner/alerts/rules")
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Tenant isolation: Owner in Org 2 cannot read or delete Org 1's alert rules")
    void testTenantIsolationOnAlertRules() throws Exception {
        AlertRule rule1 = alertRuleRepository.save(new AlertRule(org1, null, AlertRuleType.LOW_BALANCE, 20L));

        mockMvc.perform(get("/owner/alerts/rules")
                        .with(httpBasic(owner2.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete("/owner/alerts/rules/" + rule1.getId())
                        .with(httpBasic(owner2.getEmail(), "Password123!")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Usage evaluation triggers notification and deduplicates consecutive crossings")
    void testAlertEvaluationAndDeduplication() throws Exception {
        // Create LOW_BALANCE rule with threshold = 50
        alertRuleRepository.save(new AlertRule(org1, null, AlertRuleType.LOW_BALANCE, 50L));

        // Initial wallet balance = 100.
        // Debit 6 units * 10 credits = 60 credits -> balance drops to 40 (<= 50)
        String usage1 = """
                {
                    "featureCode": "API_CALL",
                    "units": 6,
                    "idempotencyKey": "alert-test-usage-1"
                }
                """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic(custUser1.getEmail(), "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usage1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceRemaining").value(40));

        // Exactly 1 notification should be created
        List<Notification> notifications = notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(notifications).hasSize(1);
        Notification notif = notifications.get(0);
        assertThat(notif.getTitle()).isEqualTo("Low Credit Balance Alert");
        assertThat(notif.getMessage()).contains("40 credits");

        // Second usage: 1 unit * 10 credits = 10 credits -> balance drops to 30 (still <= 50)
        String usage2 = """
                {
                    "featureCode": "API_CALL",
                    "units": 1,
                    "idempotencyKey": "alert-test-usage-2"
                }
                """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic(custUser1.getEmail(), "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usage2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceRemaining").value(30));

        // Deduplication invariant: No duplicate notification should be added because the eventKey already exists
        List<Notification> notificationsAfter = notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(notificationsAfter).hasSize(1);

        // Customer can read notification and mark as read
        mockMvc.perform(get("/customer/notifications")
                        .with(httpBasic(custUser1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].read").value(false));

        mockMvc.perform(post("/customer/notifications/" + notif.getId() + "/read")
                        .with(httpBasic(custUser1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @DisplayName("Zero balance blocks usage independently of whether alert rules exist")
    void testZeroBalanceBlocksUsage() throws Exception {
        // Set wallet balance to 0
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        wallet.setBalance(0L);
        walletRepository.save(wallet);

        String usage = """
                {
                    "featureCode": "API_CALL",
                    "units": 1,
                    "idempotencyKey": "zero-bal-attempt"
                }
                """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic(custUser1.getEmail(), "Password123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Balance"));
    }
}
