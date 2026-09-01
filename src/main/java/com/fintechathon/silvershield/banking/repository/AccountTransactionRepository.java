package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.AccountTransaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    List<AccountTransaction> findByAccountIdAndBookingTimeBetweenOrderByBookingTimeDesc(
            UUID accountId, Instant from, Instant to, Pageable pageable);
}
