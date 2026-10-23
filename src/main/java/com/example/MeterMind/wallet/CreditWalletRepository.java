package com.example.MeterMind.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditWalletRepository extends JpaRepository<CreditWallet, Long> {
    Optional<CreditWallet> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CreditWallet w WHERE w.customer.id = :customerId")
    Optional<CreditWallet> findByCustomerIdWithLock(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM CreditWallet w WHERE w.customer.organization.id = :organizationId")
    Long sumBalanceByOrganizationId(@Param("organizationId") Long organizationId);
}
