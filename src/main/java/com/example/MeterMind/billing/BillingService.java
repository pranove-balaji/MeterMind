package com.example.MeterMind.billing;

import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingService(CustomerRepository customerRepository,
                          SubscriptionRepository subscriptionRepository,
                          InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse generateInvoicePreview(Long organizationId, Long customerId) {
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Subscription subscription = subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Customer has no active subscription to generate invoice preview against"));

        Plan plan = subscription.getPlan();
        Instant start = subscription.getCurrentPeriodStart();
        Instant end = subscription.getCurrentPeriodEnd();

        String invoiceNumber = "INV-PREVIEW-" + customer.getId() + "-" + start.toEpochMilli();

        BigDecimal basePrice = plan.getMonthlyPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = basePrice;
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        List<InvoiceLineResponse> lineResponses = new ArrayList<>();
        lineResponses.add(new InvoiceLineResponse(
                "Base Subscription: " + plan.getName() + " (" + start + " to " + end + ")",
                1L,
                basePrice,
                basePrice
        ));

        lineResponses.add(new InvoiceLineResponse(
                "Included Monthly Credits (" + plan.getMonthlyCredits() + " credits)",
                plan.getMonthlyCredits(),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        ));

        String notes = "SIMULATED PREVIEW — Draft only. No real payment or tax calculation has been executed.";

        return new InvoiceResponse(
                null,
                invoiceNumber,
                organizationId,
                customer.getId(),
                customer.getDisplayName(),
                subscription.getId(),
                InvoiceStatus.SIMULATED,
                start,
                end,
                "USD",
                subtotal,
                tax,
                total,
                true,
                notes,
                lineResponses,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getCustomerInvoices(Long organizationId, Long customerId) {
        customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        return invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(InvoiceResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long organizationId, Long customerId, Long invoiceId) {
        customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Invoice invoice = invoiceRepository.findByIdAndCustomerId(invoiceId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        return new InvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOrganizationInvoices(Long organizationId) {
        return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(InvoiceResponse::new)
                .collect(Collectors.toList());
    }
}
