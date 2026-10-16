package com.example.MeterMind.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Public endpoints are accessible without authentication")
    void publicEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/public/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Protected owner endpoint denies unauthenticated requests with 401")
    void unauthenticatedRequestToOwnerEndpointFails() throws Exception {
        mockMvc.perform(get("/owner/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected customer endpoint denies unauthenticated requests with 401")
    void unauthenticatedRequestToCustomerEndpointFails() throws Exception {
        mockMvc.perform(get("/customer/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OWNER can access /owner/summary but is forbidden from /customer/profile")
    void ownerRoleAccessControl() throws Exception {
        // Access owner endpoint
        mockMvc.perform(get("/owner/summary")
                        .with(httpBasic("owner@acme.test", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.organizationId").exists());

        // Forbidden from customer endpoint
        mockMvc.perform(get("/customer/profile")
                        .with(httpBasic("owner@acme.test", "owner123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMER can access /customer/profile but is forbidden from /owner/summary")
    void customerRoleAccessControl() throws Exception {
        // Access customer endpoint
        mockMvc.perform(get("/customer/profile")
                        .with(httpBasic("customer@acme.test", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").value(1));

        // Forbidden from owner endpoint
        mockMvc.perform(get("/owner/summary")
                        .with(httpBasic("customer@acme.test", "customer123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid credentials fail with 401 Unauthorized")
    void invalidCredentialsFail() throws Exception {
        mockMvc.perform(get("/owner/summary")
                        .with(httpBasic("owner@acme.test", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Passwords must be stored hashed with BCrypt, never in plaintext")
    void passwordsAreHashedWithBCrypt() {
        AppUser owner = userRepository.findByEmail("owner@acme.test").orElseThrow();
        assertThat(owner.getPasswordHash()).startsWith("$2a$");
        assertThat(owner.getPasswordHash()).isNotEqualTo("owner123");
        assertThat(passwordEncoder.matches("owner123", owner.getPasswordHash())).isTrue();
    }
}
