package com.example.MeterMind.usage;

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
import com.example.MeterMind.wallet.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsageIntegrationTests {

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
    private Customer customer1;
    private AppUser customerUser;
    private Plan plan1;
    private FeaturePrice featurePrice1;

    @BeforeEach
    void setup() {
        usageEventRepository.deleteAll();
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        subscriptionRepository.deleteAll();
        featurePriceRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();

        org1 = userRepository.findByEmail("owner@acme.test").orElseThrow().getOrganization();

        // Create test plan with a feature price
        plan1 = planRepository.save(new Plan(org1, "Pro Plan", new BigDecimal("49.00"), 1000L));
        featurePrice1 = featurePriceRepository.save(new FeaturePrice(org1, plan1, "AI_SUMMARY", 5L));

        // Create customer and link to customer user
        customer1 = customerRepository.save(new Customer(org1, "Acme AI Labs", "client@acmelabs.test"));

        customerUser = userRepository.findByEmail("customer@acme.test").orElseGet(() -> {
            AppUser u = new AppUser(org1, "customer@acme.test", passwordEncoder.encode("customer123"), Role.CUSTOMER);
            return userRepository.save(u);
        });
        customerUser.setCustomerId(customer1.getId());
        userRepository.save(customerUser);

        // Create active subscription
        Instant now = Instant.now();
        subscriptionRepository.save(new Subscription(
                org1,
                customer1,
                plan1,
                SubscriptionStatus.ACTIVE,
                now,
                now.plus(30, ChronoUnit.DAYS)
        ));

        // Seed wallet with 100 credits
        walletService.grantCredits(
                org1.getId(),
                customer1.getId(),
                null,
                new GrantCreditsRequest(100L, "Seed 100 credits", "SEED", "SEED-1")
        );
    }

    @Test
    @DisplayName("Authenticated customer records usage: resolves server price, debits wallet atomically, saves price snapshot")
    void recordUsageSuccess() throws Exception {
        String json = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 3,
                "idempotencyKey": "req-usage-001"
            }
        """;

        // 3 units * 5 credits/unit = 15 credits charged. Initial: 100 -> Remaining: 85
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureCode").value("AI_SUMMARY"))
                .andExpect(jsonPath("$.units").value(3))
                .andExpect(jsonPath("$.unitCreditCost").value(5))
                .andExpect(jsonPath("$.creditsCharged").value(15))
                .andExpect(jsonPath("$.balanceRemaining").value(85))
                .andExpect(jsonPath("$.idempotencyKey").value("req-usage-001"))
                .andExpect(jsonPath("$.replayed").value(false));

        // Verify database state: UsageEvent
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(85L);

        List<UsageEvent> events = usageEventRepository.findByCustomerIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(events).hasSize(1);
        UsageEvent event = events.get(0);
        assertThat(event.getUnitCreditCost()).isEqualTo(5L);
        assertThat(event.getCreditsCharged()).isEqualTo(15L);

        // Verify ledger entries: 1 GRANT (100) + 1 DEBIT (15)
        List<CreditLedgerEntry> entries = ledgerRepository.findByWalletId(wallet.getId());
        assertThat(entries).hasSize(2);

        // Verify reconciliation
        assertThat(walletService.reconcileBalance(org1.getId(), customer1.getId())).isTrue();
    }

    @Test
    @DisplayName("Same idempotency key + same payload is replay-safe: returns existing response without double-debiting")
    void sameKeySamePayloadIsReplaySafe() throws Exception {
        String json = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 4,
                "idempotencyKey": "req-replay-100"
            }
        """;

        // First call: 4 units * 5 cr/unit = 20 cr charged. Balance: 100 -> 80
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditsCharged").value(20))
                .andExpect(jsonPath("$.balanceRemaining").value(80))
                .andExpect(jsonPath("$.replayed").value(false));

        // Second call with EXACT SAME key and payload
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditsCharged").value(20))
                .andExpect(jsonPath("$.balanceRemaining").value(80))
                .andExpect(jsonPath("$.replayed").value(true));

        // Invariant: Wallet balance must STILL be exactly 80 (NOT 60!)
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(80L);

        // Invariant: Exactly ONE usage event and ONE DEBIT ledger entry
        assertThat(usageEventRepository.countByCustomerId(customer1.getId())).isEqualTo(1L);
        List<CreditLedgerEntry> debits = ledgerRepository.findByWalletId(wallet.getId()).stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .toList();
        assertThat(debits).hasSize(1);
    }

    @Test
    @DisplayName("Same idempotency key + different payload returns 409 Conflict without side effects")
    void sameKeyDifferentPayloadReturnsConflict() throws Exception {
        String originalJson = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 2,
                "idempotencyKey": "key-conflict-check"
            }
        """;

        // First call: 2 units * 5 = 10 cr charged. Balance: 100 -> 90
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditsCharged").value(10));

        // Second call: SAME key but units changed to 5!
        String modifiedPayloadJson = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 5,
                "idempotencyKey": "key-conflict-check"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modifiedPayloadJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Idempotency Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("different request payload")));

        // Invariant: Balance must remain strictly 90 (no extra debit)
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(90L);
    }

    @Test
    @DisplayName("Insufficient balance rolls back transaction atomically: no usage event, no ledger debit")
    void insufficientBalanceRollsBackAtomically() throws Exception {
        // Customer needs 25 units * 5 = 125 credits, but only has 100
        String json = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 25,
                "idempotencyKey": "req-overdraw"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Balance"))
                .andExpect(jsonPath("$.availableBalance").value(100))
                .andExpect(jsonPath("$.requiredCredits").value(125));

        // Invariant: Balance remains strictly 100
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(100L);

        // Invariant: Zero usage events saved
        assertThat(usageEventRepository.countByCustomerId(customer1.getId())).isEqualTo(0L);

        // Invariant: Only the initial grant ledger entry exists
        var ledgerEntries = ledgerRepository.findByWalletId(wallet.getId());
        assertThat(ledgerEntries).hasSize(1);
        assertThat(ledgerEntries.get(0).getEntryType()).isEqualTo(LedgerEntryType.GRANT);
    }

    @Test
    @DisplayName("Zero or negative units are rejected with 400 Bad Request")
    void invalidUnitsRejected() throws Exception {
        String json = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 0,
                "idempotencyKey": "req-invalid-units"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("Unpriced feature code is rejected with 404 Not Found")
    void unpricedFeatureRejected() throws Exception {
        String json = """
            {
                "featureCode": "VIDEO_GENERATION",
                "units": 1,
                "idempotencyKey": "req-unknown-feat"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Customer can query their usage history via GET /customer/usage")
    void getCustomerUsageHistory() throws Exception {
        // Record 1 event
        String json = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 2,
                "idempotencyKey": "hist-req-1"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customer/usage")
                        .with(httpBasic("customer@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].featureCode").value("AI_SUMMARY"))
                .andExpect(jsonPath("$[0].creditsCharged").value(10));
    }

    @Test
    @DisplayName("Owner role is forbidden from customer usage endpoint with 403 Forbidden")
    void ownerRoleForbiddenFromCustomerUsage() throws Exception {
        String json = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 1,
                "idempotencyKey": "owner-forbidden"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
