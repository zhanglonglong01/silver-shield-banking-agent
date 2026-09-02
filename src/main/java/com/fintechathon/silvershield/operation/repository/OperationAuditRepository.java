package com.fintechathon.silvershield.operation.repository;
import com.fintechathon.silvershield.operation.domain.OperationAudit; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OperationAuditRepository extends JpaRepository<OperationAudit,UUID> {}
