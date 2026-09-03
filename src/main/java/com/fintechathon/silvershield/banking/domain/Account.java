package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {

    public enum Type { CHECKING, SAVINGS }
    public enum Status { ACTIVE, FROZEN, CLOSED }

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "account_token", nullable = false, unique = true) private String accountToken;
    @Column(name = "account_masked", nullable = false) private String accountMasked;
    @Column(nullable = false) private String alias;
    @Enumerated(EnumType.STRING) @Column(name = "account_type", nullable = false) private Type accountType;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 4) private BigDecimal ledgerBalance;
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4) private BigDecimal availableBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "daily_transfer_limit", nullable = false, precision = 19, scale = 4) private BigDecimal dailyTransferLimit;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected Account() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getAccountMasked() { return accountMasked; }
    public String getAlias() { return alias; }
    public Type getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public Status getStatus() { return status; }
    public BigDecimal getDailyTransferLimit() { return dailyTransferLimit; }
    public long getVersion() { return version; }
    public void debit(BigDecimal amount) {
        if (status != Status.ACTIVE || amount == null || amount.signum() <= 0
                || availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Account cannot be debited");
        }
        ledgerBalance = ledgerBalance.subtract(amount);
        availableBalance = availableBalance.subtract(amount);
    }
    public void credit(BigDecimal amount) {
        if (status != Status.ACTIVE || amount == null || amount.signum() <= 0) {
            throw new IllegalStateException("Account cannot be credited");
        }
        ledgerBalance = ledgerBalance.add(amount);
        availableBalance = availableBalance.add(amount);
    }
}
