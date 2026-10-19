package com.example.MeterMind.catalog;

import com.example.MeterMind.identity.AppUser;
import com.example.MeterMind.identity.AppUserRepository;
import com.example.MeterMind.identity.Role;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization secondOrg;
    private AppUser secondOrgOwner;

    @BeforeEach
    void setupSecondTenant() {
        planRepository.deleteAll();
        if (!userRepository.existsByEmail("owner2@competitor.test")) {
            secondOrg = organizationRepository.save(new Organization("Beta Tech AI"));
            secondOrgOwner = userRepository.save(new AppUser(
                    secondOrg,
                    "owner2@competitor.test",
                    passwordEncoder.encode("secret123"),
                    Role.OWNER
            ));
        } else {
            secondOrgOwner = userRepository.findByEmail("owner2@competitor.test").orElseThrow();
            secondOrg = secondOrgOwner.getOrganization();
        }
    }

    @Test
    @DisplayName("Owner can create a plan and retrieve it successfully")
    void ownerCanCreatePlan() throws Exception {
        String planJson = """
            {
                "name": "Pro Startup",
                "monthlyPrice": 49.99,
                "monthlyCredits": 1000
            }
        """;

        mockMvc.perform(post("/owner/catalog/plans")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Pro Startup"))
                .andExpect(jsonPath("$.monthlyPrice").value(49.99))
                .andExpect(jsonPath("$.monthlyCredits").value(1000));
    }

    @Test
    @DisplayName("Negative monthly price or negative credits must be rejected with 400")
    void negativeValuesRejected() throws Exception {
        String invalidJson = """
            {
                "name": "Invalid Plan",
                "monthlyPrice": -10.00,
                "monthlyCredits": -50
            }
        """;

        mockMvc.perform(post("/owner/catalog/plans")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.monthlyPrice").exists())
                .andExpect(jsonPath("$.fieldErrors.monthlyCredits").exists());
    }

    @Test
    @DisplayName("Owner can add feature price to their plan and reject duplicates")
    void ownerCanAddFeaturePriceAndDuplicateIsRejected() throws Exception {
        // 1. Create plan
        Plan plan = planRepository.save(new Plan(
                userRepository.findByEmail("owner@acme.test").orElseThrow().getOrganization(),
                "Enterprise AI",
                new BigDecimal("199.00"),
                5000L
        ));

        // 2. Add Feature Price
        String featureJson = """
            {
                "featureCode": "TEXT_SUMMARY",
                "creditsPerUnit": 2
            }
        """;

        mockMvc.perform(post("/owner/catalog/plans/" + plan.getId() + "/features")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(featureJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureCode").value("TEXT_SUMMARY"))
                .andExpect(jsonPath("$.creditsPerUnit").value(2));

        // 3. Attempt duplicate feature code on same plan -> must return 409 Conflict
        mockMvc.perform(post("/owner/catalog/plans/" + plan.getId() + "/features")
                        .with(httpBasic("owner@acme.test", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(featureJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Tenant Isolation: Owner from Org 2 cannot view or modify Org 1's plan")
    void tenantIsolationEnforced() throws Exception {
        // Create plan in Org 1 (Acme)
        Plan acmePlan = planRepository.save(new Plan(
                userRepository.findByEmail("owner@acme.test").orElseThrow().getOrganization(),
                "Confidential Org1 Plan",
                new BigDecimal("99.00"),
                2000L
        ));

        // Owner 2 (Beta Tech) attempts to get Org 1's plan -> 404 (hidden resource)
        mockMvc.perform(get("/owner/catalog/plans/" + acmePlan.getId())
                        .with(httpBasic("owner2@competitor.test", "secret123")))
                .andExpect(status().isNotFound());

        // Owner 2 attempts to add feature to Org 1's plan -> 404
        String featureJson = """
            {
                "featureCode": "MALICIOUS_INJECT",
                "creditsPerUnit": 1
            }
        """;

        mockMvc.perform(post("/owner/catalog/plans/" + acmePlan.getId() + "/features")
                        .with(httpBasic("owner2@competitor.test", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(featureJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Customer role is forbidden from catalog management endpoints with 403")
    void customerRoleCannotManageCatalog() throws Exception {
        mockMvc.perform(get("/owner/catalog/plans")
                        .with(httpBasic("customer@acme.test", "customer123")))
                .andExpect(status().isForbidden());
    }
}
