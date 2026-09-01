package com.fintechathon.silvershield.tool.model;

import java.time.Instant;
import java.util.UUID;

public record ToolGatewayResult(
        UUID invocationId,
        UUID requestId,
        UUID snapshotId,
        String toolName,
        String toolVersion,
        ToolDecision decision,
        String reasonCode,
        String explanation,
        Object data,
        int recordCount,
        Instant completedAt) {}
