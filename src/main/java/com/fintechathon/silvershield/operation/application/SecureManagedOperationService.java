package com.fintechathon.silvershield.operation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.banking.domain.*;
import com.fintechathon.silvershield.banking.repository.*;
import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import com.fintechathon.silvershield.config.TransferSecurityProperties;
import com.fintechathon.silvershield.config.WealthSecurityProperties;
import com.fintechathon.silvershield.identity.repository.AppUserRepository;
import com.fintechathon.silvershield.operation.domain.*;
import com.fintechathon.silvershield.operation.repository.*;
import com.fintechathon.silvershield.security.AssuranceLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecureManagedOperationService implements ManagedOperationUseCase {
    private static final String POLICY_VERSION = "m5-managed-operations-v1";
    private static final BigDecimal MAX_LIMIT = new BigDecimal("1000000.00");
    private static final Set<String> CARD_REASON_CODES = Set.of(
            "USER_REQUEST", "SUSPECTED_FRAUD", "CARD_LOST", "SECURITY_CONTROL");
    private static final Set<String> SUBSCRIPTION_REASON_CODES = Set.of(
            "USER_REQUEST", "COST_SAVING", "NOT_USED", "OTHER");

    private final OperationRequestRepository operations;
    private final ConfirmationChallengeRepository challenges;
    private final RiskDecisionRepository risks;
    private final IdempotencyRecordRepository idempotencies;
    private final OperationAuditRepository audits;
    private final BankCardRepository cards;
    private final CardChangeHistoryRepository cardChanges;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionChangeHistoryRepository subscriptionChanges;
    private final WealthProductRepository products;
    private final CustomerRiskProfileRepository riskProfiles;
    private final SuitabilityCheckRepository suitabilityChecks;
    private final InvestmentOrderRepository investmentOrders;
    private final WealthHoldingRepository holdings;
    private final AccountRepository accounts;
    private final AccountTransactionRepository transactions;
    private final AppUserRepository users;
    private final TransferSecurityProperties operationProperties;
    private final WealthSecurityProperties wealthProperties;
    private final ObjectMapper mapper;
    private final Clock clock;

    public SecureManagedOperationService(OperationRequestRepository operations,
            ConfirmationChallengeRepository challenges, RiskDecisionRepository risks,
            IdempotencyRecordRepository idempotencies, OperationAuditRepository audits,
            BankCardRepository cards, CardChangeHistoryRepository cardChanges,
            SubscriptionRepository subscriptions, SubscriptionChangeHistoryRepository subscriptionChanges,
            WealthProductRepository products, CustomerRiskProfileRepository riskProfiles,
            SuitabilityCheckRepository suitabilityChecks, InvestmentOrderRepository investmentOrders,
            WealthHoldingRepository holdings, AccountRepository accounts,
            AccountTransactionRepository transactions, AppUserRepository users,
            TransferSecurityProperties operationProperties, WealthSecurityProperties wealthProperties,
            ObjectMapper mapper, Clock clock) {
        this.operations = operations; this.challenges = challenges; this.risks = risks;
        this.idempotencies = idempotencies; this.audits = audits; this.cards = cards;
        this.cardChanges = cardChanges; this.subscriptions = subscriptions;
        this.subscriptionChanges = subscriptionChanges; this.products = products;
        this.riskProfiles = riskProfiles; this.suitabilityChecks = suitabilityChecks;
        this.investmentOrders = investmentOrders; this.holdings = holdings; this.accounts = accounts;
        this.transactions = transactions; this.users = users; this.operationProperties = operationProperties;
        this.wealthProperties = wealthProperties; this.mapper = mapper; this.clock = clock;
        if (wealthProperties.getSuitabilityTtl() == null || wealthProperties.getSuitabilityTtl().isZero()
                || wealthProperties.getSuitabilityTtl().isNegative()) {
            throw new IllegalArgumentException("Suitability TTL must be positive");
        }
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
    public CardControlsView getCardControls(UUID userId, UUID cardId) {
        BankCard card = cards.findByIdAndUserId(cardId, userId).orElseThrow(ResourceNotFoundException::new);
        return new CardControlsView(card.getId(), card.getCardMasked(), card.getStatus().name(),
                card.isOnlineEnabled(), card.isOverseasEnabled(), card.getSingleTxLimit(),
                card.getDailyLimit(), card.getVersion());
    }

    @Override
    @Transactional
    public PreparationResult prepareCardStatus(PreparationContext context, UUID cardId,
            String targetStatus, String reasonCode) {
        BankCard card = cards.findByIdAndUserId(cardId, context.userId()).orElse(null);
        if (card == null) return unavailable();
        BankCard.Status target = BankCard.Status.valueOf(targetStatus);
        Evaluation evaluation = evaluateStatusChange(card, target);
        if (!CARD_REASON_CODES.contains(reasonCode)) {
            evaluation = Evaluation.deny("CARD_REASON_INVALID", "卡片状态变更原因不在允许范围内");
        }
        CardStatusCommand command = new CardStatusCommand(cardId, target, reasonCode, card.getVersion());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cardId", card.getId()); summary.put("card", card.getCardMasked());
        summary.put("fromStatus", card.getStatus()); summary.put("toStatus", target);
        summary.put("reasonCode", reasonCode); summary.put("impact", "确认后卡片状态立即变更");
        return prepare(context, "CARD_STATUS", "prepare_card_status_change", command, summary, evaluation);
    }

    @Override
    @Transactional
    public PreparationResult prepareCardControls(PreparationContext context, UUID cardId,
            Boolean onlineEnabled, Boolean overseasEnabled, BigDecimal singleTxLimit, BigDecimal dailyLimit) {
        BankCard card = cards.findByIdAndUserId(cardId, context.userId()).orElse(null);
        if (card == null) return unavailable();
        boolean online = onlineEnabled == null ? card.isOnlineEnabled() : onlineEnabled;
        boolean overseas = overseasEnabled == null ? card.isOverseasEnabled() : overseasEnabled;
        BigDecimal single = singleTxLimit == null ? card.getSingleTxLimit() : money(singleTxLimit);
        BigDecimal daily = dailyLimit == null ? card.getDailyLimit() : money(dailyLimit);
        Evaluation evaluation = evaluateControls(card, online, overseas, single, daily);
        CardControlsCommand command = new CardControlsCommand(cardId, online, overseas, single, daily, card.getVersion());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cardId", card.getId()); summary.put("card", card.getCardMasked());
        summary.put("before", cardState(card, card.getVersion()));
        summary.put("after", controlsState(card.getStatus(), online, overseas, single, daily, card.getVersion() + 1));
        summary.put("impact", "确认后更新卡片支付控制");
        return prepare(context, "CARD_CONTROLS", "prepare_card_controls_change", command, summary, evaluation);
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
    public SubscriptionCostAnalysis analyzeSubscriptions(UUID userId, List<UUID> subscriptionIds, int horizonMonths) {
        if (horizonMonths < 1 || horizonMonths > 12) throw new IllegalArgumentException("Invalid horizon");
        List<Subscription> owned = subscriptions.findAllByUserIdOrderByMerchantName(userId);
        List<Subscription> selected;
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            selected = owned;
        } else {
            Set<UUID> requested = new LinkedHashSet<>(subscriptionIds);
            selected = owned.stream().filter(row -> requested.contains(row.getId())).toList();
            if (selected.size() != requested.size()) throw new ResourceNotFoundException();
        }
        List<SubscriptionCostItem> items = new ArrayList<>();
        BigDecimal activeMonthly = BigDecimal.ZERO;
        for (Subscription row : selected) {
            BigDecimal monthly = monthlyEquivalent(row).setScale(2, RoundingMode.HALF_UP);
            boolean cancellable = row.getStatus() == Subscription.Status.ACTIVE
                    || row.getStatus() == Subscription.Status.PAUSED;
            if (row.getStatus() == Subscription.Status.ACTIVE) activeMonthly = activeMonthly.add(monthly);
            items.add(new SubscriptionCostItem(row.getId(), row.getServiceName(), row.getStatus().name(),
                    row.getAmount(), row.getBillingCycle().name(), monthly, row.getNextChargeDate(), cancellable));
        }
        activeMonthly = activeMonthly.setScale(2, RoundingMode.HALF_UP);
        return new SubscriptionCostAnalysis(horizonMonths, activeMonthly,
                activeMonthly.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP),
                activeMonthly.multiply(BigDecimal.valueOf(horizonMonths)).setScale(2, RoundingMode.HALF_UP),
                "CNY", List.copyOf(items));
    }

    @Override
    @Transactional
    public PreparationResult prepareSubscriptionCancellation(PreparationContext context,
            UUID subscriptionId, String reasonCode) {
        Subscription subscription = subscriptions.findByIdAndUserId(subscriptionId, context.userId()).orElse(null);
        if (subscription == null) return unavailable();
        Evaluation evaluation = evaluateCancellation(subscription);
        String normalizedReason = reasonCode == null || reasonCode.isBlank() ? "USER_REQUEST" : reasonCode;
        if (!SUBSCRIPTION_REASON_CODES.contains(normalizedReason)) {
            evaluation = Evaluation.deny("SUBSCRIPTION_REASON_INVALID", "取消原因不在允许范围内");
        }
        SubscriptionCancelCommand command = new SubscriptionCancelCommand(
                subscriptionId, normalizedReason, subscription.getVersion());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("subscriptionId", subscription.getId()); summary.put("service", subscription.getServiceName());
        summary.put("merchant", subscription.getMerchantName()); summary.put("currentStatus", subscription.getStatus());
        summary.put("amount", subscription.getAmount().setScale(2).toPlainString());
        summary.put("billingCycle", subscription.getBillingCycle());
        summary.put("nextChargeDate", subscription.getNextChargeDate());
        summary.put("effect", subscription.getCancellationEffect()); summary.put("reasonCode", normalizedReason);
        return prepare(context, "SUBSCRIPTION_CANCEL", "prepare_subscription_cancellation",
                command, summary, evaluation);
    }

    @Override
    @Transactional(noRollbackFor = ResourceNotFoundException.class)
    public SuitabilityResult checkSuitability(UUID userId, UUID productId, UUID paymentAccountId,
            BigDecimal amount, String currency) {
        validateMoney(amount, currency);
        WealthProduct product = products.findById(productId).orElseThrow(ResourceNotFoundException::new);
        Account account = accounts.findByIdAndUserId(paymentAccountId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        CustomerRiskProfile profile = riskProfiles.findByUserId(userId).orElseThrow(ResourceNotFoundException::new);
        Instant now = Instant.now(clock);
        Evaluation evaluation = evaluateWealth(account, product, profile, amount, currency);
        SuitabilityCheck check = suitabilityChecks.save(new SuitabilityCheck(UUID.randomUUID(), userId,
                productId, paymentAccountId, money(amount), currency, profile.getRiskLevel(), product.getRiskLevel(),
                product.getVersion(), account.getVersion(), evaluation.allowed()
                        ? SuitabilityCheck.Status.APPROVED : SuitabilityCheck.Status.DENIED,
                evaluation.reasonCodes().get(0), now.plus(wealthProperties.getSuitabilityTtl()), now));
        return new SuitabilityResult(check.getId(), evaluation.allowed(), evaluation.reasonCodes().get(0),
                evaluation.explanation(), productId, paymentAccountId, money(amount), currency,
                profile.getRiskLevel().name(), product.getRiskLevel().name(), check.getExpiresAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioItem> portfolio(UUID userId) {
        return holdings.findAllByUserIdOrderByOpenedAtDesc(userId).stream().map(holding -> {
            WealthProduct product = products.findById(holding.getProductId()).orElseThrow();
            return new PortfolioItem(holding.getId(), product.getId(), product.getProductCode(), product.getName(),
                    product.getRiskLevel().name(), holding.getInvestedAmount(), holding.getCurrency(),
                    holding.getStatus().name());
        }).toList();
    }

    @Override
    @Transactional
    public PreparationResult prepareWealthPurchase(PreparationContext context, UUID suitabilityCheckId,
            UUID productId, UUID paymentAccountId, BigDecimal amount, String currency) {
        validateMoney(amount, currency);
        SuitabilityCheck check = suitabilityChecks.findByIdAndUserId(suitabilityCheckId, context.userId()).orElse(null);
        WealthProduct product = products.findById(productId).orElse(null);
        Account account = accounts.findByIdAndUserId(paymentAccountId, context.userId()).orElse(null);
        CustomerRiskProfile profile = riskProfiles.findByUserId(context.userId()).orElse(null);
        if (check == null || product == null || account == null || profile == null) return unavailable();
        Evaluation evaluation = validateSuitabilityBinding(check, product, account, amount, currency);
        if (evaluation.allowed()) evaluation = evaluateWealth(account, product, profile, amount, currency);
        WealthPurchaseCommand command = new WealthPurchaseCommand(suitabilityCheckId, productId,
                paymentAccountId, money(amount), currency, product.getVersion());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("suitabilityCheckId", check.getId()); summary.put("productId", product.getId());
        summary.put("productCode", product.getProductCode()); summary.put("product", product.getName());
        summary.put("riskLevel", product.getRiskLevel()); summary.put("paymentAccount", account.getAccountMasked());
        summary.put("amount", money(amount).setScale(2).toPlainString()); summary.put("currency", currency);
        summary.put("expectedYieldText", product.getExpectedYieldText());
        summary.put("riskNotice", "模拟理财产品不保本，展示收益不构成承诺");
        return prepare(context, "WEALTH_PURCHASE", "prepare_wealth_purchase", command, summary,
                evaluation.allowed() ? Evaluation.allow(AssuranceLevel.AAL2, "SUITABILITY_APPROVED",
                        "适当性、产品、账户和金额检查通过；购买仍需 AAL2 与明确确认") : evaluation);
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
    public OperationView get(UUID userId, UUID operationId) {
        OperationRequest operation = operations.findByIdAndUserId(operationId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        return view(operation, challenges.findByOperationId(operationId).orElse(null));
    }

    @Override
    @Transactional
    public ConfirmationResult confirm(UUID userId, AssuranceLevel assurance, UUID operationId,
            UUID challengeId, boolean confirmed) {
        Instant now = Instant.now(clock);
        OperationRequest operation = operations.lockOwned(operationId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        if (operation.getStatus() == OperationRequest.Status.SUCCEEDED) {
            return success(operation, loadReceipt(operation), "IDEMPOTENT_REPLAY", "该操作已成功，返回原始回执");
        }
        if (operation.terminal()) {
            return denied(operation, operation.getFailureCode(), "操作已终止，不能再次执行",
                    List.of(operation.getFailureCode()));
        }
        ConfirmationChallenge challenge = challenges.lockBound(challengeId, operationId, userId)
                .orElseThrow(ResourceNotFoundException::new);
        if (!operation.getExpiresAt().isAfter(now) || !challenge.getExpiresAt().isAfter(now)) {
            operation.markExpired(now); challenge.expire();
            audit(operation, "CONFIRM", "DENY", "OPERATION_EXPIRED", "确认窗口已过期", now);
            return denied(operation, "OPERATION_EXPIRED", "确认窗口已过期", List.of("OPERATION_EXPIRED"));
        }
        if (challenge.getStatus() != ConfirmationChallenge.Status.PENDING) {
            return denied(operation, "CHALLENGE_ALREADY_USED", "确认挑战已使用或失效",
                    List.of("CHALLENGE_ALREADY_USED"));
        }
        if (!confirmed) {
            operation.cancel(now); challenge.cancel();
            audit(operation, "CONFIRM", "DENY", "USER_CANCELLED", "用户取消了操作", now);
            return denied(operation, "USER_CANCELLED", "用户取消了操作", List.of("USER_CANCELLED"));
        }
        if (!assurance.atLeast(operation.getRequiredAal())) {
            audit(operation, "CONFIRM", "DENY", "AAL2_REQUIRED", "当前会话认证强度不足", now);
            return denied(operation, "AAL2_REQUIRED", "请先完成 AAL2 验证后再次确认", List.of("AAL2_REQUIRED"));
        }
        if (!hash(operation.getCanonicalArgsJson()).equals(operation.getArgsHash())
                || !hash(operation.getDisplaySummaryJson()).equals(operation.getSummaryHash())) {
            return finalDeny(operation, challenge, "OPERATION_INTEGRITY_FAILED", "操作完整性校验失败", now);
        }
        users.lockById(userId).orElseThrow(ResourceNotFoundException::new);
        Execution execution = switch (operation.getToolName()) {
            case "prepare_card_status_change" -> executeCardStatus(operation, read(operation, CardStatusCommand.class), now);
            case "prepare_card_controls_change" -> executeCardControls(operation, read(operation, CardControlsCommand.class), now);
            case "prepare_subscription_cancellation" -> executeSubscriptionCancellation(
                    operation, read(operation, SubscriptionCancelCommand.class), now);
            case "prepare_wealth_purchase" -> executeWealthPurchase(
                    operation, read(operation, WealthPurchaseCommand.class), now);
            default -> throw new ResourceNotFoundException();
        };
        risks.save(risk(operation, RiskDecision.Phase.FINAL_GUARD, execution.evaluation(), now));
        if (!execution.evaluation().allowed()) {
            return finalDeny(operation, challenge, execution.evaluation().reasonCodes().get(0),
                    execution.evaluation().explanation(), now);
        }
        challenge.consume(now);
        operation.markSucceeded(execution.receipt().resultType(), execution.receipt().receiptId(), now);
        idempotencies.findByOperationId(operationId).ifPresent(row -> row.succeeded(now));
        audit(operation, "FINAL_GUARD", "ALLOW", execution.successCode(), execution.explanation(), now);
        return success(operation, execution.receipt(), execution.successCode(), execution.explanation());
    }

    private synchronized PreparationResult prepare(PreparationContext context, String scope, String toolName,
            Object command, Object summary, Evaluation evaluation) {
        if (context == null || context.userId() == null || context.requestId() == null
                || context.idempotencyKey() == null || context.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Invalid preparation context");
        }
        Instant now = Instant.now(clock);
        String canonical = json(command); String requestHash = hash(canonical);
        String keyHash = hash(context.idempotencyKey());
        Optional<IdempotencyRecord> existing = idempotencies.findByUserIdAndScopeAndIdempotencyKeyHash(
                context.userId(), scope, keyHash);
        if (existing.isPresent()) {
            OperationView prior = get(context.userId(), existing.get().getOperationId());
            if (!MessageDigest.isEqual(existing.get().getRequestHash().getBytes(StandardCharsets.US_ASCII),
                    requestHash.getBytes(StandardCharsets.US_ASCII))) {
                return new PreparationResult(false, "IDEMPOTENCY_CONFLICT",
                        "同一幂等键不能用于不同参数", prior);
            }
            boolean reusable = !Set.of("DENIED", "FAILED", "EXPIRED", "CANCELLED").contains(prior.status());
            return new PreparationResult(reusable, "IDEMPOTENT_REPLAY",
                    reusable ? "返回此前创建的同一操作" : "此前操作已终止", prior);
        }
        UUID operationId = UUID.randomUUID(); Instant expires = now.plus(operationProperties.getOperationTtl());
        String summaryJson = json(summary);
        OperationRequest operation = new OperationRequest(operationId, context.requestId(), context.conversationId(),
                context.userId(), toolName, "1.0", canonical, requestHash, summaryJson, hash(summaryJson),
                evaluation.requiredAal(), keyHash, now, expires);
        if (!evaluation.allowed()) operation.markDenied(evaluation.reasonCodes().get(0), now);
        operations.save(operation);
        risks.save(risk(operation, RiskDecision.Phase.PRE_CONFIRM, evaluation, now));
        audits.save(new OperationAudit(operationId, context.userId(), "PREPARE",
                evaluation.allowed() ? "ALLOW" : "DENY", evaluation.reasonCodes().get(0),
                evaluation.explanation(), now));
        idempotencies.save(new IdempotencyRecord(context.userId(), scope, keyHash, requestHash,
                operationId, now, now.plus(operationProperties.getIdempotencyTtl())));
        if (!evaluation.allowed()) {
            idempotencies.findByOperationId(operationId).ifPresent(row -> row.denied(now));
            return new PreparationResult(false, evaluation.reasonCodes().get(0),
                    evaluation.explanation(), view(operation, null));
        }
        ConfirmationChallenge challenge = challenges.save(new ConfirmationChallenge(
                UUID.randomUUID(), operationId, context.userId(), now, expires));
        return new PreparationResult(true, "CONFIRMATION_REQUIRED", evaluation.explanation(),
                view(operation, challenge));
    }

    private Execution executeCardStatus(OperationRequest operation, CardStatusCommand command, Instant now) {
        BankCard card = cards.lockOwned(command.cardId(), operation.getUserId()).orElse(null);
        if (card == null) return Execution.deny("RESOURCE_NOT_AVAILABLE", "卡片不存在或不属于当前用户");
        if (card.getVersion() != command.expectedVersion()) {
            return Execution.deny("RESOURCE_VERSION_CHANGED", "确认期间卡片状态发生变化");
        }
        Evaluation evaluation = evaluateStatusChange(card, command.targetStatus());
        if (!evaluation.allowed()) return new Execution(evaluation, null, null, null);
        Map<String, Object> before = cardState(card, card.getVersion());
        card.changeStatus(command.targetStatus());
        Map<String, Object> after = cardState(card, card.getVersion() + 1);
        UUID receiptId = UUID.randomUUID();
        cardChanges.save(new CardChangeHistory(receiptId, operation.getId(), operation.getUserId(), card.getId(),
                CardChangeHistory.ChangeType.STATUS, json(before), json(after), command.reasonCode(), now));
        OperationReceipt receipt = new OperationReceipt(receiptId, operation.getId(), "CARD_CHANGE", "SUCCEEDED",
                Map.of("cardId", card.getId(), "before", before, "after", after,
                        "reasonCode", command.reasonCode()), now);
        return Execution.allow("CARD_STATUS_UPDATED", "卡片状态已更新并记录历史", receipt);
    }

    private Execution executeCardControls(OperationRequest operation, CardControlsCommand command, Instant now) {
        BankCard card = cards.lockOwned(command.cardId(), operation.getUserId()).orElse(null);
        if (card == null) return Execution.deny("RESOURCE_NOT_AVAILABLE", "卡片不存在或不属于当前用户");
        if (card.getVersion() != command.expectedVersion()) {
            return Execution.deny("RESOURCE_VERSION_CHANGED", "确认期间卡片控制发生变化");
        }
        Evaluation evaluation = evaluateControls(card, command.onlineEnabled(), command.overseasEnabled(),
                command.singleTxLimit(), command.dailyLimit());
        if (!evaluation.allowed()) return new Execution(evaluation, null, null, null);
        Map<String, Object> before = cardState(card, card.getVersion());
        card.updateControls(command.onlineEnabled(), command.overseasEnabled(),
                command.singleTxLimit(), command.dailyLimit());
        Map<String, Object> after = cardState(card, card.getVersion() + 1);
        UUID receiptId = UUID.randomUUID();
        cardChanges.save(new CardChangeHistory(receiptId, operation.getId(), operation.getUserId(), card.getId(),
                CardChangeHistory.ChangeType.CONTROLS, json(before), json(after), "USER_REQUEST", now));
        OperationReceipt receipt = new OperationReceipt(receiptId, operation.getId(), "CARD_CHANGE", "SUCCEEDED",
                Map.of("cardId", card.getId(), "before", before, "after", after), now);
        return Execution.allow("CARD_CONTROLS_UPDATED", "卡片支付控制已更新并记录历史", receipt);
    }

    private Execution executeSubscriptionCancellation(OperationRequest operation,
            SubscriptionCancelCommand command, Instant now) {
        Subscription subscription = subscriptions.lockOwned(command.subscriptionId(), operation.getUserId()).orElse(null);
        if (subscription == null) return Execution.deny("RESOURCE_NOT_AVAILABLE", "订阅不存在或不属于当前用户");
        if (subscription.getVersion() != command.expectedVersion()) {
            return Execution.deny("RESOURCE_VERSION_CHANGED", "确认期间订阅状态发生变化");
        }
        Evaluation evaluation = evaluateCancellation(subscription);
        if (!evaluation.allowed()) return new Execution(evaluation, null, null, null);
        Subscription.Status before = subscription.getStatus();
        subscription.cancel();
        UUID receiptId = UUID.randomUUID();
        subscriptionChanges.save(new SubscriptionChangeHistory(receiptId, operation.getId(), operation.getUserId(),
                subscription.getId(), before, command.reasonCode(), now, now));
        OperationReceipt receipt = new OperationReceipt(receiptId, operation.getId(), "SUBSCRIPTION_CHANGE",
                "SUCCEEDED", Map.of("subscriptionId", subscription.getId(), "beforeStatus", before,
                        "afterStatus", Subscription.Status.CANCELLED, "effectiveAt", now,
                        "reasonCode", command.reasonCode()), now);
        return Execution.allow("SUBSCRIPTION_CANCELLED", "订阅已取消并记录生效时间", receipt);
    }

    private Execution executeWealthPurchase(OperationRequest operation, WealthPurchaseCommand command, Instant now) {
        SuitabilityCheck check = suitabilityChecks.findByIdAndUserId(
                command.suitabilityCheckId(), operation.getUserId()).orElse(null);
        CustomerRiskProfile profile = riskProfiles.findByUserId(operation.getUserId()).orElse(null);
        WealthProduct product = products.lockById(command.productId()).orElse(null);
        Account account = accounts.lockById(command.paymentAccountId()).orElse(null);
        if (check == null || profile == null || product == null || account == null
                || !account.getUserId().equals(operation.getUserId())) {
            return Execution.deny("RESOURCE_NOT_AVAILABLE", "产品、账户或适当性检查不可用");
        }
        Evaluation binding = validateSuitabilityBinding(check, product, account, command.amount(), command.currency());
        if (!binding.allowed()) return new Execution(binding, null, null, null);
        if (product.getVersion() != command.expectedProductVersion()) {
            return Execution.deny("PRODUCT_CHANGED", "确认期间产品信息发生变化");
        }
        Evaluation evaluation = evaluateWealth(account, product, profile, command.amount(), command.currency());
        if (!evaluation.allowed()) return new Execution(evaluation, null, null, null);
        BigDecimal before = account.getAvailableBalance(); account.debit(command.amount());
        transactions.save(new AccountTransaction(UUID.randomUUID(), account.getId(), now,
                AccountTransaction.Direction.OUT, command.amount(), command.currency(), "INVESTMENT",
                product.getName(), "模拟理财购买", product.getProductCode(), account.getLedgerBalance()));
        WealthHolding holding = holdings.lockOwnedProduct(operation.getUserId(), product.getId()).orElse(null);
        if (holding == null) holding = holdings.save(new WealthHolding(
                operation.getUserId(), product.getId(), command.amount(), command.currency(), now));
        else holding.add(command.amount(), now);
        UUID receiptId = UUID.randomUUID();
        investmentOrders.save(new InvestmentOrder(receiptId, operation.getId(), operation.getUserId(),
                product.getId(), account.getId(), check.getId(), command.amount(), command.currency(),
                before, account.getAvailableBalance(), now));
        OperationReceipt receipt = new OperationReceipt(receiptId, operation.getId(), "INVESTMENT_ORDER",
                "SUCCEEDED", Map.of("orderId", receiptId, "holdingId", holding.getId(),
                        "productId", product.getId(), "productCode", product.getProductCode(),
                        "amount", command.amount(), "currency", command.currency(),
                        "balanceBefore", before, "balanceAfter", account.getAvailableBalance()), now);
        return Execution.allow("WEALTH_PURCHASE_EXECUTED", "理财购买已扣款并生成订单与持仓", receipt);
    }

    private ConfirmationResult finalDeny(OperationRequest operation, ConfirmationChallenge challenge,
            String code, String explanation, Instant now) {
        Evaluation evaluation = Evaluation.deny(code, explanation);
        if (code.equals("OPERATION_INTEGRITY_FAILED")) {
            risks.save(risk(operation, RiskDecision.Phase.FINAL_GUARD, evaluation, now));
        }
        operation.markDenied(code, now); challenge.consume(now);
        idempotencies.findByOperationId(operation.getId()).ifPresent(row -> row.denied(now));
        audit(operation, "FINAL_GUARD", "DENY", code, explanation, now);
        return denied(operation, code, explanation, List.of(code));
    }

    private OperationReceipt loadReceipt(OperationRequest operation) {
        return switch (operation.getToolName()) {
            case "prepare_card_status_change", "prepare_card_controls_change" -> {
                CardChangeHistory row = cardChanges.findByOperationId(operation.getId()).orElseThrow();
                yield new OperationReceipt(row.getId(), operation.getId(), "CARD_CHANGE", "SUCCEEDED",
                        readTree(row.getAfterStateJson()), row.getCompletedAt());
            }
            case "prepare_subscription_cancellation" -> {
                SubscriptionChangeHistory row = subscriptionChanges.findByOperationId(operation.getId()).orElseThrow();
                yield new OperationReceipt(row.getId(), operation.getId(), "SUBSCRIPTION_CHANGE", "SUCCEEDED",
                        Map.of("subscriptionId", row.getSubscriptionId(), "effectiveAt", row.getEffectiveAt()),
                        row.getEffectiveAt());
            }
            case "prepare_wealth_purchase" -> {
                InvestmentOrder row = investmentOrders.findByOperationId(operation.getId()).orElseThrow();
                yield new OperationReceipt(row.getId(), operation.getId(), "INVESTMENT_ORDER", "SUCCEEDED",
                        Map.of("orderId", row.getId(), "productId", row.getProductId(), "amount", row.getAmount(),
                                "currency", row.getCurrency(), "balanceBefore", row.getBalanceBefore(),
                                "balanceAfter", row.getBalanceAfter()), row.getCompletedAt());
            }
            default -> throw new IllegalStateException("Unsupported operation receipt");
        };
    }

    private Evaluation evaluateStatusChange(BankCard card, BankCard.Status target) {
        if (target == BankCard.Status.CLOSED) return Evaluation.deny("CARD_TRANSITION_INVALID", "不允许自助关闭卡片");
        if (card.getStatus() == BankCard.Status.CLOSED || card.getStatus() == BankCard.Status.LOST) {
            return Evaluation.deny("CARD_TRANSITION_INVALID", "当前卡片状态不允许该操作");
        }
        if (card.getStatus() == target) return Evaluation.deny("NO_CHANGE", "目标状态与当前状态相同");
        boolean valid = (card.getStatus() == BankCard.Status.ACTIVE && Set.of(BankCard.Status.FROZEN, BankCard.Status.LOST).contains(target))
                || (card.getStatus() == BankCard.Status.FROZEN && Set.of(BankCard.Status.ACTIVE, BankCard.Status.LOST).contains(target));
        if (!valid) return Evaluation.deny("CARD_TRANSITION_INVALID", "卡片状态转换不合法");
        AssuranceLevel aal = target == BankCard.Status.ACTIVE ? AssuranceLevel.AAL2 : AssuranceLevel.AAL1;
        return Evaluation.allow(aal, aal == AssuranceLevel.AAL2 ? "CARD_RELAXATION_REQUIRES_AAL2" : "CARD_PROTECTION_TIGHTENED",
                aal == AssuranceLevel.AAL2 ? "解冻卡片需要 AAL2 与明确确认" : "卡片保护性状态变更需要明确确认");
    }

    private Evaluation evaluateControls(BankCard card, boolean online, boolean overseas,
            BigDecimal single, BigDecimal daily) {
        if (card.getStatus() == BankCard.Status.CLOSED || card.getStatus() == BankCard.Status.LOST) {
            return Evaluation.deny("CARD_TRANSITION_INVALID", "当前卡片状态不允许调整支付控制");
        }
        if (single == null || daily == null || single.signum() < 0 || daily.signum() < 0
                || single.scale() > 2 || daily.scale() > 2 || single.compareTo(daily) > 0
                || daily.compareTo(MAX_LIMIT) > 0) {
            return Evaluation.deny("CARD_LIMIT_INVALID", "卡片限额必须非负、单笔不高于日限额且不超过系统上限");
        }
        boolean changed = online != card.isOnlineEnabled() || overseas != card.isOverseasEnabled()
                || single.compareTo(card.getSingleTxLimit()) != 0 || daily.compareTo(card.getDailyLimit()) != 0;
        if (!changed) return Evaluation.deny("NO_CHANGE", "卡片控制没有发生变化");
        boolean relaxation = (online && !card.isOnlineEnabled()) || (overseas && !card.isOverseasEnabled())
                || single.compareTo(card.getSingleTxLimit()) > 0 || daily.compareTo(card.getDailyLimit()) > 0;
        AssuranceLevel aal = relaxation ? AssuranceLevel.AAL2 : AssuranceLevel.AAL1;
        return Evaluation.allow(aal, relaxation ? "CARD_RELAXATION_REQUIRES_AAL2" : "CARD_CONTROL_TIGHTENED",
                relaxation ? "放宽卡片控制需要 AAL2 与明确确认" : "收紧卡片控制需要明确确认");
    }

    private Evaluation evaluateCancellation(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case ACTIVE, PAUSED -> Evaluation.allow(AssuranceLevel.AAL1, "SUBSCRIPTION_CANCELLABLE",
                    "订阅状态允许取消；仍需明确确认");
            case CANCELLED -> Evaluation.deny("ALREADY_CANCELLED", "订阅已经取消");
            case EXPIRED -> Evaluation.deny("SUBSCRIPTION_NOT_CANCELLABLE", "已过期订阅不能取消");
        };
    }

    private Evaluation evaluateWealth(Account account, WealthProduct product, CustomerRiskProfile profile,
            BigDecimal amount, String currency) {
        LocalDate today = LocalDate.now(clock);
        if (account.getStatus() != Account.Status.ACTIVE) return Evaluation.deny("ACCOUNT_NOT_ACTIVE", "付款账户不可用");
        if (!account.getCurrency().equals(currency) || !product.getCurrency().equals(currency)) {
            return Evaluation.deny("CURRENCY_MISMATCH", "账户、产品和购买币种必须一致");
        }
        if (product.getSaleStatus() != WealthProduct.SaleStatus.ON_SALE) {
            return Evaluation.deny("PRODUCT_NOT_ON_SALE", "产品当前不在售");
        }
        if (today.isBefore(product.getValidFrom()) || today.isAfter(product.getValidTo())) {
            return Evaluation.deny("PRODUCT_SALE_WINDOW_CLOSED", "产品销售期已关闭");
        }
        if (today.isAfter(profile.getValidUntil())) return Evaluation.deny("RISK_PROFILE_EXPIRED", "客户风险测评已过期");
        if (product.getRiskLevel().ordinal() > profile.getRiskLevel().ordinal()) {
            return Evaluation.deny("RISK_LEVEL_MISMATCH", "客户风险等级低于产品风险等级");
        }
        if (amount.compareTo(product.getMinPurchase()) < 0) {
            return Evaluation.deny("BELOW_MINIMUM_PURCHASE", "购买金额低于产品起购金额");
        }
        if (amount.subtract(product.getMinPurchase()).remainder(product.getIncrementAmount()).signum() != 0) {
            return Evaluation.deny("INVALID_PURCHASE_INCREMENT", "购买金额不符合递增金额要求");
        }
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            return Evaluation.deny("INSUFFICIENT_FUNDS", "账户可用余额不足");
        }
        return Evaluation.allow(AssuranceLevel.AAL2, "SUITABLE", "产品风险、销售状态、金额和账户余额均匹配");
    }

    private Evaluation validateSuitabilityBinding(SuitabilityCheck check, WealthProduct product,
            Account account, BigDecimal amount, String currency) {
        if (check.getStatus() != SuitabilityCheck.Status.APPROVED) {
            return Evaluation.deny("SUITABILITY_NOT_APPROVED", "适当性检查未通过");
        }
        if (!check.getExpiresAt().isAfter(Instant.now(clock))) {
            return Evaluation.deny("SUITABILITY_EXPIRED", "适当性检查已过期");
        }
        if (!check.getProductId().equals(product.getId()) || !check.getPaymentAccountId().equals(account.getId())
                || check.getAmount().compareTo(amount) != 0 || !check.getCurrency().equals(currency)) {
            return Evaluation.deny("SUITABILITY_BINDING_MISMATCH", "适当性检查与购买参数不匹配");
        }
        if (check.getProductVersion() != product.getVersion()) {
            return Evaluation.deny("PRODUCT_CHANGED", "适当性检查后产品信息发生变化");
        }
        return Evaluation.allow(AssuranceLevel.AAL2, "SUITABILITY_APPROVED", "适当性检查绑定有效");
    }

    private OperationView view(OperationRequest operation, ConfirmationChallenge challenge) {
        Object summary = readTree(operation.getDisplaySummaryJson());
        String effectiveStatus = !operation.terminal() && !operation.getExpiresAt().isAfter(Instant.now(clock))
                ? OperationRequest.Status.EXPIRED.name() : operation.getStatus().name();
        return new OperationView(operation.getId(), operation.getToolName(), effectiveStatus,
                operation.getRequiredAal(), operation.getExpiresAt(), challenge == null ? null : challenge.getId(),
                "/api/v1/operations/" + operation.getId() + "/confirm", summary,
                operation.getResultRefId(), operation.getFailureCode());
    }

    private ConfirmationResult denied(OperationRequest operation, String code, String explanation, List<String> reasons) {
        return new ConfirmationResult("DENIED", code, explanation,
                view(operation, challenges.findByOperationId(operation.getId()).orElse(null)), null, reasons);
    }

    private ConfirmationResult success(OperationRequest operation, OperationReceipt receipt,
            String code, String explanation) {
        return new ConfirmationResult("SUCCEEDED", code, explanation,
                view(operation, challenges.findByOperationId(operation.getId()).orElse(null)), receipt,
                List.of("ALL_FINAL_GUARDS_PASSED"));
    }

    private RiskDecision risk(OperationRequest operation, RiskDecision.Phase phase,
            Evaluation evaluation, Instant now) {
        RiskDecision.Outcome outcome = !evaluation.allowed() ? RiskDecision.Outcome.DENY
                : phase == RiskDecision.Phase.FINAL_GUARD ? RiskDecision.Outcome.ALLOW
                : evaluation.requiredAal() == AssuranceLevel.AAL2
                        ? RiskDecision.Outcome.REQUIRE_STEP_UP : RiskDecision.Outcome.REQUIRE_CONFIRMATION;
        return new RiskDecision(operation.getId(), operation.getUserId(), phase, outcome, POLICY_VERSION,
                operation.getArgsHash(), json(evaluation.reasonCodes()), evaluation.explanation(), now);
    }

    private void audit(OperationRequest operation, String stage, String outcome,
            String code, String explanation, Instant now) {
        audits.save(new OperationAudit(operation.getId(), operation.getUserId(), stage,
                outcome, code, explanation, now));
    }

    private static Map<String, Object> cardState(BankCard card, long version) {
        return controlsState(card.getStatus(), card.isOnlineEnabled(), card.isOverseasEnabled(),
                card.getSingleTxLimit(), card.getDailyLimit(), version);
    }

    private static Map<String, Object> controlsState(BankCard.Status status, boolean online, boolean overseas,
            BigDecimal single, BigDecimal daily, long version) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("status", status); state.put("onlineEnabled", online); state.put("overseasEnabled", overseas);
        state.put("singleTxLimit", single); state.put("dailyLimit", daily); state.put("version", version);
        return state;
    }

    private static BigDecimal monthlyEquivalent(Subscription subscription) {
        return switch (subscription.getBillingCycle()) {
            case WEEKLY -> subscription.getAmount().multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            case MONTHLY -> subscription.getAmount();
            case QUARTERLY -> subscription.getAmount().divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
            case YEARLY -> subscription.getAmount().divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        };
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static void validateMoney(BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.compareTo(MAX_LIMIT) > 0
                || currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Invalid money");
        }
    }

    private PreparationResult unavailable() {
        return new PreparationResult(false, "RESOURCE_NOT_AVAILABLE",
                "资源不存在或不属于当前用户", null);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to serialize operation", exception); }
    }

    private <T> T read(OperationRequest operation, Class<T> type) {
        try { return mapper.readValue(operation.getCanonicalArgsJson(), type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read operation", exception); }
    }

    private Object readTree(String value) {
        try { return mapper.readTree(value); }
        catch (JsonProcessingException exception) { return Map.of("unavailable", true); }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record CardStatusCommand(UUID cardId, BankCard.Status targetStatus,
            String reasonCode, long expectedVersion) {}
    private record CardControlsCommand(UUID cardId, boolean onlineEnabled, boolean overseasEnabled,
            BigDecimal singleTxLimit, BigDecimal dailyLimit, long expectedVersion) {}
    private record SubscriptionCancelCommand(UUID subscriptionId, String reasonCode, long expectedVersion) {}
    private record WealthPurchaseCommand(UUID suitabilityCheckId, UUID productId, UUID paymentAccountId,
            BigDecimal amount, String currency, long expectedProductVersion) {}

    private record Evaluation(boolean allowed, AssuranceLevel requiredAal,
            List<String> reasonCodes, String explanation) {
        static Evaluation allow(AssuranceLevel aal, String code, String explanation) {
            return new Evaluation(true, aal, List.of(code), explanation);
        }
        static Evaluation deny(String code, String explanation) {
            return new Evaluation(false, AssuranceLevel.AAL1, List.of(code), explanation);
        }
    }

    private record Execution(Evaluation evaluation, String successCode,
            String explanation, OperationReceipt receipt) {
        static Execution allow(String code, String explanation, OperationReceipt receipt) {
            return new Execution(Evaluation.allow(AssuranceLevel.AAL1, code, explanation), code, explanation, receipt);
        }
        static Execution deny(String code, String explanation) {
            return new Execution(Evaluation.deny(code, explanation), null, null, null);
        }
    }
}
