package com.fintechathon.silvershield.agent.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelRequest;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelResponse;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelToolCall;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ToolExchange;
import com.fintechathon.silvershield.config.SandboxProperties;
import com.fintechathon.silvershield.agent.planning.SubscriptionIntent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic competition fallback. It only proposes calls from the schemas exposed for this round;
 * SecureToolGateway remains the execution and authorization boundary.
 */
@Component
@Profile("offline-demo")
public class OfflineDemoBankingModelClient implements BankingModelClient {

    private static final Pattern AMOUNT = Pattern.compile("(?<![0-9])(\\d{1,7}(?:\\.\\d{1,2})?)\\s*(?:元|cny)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAST_FOUR = Pattern.compile("(?:尾号|末四位|\\*{2,})\\s*(\\d{4})");
    private static final Set<String> ROOT_TOOLS = Set.of(
            "get_accounts", "list_payees", "list_cards", "list_subscriptions",
            "list_wealth_products", "get_wealth_portfolio");
    private static final List<String> ORDER = List.of(
            "get_accounts", "list_payees", "list_cards", "list_subscriptions",
            "list_wealth_products", "get_wealth_portfolio", "get_account_balance",
            "list_transactions", "get_card_controls", "analyze_subscription_costs",
            "check_wealth_suitability", "prepare_transfer", "prepare_card_status_change",
            "prepare_card_controls_change", "prepare_subscription_cancellation",
            "prepare_wealth_purchase", "get_operation_status");

    private final ObjectMapper objectMapper;
    private final SandboxProperties sandbox;

    public OfflineDemoBankingModelClient(ObjectMapper objectMapper, SandboxProperties sandbox) {
        this.objectMapper = objectMapper;
        this.sandbox = sandbox;
    }

    @Override
    public ModelResponse complete(ModelRequest request) {
        String message = request.userMessage().trim();
        String normalized = message.toLowerCase(Locale.ROOT);
        if (looksLikePromptInjection(normalized)) {
            return ModelResponse.answer("检测到试图改变身份、权限或工具边界的内容。该内容不会被执行；你可以重新描述正常的银行业务需求。");
        }

        Map<String, JsonNode> completed = completed(request.toolExchanges());
        Set<String> allowed = request.allowedTools().stream()
                .map(schema -> schema.id().name()).collect(java.util.stream.Collectors.toSet());
        List<ModelToolCall> calls = new ArrayList<>();
        AtomicInteger sequence = new AtomicInteger(request.toolExchanges().size() + 1);

        for (String name : ORDER) {
            if (calls.size() >= 4 || !allowed.contains(name) || completed.containsKey(name)) {
                continue;
            }
            ObjectNode arguments = arguments(name, message, normalized, completed);
            if (arguments != null) {
                calls.add(new ModelToolCall("offline-" + sequence.getAndIncrement(), name, "1.0", arguments));
            }
        }
        if (!calls.isEmpty()) {
            return new ModelResponse("", calls);
        }
        return ModelResponse.answer(finalAnswer(request, completed));
    }

    private ObjectNode arguments(String tool, String message, String normalized, Map<String, JsonNode> completed) {
        if (ROOT_TOOLS.contains(tool)) {
            ObjectNode result = objectMapper.createObjectNode();
            if (tool.equals("list_wealth_products")) {
                result.put("currency", "CNY");
                result.put("saleStatus", "ON_SALE");
                result.put("limit", 20);
            }
            return result;
        }
        return switch (tool) {
            case "get_account_balance" -> selectAccount(message, completed)
                    .map(node -> object("accountId", node.path("id").asText())).orElse(null);
            case "list_transactions" -> selectAccount(message, completed)
                    .map(node -> transactions(node.path("id").asText())).orElse(null);
            case "get_card_controls" -> selectCard(message, completed).map(id -> object("cardId", id)).orElse(null);
            case "analyze_subscription_costs" -> completed.containsKey("list_subscriptions")
                    ? objectMapper.createObjectNode().put("horizonMonths", 12) : null;
            case "check_wealth_suitability" -> suitability(message, normalized, completed);
            case "prepare_transfer" -> transfer(message, normalized, completed);
            case "prepare_card_status_change" -> cardStatus(message, normalized, completed);
            case "prepare_card_controls_change" -> cardControls(message, normalized, completed);
            case "prepare_subscription_cancellation" -> subscriptionCancellation(message, normalized, completed);
            case "prepare_wealth_purchase" -> wealthPurchase(message, normalized, completed);
            case "get_operation_status" -> operationId(completed)
                    .map(id -> object("operationId", id)).orElse(null);
            default -> null;
        };
    }

    private ObjectNode transactions(String accountId) {
        LocalDate to = sandbox.getBaselineDate();
        return objectMapper.createObjectNode().put("accountId", accountId)
                .put("from", to.minusDays(29).toString()).put("to", to.toString()).put("limit", 100);
    }

    private ObjectNode transfer(String message, String normalized, Map<String, JsonNode> completed) {
        if (!containsAny(normalized, "转账", "汇款", "打款", "transfer", "remit")) return null;
        Optional<JsonNode> account = selectAccount(message, completed);
        Optional<String> payee = selectByText(message, data(completed, "list_payees"), "name", "id", true);
        Optional<String> amount = amount(message);
        if (account.isEmpty() || payee.isEmpty() || amount.isEmpty()) return null;
        return objectMapper.createObjectNode()
                .put("sourceAccountId", account.get().path("id").asText())
                .put("payeeId", payee.get()).put("amount", amount.get())
                .put("currency", "CNY").put("purpose", "离线比赛演示");
    }

    private ObjectNode cardStatus(String message, String normalized, Map<String, JsonNode> completed) {
        if (!containsAny(normalized, "冻结", "解冻", "挂失", "丢失", "丢了", "freeze", "unfreeze", "lost")) {
            return null;
        }
        Optional<String> card = selectCard(message, completed);
        if (card.isEmpty()) return null;
        String target = containsAny(normalized, "解冻", "解除挂失", "恢复", "unfreeze") ? "ACTIVE"
                : containsAny(normalized, "挂失", "丢失", "丢了", "lost") ? "LOST" : "FROZEN";
        String reason = target.equals("LOST") ? "CARD_LOST" : "USER_REQUEST";
        return objectMapper.createObjectNode().put("cardId", card.get())
                .put("targetStatus", target).put("reasonCode", reason);
    }

    private ObjectNode cardControls(String message, String normalized, Map<String, JsonNode> completed) {
        boolean targetsControl = containsAny(normalized,
                "境外", "海外", "线上", "网络", "限额", "控制", "overseas", "online", "limit");
        boolean requestsChange = containsAny(normalized,
                "开启", "打开", "启用", "关闭", "禁用", "设置", "调整", "修改", "提高", "降低",
                "enable", "disable", "set", "change", "increase", "decrease");
        if (!targetsControl || !requestsChange) {
            return null;
        }
        Optional<String> cardId = selectCard(message, completed);
        JsonNode controls = data(completed, "get_card_controls");
        if (cardId.isEmpty() || controls.isMissingNode()) return null;
        ObjectNode result = objectMapper.createObjectNode().put("cardId", cardId.get());
        boolean enable = !containsAny(normalized, "关闭", "禁用", "disable", "deactivate", "turn off");
        if (containsAny(normalized, "境外", "海外", "overseas")) result.put("overseasEnabled", enable);
        else if (containsAny(normalized, "线上", "网络", "online")) result.put("onlineEnabled", enable);
        else if (containsAny(normalized, "限额", "limit")) {
            Optional<String> requestedAmount = amount(message);
            if (requestedAmount.isEmpty()) return null;
            BigDecimal value = new BigDecimal(requestedAmount.get());
            result.put("singleTxLimit", value.toPlainString());
            result.put("dailyLimit", value.max(new BigDecimal(controls.path("dailyLimit").asText("1000.00"))).toPlainString());
        } else return null;
        return result;
    }

    private ObjectNode subscriptionCancellation(String message, String normalized, Map<String, JsonNode> completed) {
        if (!SubscriptionIntent.requestsCancellation(message)) return null;
        JsonNode subscriptions = data(completed, "list_subscriptions");
        if (!subscriptions.isArray()) return null;
        List<JsonNode> matches = new ArrayList<>();
        subscriptions.forEach(row -> {
            String label = row.path("serviceName").asText().replace("（虚构）", "").trim();
            String specific = label.replaceAll("(?:订阅|套餐|会员|服务|月卡)$", "");
            if (specific.length() >= 2 && !Set.of("订阅", "套餐", "会员", "服务", "月卡").contains(specific)
                    && (message.contains(label) || message.contains(specific))) matches.add(row);
        });
        if (matches.size() != 1) return null;
        JsonNode target = matches.get(0);
        if (!Set.of("ACTIVE", "PAUSED").contains(target.path("status").asText())) return null;
        return objectMapper.createObjectNode().put("subscriptionId", target.path("id").asText())
                .put("reasonCode", "COST_SAVING");
    }

    private ObjectNode suitability(String message, String normalized, Map<String, JsonNode> completed) {
        if (!containsAny(normalized, "购买", "买入", "申购", "投资", "purchase", "buy", "invest")) return null;
        Optional<String> requestedAmount = amount(message);
        Optional<JsonNode> account = selectAccount(message, completed);
        Optional<JsonNode> product = requestedAmount.flatMap(value -> selectProduct(message, value, completed));
        if (requestedAmount.isEmpty() || account.isEmpty() || product.isEmpty()) return null;
        return objectMapper.createObjectNode().put("productId", product.get().path("id").asText())
                .put("paymentAccountId", account.get().path("id").asText())
                .put("amount", requestedAmount.get()).put("currency", "CNY");
    }

    private ObjectNode wealthPurchase(String message, String normalized, Map<String, JsonNode> completed) {
        if (!containsAny(normalized, "购买", "买入", "申购", "投资", "purchase", "buy", "invest")) return null;
        JsonNode check = data(completed, "check_wealth_suitability");
        if (!check.path("eligible").asBoolean(false)) return null;
        String checkedAmount = check.path("amount").asText();
        if (checkedAmount.isBlank()) return null;
        return objectMapper.createObjectNode().put("suitabilityCheckId", check.path("checkId").asText())
                .put("productId", check.path("productId").asText())
                .put("paymentAccountId", check.path("paymentAccountId").asText())
                .put("amount", checkedAmount)
                .put("currency", check.path("currency").asText("CNY"));
    }

    private Optional<JsonNode> selectAccount(String message, Map<String, JsonNode> completed) {
        JsonNode accounts = data(completed, "get_accounts");
        if (!accounts.isArray()) return Optional.empty();
        List<JsonNode> values = new ArrayList<>();
        accounts.forEach(values::add);
        List<JsonNode> active = values.stream()
                .filter(node -> "ACTIVE".equals(node.path("status").asText()))
                .toList();
        Optional<String> lastFour = lastFour(message);
        List<JsonNode> matched = active.stream().filter(node ->
                lastFour.map(value -> node.path("accountMasked").asText().contains(value)).orElse(false)
                        || containsExactLabel(message, node.path("alias").asText())).toList();
        if (matched.size() == 1) return Optional.of(matched.get(0));
        if (lastFour.isPresent() || !matched.isEmpty()) return Optional.empty();
        return active.size() == 1 ? Optional.of(active.get(0)) : Optional.empty();
    }

    private Optional<String> selectCard(String message, Map<String, JsonNode> completed) {
        JsonNode cards = data(completed, "list_cards");
        if (!cards.isArray()) return Optional.empty();
        List<JsonNode> values = new ArrayList<>();
        cards.forEach(values::add);
        Optional<String> lastFour = lastFour(message);
        List<JsonNode> matched = values.stream()
                .filter(card -> lastFour.map(value -> card.path("cardMasked").asText().contains(value)).orElse(false))
                .toList();
        if (matched.size() == 1) return Optional.of(matched.get(0).path("id").asText());
        if (lastFour.isPresent() || !matched.isEmpty()) return Optional.empty();
        return values.size() == 1 ? Optional.of(values.get(0).path("id").asText()) : Optional.empty();
    }

    private Optional<JsonNode> selectProduct(String message, String amount, Map<String, JsonNode> completed) {
        JsonNode products = data(completed, "list_wealth_products");
        if (!products.isArray()) return Optional.empty();
        BigDecimal requested = decimal(amount);
        List<JsonNode> eligible = new ArrayList<>();
        products.forEach(product -> {
            BigDecimal minimum = decimal(product.path("minPurchase").asText("0"));
            BigDecimal increment = decimal(product.path("incrementAmount").asText("1"));
            if ("ON_SALE".equals(product.path("saleStatus").asText())
                    && risk(product.path("riskLevel").asText()) <= 3
                    && requested.compareTo(minimum) >= 0
                    && requested.subtract(minimum).remainder(increment).compareTo(BigDecimal.ZERO) == 0) {
                eligible.add(product);
            }
        });
        List<JsonNode> matched = eligible.stream()
                .filter(product -> containsExactLabel(message, product.path("name").asText())).toList();
        if (matched.size() == 1) return Optional.of(matched.get(0));
        if (!matched.isEmpty()) return Optional.empty();
        return eligible.size() == 1 ? Optional.of(eligible.get(0)) : Optional.empty();
    }

    private Optional<String> operationId(Map<String, JsonNode> completed) {
        return completed.entrySet().stream().filter(entry -> entry.getKey().startsWith("prepare_"))
                .map(Map.Entry::getValue).map(node -> node.path("operationId").asText())
                .filter(value -> !value.isBlank()).findFirst();
    }

    private Optional<String> selectByText(String message, JsonNode array, String labelField, String idField,
            boolean preferTrusted) {
        if (!array.isArray()) return Optional.empty();
        List<JsonNode> values = new ArrayList<>();
        array.forEach(values::add);
        List<JsonNode> active = values.stream()
                .filter(node -> !node.has("status") || "ACTIVE".equals(node.path("status").asText()))
                .toList();
        Optional<JsonNode> matched = active.stream()
                .filter(node -> containsMeaningful(message, node.path(labelField).asText())).findFirst();
        if (matched.isPresent()) return matched.map(node -> node.path(idField).asText());
        List<JsonNode> eligible = active.stream()
                .filter(node -> !preferTrusted || node.path("trusted").asBoolean())
                .toList();
        return eligible.size() == 1 ? Optional.of(eligible.get(0).path(idField).asText()) : Optional.empty();
    }

    private Map<String, JsonNode> completed(List<ToolExchange> exchanges) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (ToolExchange exchange : exchanges) {
            try {
                JsonNode response = objectMapper.readTree(exchange.responseData());
                if ("ALLOW".equals(response.path("decision").asText())) {
                    result.put(exchange.call().name(), response.path("data"));
                }
            } catch (Exception ignored) {
                // Malformed evidence is never used to construct another call.
            }
        }
        return result;
    }

    private JsonNode data(Map<String, JsonNode> completed, String tool) {
        return completed.getOrDefault(tool, objectMapper.missingNode());
    }

    private ObjectNode object(String name, String value) {
        return objectMapper.createObjectNode().put(name, value);
    }

    private String finalAnswer(ModelRequest request, Map<String, JsonNode> completed) {
        if (completed.keySet().stream().anyMatch(name -> name.startsWith("prepare_"))) {
            return "操作预览已经由安全网关生成。当前尚未产生业务变更，请核对不可变摘要并使用独立确认按钮决定是否提交。";
        }
        String normalized = request.userMessage().toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "转账", "汇款", "打款", "transfer", "remit")) {
            return "为避免准备错误转账，请明确付款账户、收款人和金额，例如：从日常账户向张伟转账 88 元。";
        }
        if (containsAny(normalized, "购买", "买入", "申购", "投资", "purchase", "buy", "invest")) {
            return "理财准备不会猜测关键参数。请明确付款账户、产品全名和金额，例如：用日常账户购买银盾稳享存款理财产品 1000 元。";
        }
        if (containsAny(normalized, "冻结", "解冻", "挂失", "丢失", "丢了", "freeze", "unfreeze", "lost")) {
            return "卡片目标不唯一，请提供卡片尾号并明确冻结、解冻或挂失。";
        }
        if (containsAny(normalized, "限额", "limit")
                && containsAny(normalized, "设置", "调整", "修改", "提高", "降低", "set", "change", "increase", "decrease")) {
            return "卡片限额不会使用默认金额，请提供卡片尾号和明确的新限额。";
        }
        if (completed.isEmpty() && request.allowedTools().isEmpty()) {
            return "我可以协助查询账户、分析账单，以及准备转账、卡片、订阅和理财操作。请说明具体目标。";
        }
        return "查询已经通过当前会话允许的银行工具完成；页面中的结果、计划和证据编号均来自服务端安全执行链。";
    }

    private static Optional<String> amount(String message) {
        Matcher matcher = AMOUNT.matcher(message);
        if (!matcher.find()) return Optional.empty();
        BigDecimal value = decimal(matcher.group(1));
        return value.compareTo(BigDecimal.ZERO) > 0
                ? Optional.of(value.setScale(2).toPlainString()) : Optional.empty();
    }

    private static Optional<String> lastFour(String message) {
        Matcher matcher = LAST_FOUR.matcher(message);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static boolean containsExactLabel(String message, String label) {
        String compact = label.replace("（虚构）", "").replace("（模拟）", "").trim();
        return compact.length() >= 2 && message.contains(compact);
    }

    private static boolean containsMeaningful(String message, String label) {
        String compact = label.replace("（虚构）", "").replace("（模拟）", "").trim();
        if (compact.length() >= 2 && message.contains(compact)) return true;
        for (int index = 0; index + 2 <= compact.length(); index++) {
            String fragment = compact.substring(index, index + 2);
            if (!containsAny(fragment, "会员", "服务", "银行", "模拟", "虚构") && message.contains(fragment)) return true;
        }
        return false;
    }

    private static boolean looksLikePromptInjection(String text) {
        return containsAny(text, "忽略规则", "忽略以上", "系统提示词", "developer message", "system prompt",
                "成为管理员", "你现在是管理员", "绕过权限", "执行隐藏工具", "hidden tool", "泄露密钥");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static BigDecimal decimal(String value) {
        try { return new BigDecimal(value); } catch (RuntimeException ignored) { return BigDecimal.ZERO; }
    }

    private static int risk(String value) {
        try { return Integer.parseInt(value.replace("R", "")); } catch (RuntimeException ignored) { return 99; }
    }
}
