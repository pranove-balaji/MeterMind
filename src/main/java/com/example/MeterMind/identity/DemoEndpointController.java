package com.example.MeterMind.identity;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoEndpointController {

    @GetMapping("/public/info")
    public ResponseEntity<Map<String, String>> getPublicInfo() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "MeterMind Billing Engine"
        ));
    }

    @GetMapping("/owner/summary")
    public ResponseEntity<Map<String, Object>> getOwnerSummary(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to the Owner Management Portal",
                "userId", user.getUserId(),
                "organizationId", user.getOrganizationId(),
                "role", user.getRole().name()
        ));
    }

    @GetMapping("/customer/profile")
    public ResponseEntity<Map<String, Object>> getCustomerProfile(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(Map.of(
                "message", "Welcome to the Customer Portal",
                "userId", user.getUserId(),
                "organizationId", user.getOrganizationId(),
                "customerId", user.getCustomerId() != null ? user.getCustomerId() : -1,
                "role", user.getRole().name()
        ));
    }
}
