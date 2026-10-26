package com.example.MeterMind.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Invoice> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Invoice> findByIdAndCustomerId(Long id, Long customerId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
