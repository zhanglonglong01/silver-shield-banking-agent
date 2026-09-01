package com.fintechathon.silvershield.tool.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import com.fintechathon.silvershield.tool.audit.ToolSecurityAuditRecord;
import com.fintechathon.silvershield.tool.audit.ToolSecurityAuditSink;
import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import com.fintechathon.silvershield.tool.capability.CapabilityPolicy;
import com.fintechathon.silvershield.tool.capability.CapabilityVerification;
import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolArguments;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.model.ToolPayload;
import com.fintechathon.silvershield.tool.registry.ExplicitToolRegistry;
import com.fintechathon.silvershield.tool.registry.ToolHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class SecureToolGateway {

    private final CapabilityPolicy capabilityPolicy;
    private final ExplicitToolRegistry registry;
    private final ToolArgumentsValidator argumentsValidator;
    private final ToolSecurityAuditSink auditSink;
    private final Clock clock;

    public SecureToolGateway(
            CapabilityPolicy capabilityPolicy,
            ExplicitToolRegistry registry,
            ToolArgumentsValidator argumentsValidator,
            ToolSecurityAuditSink auditSink,
            Clock clock) {
        this.capabilityPolicy = capabilityPolicy;
        this.registry = registry;
        this.argumentsValidator = argumentsValidator;
        this.auditSink = auditSink;
        this.clock = clock;
    }

    public ToolGatewayResult execute(
            ToolExecutionContext context,
            AllowedToolSetSnapshot snapshot,
            String rawToolName,
            String rawToolVersion,
            JsonNode rawArguments) {
        UUID invocationId = UUID.randomUUID();
        long startedNanos = System.nanoTime();
        String argumentsHash = argumentsValidator.fingerprint(rawArguments);
        ToolId id;
        try {
            id = new ToolId(rawToolName, rawToolVersion);
        } catch (RuntimeException exception) {
            return finish(invocationId, context, snapshot, safe(rawToolName), safe(rawToolVersion),
                    ToolDecision.DENY, "MALFORMED_TOOL_ID", "Tool 标识不符合白名单格式", null, 0,
                    argumentsHash, startedNanos);
        }

        CapabilityVerification verification = capabilityPolicy.verify(snapshot, context, id);
        if (!verification.allowed()) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.DENY,
                    verification.denyReason().name(), "当前请求的 Allowed Tool Set 不允许该 Tool",
                    null, 0, argumentsHash, startedNanos);
        }

        ToolHandler<?> handler = registry.resolve(id).orElse(null);
        if (handler == null) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.DENY,
                    "TOOL_NOT_REGISTERED", "Tool 不在显式注册表中", null, 0, argumentsHash, startedNanos);
        }

        AllowedToolSetSnapshot.AllowedTool allowed = snapshot.allowed(id).orElseThrow();
        if (!allowed.descriptorDigest().equals(handler.definition().descriptorDigest())) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.DENY,
                    "TOOL_DEFINITION_CHANGED", "Tool 定义与策略快照不一致", null, 0,
                    argumentsHash, startedNanos);
        }
        if (handler.definition().accessMode() != ToolAccessMode.READ_ONLY
                || handler.definition().confirmationRequired()) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.DENY,
                    "WRITE_TOOL_BLOCKED_IN_M2", "M2 Gateway 只允许无确认的只读 Tool", null, 0,
                    argumentsHash, startedNanos);
        }

        ToolArgumentsValidator.ValidationResult validation =
                argumentsValidator.validate(rawArguments, handler.argumentsType());
        if (!validation.valid()) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.DENY,
                    "ARGUMENTS_INVALID", "Tool 参数未通过严格 Schema 与语义校验", null, 0,
                    argumentsHash, startedNanos);
        }

        try {
            ToolPayload payload = invoke(handler, context, validation.arguments());
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.ALLOW,
                    "READ_ONLY_EXECUTED", "权限、快照、白名单和参数校验均通过", payload.data(),
                    payload.recordCount(), argumentsHash, startedNanos);
        } catch (ResourceNotFoundException exception) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.DENY,
                    "RESOURCE_NOT_AVAILABLE", "资源不存在或不属于当前用户", null, 0,
                    argumentsHash, startedNanos);
        } catch (RuntimeException exception) {
            return finish(invocationId, context, snapshot, id.name(), id.version(), ToolDecision.ERROR,
                    "TOOL_EXECUTION_FAILED", "Tool 执行失败，未返回不可信结果", null, 0,
                    argumentsHash, startedNanos);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ToolPayload invoke(
            ToolHandler handler, ToolExecutionContext context, ToolArguments arguments) {
        return handler.execute(context, arguments);
    }

    private ToolGatewayResult finish(
            UUID invocationId,
            ToolExecutionContext context,
            AllowedToolSetSnapshot snapshot,
            String toolName,
            String toolVersion,
            ToolDecision decision,
            String reasonCode,
            String explanation,
            Object data,
            int resultCount,
            String argumentsHash,
            long startedNanos) {
        Instant completedAt = Instant.now(clock);
        long durationMs = Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
        UUID snapshotId = snapshot == null ? null : snapshot.snapshotId();
        String policyVersion = snapshot == null ? "unavailable" : snapshot.policyVersion();
        auditSink.record(new ToolSecurityAuditRecord(
                UUID.randomUUID(), invocationId, context.requestId(), snapshotId, context.userId(),
                toolName, toolVersion, decision, reasonCode, explanation, argumentsHash, policyVersion,
                durationMs, resultCount, completedAt));
        return new ToolGatewayResult(
                invocationId, context.requestId(), snapshotId, toolName, toolVersion, decision,
                reasonCode, explanation, data, resultCount, completedAt);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "invalid";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.substring(0, Math.min(128, sanitized.length()));
    }
}
