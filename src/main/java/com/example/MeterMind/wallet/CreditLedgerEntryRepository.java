package com.example.MeterMind.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLedgerEntryRepository extends JpaRepository<CreditLedgerEntry, Long> {
    List<CreditLedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<CreditLedgerEntry> findByWalletId(Long walletId);
}
