package com.fintechathon.silvershield.sandbox;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SandboxDataset(
        String datasetVersion,
        long randomSeed,
        LocalDate baselineDate,
        Instant generatedAt,
        List<UserSeed> users,
        List<RiskProfileSeed> riskProfiles,
        List<AccountSeed> accounts,
        List<CardSeed> cards,
        List<TransactionSeed> transactions,
        List<PayeeSeed> payees,
        List<SubscriptionSeed> subscriptions,
        List<WealthProductSeed> wealthProducts) {

    public record UserSeed(
            UUID id,
            String username,
            String passwordHash,
            String displayName,
            String role,
            String status,
            Instant createdAt) {}

    public record RiskProfileSeed(
            UUID id,
            UUID userId,
            String riskLevel,
            LocalDate assessedAt,
            LocalDate validUntil,
            String source,
            Instant createdAt) {}

    public record AccountSeed(
            UUID id,
            UUID userId,
            String accountToken,
            String accountMasked,
            String alias,
            String accountType,
            String currency,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            String status,
            BigDecimal dailyTransferLimit,
            Instant createdAt) {}

    public record CardSeed(
            UUID id,
            UUID userId,
            UUID linkedAccountId,
            String cardToken,
            String cardMasked,
            String cardType,
            String status,
            boolean onlineEnabled,
            boolean overseasEnabled,
            BigDecimal singleTxLimit,
            BigDecimal dailyLimit,
            String expiryMonth,
            Instant createdAt) {}

    public record TransactionSeed(
            UUID id,
            UUID accountId,
            Instant bookingTime,
            String direction,
            BigDecimal amount,
            String currency,
            String categoryCode,
            String merchantName,
            String description,
            String counterpartyMasked,
            String status,
            BigDecimal balanceAfter) {}

    public record PayeeSeed(
            UUID id,
            UUID userId,
            String name,
            String bankName,
            String accountToken,
            String accountMasked,
            UUID internalAccountId,
            String currency,
            String status,
            boolean trusted,
            Instant lastUsedAt,
            Instant createdAt) {}

    public record SubscriptionSeed(
            UUID id,
            UUID userId,
            UUID chargeAccountId,
            String merchantName,
            String serviceName,
            BigDecimal amount,
            String currency,
            String billingCycle,
            LocalDate nextChargeDate,
            String status,
            String detectedFrom,
            String cancellationEffect,
            Instant createdAt) {}

    public record WealthProductSeed(
            UUID id,
            String productCode,
            String name,
            String productType,
            String riskLevel,
            String currency,
            BigDecimal minPurchase,
            BigDecimal incrementAmount,
            String expectedYieldText,
            int termDays,
            String saleStatus,
            LocalDate validFrom,
            LocalDate validTo,
            Instant createdAt) {}
}
