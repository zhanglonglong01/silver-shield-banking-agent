package com.fintechathon.silvershield.agent.planning;

import com.fintechathon.silvershield.agent.application.AgentToolExecution;
import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.PlanStatus;
import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.PlanStep;
import com.fintechathon.silvershield.agent.planning.AgentTaskPlan.StepStatus;
import com.fintechathon.silvershield.config.AgentProperties;
import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import com.fintechathon.silvershield.tool.model.ToolId;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AgentOrchestrationService {

    private static final String VERSION = "1.0";
    private final ConcurrentHashMap<UUID, PlanSession> sessions = new ConcurrentHashMap<>();
    private final AgentIntentClassifier classifier;
    private final AgentProperties properties;
    private final Clock clock;

    public AgentOrchestrationService(AgentIntentClassifier classifier, AgentProperties properties, Clock clock) {
        this.classifier = classifier;
        this.properties = properties;
        this.clock = clock;
    }

    public StartResult start(UUID userId, UUID conversationId, String userMessage) {
        PlanSession existing = activeSession(conversationId, userId);
        StructuredIntent intent = classifier.classify(userMessage);
        if (intent.command() == StructuredIntent.Command.CANCEL) {
            if (existing == null) {
                return StartResult.controlled(null, "当前没有可取消的跨场景任务。");
            }
            synchronized (existing) {
                existing.status = PlanStatus.CANCELLED;
                existing.statusReason = "USER_CANCELLED";
                existing.updatedAt = now();
                return StartResult.controlled(existing.snapshot(), "跨场景任务已取消；没有因此执行任何银行写操作。");
            }
        }
        if (intent.command() == StructuredIntent.Command.RESUME) {
            if (existing == null) {
                return StartResult.controlled(null, "没有找到可恢复的跨场景任务，请重新说明目标。");
            }
            synchronized (existing) {
                if (existing.status == PlanStatus.AWAITING_CONFIRMATION) {
                    return StartResult.controlled(existing.snapshot(),
                            "任务正在等待独立确认。请先处理 pendingActions 中的确认请求。");
                }
                if (existing.status == PlanStatus.CANCELLED || existing.status == PlanStatus.COMPLETED
                        || existing.status == PlanStatus.REJECTED) {
                    return StartResult.controlled(existing.snapshot(), "该跨场景任务已结束，请创建新任务。");
                }
                existing.steps.replaceAll(step -> step.status() == StepStatus.FAILED
                        ? copy(step, StepStatus.PENDING, step.evidenceIds(), null) : step);
                existing.status = PlanStatus.ACTIVE;
                existing.statusReason = "USER_RESUMED";
                existing.updatedAt = now();
                return StartResult.runnable(existing.snapshot(), existing.priorExecutions());
            }
        }

        String effectiveGoal = userMessage;
        if (existing != null && existing.status == PlanStatus.NEEDS_INPUT) {
            effectiveGoal = existing.goal + "；" + userMessage;
            intent = classifier.classify(effectiveGoal);
        }
        if (!intent.crossSceneRequested()) {
            return StartResult.unmanaged();
        }
        if (!intent.missingSlots().isEmpty()) {
            PlanSession draft = new PlanSession(UUID.randomUUID(), conversationId, userId, effectiveGoal,
                    PlanStatus.NEEDS_INPUT, new ArrayList<>(), intent.missingSlots(),
                    "MISSING_REQUIRED_SCENES", now());
            sessions.put(conversationId, draft);
            return StartResult.controlled(draft.snapshot(),
                    "请至少再指定一个需要联动的银行场景，例如账单、订阅、转账、卡片或理财。");
        }

        List<PlanStep> steps = buildSteps(intent.scenes(), effectiveGoal);
        PlanSession session = new PlanSession(UUID.randomUUID(), conversationId, userId, effectiveGoal,
                PlanStatus.ACTIVE, new ArrayList<>(steps), List.of(), "PLAN_CREATED", now());
        sessions.put(conversationId, session);
        return StartResult.runnable(session.snapshot(), List.of());
    }

    public Set<ToolId> allTools(UUID planId) {
        PlanSession session = required(planId);
        synchronized (session) {
            return session.steps.stream().map(PlanStep::tool)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public Set<ToolId> readyTools(UUID planId) {
        PlanSession session = required(planId);
        synchronized (session) {
            if (session.status != PlanStatus.ACTIVE) {
                return Set.of();
            }
            Set<String> completed = session.steps.stream()
                    .filter(step -> step.status() == StepStatus.COMPLETED)
                    .map(PlanStep::stepId).collect(java.util.stream.Collectors.toSet());
            List<PlanStep> ready = session.steps.stream()
                    .filter(step -> step.status() == StepStatus.PENDING)
                    .filter(step -> completed.containsAll(step.dependsOn()))
                    .toList();
            List<PlanStep> reads = ready.stream()
                    .filter(step -> step.accessMode() == ToolAccessMode.READ_ONLY).toList();
            List<PlanStep> exposed = reads.isEmpty() && !ready.isEmpty() ? List.of(ready.get(0)) : reads;
            return exposed.stream().map(PlanStep::tool)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public boolean isReady(UUID planId, ToolId tool) {
        return readyTools(planId).contains(tool);
    }

    public void record(UUID planId, AgentToolExecution execution) {
        PlanSession session = required(planId);
        synchronized (session) {
            ToolId tool = new ToolId(execution.call().name(), execution.call().version());
            Set<String> completed = session.steps.stream().filter(step -> step.status() == StepStatus.COMPLETED)
                    .map(PlanStep::stepId).collect(java.util.stream.Collectors.toSet());
            int index = -1;
            for (int i = 0; i < session.steps.size(); i++) {
                PlanStep step = session.steps.get(i);
                if (step.tool().equals(tool) && step.status() == StepStatus.PENDING
                        && completed.containsAll(step.dependsOn())) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                for (int i = 0; i < session.steps.size(); i++) {
                    PlanStep step = session.steps.get(i);
                    if (step.tool().equals(tool) && step.status() == StepStatus.COMPLETED
                            && step.accessMode() == ToolAccessMode.READ_ONLY) {
                        List<UUID> evidence = new ArrayList<>(step.evidenceIds());
                        evidence.add(execution.evidence().evidenceId());
                        session.steps.set(i, copy(step, StepStatus.COMPLETED, evidence, execution.result().reasonCode()));
                        session.executions.add(execution);
                        session.updatedAt = now();
                        return;
                    }
                }
                reject(planId, "PLAN_STEP_NOT_READY");
                return;
            }

            PlanStep step = session.steps.get(index);
            List<UUID> evidence = new ArrayList<>(step.evidenceIds());
            evidence.add(execution.evidence().evidenceId());
            if (execution.result().decision() != ToolDecision.ALLOW) {
                session.steps.set(index, copy(step, StepStatus.FAILED, evidence, execution.result().reasonCode()));
                session.status = PlanStatus.PAUSED;
                session.statusReason = execution.result().reasonCode();
            } else {
                session.steps.set(index, copy(step, StepStatus.COMPLETED, evidence, execution.result().reasonCode()));
                if (step.accessMode() == ToolAccessMode.PREPARE_WRITE) {
                    session.status = PlanStatus.AWAITING_CONFIRMATION;
                    session.statusReason = "EXPLICIT_CONFIRMATION_REQUIRED";
                } else if (session.steps.stream().allMatch(item -> item.status() == StepStatus.COMPLETED)) {
                    session.status = PlanStatus.COMPLETED;
                    session.statusReason = "PLAN_COMPLETED";
                }
            }
            session.executions.add(execution);
            session.updatedAt = now();
        }
    }

    public void pause(UUID planId, String reasonCode) {
        PlanSession session = required(planId);
        synchronized (session) {
            if (session.status == PlanStatus.ACTIVE) {
                session.status = PlanStatus.PAUSED;
                session.statusReason = reasonCode;
                session.updatedAt = now();
            }
        }
    }

    public void reject(UUID planId, String reasonCode) {
        PlanSession session = required(planId);
        synchronized (session) {
            session.status = PlanStatus.REJECTED;
            session.statusReason = reasonCode;
            session.updatedAt = now();
        }
    }

    public AgentTaskPlan view(UUID planId) {
        return required(planId).snapshot();
    }

    public List<AgentToolExecution> priorExecutions(UUID planId) {
        return required(planId).priorExecutions();
    }

    private PlanSession activeSession(UUID conversationId, UUID userId) {
        PlanSession session = sessions.get(conversationId);
        if (session == null || !session.userId.equals(userId)) {
            return null;
        }
        if (session.updatedAt.isBefore(now().minus(properties.getConversationTtl()))) {
            sessions.remove(conversationId, session);
            return null;
        }
        return session;
    }

    private PlanSession required(UUID planId) {
        return sessions.values().stream().filter(session -> session.planId.equals(planId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task plan"));
    }

    private List<PlanStep> buildSteps(Set<BankingScene> scenes, String goal) {
        PlanBuilder plan = new PlanBuilder();
        boolean needsAccounts = scenes.contains(BankingScene.ACCOUNT) || scenes.contains(BankingScene.BILLING)
                || scenes.contains(BankingScene.TRANSFER) || scenes.contains(BankingScene.WEALTH);
        if (needsAccounts) {
            plan.add("get_accounts", ToolAccessMode.READ_ONLY);
        }
        if (scenes.contains(BankingScene.ACCOUNT) && containsAny(goal, "余额", "balance")) {
            plan.add("get_account_balance", ToolAccessMode.READ_ONLY, "get_accounts");
        }
        if (scenes.contains(BankingScene.BILLING)) {
            plan.add("list_transactions", ToolAccessMode.READ_ONLY, "get_accounts");
        }
        if (scenes.contains(BankingScene.SUBSCRIPTION)) {
            plan.add("list_subscriptions", ToolAccessMode.READ_ONLY);
            plan.add("analyze_subscription_costs", ToolAccessMode.READ_ONLY, "list_subscriptions");
        }
        if (scenes.contains(BankingScene.CARD)) {
            plan.add("list_cards", ToolAccessMode.READ_ONLY);
            plan.add("get_card_controls", ToolAccessMode.READ_ONLY, "list_cards");
        }
        if (scenes.contains(BankingScene.WEALTH)) {
            plan.add("list_wealth_products", ToolAccessMode.READ_ONLY);
            plan.add("get_wealth_portfolio", ToolAccessMode.READ_ONLY);
            if (containsAny(goal, "购买", "买入", "投资", "invest", "buy")) {
                plan.add("check_wealth_suitability", ToolAccessMode.READ_ONLY,
                        "get_accounts", "list_wealth_products");
                plan.add("prepare_wealth_purchase", ToolAccessMode.PREPARE_WRITE,
                        "check_wealth_suitability");
            }
        }
        if (scenes.contains(BankingScene.TRANSFER)) {
            plan.add("list_payees", ToolAccessMode.READ_ONLY);
            List<String> dependencies = new ArrayList<>(List.of("get_accounts", "list_payees"));
            if (scenes.contains(BankingScene.BILLING)) {
                dependencies.add("list_transactions");
            }
            plan.add("prepare_transfer", ToolAccessMode.PREPARE_WRITE,
                    dependencies.toArray(String[]::new));
        }
        boolean subscriptionCancellation = containsAny(goal, "取消订阅", "取消代扣", "关闭自动扣款",
                "cancel subscription") || (containsAny(goal, "取消", "关闭", "停止")
                && containsAny(goal, "订阅", "代扣", "会员"));
        if (scenes.contains(BankingScene.SUBSCRIPTION) && subscriptionCancellation) {
            plan.add("prepare_subscription_cancellation", ToolAccessMode.PREPARE_WRITE,
                    "analyze_subscription_costs");
        }
        if (scenes.contains(BankingScene.CARD)
                && containsAny(goal, "冻结", "解冻", "挂失", "freeze", "unfreeze")) {
            plan.add("prepare_card_status_change", ToolAccessMode.PREPARE_WRITE, "get_card_controls");
        } else if (scenes.contains(BankingScene.CARD)
                && containsAny(goal, "境外支付", "线上支付", "限额", "card limit")) {
            plan.add("prepare_card_controls_change", ToolAccessMode.PREPARE_WRITE, "get_card_controls");
        }
        return plan.steps();
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private static boolean containsAny(String text, String... tokens) {
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static PlanStep copy(PlanStep step, StepStatus status, List<UUID> evidence, String reasonCode) {
        return new PlanStep(step.stepId(), step.tool(), step.accessMode(), step.dependsOn(), status,
                evidence, reasonCode);
    }

    public record StartResult(
            boolean managed,
            boolean runModel,
            AgentTaskPlan plan,
            String immediateAnswer,
            List<AgentToolExecution> priorExecutions) {
        public StartResult {
            priorExecutions = List.copyOf(priorExecutions);
        }
        static StartResult unmanaged() { return new StartResult(false, true, null, null, List.of()); }
        static StartResult controlled(AgentTaskPlan plan, String answer) {
            return new StartResult(true, false, plan, answer, List.of());
        }
        static StartResult runnable(AgentTaskPlan plan, List<AgentToolExecution> executions) {
            return new StartResult(true, true, plan, null, executions);
        }
    }

    private static final class PlanBuilder {
        private final List<PlanStep> steps = new ArrayList<>();

        private void add(String toolName, ToolAccessMode mode, String... dependencyTools) {
            if (steps.stream().anyMatch(step -> step.tool().name().equals(toolName))) {
                return;
            }
            List<String> dependencies = new ArrayList<>();
            for (String dependency : dependencyTools) {
                steps.stream().filter(step -> step.tool().name().equals(dependency)).findFirst()
                        .map(PlanStep::stepId).ifPresent(dependencies::add);
            }
            steps.add(new PlanStep("step-" + (steps.size() + 1), new ToolId(toolName, VERSION), mode,
                    dependencies, StepStatus.PENDING, List.of(), null));
        }

        private List<PlanStep> steps() {
            return List.copyOf(steps);
        }
    }

    private static final class PlanSession {
        private final UUID planId;
        private final UUID conversationId;
        private final UUID userId;
        private final String goal;
        private PlanStatus status;
        private final ArrayList<PlanStep> steps;
        private final List<String> missingSlots;
        private String statusReason;
        private Instant updatedAt;
        private final ArrayList<AgentToolExecution> executions = new ArrayList<>();

        private PlanSession(UUID planId, UUID conversationId, UUID userId, String goal, PlanStatus status,
                ArrayList<PlanStep> steps, List<String> missingSlots, String statusReason, Instant updatedAt) {
            this.planId = planId;
            this.conversationId = conversationId;
            this.userId = userId;
            this.goal = goal;
            this.status = status;
            this.steps = steps;
            this.missingSlots = List.copyOf(missingSlots);
            this.statusReason = statusReason;
            this.updatedAt = updatedAt;
        }

        private synchronized AgentTaskPlan snapshot() {
            return new AgentTaskPlan(planId, conversationId, goal, status, List.copyOf(steps), missingSlots,
                    statusReason, updatedAt);
        }

        private synchronized List<AgentToolExecution> priorExecutions() {
            return List.copyOf(executions);
        }
    }
}
