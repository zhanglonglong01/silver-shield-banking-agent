package com.fintechathon.silvershield.audit.infrastructure;

import com.fintechathon.silvershield.tool.audit.ToolSecurityAuditRecord;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_security_audit")
class ToolSecurityAuditEntity {

    @Id private UUID id;
    @Column(name = "invocation_id", nullable = false, unique = true) private UUID invocationId;
    @Column(name = "request_id", nullable = false) private UUID requestId;
    @Column(name = "snapshot_id") private UUID snapshotId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "tool_name", nullable = false, length = 128) private String toolName;
    @Column(name = "tool_version", nullable = false, length = 32) private String toolVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ToolDecision decision;
    @Column(name = "reason_code", nullable = false, length = 64) private String reasonCode;
    @Column(name = "safe_explanation", nullable = false, length = 255) private String safeExplanation;
    @Column(name = "arguments_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String argumentsHash;
    @Column(name = "policy_version", nullable = false, length = 64) private String policyVersion;
    @Column(name = "duration_ms", nullable = false) private long durationMs;
    @Column(name = "result_count", nullable = false) private int resultCount;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected ToolSecurityAuditEntity() {}

    ToolSecurityAuditEntity(ToolSecurityAuditRecord record) {
        this.id = record.id();
        this.invocationId = record.invocationId();
        this.requestId = record.requestId();
        this.snapshotId = record.snapshotId();
        this.userId = record.userId();
        this.toolName = record.toolName();
        this.toolVersion = record.toolVersion();
        this.decision = record.decision();
        this.reasonCode = record.reasonCode();
        this.safeExplanation = record.safeExplanation();
        this.argumentsHash = record.argumentsHash();
        this.policyVersion = record.policyVersion();
        this.durationMs = record.durationMs();
        this.resultCount = record.resultCount();
        this.occurredAt = record.occurredAt();
    }
}
