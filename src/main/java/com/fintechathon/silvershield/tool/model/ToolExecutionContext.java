package com.fintechathon.silvershield.tool.model;

import com.fintechathon.silvershield.identity.domain.AppUser;
import com.fintechathon.silvershield.security.AssuranceLevel;
import java.util.Objects;
import java.util.UUID;

public record ToolExecutionContext(
        UUID requestId,
        UUID userId,
        AppUser.Role role,
        AssuranceLevel assuranceLevel,
        String channel,
        String idempotencyKey,
        UUID conversationId) {

    public ToolExecutionContext(
            UUID requestId, UUID userId, AppUser.Role role, AssuranceLevel assuranceLevel, String channel) {
        this(requestId, userId, role, assuranceLevel, channel, requestId.toString(), null);
    }

    public ToolExecutionContext {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(assuranceLevel, "assuranceLevel");
        Objects.requireNonNull(channel, "channel");
        if (!channel.matches("[A-Z_]{3,32}")) {
            throw new IllegalArgumentException("Malformed tool channel");
        }
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{8,128}")) {
            throw new IllegalArgumentException("Malformed idempotency key");
        }
    }
}
