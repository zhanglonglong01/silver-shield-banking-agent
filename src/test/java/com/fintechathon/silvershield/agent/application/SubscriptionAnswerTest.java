package com.fintechathon.silvershield.agent.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.agent.evidence.ExecutionEvidence;
import com.fintechathon.silvershield.agent.model.AgentStatus;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelToolCall;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionAnswerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final GroundedAnswerService service = new GroundedAnswerService(mapper, new AgentToolCandidateSelector());
    private static final String COSTS = """
        {"monthlyEquivalent":"32.00","annualizedCost":"384.00","items":[
          {"serviceName":"云盘套餐","amount":"18.00","billingCycle":"MONTHLY","monthlyEquivalent":"18.00","status":"ACTIVE","cancellable":true},
          {"serviceName":"数字刊订阅","amount":"168.00","billingCycle":"YEARLY","monthlyEquivalent":"14.00","status":"ACTIVE","cancellable":true},
          {"serviceName":"旧会员","amount":"20.00","billingCycle":"MONTHLY","monthlyEquivalent":"20.00","status":"CANCELLED","cancellable":false},
          {"serviceName":"健身月卡","amount":"99.00","billingCycle":"MONTHLY","monthlyEquivalent":"99.00","status":"PAUSED","cancellable":true}]}
        """;

    @Test
    void adviceMustDisplayVerifiedMonthlyAnnualAndPerSubscriptionCosts() throws Exception {
        var result = ground("分析我的订阅成本并找出可以取消的订阅", "查询已经完成", execution("analyze_subscription_costs", COSTS, ToolDecision.ALLOW));
        assertThat(result.status()).isEqualTo(AgentStatus.SUCCESS);
        assertThat(result.answer()).contains("月均 32.00", "年化 384.00", "云盘套餐", "18.00 CNY/月",
                "168.00 CNY/年", "折合每月 14.00", "可申请取消", "已暂停", "已取消", "不计入有效订阅成本")
                .doesNotContain("已创建待确认");
    }

    @Test
    void unspecifiedCancellationMustAskForTargetInsteadOfReportingQuerySuccess() throws Exception {
        var result = ground("取消我的订阅", "查询已经完成", execution("analyze_subscription_costs", COSTS, ToolDecision.ALLOW));
        assertThat(result.status()).isEqualTo(AgentStatus.NEEDS_INPUT);
        assertThat(result.answer()).contains("尚未生成取消操作", "完整名称").doesNotContain("已创建待确认");
    }

    @Test
    void previewMustNameTheSubscriptionAndImpactBeforeConfirmation() throws Exception {
        var result = ground("取消我的云盘套餐订阅", "", execution("prepare_subscription_cancellation", """
            {"requiredAal":"AAL1","summary":{"service":"云盘套餐","amount":"18.00","billingCycle":"MONTHLY","effect":"下一周期不再扣款"}}
            """, ToolDecision.ALLOW));
        assertThat(result.status()).isEqualTo(AgentStatus.SUCCESS);
        assertThat(result.answer()).contains("云盘套餐", "18.00 CNY/月", "下一周期不再扣款", "当前尚未取消");
    }

    @Test
    void deniedCostsCannotBeUsedAsFacts() throws Exception {
        var result = ground("分析订阅成本", "月均 32.00", execution("analyze_subscription_costs", COSTS, ToolDecision.DENY));
        assertThat(result.status()).isEqualTo(AgentStatus.TOOL_DENIED);
        assertThat(result.answer()).doesNotContain("32.00");
    }

    @Test
    void deniedPreparationMustRemainDeniedEvenAfterSuccessfulReads() throws Exception {
        var executions = List.of(execution("analyze_subscription_costs", COSTS, ToolDecision.ALLOW),
                execution("prepare_subscription_cancellation", "{}", ToolDecision.DENY));
        var result = service.ground("取消我的云盘套餐订阅", "查询完成", executions,
                new BillingAnalysisService().analyze(executions));
        assertThat(result.status()).isEqualTo(AgentStatus.TOOL_DENIED);
        assertThat(result.answer()).contains("未生成待确认操作", "TEST", "未执行资金或状态变更");
    }

    @Test
    void hallucinatedCostMustBeReplacedByAuditedCostEvidence() throws Exception {
        var result = ground("分析订阅成本", "月费 999999.00", execution("analyze_subscription_costs", COSTS, ToolDecision.ALLOW));
        assertThat(result.status()).isEqualTo(AgentStatus.GROUNDING_FAILED);
        assertThat(result.answer()).contains("32.00", "384.00").doesNotContain("999999.00");
    }

    private GroundedAnswerService.GroundedAnswer ground(String message, String answer, AgentToolExecution execution) {
        List<AgentToolExecution> executions = List.of(execution);
        return service.ground(message, answer, executions, new BillingAnalysisService().analyze(executions));
    }

    private AgentToolExecution execution(String tool, String json, ToolDecision decision) throws Exception {
        var data = mapper.readTree(json);
        var id = UUID.randomUUID();
        return new AgentToolExecution(new ModelToolCall("call", tool, "1.0", mapper.createObjectNode()),
                new ToolGatewayResult(id, id, id, tool, "1.0", decision, "TEST", "", data, 1, Instant.EPOCH),
                new ExecutionEvidence(id, id, id, tool, "1.0", decision, "TEST", "digest", data, Instant.EPOCH));
    }
}
