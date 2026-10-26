package com.example.MeterMind.billing;

import com.example.MeterMind.identity.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer/invoices")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerInvoiceController {

    private final BillingService billingService;

    public CustomerInvoiceController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/preview")
    public ResponseEntity<InvoiceResponse> getInvoicePreview(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        InvoiceResponse preview = billingService.generateInvoicePreview(
                user.getOrganizationId(),
                user.getCustomerId()
        );
        return ResponseEntity.ok(preview);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoices(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        List<InvoiceResponse> invoices = billingService.getCustomerInvoices(
                user.getOrganizationId(),
                user.getCustomerId()
        );
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        InvoiceResponse invoice = billingService.getInvoiceById(
                user.getOrganizationId(),
                user.getCustomerId(),
                id
        );
        return ResponseEntity.ok(invoice);
    }
}
