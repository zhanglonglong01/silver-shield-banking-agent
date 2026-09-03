package com.fintechathon.silvershield.agent.application;

import com.fintechathon.silvershield.tool.model.ToolId;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AgentToolCandidateSelector {

    static final ToolId GET_ACCOUNTS = new ToolId("get_accounts", "1.0");
    static final ToolId GET_ACCOUNT_BALANCE = new ToolId("get_account_balance", "1.0");
    static final ToolId LIST_TRANSACTIONS = new ToolId("list_transactions", "1.0");
    static final ToolId LIST_SUBSCRIPTIONS = new ToolId("list_subscriptions", "1.0");
    static final ToolId LIST_PAYEES = new ToolId("list_payees", "1.0");
    static final ToolId PREPARE_TRANSFER = new ToolId("prepare_transfer", "1.0");
    static final ToolId GET_OPERATION_STATUS = new ToolId("get_operation_status", "1.0");
    static final ToolId LIST_CARDS = new ToolId("list_cards", "1.0");
    static final ToolId GET_CARD_CONTROLS = new ToolId("get_card_controls", "1.0");
    static final ToolId PREPARE_CARD_STATUS = new ToolId("prepare_card_status_change", "1.0");
    static final ToolId PREPARE_CARD_CONTROLS = new ToolId("prepare_card_controls_change", "1.0");
    static final ToolId ANALYZE_SUBSCRIPTIONS = new ToolId("analyze_subscription_costs", "1.0");
    static final ToolId PREPARE_SUBSCRIPTION_CANCEL = new ToolId("prepare_subscription_cancellation", "1.0");
    static final ToolId LIST_WEALTH_PRODUCTS = new ToolId("list_wealth_products", "1.0");
    static final ToolId CHECK_WEALTH_SUITABILITY = new ToolId("check_wealth_suitability", "1.0");
    static final ToolId GET_WEALTH_PORTFOLIO = new ToolId("get_wealth_portfolio", "1.0");
    static final ToolId PREPARE_WEALTH_PURCHASE = new ToolId("prepare_wealth_purchase", "1.0");

    public Set<ToolId> select(String userMessage) {
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        LinkedHashSet<ToolId> candidates = new LinkedHashSet<>();
        if (containsAny(normalized,"确认执行","确认转账","操作状态","转账状态","confirm transfer","operation status")) {
            candidates.add(GET_OPERATION_STATUS);
            return Set.copyOf(candidates);
        }
        if (containsAny(normalized,"转账","汇款","打款","transfer","remit")) {
            candidates.add(GET_ACCOUNTS);
            candidates.add(GET_ACCOUNT_BALANCE);
            candidates.add(LIST_PAYEES);
            candidates.add(PREPARE_TRANSFER);
            candidates.add(GET_OPERATION_STATUS);
        }
        if (containsAny(normalized, "卡片", "银行卡", "冻结卡", "解冻", "挂失", "境外支付",
                "线上支付", "卡限额", "card", "freeze card")) {
            candidates.add(LIST_CARDS);
            candidates.add(GET_CARD_CONTROLS);
            candidates.add(PREPARE_CARD_STATUS);
            candidates.add(PREPARE_CARD_CONTROLS);
            candidates.add(GET_OPERATION_STATUS);
        }
        if (containsAny(normalized, "理财", "产品", "持仓", "购买基金", "投资", "wealth", "portfolio")) {
            candidates.add(GET_ACCOUNTS);
            candidates.add(GET_ACCOUNT_BALANCE);
            candidates.add(LIST_WEALTH_PRODUCTS);
            candidates.add(GET_WEALTH_PORTFOLIO);
            candidates.add(CHECK_WEALTH_SUITABILITY);
            candidates.add(PREPARE_WEALTH_PURCHASE);
            candidates.add(GET_OPERATION_STATUS);
        }
        if (containsAny(normalized, "取消订阅", "取消代扣", "关闭自动扣款", "订阅成本", "会员费用")) {
            candidates.add(LIST_SUBSCRIPTIONS);
            candidates.add(ANALYZE_SUBSCRIPTIONS);
            candidates.add(PREPARE_SUBSCRIPTION_CANCEL);
            candidates.add(GET_OPERATION_STATUS);
        }
        if (containsAny(normalized,
                "账单", "消费", "支出", "收入", "流水", "异常", "商户", "订阅", "代扣", "花",
                "spend", "bill", "transaction", "subscription", "expense", "income")) {
            candidates.add(GET_ACCOUNTS);
            candidates.add(GET_ACCOUNT_BALANCE);
            candidates.add(LIST_TRANSACTIONS);
            candidates.add(LIST_SUBSCRIPTIONS);
            candidates.add(ANALYZE_SUBSCRIPTIONS);
        }
        if (containsAny(normalized,
                "账户", "余额", "多少钱", "存款", "account", "balance")) {
            candidates.add(GET_ACCOUNTS);
            candidates.add(GET_ACCOUNT_BALANCE);
        }
        return Set.copyOf(candidates);
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
