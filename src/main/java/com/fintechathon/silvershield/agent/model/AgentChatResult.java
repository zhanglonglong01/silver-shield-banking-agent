package com.fintechathon.silvershield.agent.model;

import com.fintechathon.silvershield.agent.planning.AgentTaskPlan;
import java.util.List;
import java.util.UUID;

public record AgentChatResult(
        String answer,
        UUID conversationId,
        AgentStatus status,
        List<String> usedTools,
        List<UUID> evidenceIds,
        String executionSummary,
        List<PendingAction> pendingActions,
        AgentTaskPlan taskPlan) {

    public AgentChatResult {
        usedTools = List.copyOf(usedTools);
        evidenceIds = List.copyOf(evidenceIds);
        pendingActions = List.copyOf(pendingActions);
    }
}
