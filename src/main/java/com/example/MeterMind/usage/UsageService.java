package com.example.MeterMind.usage;

import com.example.MeterMind.catalog.FeaturePrice;
import com.example.MeterMind.catalog.FeaturePriceRepository;
import com.example.MeterMind.catalog.Plan;
import com.example.MeterMind.common.ResourceNotFoundException;
import com.example.MeterMind.customer.Customer;
import com.example.MeterMind.customer.CustomerRepository;
import com.example.MeterMind.subscription.Subscription;
import com.example.MeterMind.subscription.SubscriptionRepository;
import com.example.MeterMind.subscription.SubscriptionStatus;
import com.example.MeterMind.wallet.CreditWallet;
import com.example.MeterMind.wallet.CreditWalletRepository;
import com.example.MeterMind.wallet.LedgerEntryResponse;
import com.example.MeterMind.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private final UsageEventRepository usageEventRepository;
    private final CreditWalletRepository walletRepository;
    private final WalletService walletService;
    private final SubscriptionRepository subscriptionRepository;
    private final FeaturePriceRepository featurePriceRepository;
    private final CustomerRepository customerRepository;

    public UsageService(UsageEventRepository usageEventRepository,
                        CreditWalletRepository walletRepository,
                        WalletService walletService,
                        SubscriptionRepository subscriptionRepository,
                        FeaturePriceRepository featurePriceRepository,
                        CustomerRepository customerRepository) {
        this.usageEventRepository = usageEventRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.subscriptionRepository = subscriptionRepository;
        this.featurePriceRepository = featurePriceRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public UsageEventResponse recordUsage(Long organizationId, Long customerId, Long actorUserId,
                                          RecordUsageRequest request, String headerIdempotencyKey) {
        String idempotencyKey = (headerIdempotencyKey != null && !headerIdempotencyKey.trim().isEmpty())
                ? headerIdempotencyKey.trim()
                : request.getIdempotencyKey();

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required via 'Idempotency-Key' header or request body");
        }
        idempotencyKey = idempotencyKey.trim();

        if (request.getUnits() == null || request.getUnits() <= 0) {
            throw new IllegalArgumentException("Usage units must be strictly positive (at least 1)");
        }
        if (request.getFeatureCode() == null || request.getFeatureCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Feature code is required");
        }

        String featureCode = request.getFeatureCode().trim();
        long units = request.getUnits();
        String requestHash = computeRequestHash(featureCode, units);

        // 1. Acquire pessimistic lock on the customer's wallet row to serialize concurrent debits
        CreditWallet wallet = walletRepository.findByCustomerIdWithLock(customerId)
                .orElseGet(() -> {
                    Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
                    CreditWallet newWallet = walletRepository.save(new CreditWallet(customer, 0L));
                    walletRepository.flush();
                    return newWallet;
                });

        // 2. Check for existing idempotency key under this customer
        Optional<UsageEvent> existingOpt = usageEventRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (existingOpt.isPresent()) {
            UsageEvent existing = existingOpt.get();
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey + "' has already been used with a different request payload"
                );
            }
            // Idempotent replay: return existing event details without charging credits again
            return new UsageEventResponse(existing, wallet.getBalance(), true);
        }

        // 3. Resolve customer's active subscription
        Subscription subscription = subscriptionRepository.findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Customer has no active subscription to meter usage against"));

        if (!subscription.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Subscription not found for customer organization");
        }

        // 4. Resolve server-side pricing from the plan
        Plan plan = subscription.getPlan();
        FeaturePrice featurePrice = featurePriceRepository.findByPlanIdAndFeatureCodeAndActiveTrue(plan.getId(), featureCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Feature code '" + featureCode + "' is not active or priced in customer active plan '" + plan.getName() + "'"
                ));

        long unitCost = featurePrice.getCreditsPerUnit();
        long creditsCharged = Math.multiplyExact(unitCost, units);

        // 5. Debit wallet balance atomically (rolls back if insufficient balance)
        LedgerEntryResponse debitResponse = walletService.debitCredits(
                organizationId,
                customerId,
                actorUserId,
                creditsCharged,
                "USAGE_EVENT",
                idempotencyKey,
                "Usage: " + units + " x " + featureCode + " (" + unitCost + " cr/unit)"
        );

        // 6. Save usage event price snapshot
        UsageEvent event = new UsageEvent(
                subscription.getOrganization(),
                subscription.getCustomer(),
                subscription,
                featureCode,
                units,
                unitCost,
                creditsCharged,
                idempotencyKey,
                requestHash
        );

        UsageEvent saved = usageEventRepository.save(event);
        return new UsageEventResponse(saved, debitResponse.getBalanceAfter(), false);
    }

    @Transactional(readOnly = true)
    public List<UsageEventResponse> getCustomerUsageHistory(Long customerId) {
        long currentBalance = walletRepository.findByCustomerId(customerId)
                .map(CreditWallet::getBalance)
                .orElse(0L);

        return usageEventRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(e -> new UsageEventResponse(e, currentBalance, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UsageEventResponse> getOwnerCustomerUsageHistory(Long organizationId, Long customerId) {
        customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        return getCustomerUsageHistory(customerId);
    }

    public static String computeRequestHash(String featureCode, long units) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = featureCode.trim().toUpperCase() + ":" + units;
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }
}
