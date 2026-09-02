package com.fintechathon.silvershield.agent.evidence;

import java.util.Optional;
import java.util.UUID;

public interface ExecutionEvidenceStore {

    ExecutionEvidence save(ExecutionEvidence evidence);

    Optional<ExecutionEvidence> find(UUID evidenceId, UUID userId);
}
