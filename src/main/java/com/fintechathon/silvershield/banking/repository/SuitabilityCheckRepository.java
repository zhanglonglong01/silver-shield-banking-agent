package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.SuitabilityCheck;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuitabilityCheckRepository extends JpaRepository<SuitabilityCheck, UUID> {
    Optional<SuitabilityCheck> findByIdAndUserId(UUID id, UUID userId);
}
