package com.example.MeterMind.billing;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.subscription.Subscription;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "subscription_renewals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_renewal_sub_period", columnNames = {"subscription_id", "period_start"})
        },
        indexes = {
                @Index(name = "idx_renewals_customer", columnList = "customer_id, created_at")
        }
)
public class SubscriptionRenewal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "credits_granted", nullable = false)
    private long creditsGranted;

    @Column(nullable = false, length = 50)
    private String status = "PROCESSED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SubscriptionRenewal() {
    }

    public SubscriptionRenewal(Organization organization, Subscription subscription, Customer customer,
                               Instant periodStart, Instant periodEnd, long creditsGranted, String status) {
        this.organization = organization;
        this.subscription = subscription;
        this.customer = customer;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.creditsGranted = creditsGranted;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public long getCreditsGranted() {
        return creditsGranted;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
