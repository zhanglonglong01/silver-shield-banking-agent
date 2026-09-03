package com.fintechathon.silvershield.operation.application;

import com.fintechathon.silvershield.security.AssuranceLevel;
import java.util.UUID;

public interface OperationCommandRouter {
    Object get(UUID userId, UUID operationId);
    Object confirm(UUID userId, AssuranceLevel assuranceLevel, UUID operationId,
            UUID challengeId, boolean confirmed);
}
