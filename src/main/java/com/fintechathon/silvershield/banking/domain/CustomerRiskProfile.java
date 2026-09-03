package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_risk_profile")
public class CustomerRiskProfile {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false)
    private WealthProduct.RiskLevel riskLevel;
    @Column(name = "assessed_at", nullable = false) private LocalDate assessedAt;
    @Column(name = "valid_until", nullable = false) private LocalDate validUntil;
    @Column(nullable = false) private String source;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected CustomerRiskProfile() {}

    public UUID getUserId() { return userId; }
    public WealthProduct.RiskLevel getRiskLevel() { return riskLevel; }
    public LocalDate getAssessedAt() { return assessedAt; }
    public LocalDate getValidUntil() { return validUntil; }
    public long getVersion() { return version; }
}
