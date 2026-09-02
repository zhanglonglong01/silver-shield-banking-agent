package com.fintechathon.silvershield.operation.repository;
import com.fintechathon.silvershield.operation.domain.RiskDecision; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RiskDecisionRepository extends JpaRepository<RiskDecision,UUID> {}
