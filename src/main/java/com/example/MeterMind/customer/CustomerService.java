package com.example.MeterMind.customer;

import com.example.MeterMind.common.DuplicateResourceException;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;

    public CustomerService(CustomerRepository customerRepository,
                           OrganizationRepository organizationRepository) {
        this.customerRepository = customerRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(Long organizationId, CreateCustomerRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        if (customerRepository.existsByOrganizationIdAndEmail(organizationId, request.getEmail())) {
            throw new DuplicateResourceException("Customer with email '" + request.getEmail() + "' already exists in this organization");
        }

        Customer customer = new Customer(
                organization,
                request.getDisplayName(),
                request.getEmail()
        );

        Customer saved = customerRepository.save(customer);
        return new CustomerResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomers(Long organizationId) {
        return customerRepository.findByOrganizationId(organizationId)
                .stream()
                .map(CustomerResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long organizationId, Long customerId) {
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        return new CustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntity(Long organizationId, Long customerId) {
        return customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }
}
