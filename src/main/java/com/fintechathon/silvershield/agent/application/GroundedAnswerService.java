package com.fintechathon.silvershield.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.agent.application.BillingAnalysisService.BillingAnalysis;
import com.fintechathon.silvershield.agent.application.BillingAnalysisService.PeriodSummary;
import com.fintechathon.silvershield.agent.model.AgentStatus;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class GroundedAnswerService {

    private static final Pattern NUMBER = Pattern.compile("(?<![0-9])[0-9][0-9,]*(?:\\.[0-9]+)?");
    private static final Pattern FAKE_WRITE_SUCCESS = Pattern.compile(
            "(已|已经|成功)(完成)?(转账|汇款|冻结|挂失|取消|购买|赎回)|"
                    + "(转账|汇款|冻结|挂失|取消|购买|赎回)(已完成|成功)",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final AgentToolCandidateSelector candidateSelector;

    public GroundedAnswerService(ObjectMapper objectMapper, AgentToolCandidateSelector candidateSelector) {
        this.objectMapper = objectMapper;
        this.candidateSelector = candidateSelector;
    }

    public GroundedAnswer ground(
            String userMessage,
            String modelAnswer,
            List<AgentToolExecution> executions,
            BillingAnalysis analysis) {
        String normalizedUser = userMessage.toLowerCase(Locale.ROOT);
        boolean privilegedTargetRequest = normalizedUser.contains("bob")
                || normalizedUser.contains("他人账户")
                || normalizedUser.contains("别人的账户")
                || normalizedUser.contains("你现在是管理员");
        if (privilegedTargetRequest) {
            return new GroundedAnswer(
                    "我只能基于当前已认证用户的资源进行查询，无法切换身份或展示他人的银行数据。",
                    AgentStatus.CAPABILITY_UNAVAILABLE,
                    "拒绝了改变身份或访问他人资源的请求");
        }
        boolean successfulPreparation = executions.stream().anyMatch(e -> e.call().name().equals("prepare_transfer")
                && e.result().decision() == ToolDecision.ALLOW);
        if (successfulPreparation) {
            JsonNode operation = executions.stream().filter(e -> e.call().name().equals("prepare_transfer"))
                    .filter(e -> e.result().decision() == ToolDecision.ALLOW).findFirst().orElseThrow()
                    .evidence().relevantStructuredResult();
            JsonNode summary = operation.path("summary");
            return new GroundedAnswer(
                    "已创建待确认转账：从 %s 向 %s 转账 %s %s，所需认证等级 %s。当前尚未扣款；请通过独立确认端点完成或取消。"
                            .formatted(summary.path("sourceAccount").asText(), summary.path("payee").asText(),
                                    summary.path("amount").asText(), summary.path("currency").asText(),
                                    operation.path("requiredAal").asText()),
                    AgentStatus.SUCCESS, "仅准备了受挑战绑定的转账操作，资金未变化");
        }
        if ((isUnsupportedWriteRequest(normalizedUser) || FAKE_WRITE_SUCCESS.matcher(modelAnswer).find())
                && !successfulPreparation) {
            return new GroundedAnswer(
                    "当前除转账准备外仍是只读能力；冻结、取消订阅或理财购买等写操作不可用，因此没有执行任何资金或状态变更。未经独立确认端点，不会发生资金变化。",
                    AgentStatus.CAPABILITY_UNAVAILABLE,
                    "未暴露写操作 Tool，且没有成功写操作证据");
        }

        List<AgentToolExecution> successful = executions.stream()
                .filter(execution -> execution.result().decision() == ToolDecision.ALLOW)
                .toList();
        if (successful.isEmpty() && !candidateSelector.select(userMessage).isEmpty()) {
            return new GroundedAnswer(
                    "我没有获得成功的银行查询结果，因此不能猜测账户、余额或账单数据。请稍后重试。",
                    executions.isEmpty() ? AgentStatus.GROUNDING_FAILED : AgentStatus.TOOL_DENIED,
                    "没有可用于回答的成功 Tool Result");
        }
        if (modelAnswer == null || modelAnswer.isBlank()) {
            return fallback(successful, analysis, AgentStatus.MALFORMED_MODEL_RESPONSE,
                    "模型未返回可用的最终答案，改用确定性结果摘要");
        }

        Set<String> groundedNumbers = groundedNumbers(successful, analysis);
        Matcher matcher = NUMBER.matcher(modelAnswer);
        while (matcher.find()) {
            String claimed = normalizeNumber(matcher.group());
            if (!groundedNumbers.contains(claimed)) {
                return fallback(successful, analysis, AgentStatus.GROUNDING_FAILED,
                        "模型答案包含无法由执行证据支持的数字，已替换为确定性摘要");
            }
        }
        return new GroundedAnswer(modelAnswer, AgentStatus.SUCCESS,
                successful.isEmpty() ? "未调用银行 Tool" : "最终回答已通过执行证据一致性检查");
    }

    private GroundedAnswer fallback(
            List<AgentToolExecution> successful,
            BillingAnalysis analysis,
            AgentStatus status,
            String summary) {
        if (analysis.hasData()) {
            return new GroundedAnswer(billingFallback(analysis), status, summary);
        }
        for (AgentToolExecution execution : successful) {
            if (execution.call().name().equals("get_account_balance")) {
                JsonNode data = execution.evidence().relevantStructuredResult();
                return new GroundedAnswer(
                        "%s（%s）的可用余额为 %s %s，账面余额为 %s %s。".formatted(
                                data.path("alias").asText("该账户"), data.path("accountMasked").asText(),
                                data.path("availableBalance").asText(), data.path("currency").asText(),
                                data.path("ledgerBalance").asText(), data.path("currency").asText()),
                        status, summary);
            }
        }
        for (AgentToolExecution execution : successful) {
            if (execution.call().name().equals("get_accounts")) {
                JsonNode data = execution.evidence().relevantStructuredResult();
                BigDecimal total = BigDecimal.ZERO;
                StringBuilder accounts = new StringBuilder();
                if (data.isArray()) {
                    for (JsonNode account : data) {
                        total = total.add(new BigDecimal(account.path("availableBalance").asText("0")));
                        if (!accounts.isEmpty()) {
                            accounts.append("；");
                        }
                        accounts.append(account.path("alias").asText("账户"))
                                .append(" ").append(account.path("availableBalance").asText())
                                .append(" ").append(account.path("currency").asText());
                    }
                }
                return new GroundedAnswer(
                        "共查询到 %d 个账户，可用余额合计 %s CNY：%s。".formatted(
                                data.size(), total.setScale(2, RoundingMode.HALF_UP).toPlainString(), accounts),
                        status, summary);
            }
        }
        return new GroundedAnswer("当前没有足够的执行证据回答该问题。", status, summary);
    }

    private String billingFallback(BillingAnalysis analysis) {
        if (!analysis.periods().isEmpty()) {
            PeriodSummary current = analysis.periods().get(analysis.periods().size() - 1);
            StringBuilder answer = new StringBuilder("统计区间 ")
                    .append(current.from()).append(" 至 ").append(current.to())
                    .append("：总收入 ").append(current.income()).append(" CNY，总支出 ")
                    .append(current.expense()).append(" CNY，收支差额 ").append(current.net()).append(" CNY");
            if (!current.categorySpend().isEmpty()) {
                var top = current.categorySpend().entrySet().iterator().next();
                answer.append("；最高消费分类为 ").append(top.getKey()).append("（")
                        .append(top.getValue()).append(" CNY）");
            }
            if (analysis.comparison() != null) {
                answer.append("；较前一周期支出变化 ")
                        .append(analysis.comparison().expenseDifference()).append(" CNY");
            }
            if (analysis.subscriptions().activeCount() > 0) {
                answer.append("；发现 ").append(analysis.subscriptions().activeCount())
                        .append(" 项有效订阅，单周期金额合计 ")
                        .append(analysis.subscriptions().recurringAmountTotal()).append(" CNY");
            }
            return answer.append("。").toString();
        }
        return "发现 %d 项有效订阅，单周期金额合计 %s CNY。".formatted(
                analysis.subscriptions().activeCount(), analysis.subscriptions().recurringAmountTotal());
    }

    private Set<String> groundedNumbers(List<AgentToolExecution> successful, BillingAnalysis analysis) {
        Set<String> values = new HashSet<>();
        successful.forEach(execution -> {
            collectNumbers(execution.evidence().relevantStructuredResult(), values);
            values.add(normalizeNumber(Integer.toString(execution.result().recordCount())));
        });
        try {
            collectNumbers(objectMapper.valueToTree(analysis), values);
        } catch (IllegalArgumentException ignored) {
            // The deterministic fallback remains available even if optional number extraction fails.
        }
        BigDecimal accountTotal = successful.stream()
                .filter(execution -> execution.call().name().equals("get_accounts"))
                .flatMap(execution -> {
                    List<JsonNode> nodes = new java.util.ArrayList<>();
                    execution.evidence().relevantStructuredResult().forEach(nodes::add);
                    return nodes.stream();
                })
                .map(node -> new BigDecimal(node.path("availableBalance").asText("0")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        values.add(normalizeNumber(accountTotal.toPlainString()));
        return values;
    }

    private void collectNumbers(JsonNode node, Set<String> target) {
        if (node == null) {
            return;
        }
        if (node.isNumber()) {
            target.add(normalizeNumber(node.asText()));
        } else if (node.isTextual()) {
            Matcher matcher = NUMBER.matcher(node.asText());
            while (matcher.find()) {
                target.add(normalizeNumber(matcher.group()));
            }
        } else {
            node.forEach(child -> collectNumbers(child, target));
        }
    }

    private static String normalizeNumber(String value) {
        String normalized = value.replace(",", "");
        try {
            return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            return normalized;
        }
    }

    private static boolean isUnsupportedWriteRequest(String text) {
        return text.contains("冻结")
                || text.contains("挂失") || text.contains("取消订阅") || text.contains("购买理财")
                || text.contains("赎回") || text.contains("freeze")
                || text.contains("delete account");
    }

    public record GroundedAnswer(String answer, AgentStatus status, String executionSummary) {}
}
