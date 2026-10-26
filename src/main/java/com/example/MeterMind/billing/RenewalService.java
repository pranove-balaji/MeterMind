package com.example.MeterMind.billing;

import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
import com.example.MeterMind.wallet.GrantCreditsRequest;
import com.example.MeterMind.wallet.LedgerEntryResponse;
import com.example.MeterMind.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RenewalService {

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionRenewalRepository renewalRepository;
    private final InvoiceRepository invoiceRepository;
    private final WalletService walletService;
    private final CreditWalletRepository walletRepository;

    public RenewalService(CustomerRepository customerRepository,
                          SubscriptionRepository subscriptionRepository,
                          SubscriptionRenewalRepository renewalRepository,
                          InvoiceRepository invoiceRepository,
                          WalletService walletService,
                          CreditWalletRepository walletRepository) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.renewalRepository = renewalRepository;
        this.invoiceRepository = invoiceRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public RenewalResponse simulateRenewal(Long organizationId, Long customerId, Long actorUserId) {
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Subscription subscription = subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Customer has no active subscription to renew"));

        Instant periodStart = subscription.getCurrentPeriodStart();
        Instant periodEnd = subscription.getCurrentPeriodEnd();

        // Idempotency Invariant: Check if renewal for this specific period was already processed
        if (renewalRepository.existsBySubscriptionIdAndPeriodStart(subscription.getId(), periodStart)) {
            throw new IllegalStateException("Renewal has already been processed for subscription period starting " + periodStart);
        }

        Plan plan = subscription.getPlan();
        long creditsToGrant = plan.getMonthlyCredits();

        // 1. Grant monthly included credits idempotently
        String referenceId = "SUB_" + subscription.getId() + "_PERIOD_" + periodStart.toEpochMilli();
        LedgerEntryResponse grantResult = walletService.grantCredits(
                organizationId,
                customerId,
                actorUserId,
                new GrantCreditsRequest(
                        creditsToGrant,
                        "Monthly included credits renewal for plan: " + plan.getName(),
                        "SUBSCRIPTION_RENEWAL",
                        referenceId
                )
        );

        // 2. Persist renewal audit record
        SubscriptionRenewal renewal = new SubscriptionRenewal(
                customer.getOrganization(),
                subscription,
                customer,
                periodStart,
                periodEnd,
                creditsToGrant,
                "PROCESSED"
        );
        SubscriptionRenewal savedRenewal = renewalRepository.save(renewal);

        // 3. Generate and persist renewal invoice
        BigDecimal basePrice = plan.getMonthlyPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = basePrice;
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        String invoiceNumber = "INV-SIM-" + customer.getId() + "-" + periodStart.toEpochMilli();
        String notes = "SIMULATED RENEWAL INVOICE — Subscription renewal processed. No real payment or tax transaction executed.";

        Invoice invoice = new Invoice(
                customer.getOrganization(),
                customer,
                subscription,
                invoiceNumber,
                InvoiceStatus.SIMULATED,
                periodStart,
                periodEnd,
                "USD",
                subtotal,
                tax,
                total,
                true,
                notes
        );

        invoice.addLine(new InvoiceLine(
                "Subscription Renewal: " + plan.getName() + " (" + periodStart + " to " + periodEnd + ")",
                1L,
                basePrice,
                basePrice
        ));

        invoice.addLine(new InvoiceLine(
                "Monthly Included Credits (" + creditsToGrant + " credits)",
                creditsToGrant,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        ));

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 4. Advance subscription billing period
        Instant nextPeriodStart = periodEnd;
        Instant nextPeriodEnd = nextPeriodStart.plus(30, ChronoUnit.DAYS);
        subscription.setCurrentPeriodStart(nextPeriodStart);
        subscription.setCurrentPeriodEnd(nextPeriodEnd);
        subscriptionRepository.save(subscription);

        return new RenewalResponse(
                savedRenewal.getId(),
                subscription.getId(),
                customer.getId(),
                periodStart,
                periodEnd,
                creditsToGrant,
                savedRenewal.getStatus(),
                grantResult.getBalanceAfter(),
                new InvoiceResponse(savedInvoice)
        );
    }
}
