package com.example.MeterMind.identity;

import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(OrganizationRepository organizationRepository,
                               AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial local demo organization and tenant users...");

            Organization acmeOrg = organizationRepository.save(new Organization("Acme Cloud AI"));

            // 1. Owner
            AppUser owner = new AppUser(
                    acmeOrg,
                    "owner@acme.test",
                    passwordEncoder.encode("owner123"),
                    Role.OWNER
            );
            userRepository.save(owner);

            // 2. Billing Admin
            AppUser billingAdmin = new AppUser(
                    acmeOrg,
                    "admin@acme.test",
                    passwordEncoder.encode("admin123"),
                    Role.BILLING_ADMIN
            );
            userRepository.save(billingAdmin);

            // 3. Customer
            AppUser customer = new AppUser(
                    acmeOrg,
                    "customer@acme.test",
                    passwordEncoder.encode("customer123"),
                    Role.CUSTOMER
            );
            customer.setCustomerId(1L); // Link to demo customer ID
            userRepository.save(customer);

            log.info("Demo users created for local testing:");
            log.info(" -> OWNER: owner@acme.test (password: owner123)");
            log.info(" -> ADMIN: admin@acme.test (password: admin123)");
            log.info(" -> CUSTOMER: customer@acme.test (password: customer123)");
        }
    }
}
