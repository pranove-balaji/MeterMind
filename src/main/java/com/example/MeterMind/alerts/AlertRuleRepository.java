package com.example.MeterMind.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByOrganizationId(Long organizationId);

    List<AlertRule> findByOrganizationIdAndEnabledTrue(Long organizationId);

    Optional<AlertRule> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("SELECT r FROM AlertRule r WHERE r.organization.id = :organizationId AND r.enabled = true AND (r.customer IS NULL OR r.customer.id = :customerId)")
    List<AlertRule> findApplicableEnabledRules(@Param("organizationId") Long organizationId, @Param("customerId") Long customerId);
}
