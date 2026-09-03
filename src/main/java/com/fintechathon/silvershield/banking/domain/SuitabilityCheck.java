package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suitability_check")
public class SuitabilityCheck {
    public enum Status { APPROVED, DENIED }

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "payment_account_id", nullable = false) private UUID paymentAccountId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "user_risk_level", nullable = false)
    private WealthProduct.RiskLevel userRiskLevel;
    @Enumerated(EnumType.STRING) @Column(name = "product_risk_level", nullable = false)
    private WealthProduct.RiskLevel productRiskLevel;
    @Column(name = "product_version", nullable = false) private long productVersion;
    @Column(name = "account_version", nullable = false) private long accountVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "reason_code", nullable = false) private String reasonCode;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected SuitabilityCheck() {}

    public SuitabilityCheck(UUID id, UUID userId, UUID productId, UUID paymentAccountId, BigDecimal amount,
            String currency, WealthProduct.RiskLevel userRiskLevel, WealthProduct.RiskLevel productRiskLevel,
            long productVersion, long accountVersion, Status status, String reasonCode,
            Instant expiresAt, Instant createdAt) {
        this.id = id; this.userId = userId; this.productId = productId; this.paymentAccountId = paymentAccountId;
        this.amount = amount; this.currency = currency; this.userRiskLevel = userRiskLevel;
        this.productRiskLevel = productRiskLevel; this.productVersion = productVersion;
        this.accountVersion = accountVersion; this.status = status; this.reasonCode = reasonCode;
        this.expiresAt = expiresAt; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProductId() { return productId; }
    public UUID getPaymentAccountId() { return paymentAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public WealthProduct.RiskLevel getUserRiskLevel() { return userRiskLevel; }
    public WealthProduct.RiskLevel getProductRiskLevel() { return productRiskLevel; }
    public long getProductVersion() { return productVersion; }
    public long getAccountVersion() { return accountVersion; }
    public Status getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public Instant getExpiresAt() { return expiresAt; }
}
