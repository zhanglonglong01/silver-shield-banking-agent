package com.fintechathon.silvershield.tool.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintechathon.silvershield.security.BankingPrincipal;
import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import com.fintechathon.silvershield.tool.capability.CapabilityPolicy;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ToolRuntimeService {

    private final CapabilityPolicy capabilityPolicy;
    private final SecureToolGateway gateway;

    public ToolRuntimeService(CapabilityPolicy capabilityPolicy, SecureToolGateway gateway) {
        this.capabilityPolicy = capabilityPolicy;
        this.gateway = gateway;
    }

    public AllowedToolSetSnapshot allowedTools(BankingPrincipal principal, List<ToolReference> candidates) {
        ToolExecutionContext context = context(principal);
        Set<ToolId> requested = new LinkedHashSet<>();
        for (ToolReference candidate : candidates) {
            requested.add(new ToolId(candidate.name(), candidate.version()));
        }
        return capabilityPolicy.issue(context, Set.copyOf(requested));
    }

    public ToolGatewayResult execute(
            BankingPrincipal principal, String toolName, String toolVersion, JsonNode arguments,
            String idempotencyKey) {
        UUID requestId = UUID.randomUUID();
        ToolExecutionContext context = new ToolExecutionContext(
                requestId, principal.userId(), principal.role(), principal.assuranceLevel(), "DIRECT_API",
                idempotencyKey == null || idempotencyKey.isBlank() ? requestId.toString() : idempotencyKey, null);
        Set<ToolId> candidates;
        try {
            candidates = Set.of(new ToolId(toolName, toolVersion));
        } catch (RuntimeException exception) {
            candidates = Set.of();
        }
        AllowedToolSetSnapshot snapshot = capabilityPolicy.issue(context, candidates);
        return gateway.execute(context, snapshot, toolName, toolVersion, arguments);
    }

    public ToolGatewayResult execute(
            BankingPrincipal principal, String toolName, String toolVersion, JsonNode arguments) {
        return execute(principal, toolName, toolVersion, arguments, null);
    }

    private static ToolExecutionContext context(BankingPrincipal principal) {
        return new ToolExecutionContext(UUID.randomUUID(), principal.userId(), principal.role(),
                principal.assuranceLevel(), "DIRECT_API");
    }

    public record ToolReference(String name, String version) {}
}
