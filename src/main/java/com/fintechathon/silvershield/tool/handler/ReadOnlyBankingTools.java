package com.fintechathon.silvershield.tool.handler;

import com.fintechathon.silvershield.banking.application.BankingReadUseCase;
import com.fintechathon.silvershield.banking.domain.Account;
import com.fintechathon.silvershield.banking.domain.AccountTransaction;
import com.fintechathon.silvershield.banking.domain.BankCard;
import com.fintechathon.silvershield.banking.domain.Payee;
import com.fintechathon.silvershield.banking.domain.Subscription;
import com.fintechathon.silvershield.banking.domain.WealthProduct;
import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolArguments;
import com.fintechathon.silvershield.tool.model.ToolCapability;
import com.fintechathon.silvershield.tool.model.ToolDefinition;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.model.ToolPayload;
import com.fintechathon.silvershield.tool.registry.ToolHandler;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ReadOnlyBankingTools {

    private ReadOnlyBankingTools() {}

    public record NoArguments() implements ToolArguments {}

    public record AccountArguments(@NotNull UUID accountId) implements ToolArguments {}

    public record TransactionArguments(
            @NotNull UUID accountId,
            @NotNull LocalDate from,
            @NotNull LocalDate to,
            @Min(1) @Max(100) Integer limit) implements ToolArguments {

        @Override
        public void validateSemantics() {
            if (from != null && to != null && (from.isAfter(to) || from.plusDays(366).isBefore(to))) {
                throw new IllegalArgumentException("Transaction range must be ordered and within 366 days");
            }
        }
    }

    public record WealthProductArguments(
            WealthProduct.RiskLevel riskLevelMax,
            @Pattern(regexp = "[A-Z]{3}") String currency,
            WealthProduct.SaleStatus saleStatus,
            @Min(1) @Max(20) Integer limit) implements ToolArguments {}

    public record AccountView(
            UUID id, String alias, String accountMasked, String accountType, String currency,
            String ledgerBalance, String availableBalance, String status) {
        static AccountView from(Account row) {
            return new AccountView(row.getId(), row.getAlias(), row.getAccountMasked(), row.getAccountType().name(),
                    row.getCurrency(), money(row.getLedgerBalance()), money(row.getAvailableBalance()),
                    row.getStatus().name());
        }
    }

    public record BalanceView(
            UUID accountId, String alias, String accountMasked, String currency,
            String ledgerBalance, String availableBalance, String status) {
        static BalanceView from(Account row) {
            return new BalanceView(row.getId(), row.getAlias(), row.getAccountMasked(), row.getCurrency(),
                    money(row.getLedgerBalance()), money(row.getAvailableBalance()), row.getStatus().name());
        }
    }

    public record TransactionView(
            UUID id, Instant bookingTime, String direction, String amount, String currency,
            String categoryCode, String merchantName, String description, String counterpartyMasked,
            String status, String balanceAfter) {
        static TransactionView from(AccountTransaction row) {
            return new TransactionView(row.getId(), row.getBookingTime(), row.getDirection().name(),
                    money(row.getAmount()), row.getCurrency(), row.getCategoryCode(), row.getMerchantName(),
                    row.getDescription(), row.getCounterpartyMasked(), row.getStatus().name(),
                    money(row.getBalanceAfter()));
        }
    }

    public record CardView(
            UUID id, UUID linkedAccountId, String cardMasked, String cardType, String status,
            boolean onlineEnabled, boolean overseasEnabled, String singleTxLimit, String dailyLimit,
            String expiryMonth) {
        static CardView from(BankCard row) {
            return new CardView(row.getId(), row.getLinkedAccountId(), row.getCardMasked(),
                    row.getCardType().name(), row.getStatus().name(), row.isOnlineEnabled(),
                    row.isOverseasEnabled(), money(row.getSingleTxLimit()), money(row.getDailyLimit()),
                    row.getExpiryMonth());
        }
    }

    public record PayeeView(
            UUID id, String name, String bankName, String accountMasked, String currency,
            String status, boolean trusted, Instant lastUsedAt) {
        static PayeeView from(Payee row) {
            return new PayeeView(row.getId(), row.getName(), row.getBankName(), row.getAccountMasked(),
                    row.getCurrency(), row.getStatus().name(), row.isTrusted(), row.getLastUsedAt());
        }
    }

    public record SubscriptionView(
            UUID id, UUID chargeAccountId, String merchantName, String serviceName, String amount,
            String currency, String billingCycle, LocalDate nextChargeDate, String status,
            String cancellationEffect) {
        static SubscriptionView from(Subscription row) {
            return new SubscriptionView(row.getId(), row.getChargeAccountId(), row.getMerchantName(),
                    row.getServiceName(), money(row.getAmount()), row.getCurrency(), row.getBillingCycle().name(),
                    row.getNextChargeDate(), row.getStatus().name(), row.getCancellationEffect());
        }
    }

    public record WealthProductView(
            UUID id, String productCode, String name, String productType, String riskLevel,
            String currency, String minPurchase, String incrementAmount, String expectedYieldText,
            int termDays, String saleStatus, LocalDate validFrom, LocalDate validTo) {
        static WealthProductView from(WealthProduct row) {
            return new WealthProductView(row.getId(), row.getProductCode(), row.getName(),
                    row.getProductType().name(), row.getRiskLevel().name(), row.getCurrency(),
                    money(row.getMinPurchase()), money(row.getIncrementAmount()), row.getExpectedYieldText(),
                    row.getTermDays(), row.getSaleStatus().name(), row.getValidFrom(), row.getValidTo());
        }
    }

    public static final class GetAccountsTool implements ToolHandler<NoArguments> {
        private final BankingReadUseCase banking;

        public GetAccountsTool(BankingReadUseCase banking) {
            this.banking = banking;
        }

        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("get_accounts", "List the authenticated customer's masked bank accounts",
                    ToolCapability.ACCOUNT_READ, Set.of());
        }
        @Override public Class<NoArguments> argumentsType() { return NoArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, NoArguments arguments) {
            List<AccountView> data = banking.accounts(context.userId()).stream().map(AccountView::from).toList();
            return new ToolPayload(data, data.size());
        }
    }

    public static final class GetAccountBalanceTool implements ToolHandler<AccountArguments> {
        private final BankingReadUseCase banking;

        public GetAccountBalanceTool(BankingReadUseCase banking) {
            this.banking = banking;
        }

        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("get_account_balance", "Get balances for one authenticated-customer account",
                    ToolCapability.ACCOUNT_READ, Set.of("accountId"));
        }
        @Override public Class<AccountArguments> argumentsType() { return AccountArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, AccountArguments arguments) {
            return new ToolPayload(BalanceView.from(banking.account(context.userId(), arguments.accountId())), 1);
        }
    }

    public static final class ListTransactionsTool implements ToolHandler<TransactionArguments> {
        private final BankingReadUseCase banking;

        public ListTransactionsTool(BankingReadUseCase banking) {
            this.banking = banking;
        }

        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("list_transactions", "List transactions for one owned account and bounded date range",
                    ToolCapability.TRANSACTION_READ, Set.of("accountId", "from", "to", "limit"));
        }
        @Override public Class<TransactionArguments> argumentsType() { return TransactionArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, TransactionArguments arguments) {
            int limit = arguments.limit() == null ? 50 : arguments.limit();
            Instant from = arguments.from().atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant toExclusive = arguments.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            List<TransactionView> data = banking.transactions(
                            context.userId(), arguments.accountId(), from, toExclusive, limit)
                    .stream().map(TransactionView::from).toList();
            return new ToolPayload(data, data.size());
        }
    }

    public static final class ListCardsTool implements ToolHandler<NoArguments> {
        private final BankingReadUseCase banking;

        public ListCardsTool(BankingReadUseCase banking) { this.banking = banking; }
        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("list_cards", "List the authenticated customer's masked cards",
                    ToolCapability.CARD_READ, Set.of());
        }
        @Override public Class<NoArguments> argumentsType() { return NoArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, NoArguments arguments) {
            List<CardView> data = banking.cards(context.userId()).stream().map(CardView::from).toList();
            return new ToolPayload(data, data.size());
        }
    }

    public static final class ListPayeesTool implements ToolHandler<NoArguments> {
        private final BankingReadUseCase banking;

        public ListPayeesTool(BankingReadUseCase banking) { this.banking = banking; }
        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("list_payees", "List the authenticated customer's masked payees",
                    ToolCapability.PAYEE_READ, Set.of());
        }
        @Override public Class<NoArguments> argumentsType() { return NoArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, NoArguments arguments) {
            List<PayeeView> data = banking.payees(context.userId()).stream().map(PayeeView::from).toList();
            return new ToolPayload(data, data.size());
        }
    }

    public static final class ListSubscriptionsTool implements ToolHandler<NoArguments> {
        private final BankingReadUseCase banking;

        public ListSubscriptionsTool(BankingReadUseCase banking) { this.banking = banking; }
        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("list_subscriptions", "List the authenticated customer's subscription mandates",
                    ToolCapability.SUBSCRIPTION_READ, Set.of());
        }
        @Override public Class<NoArguments> argumentsType() { return NoArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, NoArguments arguments) {
            List<SubscriptionView> data = banking.subscriptions(context.userId()).stream()
                    .map(SubscriptionView::from).toList();
            return new ToolPayload(data, data.size());
        }
    }

    public static final class ListWealthProductsTool implements ToolHandler<WealthProductArguments> {
        private final BankingReadUseCase banking;

        public ListWealthProductsTool(BankingReadUseCase banking) { this.banking = banking; }
        @Override public ToolDefinition definition() {
            return ReadOnlyBankingTools.definition("list_wealth_products", "List sandbox wealth products using server-side filters",
                    ToolCapability.WEALTH_PRODUCT_READ,
                    Set.of("riskLevelMax", "currency", "saleStatus", "limit"));
        }
        @Override public Class<WealthProductArguments> argumentsType() { return WealthProductArguments.class; }
        @Override public ToolPayload execute(ToolExecutionContext context, WealthProductArguments arguments) {
            WealthProduct.SaleStatus status = arguments.saleStatus() == null
                    ? WealthProduct.SaleStatus.ON_SALE : arguments.saleStatus();
            int limit = arguments.limit() == null ? 20 : arguments.limit();
            List<WealthProductView> data = banking.wealthProducts().stream()
                    .filter(product -> product.getSaleStatus() == status)
                    .filter(product -> arguments.currency() == null
                            || arguments.currency().equals(product.getCurrency()))
                    .filter(product -> arguments.riskLevelMax() == null
                            || product.getRiskLevel().ordinal() <= arguments.riskLevelMax().ordinal())
                    .limit(limit)
                    .map(WealthProductView::from)
                    .toList();
            return new ToolPayload(data, data.size());
        }
    }

    private static ToolDefinition definition(
            String name, String description, ToolCapability capability, Set<String> acceptedArguments) {
        return new ToolDefinition(new ToolId(name, "1.0"), description, capability, ToolAccessMode.READ_ONLY,
                "m2-v1", acceptedArguments, false);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
