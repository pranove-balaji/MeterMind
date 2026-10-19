package com.example.MeterMind.wallet;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "credit_ledger_entries", indexes = {
        @Index(name = "idx_ledger_wallet_created", columnList = "wallet_id, created_at")
})
public class CreditLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CreditWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 50)
    private LedgerEntryType entryType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CreditLedgerEntry() {
    }

    public CreditLedgerEntry(CreditWallet wallet, LedgerEntryType entryType, long amount,
                             long balanceAfter, String referenceType, String referenceId,
                             Long actorUserId, String reason) {
        this.wallet = wallet;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.actorUserId = actorUserId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CreditWallet getWallet() {
        return wallet;
    }

    public void setWallet(CreditWallet wallet) {
        this.wallet = wallet;
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
