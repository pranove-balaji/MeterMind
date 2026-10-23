package com.example.MeterMind.wallet;

import com.example.MeterMind.identity.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/wallet")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerWalletController {

    private final WalletService walletService;

    public CustomerWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal CustomUserDetails user) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        WalletResponse response = walletService.getWallet(user.getOrganizationId(), user.getCustomerId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getLedgerHistory(@AuthenticationPrincipal CustomUserDetails user) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        List<LedgerEntryResponse> history = walletService.getLedgerHistory(user.getOrganizationId(), user.getCustomerId());
        return ResponseEntity.ok(history);
    }
}
