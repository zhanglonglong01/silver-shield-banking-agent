package com.fintechathon.silvershield.audit.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ToolSecurityAuditRepository extends JpaRepository<ToolSecurityAuditEntity, UUID> {}
