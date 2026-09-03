package com.fintechathon.silvershield.agent.planning;

import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentTaskPlan(
        UUID planId,
        UUID conversationId,
        String goal,
        PlanStatus status,
        List<PlanStep> steps,
        List<String> missingSlots,
        String statusReason,
        Instant updatedAt) {

    public AgentTaskPlan {
        steps = List.copyOf(steps);
        missingSlots = List.copyOf(missingSlots);
    }

    public enum PlanStatus {
        NEEDS_INPUT,
        ACTIVE,
        PAUSED,
        AWAITING_CONFIRMATION,
        COMPLETED,
        CANCELLED,
        REJECTED
    }

    public enum StepStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    public record PlanStep(
            String stepId,
            ToolId tool,
            ToolAccessMode accessMode,
            List<String> dependsOn,
            StepStatus status,
            List<UUID> evidenceIds,
            String reasonCode) {

        public PlanStep {
            dependsOn = List.copyOf(dependsOn);
            evidenceIds = List.copyOf(evidenceIds);
        }
    }
}
