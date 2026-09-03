package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wealth_holding")
public class WealthHolding {
    public enum Status { ACTIVE, CLOSED }

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "invested_amount", nullable = false, precision = 19, scale = 4) private BigDecimal investedAmount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected WealthHolding() {}

    public WealthHolding(UUID userId, UUID productId, BigDecimal amount, String currency, Instant now) {
        this.id = UUID.randomUUID(); this.userId = userId; this.productId = productId;
        this.investedAmount = amount; this.currency = currency; this.status = Status.ACTIVE;
        this.openedAt = now; this.updatedAt = now;
    }

    public void add(BigDecimal amount, Instant now) { investedAmount = investedAmount.add(amount); updatedAt = now; }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getInvestedAmount() { return investedAmount; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
}
