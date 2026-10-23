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
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.wallet.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for idempotent usage events against real MySQL InnoDB locking
 * and database unique constraints.
 * Intentionally NOT annotated with @Transactional so threads execute in separate DB transactions.
 */
@SpringBootTest
class UsageConcurrencyIntegrationTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditWalletRepository walletRepository;

    @Autowired
    private CreditLedgerEntryRepository ledgerRepository;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private FeaturePriceRepository featurePriceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private UsageService usageService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org1;
    private Customer customer1;
    private AppUser customerUser;
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

        org1 = userRepository.findByEmail("owner@acme.test").orElseThrow().getOrganization();

        plan1 = planRepository.save(new Plan(org1, "Pro Plan", new BigDecimal("49.00"), 1000L));
        featurePriceRepository.save(new FeaturePrice(org1, plan1, "AI_SUMMARY", 5L));

        customer1 = customerRepository.save(new Customer(org1, "Concurrent Customer", "concurrent@client.test"));

        customerUser = userRepository.findByEmail("customer@acme.test").orElseGet(() -> {
            AppUser u = new AppUser(org1, "customer@acme.test", passwordEncoder.encode("customer123"), Role.CUSTOMER);
            return userRepository.save(u);
        });
        customerUser.setCustomerId(customer1.getId());
        userRepository.save(customerUser);

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
                new GrantCreditsRequest(100L, "Seed 100 credits", "SEED", "SEED-CONC")
        );
    }

    @AfterEach
    void tearDown() {
        usageEventRepository.deleteAll();
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        subscriptionRepository.deleteAll();
        featurePriceRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent duplicate usage requests with the same idempotency key debit credits exactly once")
    void concurrentDuplicateRequestsChargeOnlyOnce() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startGate = new CountDownLatch(1);

        String sameKey = "concurrent-idempotent-key-001";
        RecordUsageRequest request = new RecordUsageRequest("AI_SUMMARY", 3L, sameKey);

        Callable<UsageEventResponse> task = () -> {
            readyLatch.countDown();
            if (!startGate.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for start gate");
            }
            return usageService.recordUsage(
                    org1.getId(),
                    customer1.getId(),
                    customerUser.getId(),
                    request,
                    sameKey
            );
        };

        Future<UsageEventResponse> f1 = executor.submit(task);
        Future<UsageEventResponse> f2 = executor.submit(task);

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startGate.countDown();

        UsageEventResponse resp1 = f1.get(10, TimeUnit.SECONDS);
        UsageEventResponse resp2 = f2.get(10, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Both responses must be valid and report 15 credits charged
        assertThat(resp1.getCreditsCharged()).isEqualTo(15L);
        assertThat(resp2.getCreditsCharged()).isEqualTo(15L);

        // Exactly one should be marked as replayed (or one false, one true)
        assertThat(resp1.isReplayed() ^ resp2.isReplayed())
                .as("One request must record the event, the other must be recognized as an idempotent replay")
                .isTrue();

        // Invariant 1: Final wallet balance MUST be strictly 85 (100 - 15)
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(wallet.getBalance())
                .as("Wallet balance must be 85, proving credits were not deducted twice")
                .isEqualTo(85L);

        // Invariant 2: Exactly ONE usage event persisted
        List<UsageEvent> events = usageEventRepository.findByCustomerIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(events).hasSize(1);

        // Invariant 3: Exactly ONE DEBIT ledger entry
        List<CreditLedgerEntry> ledgerEntries = ledgerRepository.findByWalletId(wallet.getId());
        long debitCount = ledgerEntries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .count();
        assertThat(debitCount).isEqualTo(1L);

        // Invariant 4: Audit reconciliation passes
        assertThat(walletService.reconcileBalance(org1.getId(), customer1.getId())).isTrue();
    }
}
