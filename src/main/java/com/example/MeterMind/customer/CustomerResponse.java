package com.example.MeterMind.customer;

import java.time.Instant;

public class CustomerResponse {

    private Long id;
    private Long organizationId;
    private String displayName;
    private String email;
    private Long userId;
    private Instant createdAt;

    public CustomerResponse() {
    }

    public CustomerResponse(Customer customer) {
        this.id = customer.getId();
        this.organizationId = customer.getOrganization().getId();
        this.displayName = customer.getDisplayName();
        this.email = customer.getEmail();
        this.userId = customer.getUserId();
        this.createdAt = customer.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
