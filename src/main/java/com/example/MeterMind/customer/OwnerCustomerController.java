package com.example.MeterMind.customer;

import com.example.MeterMind.identity.CustomUserDetails;
import com.example.MeterMind.subscription.CreateSubscriptionRequest;
import com.example.MeterMind.subscription.SubscriptionResponse;
import com.example.MeterMind.subscription.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/customers")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerCustomerController {

    private final CustomerService customerService;
    private final SubscriptionService subscriptionService;

    public OwnerCustomerController(CustomerService customerService,
                                   SubscriptionService subscriptionService) {
        this.customerService = customerService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(user.getOrganizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> listCustomers(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<CustomerResponse> customers = customerService.getCustomers(user.getOrganizationId());
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        CustomerResponse customer = customerService.getCustomer(user.getOrganizationId(), id);
        return ResponseEntity.ok(customer);
    }

    @PostMapping("/{id}/subscriptions")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(user.getOrganizationId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> listSubscriptions(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionsForCustomer(user.getOrganizationId(), id);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/{id}/subscriptions/active")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        SubscriptionResponse response = subscriptionService.getActiveSubscription(user.getOrganizationId(), id);
        return ResponseEntity.ok(response);
    }
}
