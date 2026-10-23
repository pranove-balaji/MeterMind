package com.example.MeterMind.usage;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.subscription.Subscription;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "usage_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usage_customer_idempotency", columnNames = {"customer_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_usage_customer_created", columnList = "customer_id, created_at"),
                @Index(name = "idx_usage_org_created", columnList = "organization_id, created_at")
        }
)
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(nullable = false)
    private long units;

    @Column(name = "unit_credit_cost", nullable = false)
    private long unitCreditCost;

    @Column(name = "credits_charged", nullable = false)
    private long creditsCharged;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UsageEvent() {
    }

    public UsageEvent(Organization organization, Customer customer, Subscription subscription,
                      String featureCode, long units, long unitCreditCost,
                      long creditsCharged, String idempotencyKey, String requestHash) {
        this.organization = organization;
        this.customer = customer;
        this.subscription = subscription;
        this.featureCode = featureCode;
        this.units = units;
        this.unitCreditCost = unitCreditCost;
        this.creditsCharged = creditsCharged;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public long getUnits() {
        return units;
    }

    public long getUnitCreditCost() {
        return unitCreditCost;
    }

    public long getCreditsCharged() {
        return creditsCharged;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
