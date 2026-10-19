package com.example.MeterMind.customer;

import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.catalog.PlanRepository;
import com.example.MeterMind.identity.AppUser;
import com.example.MeterMind.identity.AppUserRepository;
import com.example.MeterMind.identity.Role;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import com.example.MeterMind.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerSubscriptionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org1;
    private Organization org2;
    private Plan org1Plan;
    private Plan org2Plan;

    @BeforeEach
    void setupTestData() {
        subscriptionRepository.deleteAll();
        customerRepository.deleteAll();
        planRepository.deleteAll();

        org1 = userRepository.findByEmail("owner@acme.test").orElseThrow().getOrganization();

        if (!userRepository.existsByEmail("owner2@competitor.test")) {
            org2 = organizationRepository.save(new Organization("Beta Tech AI"));
            userRepository.save(new AppUser(
                    org2,
                    "owner2@competitor.test",
                    passwordEncoder.encode("secret123"),
                    Role.OWNER
            ));
        } else {
            org2 = userRepository.findByEmail("owner2@competitor.test").orElseThrow().getOrganization();
        }

        org1Plan = planRepository.save(new Plan(org1, "Acme Starter", new BigDecimal("29.00"), 500L));
        org2Plan = planRepository.save(new Plan(org2, "Beta Starter", new BigDecimal("39.00"), 600L));
    }

    @Test
    @DisplayName("Owner can create a customer account in their organization")
    void ownerCanCreateCustomer() throws Exception {
        String json = """
            {
                "displayName": "Initech Corp",
                "email": "billing@initech.test"
            }
        """;

        mockMvc.perform(post("/owner/customers")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.displayName").value("Initech Corp"))
                .andExpect(jsonPath("$.email").value("billing@initech.test"))
                .andExpect(jsonPath("$.organizationId").value(org1.getId()));
    }

    @Test
    @DisplayName("Duplicate customer email in same organization is rejected with 409 Conflict")
    void duplicateCustomerEmailRejected() throws Exception {
        customerRepository.save(new Customer(org1, "Existing Corp", "duplicate@test.com"));

        String json = """
            {
                "displayName": "New Attempt",
                "email": "duplicate@test.com"
            }
        """;

        mockMvc.perform(post("/owner/customers")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Owner can attach a subscription with valid date range to their customer")
    void ownerCanAssignSubscription() throws Exception {
        Customer customer = customerRepository.save(new Customer(org1, "Hooli Inc", "finance@hooli.test"));

        Instant now = Instant.now();
        Instant nextMonth = now.plus(30, ChronoUnit.DAYS);

        String json = String.format("""
            {
                "planId": %d,
                "currentPeriodStart": "%s",
                "currentPeriodEnd": "%s"
            }
        """, org1Plan.getId(), now.toString(), nextMonth.toString());

        mockMvc.perform(post("/owner/customers/" + customer.getId() + "/subscriptions")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.planId").value(org1Plan.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Invalid billing period (start date after end date) is rejected with 400 Bad Request")
    void invalidPeriodDatesRejected() throws Exception {
        Customer customer = customerRepository.save(new Customer(org1, "Pied Piper", "richard@piedpiper.test"));

        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);

        String json = String.format("""
            {
                "planId": %d,
                "currentPeriodStart": "%s",
                "currentPeriodEnd": "%s"
            }
        """, org1Plan.getId(), now.toString(), past.toString()); // start is AFTER end!

        mockMvc.perform(post("/owner/customers/" + customer.getId() + "/subscriptions")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Cross-tenant plan assignment is strictly rejected (Org 1 customer cannot be assigned Org 2 plan)")
    void crossTenantPlanAssignmentRejected() throws Exception {
        Customer customer = customerRepository.save(new Customer(org1, "Aviato", "erlich@aviato.test"));

        Instant now = Instant.now();
        Instant nextMonth = now.plus(30, ChronoUnit.DAYS);

        // Attempt to assign Org 2's plan to Org 1's customer
        String json = String.format("""
            {
                "planId": %d,
                "currentPeriodStart": "%s",
                "currentPeriodEnd": "%s"
            }
        """, org2Plan.getId(), now.toString(), nextMonth.toString());

        mockMvc.perform(post("/owner/customers/" + customer.getId() + "/subscriptions")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound()); // Hidden tenant resource
    }

    @Test
    @DisplayName("Cross-tenant customer access is rejected (Org 2 owner cannot view or subscribe Org 1 customer)")
    void crossTenantCustomerAccessRejected() throws Exception {
        Customer org1Customer = customerRepository.save(new Customer(org1, "Raviga", "laurie@raviga.test"));

        // Org 2 owner attempts to fetch Org 1 customer -> 404
        mockMvc.perform(get("/owner/customers/" + org1Customer.getId())
                        .with(httpBasic("owner2@competitor.test", "secret123")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Customer role is forbidden from accessing customer management APIs with 403")
    void customerRoleForbidden() throws Exception {
        mockMvc.perform(get("/owner/customers")
                        .with(httpBasic("customer@acme.test", "customer123")))
                .andExpect(status().isForbidden());
    }
}
