package com.example.MeterMind.billing;

import java.math.BigDecimal;

public class InvoiceLineResponse {

    private Long id;
    private String description;
    private long quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;

    public InvoiceLineResponse() {
    }

    public InvoiceLineResponse(InvoiceLine line) {
        this.id = line.getId();
        this.description = line.getDescription();
        this.quantity = line.getQuantity();
        this.unitPrice = line.getUnitPrice();
        this.amount = line.getAmount();
    }

    public InvoiceLineResponse(String description, long quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
