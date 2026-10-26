package com.example.MeterMind.alerts;

import java.time.Instant;

public class NotificationResponse {

    private Long id;
    private Long organizationId;
    private Long customerId;
    private String customerDisplayName;
    private Long ruleId;
    private String eventKey;
    private String title;
    private String message;
    private Instant createdAt;
    private Instant readAt;
    private boolean read;

    public NotificationResponse() {
    }

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.organizationId = notification.getOrganization().getId();
        this.customerId = notification.getCustomer().getId();
        this.customerDisplayName = notification.getCustomer().getDisplayName();
        this.ruleId = notification.getRule() != null ? notification.getRule().getId() : null;
        this.eventKey = notification.getEventKey();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
        this.readAt = notification.getReadAt();
        this.read = notification.isRead();
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public Long getRuleId() {
        return ruleId;
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

    public boolean isRead() {
        return read;
    }
}
