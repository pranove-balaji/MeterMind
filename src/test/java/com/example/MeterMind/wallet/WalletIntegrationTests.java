package com.example.MeterMind.wallet;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.identity.AppUser;
import com.example.MeterMind.identity.AppUserRepository;
import com.example.MeterMind.identity.Role;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
class WalletIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreditWalletRepository walletRepository;

    @Autowired
    private CreditLedgerEntryRepository ledgerRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org1;
    private Organization org2;
    private AppUser org1Owner;
    private Customer org1Customer;

    @BeforeEach
    void setupTestData() {
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        customerRepository.deleteAll();

        org1Owner = userRepository.findByEmail("owner@acme.test").orElseThrow();
        org1 = org1Owner.getOrganization();

        if (!userRepository.existsByEmail("owner2@competitor.test")) {
            org2 = organizationRepository.save(new Organization("Beta Tech AI"));
            userRepository.save(new AppUser(
                    org2,
                    "owner2@competitor.test",
                    passwordEncoder.encode("secret123"),
                    Role.OWNER
            ));
        } else {
            org2 = userRepository.findByEmail("owner2@competitor.test").orElseThrow().getOrganization();
        }

        org1Customer = customerRepository.save(new Customer(org1, "Stark Industries", "tony@stark.test"));
    }

    @Test
    @DisplayName("Owner can grant credits to customer, updating wallet and creating an append-only ledger entry")
    void ownerCanGrantCreditsAndRecordLedger() throws Exception {
        String json = """
            {
                "amount": 500,
                "reason": "Welcome onboarding grant",
                "referenceType": "SIGNUP_PROMO",
                "referenceId": "PROMO-2026"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.entryType").value("GRANT"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.balanceAfter").value(500))
                .andExpect(jsonPath("$.actorUserId").value(org1Owner.getId()))
                .andExpect(jsonPath("$.reason").value("Welcome onboarding grant"))
                .andExpect(jsonPath("$.referenceType").value("SIGNUP_PROMO"));

        // Verify wallet balance
        CreditWallet wallet = walletRepository.findByCustomerId(org1Customer.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(500L);

        // Verify ledger entries count
        assertThat(ledgerRepository.findByWalletId(wallet.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Sequential credit grants accumulate balance atomically and maintain complete audit trail")
    void sequentialGrantsAccumulateBalance() throws Exception {
        // First grant: 300 credits
        String grant1 = """
            {
                "amount": 300,
                "reason": "First grant"
            }
        """;
        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grant1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(300));

        // Second grant: 200 credits
        String grant2 = """
            {
                "amount": 200,
                "reason": "Second topup"
            }
        """;
        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grant2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(500));

        // Fetch ledger history
        mockMvc.perform(get("/owner/customers/" + org1Customer.getId() + "/wallet/ledger")
                        .with(httpBasic("owner@acme.test", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].amount").value(200)) // Newest first
                .andExpect(jsonPath("$[0].balanceAfter").value(500))
                .andExpect(jsonPath("$[1].amount").value(300))
                .andExpect(jsonPath("$[1].balanceAfter").value(300));

        // Audit reconciliation check
        assertThat(walletService.reconcileBalance(org1.getId(), org1Customer.getId())).isTrue();
    }

    @Test
    @DisplayName("Zero or negative credit grant amount is rejected with 400 Bad Request")
    void invalidGrantAmountRejected() throws Exception {
        String invalidJson = """
            {
                "amount": -50,
                "reason": "Negative amount test"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("Credit grant without reason is rejected with 400 Bad Request")
    void missingGrantReasonRejected() throws Exception {
        String missingReasonJson = """
            {
                "amount": 100,
                "reason": "   "
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingReasonJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("Cross-tenant grant is rejected (Org 2 owner cannot grant credits to Org 1 customer)")
    void crossTenantGrantRejected() throws Exception {
        String json = """
            {
                "amount": 100,
                "reason": "Unauthorized cross tenant grant"
            }
        """;

        // Owner 2 calls grant for Org 1's customer -> 404 Not Found (resource hidden)
        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Customer role is forbidden from granting credits with 403 Forbidden")
    void customerRoleForbiddenFromGrants() throws Exception {
        String json = """
            {
                "amount": 1000,
                "reason": "Self grant attempt"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/grants")
                        .with(httpBasic("customer@acme.test", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can debit credits from customer wallet, decrementing balance and recording ledger")
    void ownerCanDebitCreditsAndRecordLedger() throws Exception {
        // Setup initial balance: grant 200 credits
        walletService.grantCredits(org1.getId(), org1Customer.getId(), org1Owner.getId(),
                new GrantCreditsRequest(200L, "Initial grant", "SEED", "REF-1"));

        String debitJson = """
            {
                "amount": 75,
                "reason": "API usage consumption",
                "referenceType": "USAGE_BATCH",
                "referenceId": "BATCH-101"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/debit")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryType").value("DEBIT"))
                .andExpect(jsonPath("$.amount").value(75))
                .andExpect(jsonPath("$.balanceAfter").value(125))
                .andExpect(jsonPath("$.reason").value("API usage consumption"));

        // Verify wallet balance
        CreditWallet wallet = walletRepository.findByCustomerId(org1Customer.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(125L);

        // Audit reconciliation check
        assertThat(walletService.reconcileBalance(org1.getId(), org1Customer.getId())).isTrue();
    }

    @Test
    @DisplayName("A wallet with 100 credits rejects a charge of 101 without side effects")
    void insufficientBalanceRejectsChargeWithoutSideEffects() throws Exception {
        // Seed exactly 100 credits
        walletService.grantCredits(org1.getId(), org1Customer.getId(), org1Owner.getId(),
                new GrantCreditsRequest(100L, "Initial 100 credits", "TEST", "REF-100"));

        String debitJson = """
            {
                "amount": 101,
                "reason": "Excessive debit attempt",
                "referenceType": "USAGE",
                "referenceId": "EXCESS-1"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/debit")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debitJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Balance"))
                .andExpect(jsonPath("$.availableBalance").value(100))
                .andExpect(jsonPath("$.requiredCredits").value(101));

        // Invariant verification: Balance MUST remain strictly 100
        CreditWallet wallet = walletRepository.findByCustomerId(org1Customer.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(100L);

        // Invariant verification: Ledger entries MUST contain ONLY the 1 grant, NO debit entries
        var entries = ledgerRepository.findByWalletId(wallet.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntryType.GRANT);

        // Audit reconciliation check
        assertThat(walletService.reconcileBalance(org1.getId(), org1Customer.getId())).isTrue();
    }

    @Test
    @DisplayName("Zero or negative debit amount is rejected with 400 Bad Request")
    void invalidDebitAmountRejected() throws Exception {
        String invalidJson = """
            {
                "amount": 0,
                "reason": "Zero amount debit"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/debit")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("Cross-tenant debit is rejected (Org 2 owner cannot debit Org 1 customer)")
    void crossTenantDebitRejected() throws Exception {
        String json = """
            {
                "amount": 50,
                "reason": "Unauthorized debit"
            }
        """;

        mockMvc.perform(post("/owner/customers/" + org1Customer.getId() + "/wallet/debit")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }
}
