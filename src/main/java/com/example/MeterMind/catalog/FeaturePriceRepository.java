package com.example.MeterMind.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeaturePriceRepository extends JpaRepository<FeaturePrice, Long> {
    List<FeaturePrice> findByPlanId(Long planId);
    Optional<FeaturePrice> findByPlanIdAndFeatureCodeAndActiveTrue(Long planId, String featureCode);
    boolean existsByPlanIdAndFeatureCode(Long planId, String featureCode);
}
