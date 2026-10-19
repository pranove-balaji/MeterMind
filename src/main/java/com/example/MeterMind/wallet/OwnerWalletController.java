package com.example.MeterMind.wallet;

import com.example.MeterMind.identity.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/customers/{customerId}/wallet")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerWalletController {

    private final WalletService walletService;

    public OwnerWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId) {
        WalletResponse response = walletService.getWallet(user.getOrganizationId(), customerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/grants")
    public ResponseEntity<LedgerEntryResponse> grantCredits(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId,
            @Valid @RequestBody GrantCreditsRequest request) {
        LedgerEntryResponse response = walletService.grantCredits(
                user.getOrganizationId(),
                customerId,
                user.getUserId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<LedgerEntryResponse> debitCredits(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId,
            @Valid @RequestBody DebitCreditsRequest request) {
        LedgerEntryResponse response = walletService.debitCredits(
                user.getOrganizationId(),
                customerId,
                user.getUserId(),
                request
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedgerHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId) {
        List<LedgerEntryResponse> history = walletService.getLedgerHistory(user.getOrganizationId(), customerId);
        return ResponseEntity.ok(history);
    }
}
