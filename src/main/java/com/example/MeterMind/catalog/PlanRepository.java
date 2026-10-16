package com.example.MeterMind.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByOrganizationId(Long organizationId);
    Optional<Plan> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
