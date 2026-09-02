package com.fintechathon.silvershield.agent.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ExecutionEvidenceFactory {

    private final ObjectMapper objectMapper;
    private final ExecutionEvidenceStore store;

    public ExecutionEvidenceFactory(ObjectMapper objectMapper, ExecutionEvidenceStore store) {
        this.objectMapper = objectMapper;
        this.store = store;
    }

    public ExecutionEvidence record(UUID userId, ToolGatewayResult result) {
        JsonNode structured = result.data() == null
                ? NullNode.getInstance()
                : objectMapper.valueToTree(result.data()).deepCopy();
        ExecutionEvidence evidence = new ExecutionEvidence(
                UUID.randomUUID(), userId, result.invocationId(), result.toolName(), result.toolVersion(),
                result.decision(), result.reasonCode(), digest(structured), structured, result.completedAt());
        return store.save(evidence);
    }

    private String digest(JsonNode structured) {
        try {
            byte[] canonical = objectMapper.writeValueAsString(structured).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to create execution evidence digest", exception);
        }
    }
}
