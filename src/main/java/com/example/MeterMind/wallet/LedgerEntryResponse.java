package com.example.MeterMind.wallet;

import java.time.Instant;

public class LedgerEntryResponse {

    private Long id;
    private Long walletId;
    private LedgerEntryType entryType;
    private long amount;
    private long balanceAfter;
    private String referenceType;
    private String referenceId;
    private Long actorUserId;
    private String reason;
    private Instant createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(CreditLedgerEntry entry) {
        this.id = entry.getId();
        this.walletId = entry.getWallet().getId();
        this.entryType = entry.getEntryType();
        this.amount = entry.getAmount();
        this.balanceAfter = entry.getBalanceAfter();
        this.referenceType = entry.getReferenceType();
        this.referenceId = entry.getReferenceId();
        this.actorUserId = entry.getActorUserId();
        this.reason = entry.getReason();
        this.createdAt = entry.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
