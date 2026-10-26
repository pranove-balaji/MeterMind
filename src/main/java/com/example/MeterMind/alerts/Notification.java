package com.example.MeterMind.alerts;

import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.organization.Organization;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_customer_event", columnNames = {"customer_id", "event_key"})
        },
        indexes = {
                @Index(name = "idx_notifications_customer_created", columnList = "customer_id, created_at"),
                @Index(name = "idx_notifications_org_created", columnList = "organization_id, created_at")
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AlertRule rule;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;

    public Notification() {
    }

    public Notification(Organization organization, Customer customer, AlertRule rule,
                        String eventKey, String title, String message) {
        this.organization = organization;
        this.customer = customer;
        this.rule = rule;
        this.eventKey = eventKey;
        this.title = title;
        this.message = message;
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

    public AlertRule getRule() {
        return rule;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }
}
