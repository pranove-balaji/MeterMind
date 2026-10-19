package com.example.MeterMind.wallet;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.identity.AppUser;
import com.example.MeterMind.identity.AppUserRepository;
import com.example.MeterMind.organization.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency integration test executing real multi-threaded transactions
 * against MySQL InnoDB row-level locking.
 * Note: Intentionally NOT annotated with @Transactional so each thread
 * runs in its own distinct, committed database transaction.
 */
@SpringBootTest
class WalletConcurrencyIntegrationTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditWalletRepository walletRepository;

    @Autowired
    private CreditLedgerEntryRepository ledgerRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private WalletService walletService;

    private Organization org1;
    private AppUser org1Owner;
    private Customer testCustomer;

    @BeforeEach
    void setup() {
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        customerRepository.deleteAll();

        org1Owner = userRepository.findByEmail("owner@acme.test").orElseThrow();
        org1 = org1Owner.getOrganization();

        testCustomer = customerRepository.save(new Customer(org1, "Concurrent Corp", "billing@concurrent.test"));

        // Seed wallet with exactly 100 credits
        walletService.grantCredits(
                org1.getId(),
                testCustomer.getId(),
                org1Owner.getId(),
                new GrantCreditsRequest(100L, "Initial 100 credits", "SEED", "CONCURRENCY-INIT")
        );

        CreditWallet initialWallet = walletRepository.findByCustomerId(testCustomer.getId()).orElseThrow();
        assertThat(initialWallet.getBalance()).isEqualTo(100L);
    }

    @AfterEach
    void tearDown() {
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent pair of 80-credit debits against 100 credits results in exactly one success and balance 20")
    void concurrentPairOfDebitsAgainstLimitedBalanceResultsInOneSuccessAndBalance20() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);

        Callable<Boolean> debitTask = () -> {
            readyLatch.countDown();
            // Wait for both threads to be ready at the gate
            if (!startGate.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout waiting for start gate");
            }

            try {
                walletService.debitCredits(
                        org1.getId(),
                        testCustomer.getId(),
                        org1Owner.getId(),
                        80L,
                        "CONCURRENT_DEBIT",
                        "REF-" + Thread.currentThread().getId(),
                        "Concurrent debit race test"
                );
                return true;
            } catch (InsufficientBalanceException ex) {
                return false;
            }
        };

        Future<Boolean> future1 = executor.submit(debitTask);
        Future<Boolean> future2 = executor.submit(debitTask);

        // Ensure both threads have entered the task and reached the gate
        boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        // Release both threads simultaneously
        startGate.countDown();

        boolean result1 = future1.get(10, TimeUnit.SECONDS);
        boolean result2 = future2.get(10, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Invariant 1: Exactly ONE debit must succeed, and ONE must fail with InsufficientBalanceException
        assertThat(result1 ^ result2)
                .as("Exactly one of the two concurrent 80-credit debits must succeed")
                .isTrue();

        // Invariant 2: Final balance MUST be strictly 20 (100 - 80)
        CreditWallet finalWallet = walletRepository.findByCustomerId(testCustomer.getId()).orElseThrow();
        assertThat(finalWallet.getBalance())
                .as("Wallet balance must be exactly 20 after one 80-credit debit succeeds")
                .isEqualTo(20L);

        // Invariant 3: Ledger history must contain exactly 2 entries (1 GRANT of 100, 1 DEBIT of 80)
        List<CreditLedgerEntry> entries = ledgerRepository.findByWalletId(finalWallet.getId());
        assertThat(entries).hasSize(2);

        long grantsTotal = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.GRANT)
                .mapToLong(CreditLedgerEntry::getAmount)
                .sum();
        long debitsTotal = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .mapToLong(CreditLedgerEntry::getAmount)
                .sum();

        assertThat(grantsTotal).isEqualTo(100L);
        assertThat(debitsTotal).isEqualTo(80L);

        // Invariant 4: Audit reconciliation must be 100% consistent
        assertThat(walletService.reconcileBalance(org1.getId(), testCustomer.getId())).isTrue();
    }
}
