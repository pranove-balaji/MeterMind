package com.example.MeterMind.billing;

import java.time.Instant;

public class RenewalResponse {

    private Long renewalId;
    private Long subscriptionId;
    private Long customerId;
    private Instant periodStart;
    private Instant periodEnd;
    private long creditsGranted;
    private String status;
    private long balanceAfter;
    private InvoiceResponse invoice;

    public RenewalResponse() {
    }

    public RenewalResponse(Long renewalId, Long subscriptionId, Long customerId,
                           Instant periodStart, Instant periodEnd, long creditsGranted,
                           String status, long balanceAfter, InvoiceResponse invoice) {
        this.renewalId = renewalId;
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.creditsGranted = creditsGranted;
        this.status = status;
        this.balanceAfter = balanceAfter;
        this.invoice = invoice;
    }

    public Long getRenewalId() {
        return renewalId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public long getCreditsGranted() {
        return creditsGranted;
    }

    public String getStatus() {
        return status;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public InvoiceResponse getInvoice() {
        return invoice;
    }
}
