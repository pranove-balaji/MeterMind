package com.example.MeterMind.alerts;

import com.example.MeterMind.identity.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer/notifications")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerNotificationController {

    private final AlertService alertService;

    public CustomerNotificationController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        List<NotificationResponse> notifications = alertService.getCustomerNotifications(user.getCustomerId());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        if (user.getCustomerId() == null) {
            throw new IllegalStateException("Authenticated user is not linked to any customer account");
        }
        NotificationResponse response = alertService.markNotificationAsRead(user.getCustomerId(), id);
        return ResponseEntity.ok(response);
    }
}
