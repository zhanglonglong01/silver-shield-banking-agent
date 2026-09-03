package com.fintechathon.silvershield.agent.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.PlanStatus;
import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.PlanStep;
import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.StepStatus;
import com.fintechathon.silvershield.identity.domain.AppUser;
import com.fintechathon.silvershield.security.AssuranceLevel;
import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolCapability;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskPlanValidatorTest {
    private final TaskPlanValidator validator = new TaskPlanValidator();

    @Test
    void validDagMustPass() {
        AgentTaskPlan plan = plan(List.of(
                step("s1", "get_accounts", ToolAccessMode.READ_ONLY),
                step("s2", "list_payees", ToolAccessMode.READ_ONLY),
                step("s3", "prepare_transfer", ToolAccessMode.PREPARE_WRITE, "s1", "s2")));

        assertThat(validator.validate(plan, snapshot(plan.steps())).allowed()).isTrue();
    }

    @Test
    void futureOrCyclicDependencyMustFailClosed() {
        AgentTaskPlan plan = plan(List.of(
                step("s1", "get_accounts", ToolAccessMode.READ_ONLY, "s2"),
                step("s2", "list_payees", ToolAccessMode.READ_ONLY, "s1")));

        assertThat(validator.validate(plan, snapshot(plan.steps())).reasonCode()).isEqualTo("PLAN_DAG_INVALID");
    }

    @Test
    void selfDependencyMustFailClosed() {
        AgentTaskPlan plan = plan(List.of(
                step("s1", "get_accounts", ToolAccessMode.READ_ONLY, "s1")));

        assertThat(validator.validate(plan, snapshot(plan.steps())).reasonCode()).isEqualTo("PLAN_DAG_INVALID");
    }

    @Test
    void hallucinatedToolMustNotBeAcceptedByPlanSchema() {
        AgentTaskPlan plan = plan(List.of(step("s1", "execute_transfer", ToolAccessMode.READ_ONLY)));

        assertThat(validator.validate(plan, snapshot(List.of())).reasonCode()).isEqualTo("PLAN_TOOL_NOT_ALLOWED");
    }

    @Test
    void toolModeSmugglingMustBeRejected() {
        PlanStep declared = step("s1", "prepare_transfer", ToolAccessMode.READ_ONLY);
        PlanStep actual = step("s1", "prepare_transfer", ToolAccessMode.PREPARE_WRITE);

        assertThat(validator.validate(plan(List.of(declared)), snapshot(List.of(actual))).reasonCode())
                .isEqualTo("PLAN_TOOL_MODE_MISMATCH");
    }

    @Test
    void moreThanEightStepsMustBeRejected() {
        List<PlanStep> steps = new ArrayList<>();
        for (int index = 1; index <= 9; index++) {
            steps.add(step("s" + index, "query_" + index, ToolAccessMode.READ_ONLY));
        }

        assertThat(validator.validate(plan(steps), snapshot(steps)).reasonCode())
                .isEqualTo("PLAN_STEP_LIMIT_EXCEEDED");
    }

    @Test
    void multipleWritePreparationsMustBeSplitIntoSeparatePlans() {
        List<PlanStep> steps = List.of(
                step("s1", "get_accounts", ToolAccessMode.READ_ONLY),
                step("s2", "prepare_transfer", ToolAccessMode.PREPARE_WRITE, "s1"),
                step("s3", "prepare_subscription_cancellation", ToolAccessMode.PREPARE_WRITE, "s1"));

        assertThat(validator.validate(plan(steps), snapshot(steps)).reasonCode())
                .isEqualTo("PLAN_WRITE_SCOPE_TOO_BROAD");
    }

    private static AgentTaskPlan plan(List<PlanStep> steps) {
        return new AgentTaskPlan(UUID.randomUUID(), UUID.randomUUID(), "test", PlanStatus.ACTIVE,
                steps, List.of(), "PLAN_CREATED", Instant.parse("2026-09-03T00:00:00Z"));
    }

    private static PlanStep step(String id, String name, ToolAccessMode mode, String... dependencies) {
        return new PlanStep(id, new ToolId(name, "1.0"), mode, List.of(dependencies),
                StepStatus.PENDING, List.of(), null);
    }

    private static AllowedToolSetSnapshot snapshot(List<PlanStep> steps) {
        List<AllowedToolSetSnapshot.AllowedTool> tools = steps.stream().map(step ->
                new AllowedToolSetSnapshot.AllowedTool(step.tool(), step.tool().name(),
                        ToolCapability.ACCOUNT_READ, step.accessMode(), "test-v1", Set.of(), "digest"))
                .toList();
        Instant now = Instant.parse("2026-09-03T00:00:00Z");
        return new AllowedToolSetSnapshot(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppUser.Role.CUSTOMER, AssuranceLevel.AAL1, "test", now, now.plusSeconds(60),
                tools, List.of(), "signature");
    }
}
