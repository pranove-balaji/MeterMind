package com.example.MeterMind.billing;

import com.example.MeterMind.identity.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerBillingController {

    private final BillingService billingService;
    private final RenewalService renewalService;

    public OwnerBillingController(BillingService billingService, RenewalService renewalService) {
        this.billingService = billingService;
        this.renewalService = renewalService;
    }

    @GetMapping("/customers/{customerId}/invoices/preview")
    public ResponseEntity<InvoiceResponse> getInvoicePreview(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId) {
        InvoiceResponse preview = billingService.generateInvoicePreview(user.getOrganizationId(), customerId);
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/customers/{customerId}/invoices")
    public ResponseEntity<List<InvoiceResponse>> getCustomerInvoices(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId) {
        List<InvoiceResponse> invoices = billingService.getCustomerInvoices(user.getOrganizationId(), customerId);
        return ResponseEntity.ok(invoices);
    }

    @PostMapping("/customers/{customerId}/renew")
    public ResponseEntity<RenewalResponse> simulateRenewal(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId) {
        RenewalResponse response = renewalService.simulateRenewal(
                user.getOrganizationId(),
                customerId,
                user.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> getOrganizationInvoices(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<InvoiceResponse> invoices = billingService.getOrganizationInvoices(user.getOrganizationId());
        return ResponseEntity.ok(invoices);
    }
}
