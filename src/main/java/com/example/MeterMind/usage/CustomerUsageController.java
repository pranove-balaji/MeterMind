package com.example.MeterMind.usage;

import com.example.MeterMind.identity.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer/usage")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerUsageController {

    private final UsageService usageService;

    public CustomerUsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping
    public ResponseEntity<UsageEventResponse> recordUsage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestHeader(value = "Idempotency-Key", required = false) String headerIdempotencyKey,
            @Valid @RequestBody RecordUsageRequest request) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }

        UsageEventResponse response = usageService.recordUsage(
                user.getOrganizationId(),
                user.getCustomerId(),
                user.getUserId(),
                request,
                headerIdempotencyKey
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UsageEventResponse>> getUsageHistory(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }

        List<UsageEventResponse> history = usageService.getCustomerUsageHistory(user.getCustomerId());
        return ResponseEntity.ok(history);
    }
}
