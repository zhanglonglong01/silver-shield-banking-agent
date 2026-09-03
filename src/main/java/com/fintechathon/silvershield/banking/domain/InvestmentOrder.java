package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investment_order")
public class InvestmentOrder {
    public enum Status { SUCCEEDED, FAILED }

    @Id private UUID id;
    @Column(name = "operation_id", nullable = false) private UUID operationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "payment_account_id", nullable = false) private UUID paymentAccountId;
    @Column(name = "suitability_check_id", nullable = false) private UUID suitabilityCheckId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4) private BigDecimal balanceBefore;
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4) private BigDecimal balanceAfter;
    @Column(name = "completed_at", nullable = false) private Instant completedAt;
    @Version private long version;

    protected InvestmentOrder() {}

    public InvestmentOrder(UUID id, UUID operationId, UUID userId, UUID productId, UUID paymentAccountId,
            UUID suitabilityCheckId, BigDecimal amount, String currency, BigDecimal balanceBefore,
            BigDecimal balanceAfter, Instant completedAt) {
        this.id = id; this.operationId = operationId; this.userId = userId; this.productId = productId;
        this.paymentAccountId = paymentAccountId; this.suitabilityCheckId = suitabilityCheckId;
        this.amount = amount; this.currency = currency; this.status = Status.SUCCEEDED;
        this.balanceBefore = balanceBefore; this.balanceAfter = balanceAfter; this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getOperationId() { return operationId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getCompletedAt() { return completedAt; }
}
