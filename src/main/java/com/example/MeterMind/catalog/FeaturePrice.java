package com.example.MeterMind.catalog;

import com.example.MeterMind.organization.Organization;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feature_prices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_feature", columnNames = {"plan_id", "feature_code"})
})
public class FeaturePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "credits_per_unit", nullable = false)
    private long creditsPerUnit = 1L;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FeaturePrice() {
    }

    public FeaturePrice(Organization organization, Plan plan, String featureCode, long creditsPerUnit) {
        this.organization = organization;
        this.plan = plan;
        this.featureCode = featureCode;
        this.creditsPerUnit = creditsPerUnit;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public long getCreditsPerUnit() {
        return creditsPerUnit;
    }

    public void setCreditsPerUnit(long creditsPerUnit) {
        this.creditsPerUnit = creditsPerUnit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
