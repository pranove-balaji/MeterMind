package com.example.MeterMind.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByOrganizationIdAndCustomerId(Long organizationId, Long customerId);
    Optional<Subscription> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Subscription> findByCustomerIdAndStatus(Long customerId, SubscriptionStatus status);
}
