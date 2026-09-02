package com.fintechathon.silvershield.operation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="risk_decision")
public class RiskDecision {
    public enum Phase { PRE_CONFIRM, FINAL_GUARD }
    public enum Outcome { ALLOW, REQUIRE_CONFIRMATION, REQUIRE_STEP_UP, DENY }
    @Id private UUID id;
    @Column(name="operation_id",nullable=false) private UUID operationId;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(name="evaluation_phase",nullable=false) private Phase phase;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Outcome outcome;
    @Column(name="policy_version",nullable=false) private String policyVersion;
    @Column(name="input_hash",nullable=false) private String inputHash;
    @Column(name="reason_codes_json",nullable=false,columnDefinition="longtext") private String reasonCodesJson;
    @Column(name="safe_explanation",nullable=false) private String safeExplanation;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RiskDecision() {}
    public RiskDecision(UUID operationId, UUID userId, Phase phase, Outcome outcome, String inputHash,
            String reasonCodesJson, String safeExplanation, Instant now){
        this.id=UUID.randomUUID();this.operationId=operationId;this.userId=userId;this.phase=phase;this.outcome=outcome;
        this.policyVersion="transfer-risk-v1";this.inputHash=inputHash;this.reasonCodesJson=reasonCodesJson;
        this.safeExplanation=safeExplanation;this.createdAt=now;
    }
}
