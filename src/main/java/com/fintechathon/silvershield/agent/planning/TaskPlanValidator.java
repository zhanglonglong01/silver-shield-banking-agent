package com.fintechathon.silvershield.agent.planning;

import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.PlanStep;
import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TaskPlanValidator {

    public static final int MAX_STEPS = 8;

    public ValidationResult validate(AgentTaskPlan plan, AllowedToolSetSnapshot allowedToolSet) {
        if (plan == null || plan.steps().isEmpty()) {
            return ValidationResult.deny("PLAN_EMPTY", "跨场景计划不能为空");
        }
        if (plan.steps().size() > MAX_STEPS) {
            return ValidationResult.deny("PLAN_STEP_LIMIT_EXCEEDED", "跨场景计划最多允许 8 个步骤");
        }
        Map<ToolId, AllowedToolSetSnapshot.AllowedTool> allowed = allowedToolSet.allowedTools().stream()
                .collect(Collectors.toMap(AllowedToolSetSnapshot.AllowedTool::id, Function.identity()));
        Set<String> seen = new HashSet<>();
        int writes = 0;
        for (PlanStep step : plan.steps()) {
            if (step.stepId() == null || step.stepId().isBlank() || seen.contains(step.stepId())) {
                return ValidationResult.deny("PLAN_STEP_ID_INVALID", "计划步骤标识必须唯一且非空");
            }
            if (!seen.containsAll(step.dependsOn())) {
                return ValidationResult.deny("PLAN_DAG_INVALID", "计划依赖必须存在、无环且按拓扑顺序排列");
            }
            seen.add(step.stepId());
            AllowedToolSetSnapshot.AllowedTool definition = allowed.get(step.tool());
            if (definition == null) {
                return ValidationResult.deny("PLAN_TOOL_NOT_ALLOWED", "计划包含当前请求未授权的 Tool");
            }
            if (definition.accessMode() != step.accessMode()) {
                return ValidationResult.deny("PLAN_TOOL_MODE_MISMATCH", "计划声明的 Tool 模式与安全定义不一致");
            }
            if (step.accessMode() == ToolAccessMode.PREPARE_WRITE) {
                writes++;
            }
        }
        if (writes > 1) {
            return ValidationResult.deny("PLAN_WRITE_SCOPE_TOO_BROAD", "单个跨场景计划最多准备一个写操作");
        }
        return ValidationResult.allow();
    }

    public record ValidationResult(boolean allowed, String reasonCode, String explanation) {
        static ValidationResult allow() {
            return new ValidationResult(true, "PLAN_ALLOWED", "计划 DAG、Tool 与执行模式校验通过");
        }

        static ValidationResult deny(String reasonCode, String explanation) {
            return new ValidationResult(false, reasonCode, explanation);
        }
    }
}
