package com.example.MeterMind.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateCustomerRequest {

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    public CreateCustomerRequest() {
    }

    public CreateCustomerRequest(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
