package com.fintechathon.silvershield.banking.repository;

import com.fintechathon.silvershield.banking.domain.CustomerRiskProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRiskProfileRepository extends JpaRepository<CustomerRiskProfile, UUID> {
    Optional<CustomerRiskProfile> findByUserId(UUID userId);
}
