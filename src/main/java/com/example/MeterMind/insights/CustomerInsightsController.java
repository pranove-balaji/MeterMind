package com.example.MeterMind.insights;

import com.example.MeterMind.identity.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/insights")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerInsightsController {

    private final InsightsService insightsService;

    public CustomerInsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping
    public ResponseEntity<CustomerInsightsDto> getInsights(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "30") int windowDays) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        CustomerInsightsDto insights = insightsService.getCustomerInsights(
                user.getOrganizationId(),
                user.getCustomerId(),
                windowDays
        );
        return ResponseEntity.ok(insights);
    }
}
