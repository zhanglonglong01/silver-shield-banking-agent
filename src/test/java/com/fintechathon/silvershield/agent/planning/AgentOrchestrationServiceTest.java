package com.fintechathon.silvershield.agent.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.PlanStatus;
import com.fintechathon.silvershield.config.AgentProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentOrchestrationServiceTest {
    @Test
    void linkedAdviceMustNotAddCancellationStep() {
        var result = service.start(USER, UUID.randomUUID(), "分析账单，然后找出可以取消的订阅");
        assertThat(result.plan().steps()).extracting(step -> step.tool().name())
                .contains("analyze_subscription_costs").doesNotContain("prepare_subscription_cancellation");
    }
    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC);
    private final AgentOrchestrationService service = new AgentOrchestrationService(
            new AgentIntentClassifier(), new AgentProperties(), CLOCK);

    @Test
    void billingSubscriptionGoldenPathMustBeAnOrderedFiveStepDag() {
        UUID conversation = UUID.randomUUID();
        var result = service.start(USER, conversation, "分析本月账单，然后取消视频订阅");

        assertThat(result.managed()).isTrue();
        assertThat(result.runModel()).isTrue();
        assertThat(result.plan().steps()).extracting(step -> step.tool().name()).containsExactly(
                "get_accounts", "list_transactions", "list_subscriptions",
                "analyze_subscription_costs", "prepare_subscription_cancellation");
        assertThat(result.plan().steps()).hasSizeLessThanOrEqualTo(TaskPlanValidator.MAX_STEPS);
        assertThat(service.readyTools(result.plan().planId())).extracting(tool -> tool.name())
                .containsExactlyInAnyOrder("get_accounts", "list_subscriptions");
    }

    @Test
    void accountWealthGoldenPathMustRequireSuitabilityBeforePreparation() {
        var result = service.start(USER, UUID.randomUUID(), "查看账户余额并购买适合我的理财产品");

        assertThat(result.plan().steps()).extracting(step -> step.tool().name()).containsExactly(
                "get_accounts", "get_account_balance", "list_wealth_products", "get_wealth_portfolio",
                "check_wealth_suitability", "prepare_wealth_purchase");
        var purchase = result.plan().steps().get(5);
        assertThat(purchase.dependsOn()).containsExactly("step-5");
    }

    @Test
    void missingSceneMustBeFilledInSameOwnedConversation() {
        UUID conversation = UUID.randomUUID();
        var first = service.start(USER, conversation, "分析账单并联动处理");
        var second = service.start(USER, conversation, "再取消视频订阅");

        assertThat(first.runModel()).isFalse();
        assertThat(first.plan().status()).isEqualTo(PlanStatus.NEEDS_INPUT);
        assertThat(second.runModel()).isTrue();
        assertThat(second.plan().goal()).contains("分析账单", "取消视频订阅");
    }

    @Test
    void pausedPlanCanResumeWithoutRebuildingCompletedState() {
        UUID conversation = UUID.randomUUID();
        var started = service.start(USER, conversation, "分析账单，然后取消订阅");
        service.pause(started.plan().planId(), "PROVIDER_TIMEOUT");

        var resumed = service.start(USER, conversation, "继续执行");

        assertThat(resumed.runModel()).isTrue();
        assertThat(resumed.plan().planId()).isEqualTo(started.plan().planId());
        assertThat(resumed.plan().status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(resumed.plan().statusReason()).isEqualTo("USER_RESUMED");
    }

    @Test
    void cancellationMustBeTerminalAndSideEffectFree() {
        UUID conversation = UUID.randomUUID();
        service.start(USER, conversation, "分析账单，然后取消订阅");

        var cancelled = service.start(USER, conversation, "取消任务");
        var attemptedResume = service.start(USER, conversation, "继续执行");

        assertThat(cancelled.plan().status()).isEqualTo(PlanStatus.CANCELLED);
        assertThat(cancelled.immediateAnswer()).contains("没有因此执行");
        assertThat(attemptedResume.runModel()).isFalse();
    }
}
