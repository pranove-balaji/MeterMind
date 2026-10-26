package com.example.MeterMind.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRenewalRepository extends JpaRepository<SubscriptionRenewal, Long> {

    boolean existsBySubscriptionIdAndPeriodStart(Long subscriptionId, Instant periodStart);

    Optional<SubscriptionRenewal> findBySubscriptionIdAndPeriodStart(Long subscriptionId, Instant periodStart);

    List<SubscriptionRenewal> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
