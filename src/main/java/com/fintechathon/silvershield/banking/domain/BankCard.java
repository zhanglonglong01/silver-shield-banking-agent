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
@Table(name = "bank_card")
public class BankCard {

    public enum Type { DEBIT, CREDIT }
    public enum Status { ACTIVE, FROZEN, LOST, CLOSED }

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "linked_account_id", nullable = false) private UUID linkedAccountId;
    @Column(name = "card_token", nullable = false, unique = true) private String cardToken;
    @Column(name = "card_masked", nullable = false) private String cardMasked;
    @Enumerated(EnumType.STRING) @Column(name = "card_type", nullable = false) private Type cardType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "online_enabled", nullable = false) private boolean onlineEnabled;
    @Column(name = "overseas_enabled", nullable = false) private boolean overseasEnabled;
    @Column(name = "single_tx_limit", nullable = false, precision = 19, scale = 4) private BigDecimal singleTxLimit;
    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 4) private BigDecimal dailyLimit;
    @Column(name = "expiry_month", nullable = false, length = 7, columnDefinition = "char(7)") private String expiryMonth;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected BankCard() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getLinkedAccountId() { return linkedAccountId; }
    public String getCardMasked() { return cardMasked; }
    public Type getCardType() { return cardType; }
    public Status getStatus() { return status; }
    public boolean isOnlineEnabled() { return onlineEnabled; }
    public boolean isOverseasEnabled() { return overseasEnabled; }
    public BigDecimal getSingleTxLimit() { return singleTxLimit; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public String getExpiryMonth() { return expiryMonth; }
}
