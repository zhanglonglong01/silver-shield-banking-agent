package com.fintechathon.silvershield.agent.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgentToolCandidateSelectorTest {
    private final AgentToolCandidateSelector selector = new AgentToolCandidateSelector();

    @Test
    void cardIntentMustExposePreparationButNeverFinalMutation() {
        assertThat(selector.select("请冻结我的银行卡")).extracting(tool -> tool.name())
                .contains("list_cards", "get_card_controls", "prepare_card_status_change")
                .doesNotContain("execute_card_change");
    }

    @Test
    void subscriptionIntentMustExposeVerifiedCancellationPreparation() {
        assertThat(selector.select("分析费用并取消订阅")).extracting(tool -> tool.name())
                .contains("list_subscriptions", "analyze_subscription_costs", "prepare_subscription_cancellation")
                .doesNotContain("cancel_subscription_now");
    }

    @Test
    void wealthIntentMustExposeSuitabilityBeforePreparationAndNeverPurchaseExecutor() {
        assertThat(selector.select("用储蓄账户购买理财")).extracting(tool -> tool.name())
                .contains("list_wealth_products", "check_wealth_suitability", "prepare_wealth_purchase")
                .doesNotContain("execute_wealth_purchase");
    }

    @Test
    void confirmationTextMustOnlyExposeOperationStatus() {
        assertThat(selector.select("确认执行刚才的操作")).extracting(tool -> tool.name())
                .containsExactly("get_operation_status");
    }
}
