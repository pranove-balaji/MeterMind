package com.example.MeterMind.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long organizationId;
    private Long customerId;
    private String customerName;
    private Long subscriptionId;
    private InvoiceStatus status;
    private Instant periodStart;
    private Instant periodEnd;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private boolean simulated;
    private String notes;
    private List<InvoiceLineResponse> lines;
    private Instant createdAt;

    public InvoiceResponse() {
    }

    public InvoiceResponse(Invoice invoice) {
        this.id = invoice.getId();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.organizationId = invoice.getOrganization().getId();
        this.customerId = invoice.getCustomer().getId();
        this.customerName = invoice.getCustomer().getDisplayName();
        this.subscriptionId = invoice.getSubscription() != null ? invoice.getSubscription().getId() : null;
        this.status = invoice.getStatus();
        this.periodStart = invoice.getPeriodStart();
        this.periodEnd = invoice.getPeriodEnd();
        this.currency = invoice.getCurrency();
        this.subtotal = invoice.getSubtotal();
        this.tax = invoice.getTax();
        this.total = invoice.getTotal();
        this.simulated = invoice.isSimulated();
        this.notes = invoice.getNotes();
        this.lines = invoice.getLines().stream()
                .map(InvoiceLineResponse::new)
                .collect(Collectors.toList());
        this.createdAt = invoice.getCreatedAt();
    }

    public InvoiceResponse(Long id, String invoiceNumber, Long organizationId, Long customerId,
                           String customerName, Long subscriptionId, InvoiceStatus status,
                           Instant periodStart, Instant periodEnd, String currency,
                           BigDecimal subtotal, BigDecimal tax, BigDecimal total,
                           boolean simulated, String notes, List<InvoiceLineResponse> lines,
                           Instant createdAt) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.subscriptionId = subscriptionId;
        this.status = status;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.currency = currency;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
        this.simulated = simulated;
        this.notes = notes;
        this.lines = lines;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public String getNotes() {
        return notes;
    }

    public List<InvoiceLineResponse> getLines() {
        return lines;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
