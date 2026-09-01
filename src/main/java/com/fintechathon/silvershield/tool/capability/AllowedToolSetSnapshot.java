package com.fintechathon.silvershield.tool.capability;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintechathon.silvershield.identity.domain.AppUser;
import com.fintechathon.silvershield.security.AssuranceLevel;
import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolCapability;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record AllowedToolSetSnapshot(
        UUID snapshotId,
        UUID requestId,
        UUID userId,
        AppUser.Role role,
        AssuranceLevel assuranceLevel,
        String policyVersion,
        Instant issuedAt,
        Instant expiresAt,
        List<AllowedTool> allowedTools,
        List<DeniedTool> deniedTools,
        @JsonIgnore String signature) {

    public AllowedToolSetSnapshot {
        allowedTools = allowedTools.stream().sorted(Comparator.comparing(AllowedTool::id)).toList();
        deniedTools = deniedTools.stream().sorted(Comparator.comparing(DeniedTool::id)).toList();
    }

    public Optional<AllowedTool> allowed(ToolId id) {
        return allowedTools.stream().filter(tool -> tool.id().equals(id)).findFirst();
    }

    public record AllowedTool(
            ToolId id,
            String description,
            ToolCapability capability,
            ToolAccessMode accessMode,
            String inputSchemaVersion,
            java.util.Set<String> acceptedArguments,
            String descriptorDigest) {
        public AllowedTool {
            acceptedArguments = java.util.Set.copyOf(acceptedArguments);
        }
    }

    public record DeniedTool(ToolId id, CapabilityDenyReason reason) {}
}
