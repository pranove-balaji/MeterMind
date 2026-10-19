package com.example.MeterMind.wallet;

import java.time.Instant;

public class WalletResponse {

    private Long id;
    private Long customerId;
    private long balance;
    private Instant updatedAt;

    public WalletResponse() {
    }

    public WalletResponse(CreditWallet wallet) {
        this.id = wallet.getId();
        this.customerId = wallet.getCustomer().getId();
        this.balance = wallet.getBalance();
        this.updatedAt = wallet.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public long getBalance() {
        return balance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
