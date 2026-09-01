package com.fintechathon.silvershield.banking.api;

import com.fintechathon.silvershield.banking.application.BankingResourceQueryService;
import com.fintechathon.silvershield.banking.domain.Account;
import com.fintechathon.silvershield.banking.domain.AccountTransaction;
import com.fintechathon.silvershield.banking.domain.BankCard;
import com.fintechathon.silvershield.banking.domain.Payee;
import com.fintechathon.silvershield.banking.domain.Subscription;
import com.fintechathon.silvershield.banking.domain.WealthProduct;
import com.fintechathon.silvershield.security.BankingPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BankingResourceController {

    private final BankingResourceQueryService queryService;

    public BankingResourceController(BankingResourceQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> accounts(@AuthenticationPrincipal BankingPrincipal principal) {
        return queryService.accounts(principal.userId()).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/accounts/{accountId}")
    public AccountResponse account(
            @AuthenticationPrincipal BankingPrincipal principal, @PathVariable UUID accountId) {
        return AccountResponse.from(queryService.account(principal.userId(), accountId));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> transactions(
            @AuthenticationPrincipal BankingPrincipal principal,
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "50") int limit) {
        if (from.isAfter(to) || from.plusDays(366).isBefore(to)) {
            throw new IllegalArgumentException("Transaction range must be within 366 days");
        }
        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return queryService.transactions(principal.userId(), accountId, fromInstant, toExclusive, limit)
                .stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/cards")
    public List<CardResponse> cards(@AuthenticationPrincipal BankingPrincipal principal) {
        return queryService.cards(principal.userId()).stream().map(CardResponse::from).toList();
    }

    @GetMapping("/payees")
    public List<PayeeResponse> payees(@AuthenticationPrincipal BankingPrincipal principal) {
        return queryService.payees(principal.userId()).stream().map(PayeeResponse::from).toList();
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionResponse> subscriptions(@AuthenticationPrincipal BankingPrincipal principal) {
        return queryService.subscriptions(principal.userId()).stream().map(SubscriptionResponse::from).toList();
    }

    @GetMapping("/wealth-products")
    public List<WealthProductResponse> wealthProducts() {
        return queryService.wealthProducts().stream().map(WealthProductResponse::from).toList();
    }

    public record AccountResponse(
            UUID id, String alias, String accountMasked, String accountType, String currency,
            String ledgerBalance, String availableBalance, String status, String dailyTransferLimit) {
        static AccountResponse from(Account row) {
            return new AccountResponse(row.getId(), row.getAlias(), row.getAccountMasked(),
                    row.getAccountType().name(), row.getCurrency(), money(row.getLedgerBalance()),
                    money(row.getAvailableBalance()), row.getStatus().name(), money(row.getDailyTransferLimit()));
        }
    }

    public record TransactionResponse(
            UUID id, Instant bookingTime, String direction, String amount, String currency,
            String categoryCode, String merchantName, String description, String counterpartyMasked,
            String status, String balanceAfter) {
        static TransactionResponse from(AccountTransaction row) {
            return new TransactionResponse(row.getId(), row.getBookingTime(), row.getDirection().name(),
                    money(row.getAmount()), row.getCurrency(), row.getCategoryCode(), row.getMerchantName(),
                    row.getDescription(), row.getCounterpartyMasked(), row.getStatus().name(),
                    money(row.getBalanceAfter()));
        }
    }

    public record CardResponse(
            UUID id, UUID linkedAccountId, String cardMasked, String cardType, String status,
            boolean onlineEnabled, boolean overseasEnabled, String singleTxLimit, String dailyLimit,
            String expiryMonth) {
        static CardResponse from(BankCard row) {
            return new CardResponse(row.getId(), row.getLinkedAccountId(), row.getCardMasked(),
                    row.getCardType().name(), row.getStatus().name(), row.isOnlineEnabled(),
                    row.isOverseasEnabled(), money(row.getSingleTxLimit()), money(row.getDailyLimit()),
                    row.getExpiryMonth());
        }
    }

    public record PayeeResponse(
            UUID id, String name, String bankName, String accountMasked, String currency,
            String status, boolean trusted, Instant lastUsedAt) {
        static PayeeResponse from(Payee row) {
            return new PayeeResponse(row.getId(), row.getName(), row.getBankName(), row.getAccountMasked(),
                    row.getCurrency(), row.getStatus().name(), row.isTrusted(), row.getLastUsedAt());
        }
    }

    public record SubscriptionResponse(
            UUID id, UUID chargeAccountId, String merchantName, String serviceName, String amount,
            String currency, String billingCycle, LocalDate nextChargeDate, String status,
            String cancellationEffect) {
        static SubscriptionResponse from(Subscription row) {
            return new SubscriptionResponse(row.getId(), row.getChargeAccountId(), row.getMerchantName(),
                    row.getServiceName(), money(row.getAmount()), row.getCurrency(), row.getBillingCycle().name(),
                    row.getNextChargeDate(), row.getStatus().name(), row.getCancellationEffect());
        }
    }

    public record WealthProductResponse(
            UUID id, String productCode, String name, String productType, String riskLevel, String currency,
            String minPurchase, String incrementAmount, String expectedYieldText, int termDays,
            String saleStatus, LocalDate validFrom, LocalDate validTo) {
        static WealthProductResponse from(WealthProduct row) {
            return new WealthProductResponse(row.getId(), row.getProductCode(), row.getName(),
                    row.getProductType().name(), row.getRiskLevel().name(), row.getCurrency(),
                    money(row.getMinPurchase()), money(row.getIncrementAmount()), row.getExpectedYieldText(),
                    row.getTermDays(), row.getSaleStatus().name(), row.getValidFrom(), row.getValidTo());
        }
    }

    private static String money(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
