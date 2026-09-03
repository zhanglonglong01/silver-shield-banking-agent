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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "wealth_product")
public class WealthProduct {

    public enum Type { DEPOSIT, BOND, FUND, STRUCTURED }
    public enum RiskLevel { R1, R2, R3, R4, R5 }
    public enum SaleStatus { ON_SALE, SUSPENDED, ENDED }

    @Id private UUID id;
    @Column(name = "product_code", nullable = false, unique = true) private String productCode;
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "product_type", nullable = false) private Type productType;
    @Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false) private RiskLevel riskLevel;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "min_purchase", nullable = false, precision = 19, scale = 4) private BigDecimal minPurchase;
    @Column(name = "increment_amount", nullable = false, precision = 19, scale = 4) private BigDecimal incrementAmount;
    @Column(name = "expected_yield_text", nullable = false) private String expectedYieldText;
    @Column(name = "term_days", nullable = false) private int termDays;
    @Enumerated(EnumType.STRING) @Column(name = "sale_status", nullable = false) private SaleStatus saleStatus;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_to", nullable = false) private LocalDate validTo;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected WealthProduct() {}

    public UUID getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getName() { return name; }
    public Type getProductType() { return productType; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getCurrency() { return currency; }
    public BigDecimal getMinPurchase() { return minPurchase; }
    public BigDecimal getIncrementAmount() { return incrementAmount; }
    public String getExpectedYieldText() { return expectedYieldText; }
    public int getTermDays() { return termDays; }
    public SaleStatus getSaleStatus() { return saleStatus; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public long getVersion() { return version; }
}
