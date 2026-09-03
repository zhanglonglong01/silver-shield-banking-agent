package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.CardChangeHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChangeHistoryRepository extends JpaRepository<CardChangeHistory, UUID> {
    Optional<CardChangeHistory> findByOperationId(UUID operationId);
}
