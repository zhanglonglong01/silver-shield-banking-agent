package com.fintechathon.silvershield.operation.application;

import com.fintechathon.silvershield.security.AssuranceLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransferOperationUseCase {
    PreparationResult prepare(PreparationContext context, TransferCommand command);
    OperationView get(UUID userId, UUID operationId);
    ConfirmationResult confirm(UUID userId, AssuranceLevel assuranceLevel, UUID operationId,
            UUID challengeId, boolean confirmed);

    record PreparationContext(UUID requestId, UUID conversationId, UUID userId, String idempotencyKey) {}
    record TransferCommand(UUID sourceAccountId, UUID payeeId, BigDecimal amount, String currency, String purpose) {}
    record PreparationResult(boolean accepted, String reasonCode, String explanation, OperationView operation) {}
    record OperationView(UUID operationId, String status, AssuranceLevel requiredAal, Instant expiresAt,
            UUID challengeId, String confirmEndpoint, Object summary, UUID receiptId, String failureCode) {}
    record ConfirmationResult(String outcome, String reasonCode, String explanation, OperationView operation,
            TransferReceipt receipt, List<String> riskReasonCodes) {}
    record TransferReceipt(UUID transferId, UUID operationId, String status, BigDecimal amount, String currency,
            BigDecimal sourceBalanceBefore, BigDecimal sourceBalanceAfter, Instant completedAt) {}
}
