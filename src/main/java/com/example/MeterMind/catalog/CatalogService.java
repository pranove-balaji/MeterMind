package com.example.MeterMind.catalog;

import com.example.MeterMind.common.DuplicateResourceException;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final PlanRepository planRepository;
    private final FeaturePriceRepository featurePriceRepository;
    private final OrganizationRepository organizationRepository;

    public CatalogService(PlanRepository planRepository,
                          FeaturePriceRepository featurePriceRepository,
                          OrganizationRepository organizationRepository) {
        this.planRepository = planRepository;
        this.featurePriceRepository = featurePriceRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public PlanResponse createPlan(Long organizationId, CreatePlanRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        if (planRepository.existsByOrganizationIdAndName(organizationId, request.getName())) {
            throw new DuplicateResourceException("Plan with name '" + request.getName() + "' already exists in this organization");
        }

        Plan plan = new Plan(
                organization,
                request.getName(),
                request.getMonthlyPrice(),
                request.getMonthlyCredits()
        );

        Plan savedPlan = planRepository.save(plan);
        return new PlanResponse(savedPlan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getPlansForOrganization(Long organizationId) {
        return planRepository.findByOrganizationId(organizationId)
                .stream()
                .map(PlanResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlan(Long organizationId, Long planId) {
        Plan plan = planRepository.findByIdAndOrganizationId(planId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));
        return new PlanResponse(plan);
    }

    @Transactional
    public FeaturePriceResponse addFeaturePrice(Long organizationId, Long planId, CreateFeaturePriceRequest request) {
        Plan plan = planRepository.findByIdAndOrganizationId(planId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        if (featurePriceRepository.existsByPlanIdAndFeatureCode(planId, request.getFeatureCode())) {
            throw new DuplicateResourceException("Feature '" + request.getFeatureCode() + "' is already configured for plan: " + plan.getName());
        }

        FeaturePrice featurePrice = new FeaturePrice(
                plan.getOrganization(),
                plan,
                request.getFeatureCode(),
                request.getCreditsPerUnit()
        );

        FeaturePrice saved = featurePriceRepository.save(featurePrice);
        return new FeaturePriceResponse(saved);
    }

    @Transactional(readOnly = true)
    public FeaturePriceResponse getActivePrice(Long organizationId, Long planId, String featureCode) {
        // Enforce tenant ownership of plan
        planRepository.findByIdAndOrganizationId(planId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        FeaturePrice featurePrice = featurePriceRepository.findByPlanIdAndFeatureCodeAndActiveTrue(planId, featureCode)
                .orElseThrow(() -> new ResourceNotFoundException("No active price configured for feature code: " + featureCode));

        return new FeaturePriceResponse(featurePrice);
    }
}
