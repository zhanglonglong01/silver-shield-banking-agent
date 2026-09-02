package com.fintechathon.silvershield.sandbox;

import java.sql.Date;
import java.sql.Timestamp;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev", "test"})
public class SandboxDatasetStore {

    private final JdbcTemplate jdbcTemplate;

    public SandboxDatasetStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clear() {
        jdbcTemplate.update("DELETE FROM sandbox_dataset_metadata");
        jdbcTemplate.update("DELETE FROM operation_audit");
        jdbcTemplate.update("DELETE FROM transfer_order");
        jdbcTemplate.update("DELETE FROM idempotency_record");
        jdbcTemplate.update("DELETE FROM risk_decision");
        jdbcTemplate.update("DELETE FROM confirmation_challenge");
        jdbcTemplate.update("DELETE FROM operation_request");
        jdbcTemplate.update("DELETE FROM tool_security_audit");
        jdbcTemplate.update("DELETE FROM account_transaction");
        jdbcTemplate.update("DELETE FROM subscription");
        jdbcTemplate.update("DELETE FROM payee");
        jdbcTemplate.update("DELETE FROM bank_card");
        jdbcTemplate.update("DELETE FROM wealth_product");
        jdbcTemplate.update("DELETE FROM account");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    public void insert(SandboxDataset dataset) {
        for (SandboxDataset.UserSeed row : dataset.users()) {
            jdbcTemplate.update("""
                    INSERT INTO app_user
                    (id, username, password_hash, display_name, role_code, status, created_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                    """, row.id().toString(), row.username(), row.passwordHash(), row.displayName(), row.role(),
                    row.status(), Timestamp.from(row.createdAt()));
        }
        for (SandboxDataset.AccountSeed row : dataset.accounts()) {
            jdbcTemplate.update("""
                    INSERT INTO account
                    (id, user_id, account_token, account_masked, alias, account_type, currency,
                     ledger_balance, available_balance, status, daily_transfer_limit, created_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, row.id().toString(), row.userId().toString(), row.accountToken(), row.accountMasked(),
                    row.alias(), row.accountType(), row.currency(), row.ledgerBalance(), row.availableBalance(),
                    row.status(), row.dailyTransferLimit(), Timestamp.from(row.createdAt()));
        }
        for (SandboxDataset.CardSeed row : dataset.cards()) {
            jdbcTemplate.update("""
                    INSERT INTO bank_card
                    (id, user_id, linked_account_id, card_token, card_masked, card_type, status,
                     online_enabled, overseas_enabled, single_tx_limit, daily_limit, expiry_month, created_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, row.id().toString(), row.userId().toString(), row.linkedAccountId().toString(),
                    row.cardToken(), row.cardMasked(), row.cardType(), row.status(), row.onlineEnabled(),
                    row.overseasEnabled(), row.singleTxLimit(), row.dailyLimit(), row.expiryMonth(),
                    Timestamp.from(row.createdAt()));
        }
        for (SandboxDataset.PayeeSeed row : dataset.payees()) {
            jdbcTemplate.update("""
                    INSERT INTO payee
                    (id, user_id, name, bank_name, account_token, account_masked, internal_account_id,
                     currency, status, trusted, last_used_at, created_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, row.id().toString(), row.userId().toString(), row.name(), row.bankName(), row.accountToken(),
                    row.accountMasked(), row.internalAccountId() == null ? null : row.internalAccountId().toString(),
                    row.currency(), row.status(), row.trusted(),
                    row.lastUsedAt() == null ? null : Timestamp.from(row.lastUsedAt()), Timestamp.from(row.createdAt()));
        }
        for (SandboxDataset.TransactionSeed row : dataset.transactions()) {
            jdbcTemplate.update("""
                    INSERT INTO account_transaction
                    (id, account_id, booking_time, direction, amount, currency, category_code, merchant_name,
                     description, counterparty_masked, status, balance_after)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row.id().toString(), row.accountId().toString(), Timestamp.from(row.bookingTime()),
                    row.direction(), row.amount(), row.currency(), row.categoryCode(), row.merchantName(),
                    row.description(), row.counterpartyMasked(), row.status(), row.balanceAfter());
        }
        for (SandboxDataset.SubscriptionSeed row : dataset.subscriptions()) {
            jdbcTemplate.update("""
                    INSERT INTO subscription
                    (id, user_id, charge_account_id, merchant_name, service_name, amount, currency, billing_cycle,
                     next_charge_date, status, detected_from, cancellation_effect, created_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, row.id().toString(), row.userId().toString(), row.chargeAccountId().toString(),
                    row.merchantName(), row.serviceName(), row.amount(), row.currency(), row.billingCycle(),
                    row.nextChargeDate() == null ? null : Date.valueOf(row.nextChargeDate()), row.status(),
                    row.detectedFrom(), row.cancellationEffect(), Timestamp.from(row.createdAt()));
        }
        for (SandboxDataset.WealthProductSeed row : dataset.wealthProducts()) {
            jdbcTemplate.update("""
                    INSERT INTO wealth_product
                    (id, product_code, name, product_type, risk_level, currency, min_purchase, increment_amount,
                     expected_yield_text, term_days, sale_status, valid_from, valid_to, created_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, row.id().toString(), row.productCode(), row.name(), row.productType(), row.riskLevel(),
                    row.currency(), row.minPurchase(), row.incrementAmount(), row.expectedYieldText(), row.termDays(),
                    row.saleStatus(), Date.valueOf(row.validFrom()), Date.valueOf(row.validTo()),
                    Timestamp.from(row.createdAt()));
        }
    }

    public void insertMetadata(SandboxDataset dataset, String digest) {
        jdbcTemplate.update("""
                INSERT INTO sandbox_dataset_metadata
                (dataset_id, dataset_version, random_seed, baseline_date, dataset_digest, generated_at)
                VALUES ('banking-sandbox', ?, ?, ?, ?, ?)
                """, dataset.datasetVersion(), dataset.randomSeed(), Date.valueOf(dataset.baselineDate()), digest,
                Timestamp.from(dataset.generatedAt()));
    }
}
