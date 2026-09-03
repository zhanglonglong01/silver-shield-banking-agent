package com.fintechathon.silvershield.tool.capability;

import com.fintechathon.silvershield.config.ToolSecurityProperties;
import com.fintechathon.silvershield.identity.domain.AppUser;
import com.fintechathon.silvershield.tool.model.ToolCapability;
import com.fintechathon.silvershield.tool.model.ToolDefinition;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.registry.ExplicitToolRegistry;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class DefaultCapabilityPolicy implements CapabilityPolicy {

    private static final int MAX_CANDIDATES = 20;
    private static final Set<ToolCapability> CUSTOMER_CAPABILITIES = EnumSet.of(
            ToolCapability.ACCOUNT_READ,
            ToolCapability.TRANSACTION_READ,
            ToolCapability.CARD_READ,
            ToolCapability.PAYEE_READ,
            ToolCapability.SUBSCRIPTION_READ,
            ToolCapability.WEALTH_PRODUCT_READ,
            ToolCapability.TRANSFER_PREPARE,
            ToolCapability.CARD_MANAGE,
            ToolCapability.SUBSCRIPTION_MANAGE,
            ToolCapability.WEALTH_SUITABILITY,
            ToolCapability.WEALTH_PURCHASE,
            ToolCapability.OPERATION_READ);

    private final ExplicitToolRegistry registry;
    private final ToolSecurityProperties properties;
    private final Clock clock;
    private final byte[] signingKey = new byte[32];

    public DefaultCapabilityPolicy(
            ExplicitToolRegistry registry, ToolSecurityProperties properties, Clock clock) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
        new SecureRandom().nextBytes(signingKey);
        if (properties.getSnapshotTtl() == null || properties.getSnapshotTtl().isZero()
                || properties.getSnapshotTtl().isNegative()) {
            throw new IllegalArgumentException("Capability snapshot TTL must be positive");
        }
    }

    @Override
    public AllowedToolSetSnapshot issue(ToolExecutionContext context, Set<ToolId> requestedCandidates) {
        if (requestedCandidates == null || requestedCandidates.size() > MAX_CANDIDATES) {
            throw new IllegalArgumentException("Capability candidate count exceeds limit");
        }
        List<AllowedToolSetSnapshot.AllowedTool> allowed = new ArrayList<>();
        List<AllowedToolSetSnapshot.DeniedTool> denied = new ArrayList<>();

        for (ToolId candidate : requestedCandidates) {
            ToolDefinition definition = registry.definition(candidate).orElse(null);
            if (definition == null) {
                denied.add(new AllowedToolSetSnapshot.DeniedTool(
                        candidate, CapabilityDenyReason.TOOL_NOT_REGISTERED));
            } else if (context.role() != AppUser.Role.CUSTOMER) {
                denied.add(new AllowedToolSetSnapshot.DeniedTool(candidate, CapabilityDenyReason.ROLE_NOT_ALLOWED));
            } else if (!CUSTOMER_CAPABILITIES.contains(definition.requiredCapability())) {
                denied.add(new AllowedToolSetSnapshot.DeniedTool(
                        candidate, CapabilityDenyReason.CAPABILITY_NOT_GRANTED));
            } else {
                allowed.add(new AllowedToolSetSnapshot.AllowedTool(
                        candidate, definition.description(), definition.requiredCapability(), definition.accessMode(),
                        definition.inputSchemaVersion(), definition.acceptedArguments(), definition.descriptorDigest()));
            }
        }

        Instant issuedAt = Instant.now(clock);
        AllowedToolSetSnapshot unsigned = new AllowedToolSetSnapshot(
                UUID.randomUUID(), context.requestId(), context.userId(), context.role(), context.assuranceLevel(),
                properties.getPolicyVersion(), issuedAt, issuedAt.plus(properties.getSnapshotTtl()),
                allowed, denied, "");
        return new AllowedToolSetSnapshot(
                unsigned.snapshotId(), unsigned.requestId(), unsigned.userId(), unsigned.role(),
                unsigned.assuranceLevel(), unsigned.policyVersion(), unsigned.issuedAt(), unsigned.expiresAt(),
                unsigned.allowedTools(), unsigned.deniedTools(), sign(unsigned));
    }

    @Override
    public CapabilityVerification verify(
            AllowedToolSetSnapshot snapshot, ToolExecutionContext context, ToolId requestedTool) {
        if (snapshot == null || snapshot.signature() == null || !MessageDigest.isEqual(
                sign(withoutSignature(snapshot)).getBytes(StandardCharsets.US_ASCII),
                snapshot.signature().getBytes(StandardCharsets.US_ASCII))) {
            return CapabilityVerification.deny(CapabilityDenyReason.SNAPSHOT_INVALID);
        }
        if (!properties.getPolicyVersion().equals(snapshot.policyVersion())) {
            return CapabilityVerification.deny(CapabilityDenyReason.SNAPSHOT_INVALID);
        }
        Instant now = Instant.now(clock);
        if (now.isBefore(snapshot.issuedAt()) || !now.isBefore(snapshot.expiresAt())) {
            return CapabilityVerification.deny(CapabilityDenyReason.SNAPSHOT_EXPIRED);
        }
        if (!snapshot.requestId().equals(context.requestId()) || !snapshot.userId().equals(context.userId())
                || snapshot.role() != context.role() || snapshot.assuranceLevel() != context.assuranceLevel()) {
            return CapabilityVerification.deny(CapabilityDenyReason.SNAPSHOT_CONTEXT_MISMATCH);
        }
        return snapshot.allowed(requestedTool).isPresent()
                ? CapabilityVerification.allow()
                : CapabilityVerification.deny(CapabilityDenyReason.TOOL_NOT_IN_ALLOWED_SET);
    }

    private AllowedToolSetSnapshot withoutSignature(AllowedToolSetSnapshot snapshot) {
        return new AllowedToolSetSnapshot(
                snapshot.snapshotId(), snapshot.requestId(), snapshot.userId(), snapshot.role(),
                snapshot.assuranceLevel(), snapshot.policyVersion(), snapshot.issuedAt(), snapshot.expiresAt(),
                snapshot.allowedTools(), snapshot.deniedTools(), "");
    }

    private String sign(AllowedToolSetSnapshot snapshot) {
        StringBuilder canonical = new StringBuilder()
                .append(snapshot.snapshotId()).append('|')
                .append(snapshot.requestId()).append('|')
                .append(snapshot.userId()).append('|')
                .append(snapshot.role()).append('|')
                .append(snapshot.assuranceLevel()).append('|')
                .append(snapshot.policyVersion()).append('|')
                .append(snapshot.issuedAt()).append('|')
                .append(snapshot.expiresAt());
        snapshot.allowedTools().forEach(tool -> canonical.append("|A:").append(tool.id()).append(':')
                .append(tool.description()).append(':').append(tool.capability()).append(':')
                .append(tool.accessMode()).append(':').append(tool.inputSchemaVersion()).append(':')
                .append(String.join(",", new java.util.TreeSet<>(tool.acceptedArguments()))).append(':')
                .append(tool.descriptorDigest()));
        snapshot.deniedTools().forEach(tool -> canonical.append("|D:").append(tool.id()).append(':')
                .append(tool.reason()));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
