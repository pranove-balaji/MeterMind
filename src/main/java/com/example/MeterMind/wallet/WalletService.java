package com.example.MeterMind.wallet;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final CreditWalletRepository walletRepository;
    private final CreditLedgerEntryRepository ledgerRepository;
    private final CustomerService customerService;

    public WalletService(CreditWalletRepository walletRepository,
                         CreditLedgerEntryRepository ledgerRepository,
                         CustomerService customerService) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.customerService = customerService;
    }

    @Transactional
    public CreditWallet getOrCreateWalletEntity(Long organizationId, Long customerId) {
        Customer customer = customerService.getCustomerEntity(organizationId, customerId);

        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> walletRepository.save(new CreditWallet(customer, 0L)));
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long organizationId, Long customerId) {
        Customer customer = customerService.getCustomerEntity(organizationId, customerId);

        CreditWallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> new CreditWallet(customer, 0L));

        return new WalletResponse(wallet);
    }

    @Transactional
    public LedgerEntryResponse grantCredits(Long organizationId, Long customerId, Long actorUserId, GrantCreditsRequest request) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Grant amount must be strictly greater than 0");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Grant reason is required for auditability");
        }

        Customer customer = customerService.getCustomerEntity(organizationId, customerId);

        CreditWallet wallet = walletRepository.findByCustomerIdWithLock(customerId)
                .orElseGet(() -> {
                    CreditWallet newWallet = walletRepository.save(new CreditWallet(customer, 0L));
                    walletRepository.flush();
                    return newWallet;
                });

        long newBalance = Math.addExact(wallet.getBalance(), request.getAmount());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        String refType = request.getReferenceType() != null ? request.getReferenceType() : "MANUAL_GRANT";

        CreditLedgerEntry entry = new CreditLedgerEntry(
                wallet,
                LedgerEntryType.GRANT,
                request.getAmount(),
                newBalance,
                refType,
                request.getReferenceId(),
                actorUserId,
                request.getReason().trim()
        );

        CreditLedgerEntry savedEntry = ledgerRepository.save(entry);
        return new LedgerEntryResponse(savedEntry);
    }

    @Transactional
    public LedgerEntryResponse debitCredits(Long organizationId, Long customerId, Long actorUserId, DebitCreditsRequest request) {
        return debitCredits(
                organizationId,
                customerId,
                actorUserId,
                request.getAmount(),
                request.getReferenceType() != null ? request.getReferenceType() : "MANUAL_DEBIT",
                request.getReferenceId(),
                request.getReason()
        );
    }

    @Transactional
    public LedgerEntryResponse debitCredits(Long organizationId, Long customerId, Long actorUserId,
                                            long amount, String referenceType, String referenceId, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly greater than 0");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Debit reason is required for auditability");
        }

        Customer customer = customerService.getCustomerEntity(organizationId, customerId);

        CreditWallet wallet = walletRepository.findByCustomerIdWithLock(customerId)
                .orElseGet(() -> {
                    CreditWallet newWallet = walletRepository.save(new CreditWallet(customer, 0L));
                    walletRepository.flush();
                    return newWallet;
                });

        if (wallet.getBalance() < amount) {
            throw new InsufficientBalanceException(
                    "Insufficient credit balance. Available: " + wallet.getBalance() + ", Required: " + amount,
                    wallet.getBalance(),
                    amount
            );
        }

        long newBalance = Math.subtractExact(wallet.getBalance(), amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        String refType = referenceType != null ? referenceType : "MANUAL_DEBIT";

        CreditLedgerEntry entry = new CreditLedgerEntry(
                wallet,
                LedgerEntryType.DEBIT,
                amount,
                newBalance,
                refType,
                referenceId,
                actorUserId,
                reason.trim()
        );

        CreditLedgerEntry savedEntry = ledgerRepository.save(entry);
        return new LedgerEntryResponse(savedEntry);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedgerHistory(Long organizationId, Long customerId) {
        customerService.getCustomerEntity(organizationId, customerId);

        return walletRepository.findByCustomerId(customerId)
                .map(wallet -> ledgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                        .stream()
                        .map(LedgerEntryResponse::new)
                        .collect(Collectors.toList()))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public boolean reconcileBalance(Long organizationId, Long customerId) {
        customerService.getCustomerEntity(organizationId, customerId);

        CreditWallet wallet = walletRepository.findByCustomerId(customerId).orElse(null);
        if (wallet == null) {
            return true;
        }

        List<CreditLedgerEntry> entries = ledgerRepository.findByWalletId(wallet.getId());

        long computedBalance = 0L;
        for (CreditLedgerEntry entry : entries) {
            if (entry.getEntryType() == LedgerEntryType.GRANT || entry.getEntryType() == LedgerEntryType.ADJUSTMENT) {
                computedBalance = Math.addExact(computedBalance, entry.getAmount());
            } else if (entry.getEntryType() == LedgerEntryType.DEBIT) {
                computedBalance = Math.subtractExact(computedBalance, entry.getAmount());
            }
        }

        return computedBalance == wallet.getBalance();
    }
}
