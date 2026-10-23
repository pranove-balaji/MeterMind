package com.example.MeterMind.security;

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
class TenantIsolationAndBillingInvariantsIntegrationTests {

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
    private AppUser org1Owner;
    private AppUser org2Owner;

    private Customer customer1A;
    private Customer customer1B;
    private Customer customer2A;

    private AppUser customerUser1A;
    private AppUser customerUser1B;
    private AppUser customerUser2A;

    private Plan plan1;
    private Plan plan2;

    @BeforeEach
    void setupTestData() {
        usageEventRepository.deleteAll();
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        subscriptionRepository.deleteAll();
        featurePriceRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();

        // 1. Tenants
        org1Owner = userRepository.findByEmail("owner@acme.test").orElseThrow();
        org1 = org1Owner.getOrganization();

        if (!userRepository.existsByEmail("owner2@competitor.test")) {
            org2 = organizationRepository.save(new Organization("Beta Tech AI"));
            org2Owner = userRepository.save(new AppUser(
                    org2,
                    "owner2@competitor.test",
                    passwordEncoder.encode("secret123"),
                    Role.OWNER
            ));
        } else {
            org2Owner = userRepository.findByEmail("owner2@competitor.test").orElseThrow();
            org2 = org2Owner.getOrganization();
        }

        // 2. Plans and Feature Prices
        plan1 = planRepository.save(new Plan(org1, "Acme Standard", new BigDecimal("49.00"), 1000L));
        featurePriceRepository.save(new FeaturePrice(org1, plan1, "AI_SUMMARY", 5L));

        plan2 = planRepository.save(new Plan(org2, "Beta Enterprise", new BigDecimal("99.00"), 2000L));
        featurePriceRepository.save(new FeaturePrice(org2, plan2, "AI_SUMMARY", 10L));

        // 3. Customers
        customer1A = customerRepository.save(new Customer(org1, "Acme Client Alpha", "alpha@acmeclient.test"));
        customer1B = customerRepository.save(new Customer(org1, "Acme Client Beta", "beta@acmeclient.test"));
        customer2A = customerRepository.save(new Customer(org2, "Beta Client Gamma", "gamma@betaclient.test"));

        // 4. Customer Login Users
        customerUser1A = userRepository.findByEmail("customerA@acme.test").orElseGet(() ->
                userRepository.save(new AppUser(org1, "customerA@acme.test", passwordEncoder.encode("customer123"), Role.CUSTOMER))
        );
        customerUser1A.setCustomerId(customer1A.getId());
        userRepository.save(customerUser1A);

        customerUser1B = userRepository.findByEmail("customerB@acme.test").orElseGet(() ->
                userRepository.save(new AppUser(org1, "customerB@acme.test", passwordEncoder.encode("customer123"), Role.CUSTOMER))
        );
        customerUser1B.setCustomerId(customer1B.getId());
        userRepository.save(customerUser1B);

        customerUser2A = userRepository.findByEmail("customer2@beta.test").orElseGet(() ->
                userRepository.save(new AppUser(org2, "customer2@beta.test", passwordEncoder.encode("customer123"), Role.CUSTOMER))
        );
        customerUser2A.setCustomerId(customer2A.getId());
        userRepository.save(customerUser2A);

        // 5. Active Subscriptions
        Instant now = Instant.now();
        subscriptionRepository.save(new Subscription(org1, customer1A, plan1, SubscriptionStatus.ACTIVE, now, now.plus(30, ChronoUnit.DAYS)));
        subscriptionRepository.save(new Subscription(org1, customer1B, plan1, SubscriptionStatus.ACTIVE, now, now.plus(30, ChronoUnit.DAYS)));
        subscriptionRepository.save(new Subscription(org2, customer2A, plan2, SubscriptionStatus.ACTIVE, now, now.plus(30, ChronoUnit.DAYS)));

        // 6. Wallets with distinct balances
        walletService.grantCredits(org1.getId(), customer1A.getId(), org1Owner.getId(),
                new GrantCreditsRequest(100L, "Seed 1A", "SEED", "REF-1A"));
        walletService.grantCredits(org1.getId(), customer1B.getId(), org1Owner.getId(),
                new GrantCreditsRequest(250L, "Seed 1B", "SEED", "REF-1B"));
        walletService.grantCredits(org2.getId(), customer2A.getId(), org2Owner.getId(),
                new GrantCreditsRequest(500L, "Seed 2A", "SEED", "REF-2A"));
    }

    @Test
    @DisplayName("Invariant 1: Customer A cannot read Customer B's wallet or ledger history")
    void customerCannotReadOtherCustomerWalletOrLedger() throws Exception {
        // Customer A reads their own wallet -> 100 credits
        mockMvc.perform(get("/customer/wallet")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer1A.getId()))
                .andExpect(jsonPath("$.balance").value(100));

        // Customer B reads their own wallet -> 250 credits
        mockMvc.perform(get("/customer/wallet")
                        .with(httpBasic("customerB@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer1B.getId()))
                .andExpect(jsonPath("$.balance").value(250));

        // Customer A attempts to view Customer B's wallet via owner routes -> 403 Forbidden
        mockMvc.perform(get("/owner/customers/" + customer1B.getId() + "/wallet")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().isForbidden());

        // Customer A attempts to view Customer B's ledger via owner routes -> 403 Forbidden
        mockMvc.perform(get("/owner/customers/" + customer1B.getId() + "/wallet/ledger")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invariant 2: Customer A cannot read Customer B's usage events")
    void customerCannotReadOtherCustomerUsageEvents() throws Exception {
        // Customer A records usage (2 units * 5 = 10 cr)
        String usageA = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 2,
                "idempotencyKey": "usage-sec-a"
            }
        """;
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usageA))
                .andExpect(status().isOk());

        // Customer B records usage (4 units * 5 = 20 cr)
        String usageB = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 4,
                "idempotencyKey": "usage-sec-b"
            }
        """;
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerB@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usageB))
                .andExpect(status().isOk());

        // Customer A queries usage history: sees ONLY 1 event (the 2-unit charge)
        mockMvc.perform(get("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].units").value(2))
                .andExpect(jsonPath("$[0].creditsCharged").value(10))
                .andExpect(jsonPath("$[0].customerId").value(customer1A.getId()));

        // Customer B queries usage history: sees ONLY 1 event (the 4-unit charge)
        mockMvc.perform(get("/customer/usage")
                        .with(httpBasic("customerB@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].units").value(4))
                .andExpect(jsonPath("$[0].creditsCharged").value(20))
                .andExpect(jsonPath("$[0].customerId").value(customer1B.getId()));
    }

    @Test
    @DisplayName("Invariant 3: Organization 2 cannot read or manage Organization 1's plans")
    void organizationCannotManageOtherOrganizationPlans() throws Exception {
        // Org 2 Owner queries Org 1's plan -> 404 Not Found
        mockMvc.perform(get("/owner/plans/" + plan1.getId())
                        .with(httpBasic("owner2@competitor.test", "secret123")))
                .andExpect(status().isNotFound());

        // Org 2 Owner attempts to add a feature price to Org 1's plan -> 404 Not Found
        String newFeatureJson = """
            {
                "featureCode": "EXPLOIT_FEAT",
                "creditsPerUnit": 1
            }
        """;
        mockMvc.perform(post("/owner/plans/" + plan1.getId() + "/features")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newFeatureJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Invariant 4: Organization 2 cannot read, manage, or subscribe Organization 1's customers")
    void organizationCannotManageOtherOrganizationCustomers() throws Exception {
        // Org 2 Owner queries Org 1's customer -> 404 Not Found
        mockMvc.perform(get("/owner/customers/" + customer1A.getId())
                        .with(httpBasic("owner2@competitor.test", "secret123")))
                .andExpect(status().isNotFound());

        // Org 2 Owner attempts to subscribe Org 1's customer -> 404 Not Found
        String subJson = """
            {
                "planId": %d,
                "currentPeriodStart": "2026-10-01T00:00:00Z",
                "currentPeriodEnd": "2026-11-01T00:00:00Z"
            }
        """.formatted(plan2.getId());
        mockMvc.perform(post("/owner/customers/" + customer1A.getId() + "/subscriptions")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subJson))
                .andExpect(status().isNotFound());

        // Org 1 Owner attempts to subscribe Org 1's customer to Org 2's plan -> 404 Not Found
        String crossPlanSub = """
            {
                "planId": %d,
                "currentPeriodStart": "2026-10-01T00:00:00Z",
                "currentPeriodEnd": "2026-11-01T00:00:00Z"
            }
        """.formatted(plan2.getId());
        mockMvc.perform(post("/owner/customers/" + customer1A.getId() + "/subscriptions")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crossPlanSub))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Invariant 5: Organization 2 cannot read, grant, or debit Organization 1's wallets")
    void organizationCannotAccessOtherOrganizationWallets() throws Exception {
        // Read wallet -> 404
        mockMvc.perform(get("/owner/customers/" + customer1A.getId() + "/wallet")
                        .with(httpBasic("owner2@competitor.test", "secret123")))
                .andExpect(status().isNotFound());

        // Read ledger -> 404
        mockMvc.perform(get("/owner/customers/" + customer1A.getId() + "/wallet/ledger")
                        .with(httpBasic("owner2@competitor.test", "secret123")))
                .andExpect(status().isNotFound());

        // Grant credits -> 404
        String grantJson = """
            {
                "amount": 100,
                "reason": "Malicious grant"
            }
        """;
        mockMvc.perform(post("/owner/customers/" + customer1A.getId() + "/wallet/grants")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantJson))
                .andExpect(status().isNotFound());

        // Debit credits -> 404
        String debitJson = """
            {
                "amount": 50,
                "reason": "Malicious debit"
            }
        """;
        mockMvc.perform(post("/owner/customers/" + customer1A.getId() + "/wallet/debit")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debitJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Invariant 6: Caller cannot set the price (server resolves strictly from plan)")
    void callerCannotSetUsagePrice() throws Exception {
        // Client attempts to pass 0 cost and 0 price
        String exploitPayload = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 2,
                "idempotencyKey": "price-injection-test",
                "price": 0,
                "creditsCharged": 0,
                "unitCreditCost": 0,
                "creditsPerUnit": 0
            }
        """;

        // Plan defines AI_SUMMARY = 5 credits/unit. 2 units MUST cost exactly 10 credits!
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exploitPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitCreditCost").value(5))
                .andExpect(jsonPath("$.creditsCharged").value(10))
                .andExpect(jsonPath("$.balanceRemaining").value(90));

        CreditWallet wallet = walletRepository.findByCustomerId(customer1A.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(90L);
    }

    @Test
    @DisplayName("Invariant 7: Failed consumption leaves zero side effects on events, balance, and ledger")
    void failedConsumptionLeavesNoSideEffects() throws Exception {
        // Customer 1A has 100 credits. Attempting 25 units * 5 = 125 credits!
        String overdraftPayload = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 25,
                "idempotencyKey": "overdraft-invariants-test"
            }
        """;

        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overdraftPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Balance"));

        // Verify balance untouched
        CreditWallet wallet = walletRepository.findByCustomerId(customer1A.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(100L);

        // Verify zero usage events created
        assertThat(usageEventRepository.countByCustomerId(customer1A.getId())).isEqualTo(0L);

        // Verify zero debit ledger entries created
        List<CreditLedgerEntry> debits = ledgerRepository.findByWalletId(wallet.getId()).stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .toList();
        assertThat(debits).isEmpty();

        // Verify audit reconciliation
        assertThat(walletService.reconcileBalance(org1.getId(), customer1A.getId())).isTrue();
    }

    @Test
    @DisplayName("Invariant 8: Replay safety and conflicting key reuse regression verification")
    void replaySafetyAndConflictRegression() throws Exception {
        String payload = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 2,
                "idempotencyKey": "reg-key-001"
            }
        """;

        // Call 1: Debits 10 credits -> 90 remaining
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(false))
                .andExpect(jsonPath("$.balanceRemaining").value(90));

        // Call 2: Replay same payload -> 90 remaining, replayed: true, 0 new debits
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.balanceRemaining").value(90));

        // Call 3: Mutated payload with same key -> 409 Conflict
        String mutated = """
            {
                "featureCode": "AI_SUMMARY",
                "units": 5,
                "idempotencyKey": "reg-key-001"
            }
        """;
        mockMvc.perform(post("/customer/usage")
                        .with(httpBasic("customerA@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mutated))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Idempotency Conflict"));

        // Verify final balance is STILL 90
        CreditWallet wallet = walletRepository.findByCustomerId(customer1A.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(90L);

        // Verify exactly 1 usage event and 1 debit
        assertThat(usageEventRepository.countByCustomerId(customer1A.getId())).isEqualTo(1L);
        List<CreditLedgerEntry> debits = ledgerRepository.findByWalletId(wallet.getId()).stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .toList();
        assertThat(debits).hasSize(1);
    }
}
