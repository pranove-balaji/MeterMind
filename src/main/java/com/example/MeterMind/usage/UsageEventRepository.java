package com.example.MeterMind.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    Optional<UsageEvent> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);

    List<UsageEvent> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<UsageEvent> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    long countByCustomerId(Long customerId);

    long countByOrganizationId(Long organizationId);

    List<UsageEvent> findTop10ByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<UsageEvent> findTop10ByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<UsageEvent> findByCustomerIdAndCreatedAtAfterOrderByCreatedAtDesc(Long customerId, java.time.Instant after);
}
