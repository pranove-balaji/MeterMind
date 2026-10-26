package com.example.MeterMind.billing;

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
import com.example.MeterMind.wallet.CreditLedgerEntry;
import com.example.MeterMind.wallet.CreditLedgerEntryRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillingIntegrationTests {

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
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CreditWalletRepository walletRepository;

    @Autowired
    private CreditLedgerEntryRepository ledgerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SubscriptionRenewalRepository renewalRepository;

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
        invoiceRepository.deleteAll();
        renewalRepository.deleteAll();
        subscriptionRepository.deleteAll();
        walletRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();

        String suffix = String.valueOf(System.nanoTime());
        org1 = organizationRepository.save(new Organization("Billing Org 1 " + suffix));
        org2 = organizationRepository.save(new Organization("Billing Org 2 " + suffix));

        owner1 = userRepository.save(new AppUser(
                org1,
                "owner1_" + suffix + "@billing.test",
                passwordEncoder.encode("Password123!"),
                Role.OWNER
        ));

        owner2 = userRepository.save(new AppUser(
                org2,
                "owner2_" + suffix + "@billing.test",
                passwordEncoder.encode("Password123!"),
                Role.OWNER
        ));

        customer1 = customerRepository.save(new Customer(
                org1,
                "Apex SaaS " + suffix,
                "apex_" + suffix + "@client.test"
        ));

        custUser1 = new AppUser(
                org1,
                "custuser1_" + suffix + "@billing.test",
                passwordEncoder.encode("Password123!"),
                Role.CUSTOMER
        );
        custUser1.setCustomerId(customer1.getId());
        custUser1 = userRepository.save(custUser1);

        plan1 = planRepository.save(new Plan(
                org1,
                "Scale Plan " + suffix,
                new BigDecimal("149.00"),
                5000L
        ));

        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(30, ChronoUnit.DAYS);

        subscription1 = subscriptionRepository.save(new Subscription(
                org1,
                customer1,
                plan1,
                SubscriptionStatus.ACTIVE,
                start,
                end
        ));

        wallet1 = walletRepository.save(new CreditWallet(customer1, 500L));
    }

    @Test
    @DisplayName("Invoice preview generates lines with exact financial scale and simulated draft label")
    void testInvoicePreviewCalculationsAndLabels() throws Exception {
        mockMvc.perform(get("/customer/invoices/preview")
                        .with(httpBasic(custUser1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer1.getId()))
                .andExpect(jsonPath("$.status").value("SIMULATED"))
                .andExpect(jsonPath("$.simulated").value(true))
                .andExpect(jsonPath("$.subtotal").value(149.00))
                .andExpect(jsonPath("$.tax").value(0.00))
                .andExpect(jsonPath("$.total").value(149.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.notes").value(containsString("SIMULATED PREVIEW")))
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].description").value(containsString("Base Subscription: Scale Plan")))
                .andExpect(jsonPath("$.lines[0].quantity").value(1))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(149.00))
                .andExpect(jsonPath("$.lines[0].amount").value(149.00))
                .andExpect(jsonPath("$.lines[1].description").value(containsString("Included Monthly Credits (5000 credits)")))
                .andExpect(jsonPath("$.lines[1].quantity").value(5000))
                .andExpect(jsonPath("$.lines[1].unitPrice").value(0.00))
                .andExpect(jsonPath("$.lines[1].amount").value(0.00));
    }

    @Test
    @DisplayName("Renewal simulation: grants monthly credits, stores invoice, and advances billing period")
    void testRenewalSimulationSuccess() throws Exception {
        Instant initialPeriodEnd = subscription1.getCurrentPeriodEnd();

        mockMvc.perform(post("/owner/customers/" + customer1.getId() + "/renew")
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditsGranted").value(5000))
                .andExpect(jsonPath("$.balanceAfter").value(5500))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.invoice.subtotal").value(149.00))
                .andExpect(jsonPath("$.invoice.simulated").value(true));

        // Verify wallet balance increased
        CreditWallet updatedWallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(updatedWallet.getBalance()).isEqualTo(5500L);

        // Verify ledger entry
        List<CreditLedgerEntry> entries = ledgerRepository.findByWalletId(updatedWallet.getId());
        assertThat(entries).hasSize(1);
        CreditLedgerEntry grantEntry = entries.get(0);
        assertThat(grantEntry.getAmount()).isEqualTo(5000L);
        assertThat(grantEntry.getReferenceType()).isEqualTo("SUBSCRIPTION_RENEWAL");
        assertThat(grantEntry.getBalanceAfter()).isEqualTo(5500L);

        // Verify subscription period dates advanced
        Subscription updatedSub = subscriptionRepository.findById(subscription1.getId()).orElseThrow();
        assertThat(updatedSub.getCurrentPeriodStart()).isEqualTo(initialPeriodEnd);
        assertThat(updatedSub.getCurrentPeriodEnd()).isEqualTo(initialPeriodEnd.plus(30, ChronoUnit.DAYS));

        // Verify invoice was persisted in the database
        List<Invoice> invoices = invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(invoices).hasSize(1);
        Invoice inv = invoices.get(0);
        assertThat(inv.getTotal()).isEqualByComparingTo("149.00");
        assertThat(inv.getLines()).hasSize(2);
    }

    @Test
    @DisplayName("Renewal idempotency invariant: repeat renewal invocation for same period is rejected")
    void testRenewalIdempotencyPreventsDuplicateGrants() throws Exception {
        Instant initialStart = subscription1.getCurrentPeriodStart();

        // First renewal succeeds
        mockMvc.perform(post("/owner/customers/" + customer1.getId() + "/renew")
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(5500));

        // Reset subscription period back to initialStart to simulate rerun or retry of the exact same period
        Subscription sub = subscriptionRepository.findById(subscription1.getId()).orElseThrow();
        sub.setCurrentPeriodStart(initialStart);
        subscriptionRepository.saveAndFlush(sub);

        // Second renewal attempt for the same period start must be rejected
        mockMvc.perform(post("/owner/customers/" + customer1.getId() + "/renew")
                        .with(httpBasic(owner1.getEmail(), "Password123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Renewal has already been processed for subscription period starting")));

        // Verify wallet balance was NOT granted twice
        CreditWallet wallet = walletRepository.findByCustomerId(customer1.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(5500L);
    }

    @Test
    @DisplayName("Tenant Isolation: Org 2 owner cannot preview or renew Org 1's customer")
    void testTenantIsolationOnBillingOperations() throws Exception {
        mockMvc.perform(get("/owner/customers/" + customer1.getId() + "/invoices/preview")
                        .with(httpBasic(owner2.getEmail(), "Password123!")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/owner/customers/" + customer1.getId() + "/renew")
                        .with(httpBasic(owner2.getEmail(), "Password123!")))
                .andExpect(status().isNotFound());
    }
}
