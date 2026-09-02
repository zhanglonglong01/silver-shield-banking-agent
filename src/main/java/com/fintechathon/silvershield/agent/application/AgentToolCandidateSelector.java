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

    public Set<ToolId> select(String userMessage) {
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        LinkedHashSet<ToolId> candidates = new LinkedHashSet<>();
        if (containsAny(normalized,
                "账单", "消费", "支出", "收入", "流水", "异常", "商户", "订阅", "代扣", "花",
                "spend", "bill", "transaction", "subscription", "expense", "income")) {
            candidates.add(GET_ACCOUNTS);
            candidates.add(GET_ACCOUNT_BALANCE);
            candidates.add(LIST_TRANSACTIONS);
            candidates.add(LIST_SUBSCRIPTIONS);
        } else if (containsAny(normalized,
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
