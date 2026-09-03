package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.InvestmentOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentOrderRepository extends JpaRepository<InvestmentOrder, UUID> {
    Optional<InvestmentOrder> findByOperationId(UUID operationId);
}
