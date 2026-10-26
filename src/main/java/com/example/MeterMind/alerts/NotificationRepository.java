package com.example.MeterMind.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByCustomerIdAndEventKey(Long customerId, String eventKey);

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Notification> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<Notification> findByIdAndCustomerId(Long id, Long customerId);

    long countByCustomerIdAndReadAtIsNull(Long customerId);
}
