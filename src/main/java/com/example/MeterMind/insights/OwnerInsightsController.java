package com.example.MeterMind.insights;

import com.example.MeterMind.identity.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner/customers/{customerId}/insights")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerInsightsController {

    private final InsightsService insightsService;

    public OwnerInsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping
    public ResponseEntity<CustomerInsightsDto> getCustomerInsights(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "30") int windowDays) {
        CustomerInsightsDto insights = insightsService.getCustomerInsights(
                user.getOrganizationId(),
                customerId,
                windowDays
        );
        return ResponseEntity.ok(insights);
    }
}
