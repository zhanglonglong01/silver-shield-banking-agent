package com.fintechathon.silvershield.operation.application;

import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import com.fintechathon.silvershield.operation.domain.OperationRequest;
import com.fintechathon.silvershield.operation.repository.OperationRequestRepository;
import com.fintechathon.silvershield.security.AssuranceLevel;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecureOperationCommandRouter implements OperationCommandRouter {
    private static final Set<String> MANAGED_TOOLS = Set.of(
            "prepare_card_status_change", "prepare_card_controls_change",
            "prepare_subscription_cancellation", "prepare_wealth_purchase");

    private final OperationRequestRepository operations;
    private final TransferOperationUseCase transfers;
    private final ManagedOperationUseCase managedOperations;

    public SecureOperationCommandRouter(OperationRequestRepository operations,
            TransferOperationUseCase transfers, ManagedOperationUseCase managedOperations) {
        this.operations = operations;
        this.transfers = transfers;
        this.managedOperations = managedOperations;
    }

    @Override
    public Object get(UUID userId, UUID operationId) {
        OperationRequest operation = operations.findByIdAndUserId(operationId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        return operation.getToolName().equals("prepare_transfer")
                ? transfers.get(userId, operationId)
                : managedOperations.get(userId, operationId);
    }

    @Override
    @Transactional
    public Object confirm(UUID userId, AssuranceLevel assuranceLevel, UUID operationId,
            UUID challengeId, boolean confirmed) {
        OperationRequest operation = operations.findByIdAndUserId(operationId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        if (operation.getToolName().equals("prepare_transfer")) {
            return transfers.confirm(userId, assuranceLevel, operationId, challengeId, confirmed);
        }
        if (MANAGED_TOOLS.contains(operation.getToolName())) {
            return managedOperations.confirm(userId, assuranceLevel, operationId, challengeId, confirmed);
        }
        throw new ResourceNotFoundException();
    }
}
