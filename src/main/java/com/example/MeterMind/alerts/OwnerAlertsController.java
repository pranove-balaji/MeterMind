package com.example.MeterMind.alerts;

import com.example.MeterMind.identity.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/alerts")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerAlertsController {

    private final AlertService alertService;

    public OwnerAlertsController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/rules")
    public ResponseEntity<AlertRuleResponse> createRule(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateAlertRuleRequest request) {
        AlertRuleResponse response = alertService.createRule(user.getOrganizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRuleResponse>> listRules(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<AlertRuleResponse> rules = alertService.getRulesForOrganization(user.getOrganizationId());
        return ResponseEntity.ok(rules);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        alertService.deleteRule(user.getOrganizationId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<NotificationResponse> notifications = alertService.getOwnerNotifications(user.getOrganizationId());
        return ResponseEntity.ok(notifications);
    }
}
