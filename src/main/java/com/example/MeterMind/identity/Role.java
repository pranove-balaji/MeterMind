package com.example.MeterMind.identity;

public enum Role {
    OWNER,
    BILLING_ADMIN,
    CUSTOMER;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
