package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.SubscriptionChangeHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionChangeHistoryRepository extends JpaRepository<SubscriptionChangeHistory, UUID> {
    Optional<SubscriptionChangeHistory> findByOperationId(UUID operationId);
}
