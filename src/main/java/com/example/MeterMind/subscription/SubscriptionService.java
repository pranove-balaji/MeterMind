package com.example.MeterMind.subscription;

import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.catalog.PlanRepository;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final PlanRepository planRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               CustomerService customerService,
                               PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerService = customerService;
        this.planRepository = planRepository;
    }

    @Transactional
    public SubscriptionResponse createSubscription(Long organizationId, Long customerId, CreateSubscriptionRequest request) {
        // 1. Verify customer exists and belongs to the owner's organization
        Customer customer = customerService.getCustomerEntity(organizationId, customerId);

        // 2. Verify plan exists and belongs to the owner's organization (reject cross-tenant plan)
        Plan plan = planRepository.findByIdAndOrganizationId(request.getPlanId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        // 3. Validate billing period dates
        if (!request.getCurrentPeriodStart().isBefore(request.getCurrentPeriodEnd())) {
            throw new IllegalArgumentException("Current period start must be strictly before current period end");
        }

        // 4. If customer already has an active subscription, retire it
        subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> existing.setStatus(SubscriptionStatus.CANCELED));

        Subscription subscription = new Subscription(
                customer.getOrganization(),
                customer,
                plan,
                SubscriptionStatus.ACTIVE,
                request.getCurrentPeriodStart(),
                request.getCurrentPeriodEnd()
        );

        Subscription saved = subscriptionRepository.save(subscription);
        return new SubscriptionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsForCustomer(Long organizationId, Long customerId) {
        // Enforce customer ownership
        customerService.getCustomerEntity(organizationId, customerId);

        return subscriptionRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
                .stream()
                .map(SubscriptionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(Long organizationId, Long customerId) {
        customerService.getCustomerEntity(organizationId, customerId);

        Subscription subscription = subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for customer ID: " + customerId));
        return new SubscriptionResponse(subscription);
    }
}
