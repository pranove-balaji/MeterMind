package com.example.MeterMind.catalog;

import com.example.MeterMind.identity.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/catalog")
@PreAuthorize("hasAnyRole('OWNER', 'BILLING_ADMIN')")
public class OwnerCatalogController {

    private final CatalogService catalogService;

    public OwnerCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> createPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreatePlanRequest request) {
        PlanResponse response = catalogService.createPlan(user.getOrganizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> listPlans(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<PlanResponse> plans = catalogService.getPlansForOrganization(user.getOrganizationId());
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<PlanResponse> getPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        PlanResponse plan = catalogService.getPlan(user.getOrganizationId(), id);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/plans/{id}/features")
    public ResponseEntity<FeaturePriceResponse> addFeaturePrice(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody CreateFeaturePriceRequest request) {
        FeaturePriceResponse response = catalogService.addFeaturePrice(user.getOrganizationId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/plans/{id}/features/{featureCode}")
    public ResponseEntity<FeaturePriceResponse> getActivePrice(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @PathVariable String featureCode) {
        FeaturePriceResponse response = catalogService.getActivePrice(user.getOrganizationId(), id, featureCode);
        return ResponseEntity.ok(response);
    }
}
