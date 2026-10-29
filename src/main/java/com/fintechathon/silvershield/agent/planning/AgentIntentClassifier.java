package com.fintechathon.silvershield.agent.planning;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AgentIntentClassifier {

    public StructuredIntent classify(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "取消任务", "停止任务", "放弃计划", "cancel task", "abort task")) {
            return new StructuredIntent(StructuredIntent.Command.CANCEL, Set.of(), false, List.of());
        }
        if (Set.of("继续", "继续执行", "恢复任务", "resume", "continue").contains(normalized)) {
            return new StructuredIntent(StructuredIntent.Command.RESUME, Set.of(), false, List.of());
        }

        EnumSet<BankingScene> scenes = EnumSet.noneOf(BankingScene.class);
        if (containsAny(normalized, "账户", "余额", "存款", "account", "balance")) {
            scenes.add(BankingScene.ACCOUNT);
        }
        if (containsAny(normalized, "账单", "消费", "支出", "收入", "流水", "花费", "异常", "商户",
                "bill", "expense", "income", "transaction", "spend")) {
            scenes.add(BankingScene.BILLING);
        }
        if (containsAny(normalized, "转账", "汇款", "打款", "transfer", "remit")) {
            scenes.add(BankingScene.TRANSFER);
        }
        if (containsAny(normalized, "卡片", "银行卡", "冻结卡", "解冻", "挂失", "境外支付", "线上支付",
                "卡限额", "card", "freeze card")) {
            scenes.add(BankingScene.CARD);
        }
        if (SubscriptionIntent.relatesToSubscription(normalized)) {
            scenes.add(BankingScene.SUBSCRIPTION);
        }
        if (containsAny(normalized, "理财", "投资", "持仓", "基金", "产品", "wealth", "portfolio", "invest")) {
            scenes.add(BankingScene.WEALTH);
        }

        boolean explicitOrchestration = containsAny(normalized, "联动", "跨场景", "一起处理", "组合任务");
        boolean explicitLink = containsAny(normalized, "一起", "同时", "顺便", "然后", "再把",
                "并且", "并购买", "并投资", "并转账", "并取消", "并冻结", "并解冻",
                "and then", "together");
        boolean crossScene = explicitOrchestration || (explicitLink && scenes.size() >= 2);
        List<String> missing = crossScene && scenes.size() < 2 ? List.of("secondBankingScene") : List.of();
        return new StructuredIntent(StructuredIntent.Command.START, scenes, crossScene, missing);
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
