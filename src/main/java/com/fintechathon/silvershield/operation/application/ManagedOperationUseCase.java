package com.fintechathon.silvershield.operation.application;

import com.fintechathon.silvershield.security.AssuranceLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManagedOperationUseCase {
    CardControlsView getCardControls(UUID userId, UUID cardId);
    PreparationResult prepareCardStatus(PreparationContext context, UUID cardId, String targetStatus, String reasonCode);
    PreparationResult prepareCardControls(PreparationContext context, UUID cardId, Boolean onlineEnabled,
            Boolean overseasEnabled, BigDecimal singleTxLimit, BigDecimal dailyLimit);
    SubscriptionCostAnalysis analyzeSubscriptions(UUID userId, List<UUID> subscriptionIds, int horizonMonths);
    PreparationResult prepareSubscriptionCancellation(PreparationContext context, UUID subscriptionId, String reasonCode);
    SuitabilityResult checkSuitability(UUID userId, UUID productId, UUID paymentAccountId,
            BigDecimal amount, String currency);
    List<PortfolioItem> portfolio(UUID userId);
    PreparationResult prepareWealthPurchase(PreparationContext context, UUID suitabilityCheckId,
            UUID productId, UUID paymentAccountId, BigDecimal amount, String currency);
    OperationView get(UUID userId, UUID operationId);
    ConfirmationResult confirm(UUID userId, AssuranceLevel assuranceLevel, UUID operationId,
            UUID challengeId, boolean confirmed);

    record PreparationContext(UUID requestId, UUID conversationId, UUID userId, String idempotencyKey) {}
    record PreparationResult(boolean accepted, String reasonCode, String explanation, OperationView operation) {}
    record OperationView(UUID operationId, String operationType, String status, AssuranceLevel requiredAal,
            Instant expiresAt, UUID challengeId, String confirmEndpoint, Object summary,
            UUID receiptId, String failureCode) {}
    record ConfirmationResult(String outcome, String reasonCode, String explanation, OperationView operation,
            OperationReceipt receipt, List<String> riskReasonCodes) {}
    record OperationReceipt(UUID receiptId, UUID operationId, String resultType, String status,
            Object details, Instant completedAt) {}
    record CardControlsView(UUID cardId, String cardMasked, String status, boolean onlineEnabled,
            boolean overseasEnabled, BigDecimal singleTxLimit, BigDecimal dailyLimit, long version) {}
    record SuitabilityResult(UUID checkId, boolean eligible, String reasonCode, String explanation,
            UUID productId, UUID paymentAccountId, BigDecimal amount, String currency,
            String customerRiskLevel, String productRiskLevel, Instant expiresAt) {}
    record PortfolioItem(UUID holdingId, UUID productId, String productCode, String productName,
            String riskLevel, BigDecimal investedAmount, String currency, String status) {}
    record SubscriptionCostAnalysis(int horizonMonths, BigDecimal monthlyEquivalent,
            BigDecimal annualizedCost, BigDecimal horizonCost, String currency,
            List<SubscriptionCostItem> items) {}
    record SubscriptionCostItem(UUID subscriptionId, String serviceName, String status,
            BigDecimal amount, String billingCycle, BigDecimal monthlyEquivalent,
            LocalDate nextChargeDate, boolean cancellable) {}
}
