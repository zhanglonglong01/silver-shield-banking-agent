package com.fintechathon.silvershield.tool.audit;

import com.fintechathon.silvershield.tool.model.ToolDecision;
import java.time.Instant;
import java.util.UUID;

public record ToolSecurityAuditRecord(
        UUID id,
        UUID invocationId,
        UUID requestId,
        UUID snapshotId,
        UUID userId,
        String toolName,
        String toolVersion,
        ToolDecision decision,
        String reasonCode,
        String safeExplanation,
        String argumentsHash,
        String policyVersion,
        long durationMs,
        int resultCount,
        Instant occurredAt) {}
