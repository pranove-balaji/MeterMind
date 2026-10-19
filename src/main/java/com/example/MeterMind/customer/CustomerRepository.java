package com.example.MeterMind.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByOrganizationId(Long organizationId);
    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByOrganizationIdAndEmail(Long organizationId, String email);
}
