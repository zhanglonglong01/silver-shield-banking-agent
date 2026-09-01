package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_transaction")
public class AccountTransaction {

    public enum Direction { IN, OUT }
    public enum Status { POSTED, REVERSED }

    @Id private UUID id;
    @Column(name = "account_id", nullable = false) private UUID accountId;
    @Column(name = "booking_time", nullable = false) private Instant bookingTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Direction direction;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "category_code", nullable = false) private String categoryCode;
    @Column(name = "merchant_name", nullable = false) private String merchantName;
    @Column(nullable = false) private String description;
    @Column(name = "counterparty_masked") private String counterpartyMasked;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4) private BigDecimal balanceAfter;

    protected AccountTransaction() {}

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public Instant getBookingTime() { return bookingTime; }
    public Direction getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCategoryCode() { return categoryCode; }
    public String getMerchantName() { return merchantName; }
    public String getDescription() { return description; }
    public String getCounterpartyMasked() { return counterpartyMasked; }
    public Status getStatus() { return status; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
}
