package com.fintechathon.silvershield.agent.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import java.time.Instant;
import java.util.UUID;

public record ExecutionEvidence(
        UUID evidenceId,
        UUID userId,
        UUID toolCallId,
        String toolName,
        String toolVersion,
        ToolDecision resultStatus,
        String reasonCode,
        String resultDigest,
        JsonNode relevantStructuredResult,
        Instant timestamp) {}
