package com.fintechathon.silvershield.agent.evidence;

import com.fintechathon.silvershield.config.AgentProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryExecutionEvidenceStore implements ExecutionEvidenceStore {

    private final ConcurrentHashMap<UUID, ExecutionEvidence> evidence = new ConcurrentHashMap<>();
    private final AgentProperties properties;
    private final Clock clock;

    public InMemoryExecutionEvidenceStore(AgentProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ExecutionEvidence save(ExecutionEvidence item) {
        evidence.put(item.evidenceId(), item);
        return item;
    }

    @Override
    public Optional<ExecutionEvidence> find(UUID evidenceId, UUID userId) {
        ExecutionEvidence item = evidence.get(evidenceId);
        if (item == null || !item.userId().equals(userId)) {
            return Optional.empty();
        }
        Instant cutoff = Instant.now(clock).minus(properties.getConversationTtl());
        if (item.timestamp().isBefore(cutoff)) {
            evidence.remove(evidenceId, item);
            return Optional.empty();
        }
        return Optional.of(item);
    }
}
