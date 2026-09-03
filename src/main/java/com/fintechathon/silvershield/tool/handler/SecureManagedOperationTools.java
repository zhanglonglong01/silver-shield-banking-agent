package com.fintechathon.silvershield.tool.handler;

import com.fintechathon.silvershield.operation.application.ManagedOperationUseCase;
import com.fintechathon.silvershield.operation.application.ManagedOperationUseCase.*;
import com.fintechathon.silvershield.tool.gateway.ToolDeniedException;
import com.fintechathon.silvershield.tool.model.*;
import com.fintechathon.silvershield.tool.registry.ToolHandler;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SecureManagedOperationTools {
    private SecureManagedOperationTools() {}

    public record CardIdArguments(@NotNull UUID cardId) implements ToolArguments {}
    public record EmptyArguments() implements ToolArguments {}
    public record PrepareCardStatusArguments(@NotNull UUID cardId,
            @NotBlank @Pattern(regexp = "ACTIVE|FROZEN|LOST") String targetStatus,
            @NotBlank @Pattern(regexp = "USER_REQUEST|SUSPECTED_FRAUD|CARD_LOST|SECURITY_CONTROL")
            String reasonCode) implements ToolArguments {}
    public record PrepareCardControlsArguments(@NotNull UUID cardId, Boolean onlineEnabled,
            Boolean overseasEnabled, @DecimalMin("0.00") @Digits(integer = 7, fraction = 2) BigDecimal singleTxLimit,
            @DecimalMin("0.00") @Digits(integer = 7, fraction = 2) BigDecimal dailyLimit) implements ToolArguments {}
    public record SubscriptionAnalysisArguments(
            @Size(max = 50) List<@NotNull UUID> subscriptionIds,
            @NotNull @Min(1) @Max(12) Integer horizonMonths) implements ToolArguments {}
    public record PrepareSubscriptionCancellationArguments(@NotNull UUID subscriptionId,
            @Pattern(regexp = "USER_REQUEST|COST_SAVING|NOT_USED|OTHER") String reasonCode)
            implements ToolArguments {}
    public record SuitabilityArguments(@NotNull UUID productId, @NotNull UUID paymentAccountId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) implements ToolArguments {}
    public record PrepareWealthPurchaseArguments(@NotNull UUID suitabilityCheckId, @NotNull UUID productId,
            @NotNull UUID paymentAccountId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) implements ToolArguments {}

    private abstract static class ManagedTool<A extends ToolArguments> implements ToolHandler<A> {
        protected final ManagedOperationUseCase operations;
        protected ManagedTool(ManagedOperationUseCase operations) { this.operations = operations; }
        protected static PreparationContext context(ToolExecutionContext value) {
            return new PreparationContext(value.requestId(), value.conversationId(), value.userId(), value.idempotencyKey());
        }
        protected static ToolPayload prepared(PreparationResult result) {
            if (!result.accepted()) throw new ToolDeniedException(result.reasonCode(), result.explanation());
            return new ToolPayload(result.operation(), 1);
        }
    }

    public static final class GetCardControlsTool extends ManagedTool<CardIdArguments> {
        public GetCardControlsTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("get_card_controls",
                "Read one owned card's payment controls and version", ToolCapability.CARD_READ,
                ToolAccessMode.READ_ONLY, Set.of("cardId"), false); }
        @Override public Class<CardIdArguments> argumentsType() { return CardIdArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, CardIdArguments args) {
            return new ToolPayload(operations.getCardControls(context.userId(), args.cardId()), 1);
        }
    }

    public static final class PrepareCardStatusChangeTool extends ManagedTool<PrepareCardStatusArguments> {
        public PrepareCardStatusChangeTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("prepare_card_status_change",
                "Create a confirmation-bound owned-card status change without changing the card",
                ToolCapability.CARD_MANAGE, ToolAccessMode.PREPARE_WRITE,
                Set.of("cardId", "targetStatus", "reasonCode"), true); }
        @Override public Class<PrepareCardStatusArguments> argumentsType() { return PrepareCardStatusArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, PrepareCardStatusArguments args) {
            return prepared(operations.prepareCardStatus(context(context), args.cardId(), args.targetStatus(), args.reasonCode()));
        }
    }

    public static final class PrepareCardControlsChangeTool extends ManagedTool<PrepareCardControlsArguments> {
        public PrepareCardControlsChangeTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("prepare_card_controls_change",
                "Create a confirmation-bound owned-card controls change without changing the card",
                ToolCapability.CARD_MANAGE, ToolAccessMode.PREPARE_WRITE,
                Set.of("cardId", "onlineEnabled", "overseasEnabled", "singleTxLimit", "dailyLimit"), true); }
        @Override public Class<PrepareCardControlsArguments> argumentsType() { return PrepareCardControlsArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, PrepareCardControlsArguments args) {
            return prepared(operations.prepareCardControls(context(context), args.cardId(), args.onlineEnabled(),
                    args.overseasEnabled(), args.singleTxLimit(), args.dailyLimit()));
        }
    }

    public static final class AnalyzeSubscriptionCostsTool extends ManagedTool<SubscriptionAnalysisArguments> {
        public AnalyzeSubscriptionCostsTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("analyze_subscription_costs",
                "Deterministically calculate owned subscription monthly, annual and horizon costs",
                ToolCapability.SUBSCRIPTION_READ, ToolAccessMode.READ_ONLY,
                Set.of("subscriptionIds", "horizonMonths"), false); }
        @Override public Class<SubscriptionAnalysisArguments> argumentsType() { return SubscriptionAnalysisArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, SubscriptionAnalysisArguments args) {
            SubscriptionCostAnalysis data = operations.analyzeSubscriptions(
                    context.userId(), args.subscriptionIds(), args.horizonMonths());
            return new ToolPayload(data, data.items().size());
        }
    }

    public static final class PrepareSubscriptionCancellationTool
            extends ManagedTool<PrepareSubscriptionCancellationArguments> {
        public PrepareSubscriptionCancellationTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("prepare_subscription_cancellation",
                "Create a confirmation-bound cancellation for a verified owned subscription",
                ToolCapability.SUBSCRIPTION_MANAGE, ToolAccessMode.PREPARE_WRITE,
                Set.of("subscriptionId", "reasonCode"), true); }
        @Override public Class<PrepareSubscriptionCancellationArguments> argumentsType() {
            return PrepareSubscriptionCancellationArguments.class;
        }
        @Override public ToolPayload execute(ToolExecutionContext context, PrepareSubscriptionCancellationArguments args) {
            return prepared(operations.prepareSubscriptionCancellation(
                    context(context), args.subscriptionId(), args.reasonCode()));
        }
    }

    public static final class CheckWealthSuitabilityTool extends ManagedTool<SuitabilityArguments> {
        public CheckWealthSuitabilityTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("check_wealth_suitability",
                "Check customer risk, product, amount and owned payment account without purchasing",
                ToolCapability.WEALTH_SUITABILITY, ToolAccessMode.READ_ONLY,
                Set.of("productId", "paymentAccountId", "amount", "currency"), false); }
        @Override public Class<SuitabilityArguments> argumentsType() { return SuitabilityArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, SuitabilityArguments args) {
            return new ToolPayload(operations.checkSuitability(context.userId(), args.productId(),
                    args.paymentAccountId(), args.amount(), args.currency()), 1);
        }
    }

    public static final class GetWealthPortfolioTool extends ManagedTool<EmptyArguments> {
        public GetWealthPortfolioTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("get_wealth_portfolio",
                "Read the authenticated customer's mock wealth holdings", ToolCapability.WEALTH_PRODUCT_READ,
                ToolAccessMode.READ_ONLY, Set.<String>of(), false); }
        @Override public Class<EmptyArguments> argumentsType() { return EmptyArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, EmptyArguments args) {
            List<PortfolioItem> data = operations.portfolio(context.userId());
            return new ToolPayload(data, data.size());
        }
    }

    public static final class PrepareWealthPurchaseTool extends ManagedTool<PrepareWealthPurchaseArguments> {
        public PrepareWealthPurchaseTool(ManagedOperationUseCase operations) { super(operations); }
        @Override public ToolDefinition definition() { return toolDefinition("prepare_wealth_purchase",
                "Create an AAL2 confirmation-bound mock wealth purchase after suitability approval",
                ToolCapability.WEALTH_PURCHASE, ToolAccessMode.PREPARE_WRITE,
                Set.of("suitabilityCheckId", "productId", "paymentAccountId", "amount", "currency"), true); }
        @Override public Class<PrepareWealthPurchaseArguments> argumentsType() { return PrepareWealthPurchaseArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, PrepareWealthPurchaseArguments args) {
            return prepared(operations.prepareWealthPurchase(context(context), args.suitabilityCheckId(),
                    args.productId(), args.paymentAccountId(), args.amount(), args.currency()));
        }
    }

    private static ToolDefinition toolDefinition(String name, String description, ToolCapability capability,
            ToolAccessMode mode, Set<String> arguments, boolean confirmation) {
        return new ToolDefinition(new ToolId(name, "1.0"), description, capability, mode,
                "m5-v1", arguments, confirmation);
    }
}
