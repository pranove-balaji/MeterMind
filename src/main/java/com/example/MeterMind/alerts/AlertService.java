package com.example.MeterMind.alerts;

import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.organization.Organization;
import com.example.MeterMind.organization.OrganizationRepository;
import com.example.MeterMind.usage.UsageEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationRepository notificationRepository;
    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;

    public AlertService(AlertRuleRepository alertRuleRepository,
                        NotificationRepository notificationRepository,
                        OrganizationRepository organizationRepository,
                        CustomerRepository customerRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.notificationRepository = notificationRepository;
        this.organizationRepository = organizationRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public AlertRuleResponse createRule(Long organizationId, CreateAlertRuleRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        if (request.getThresholdValue() == null || request.getThresholdValue() < 0) {
            throw new IllegalArgumentException("Threshold value must be greater than or equal to 0");
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndOrganizationId(request.getCustomerId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));
        }

        AlertRule rule = new AlertRule(
                organization,
                customer,
                request.getRuleType(),
                request.getThresholdValue()
        );
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }

        AlertRule saved = alertRuleRepository.save(rule);
        return new AlertRuleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> getRulesForOrganization(Long organizationId) {
        return alertRuleRepository.findByOrganizationId(organizationId)
                .stream()
                .map(AlertRuleResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRule(Long organizationId, Long ruleId) {
        AlertRule rule = alertRuleRepository.findByIdAndOrganizationId(ruleId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));
        alertRuleRepository.delete(rule);
    }

    @Transactional
    public void evaluateAlertsOnUsage(Long organizationId, Long customerId, long balanceAfter, UsageEvent usageEvent) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return;
        }

        List<AlertRule> rules = alertRuleRepository.findApplicableEnabledRules(organizationId, customerId);

        for (AlertRule rule : rules) {
            if (rule.getRuleType() == AlertRuleType.LOW_BALANCE) {
                if (balanceAfter <= rule.getThresholdValue()) {
                    String eventKey = "RULE_" + rule.getId() + "_CUST_" + customerId + "_LOW_BAL_" + rule.getThresholdValue();
                    if (!notificationRepository.existsByCustomerIdAndEventKey(customerId, eventKey)) {
                        Notification n = new Notification(
                                customer.getOrganization(),
                                customer,
                                rule,
                                eventKey,
                                "Low Credit Balance Alert",
                                "Your credit balance has fallen to " + balanceAfter + " credits (threshold: " + rule.getThresholdValue() + ")."
                        );
                        notificationRepository.save(n);
                    }
                }
            } else if (rule.getRuleType() == AlertRuleType.ZERO_BALANCE) {
                if (balanceAfter == 0) {
                    String eventKey = "RULE_" + rule.getId() + "_CUST_" + customerId + "_ZERO_BAL";
                    if (!notificationRepository.existsByCustomerIdAndEventKey(customerId, eventKey)) {
                        Notification n = new Notification(
                                customer.getOrganization(),
                                customer,
                                rule,
                                eventKey,
                                "Zero Credit Balance Alert",
                                "Your credit balance is exhausted (0 credits). Further billable usage will be blocked until more credits are added."
                        );
                        notificationRepository.save(n);
                    }
                }
            } else if (rule.getRuleType() == AlertRuleType.USAGE_SPIKE) {
                if (usageEvent != null && usageEvent.getCreditsCharged() >= rule.getThresholdValue()) {
                    String eventKey = "RULE_" + rule.getId() + "_SPIKE_EVENT_" + usageEvent.getId();
                    if (!notificationRepository.existsByCustomerIdAndEventKey(customerId, eventKey)) {
                        Notification n = new Notification(
                                customer.getOrganization(),
                                customer,
                                rule,
                                eventKey,
                                "Usage Spike Detected",
                                "Single usage charge of " + usageEvent.getCreditsCharged() + " credits was processed for " + usageEvent.getFeatureCode() + " (spike threshold: " + rule.getThresholdValue() + ")."
                        );
                        notificationRepository.save(n);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getCustomerNotifications(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(Long customerId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndCustomerId(notificationId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setReadAt(Instant.now());
        Notification saved = notificationRepository.save(notification);
        return new NotificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getOwnerNotifications(Long organizationId) {
        return notificationRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }
}
