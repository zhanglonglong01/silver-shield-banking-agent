package com.fintechathon.silvershield.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BillingAnalysisService {

    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("1000.00");

    public BillingAnalysis analyze(List<AgentToolExecution> executions) {
        List<PeriodSummary> rawPeriods = executions.stream()
                .filter(execution -> execution.result().decision() == ToolDecision.ALLOW)
                .filter(execution -> execution.call().name().equals("list_transactions"))
                .map(this::period)
                .toList();
        Map<PeriodKey, List<PeriodSummary>> groupedPeriods = rawPeriods.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        period -> new PeriodKey(period.from(), period.to())));
        List<PeriodSummary> periods = groupedPeriods.values().stream()
                .map(this::mergePeriod)
                .sorted(Comparator.comparing(PeriodSummary::from))
                .toList();

        SubscriptionSummary subscriptions = executions.stream()
                .filter(execution -> execution.result().decision() == ToolDecision.ALLOW)
                .filter(execution -> execution.call().name().equals("list_subscriptions"))
                .findFirst()
                .map(this::subscriptions)
                .orElse(SubscriptionSummary.empty());

        PeriodComparison comparison = periods.size() < 2
                ? null
                : compare(periods.get(periods.size() - 1), periods.get(periods.size() - 2));
        return new BillingAnalysis(periods, comparison, subscriptions);
    }

    private PeriodSummary mergePeriod(List<PeriodSummary> accountPeriods) {
        PeriodSummary first = accountPeriods.get(0);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> categories = new java.util.HashMap<>();
        Map<String, Integer> merchantCounts = new java.util.HashMap<>();
        List<LargeTransaction> largeTransactions = new ArrayList<>();
        for (PeriodSummary accountPeriod : accountPeriods) {
            income = income.add(accountPeriod.income());
            expense = expense.add(accountPeriod.expense());
            accountPeriod.categorySpend().forEach(
                    (category, amount) -> categories.merge(category, amount, BigDecimal::add));
            accountPeriod.frequentMerchants().forEach(
                    merchant -> merchantCounts.merge(merchant.merchant(), merchant.count(), Integer::sum));
            largeTransactions.addAll(accountPeriod.largeTransactions());
        }

        LinkedHashMap<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        categories.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .forEach(entry -> categoryTotals.put(
                        entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)));
        List<MerchantFrequency> frequentMerchants = merchantCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(5)
                .map(entry -> new MerchantFrequency(entry.getKey(), entry.getValue()))
                .toList();
        largeTransactions.sort(Comparator.comparing(LargeTransaction::amount).reversed());

        return new PeriodSummary(
                first.from(), first.to(), income.setScale(2, RoundingMode.HALF_UP),
                expense.setScale(2, RoundingMode.HALF_UP),
                income.subtract(expense).setScale(2, RoundingMode.HALF_UP),
                categoryTotals, List.copyOf(largeTransactions), frequentMerchants);
    }

    private PeriodSummary period(AgentToolExecution execution) {
        JsonNode arguments = execution.call().arguments();
        LocalDate from = LocalDate.parse(arguments.path("from").asText());
        LocalDate to = LocalDate.parse(arguments.path("to").asText());
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new java.util.HashMap<>();
        Map<String, Integer> merchantCounts = new java.util.HashMap<>();
        List<LargeTransaction> largeTransactions = new ArrayList<>();

        JsonNode data = execution.evidence().relevantStructuredResult();
        if (data.isArray()) {
            for (JsonNode transaction : data) {
                BigDecimal amount = money(transaction.path("amount").asText("0"));
                String direction = transaction.path("direction").asText();
                if ("IN".equals(direction)) {
                    income = income.add(amount);
                } else if ("OUT".equals(direction)) {
                    expense = expense.add(amount);
                    String category = safeLabel(transaction.path("categoryCode").asText(), "UNCATEGORIZED");
                    categoryTotals.merge(category, amount, BigDecimal::add);
                    String merchant = safeLabel(transaction.path("merchantName").asText(), "UNKNOWN");
                    merchantCounts.merge(merchant, 1, Integer::sum);
                    if (amount.compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
                        largeTransactions.add(new LargeTransaction(
                                transaction.path("id").asText(), transaction.path("bookingTime").asText(),
                                merchant, category, amount.setScale(2, RoundingMode.HALF_UP)));
                    }
                }
            }
        }

        LinkedHashMap<String, BigDecimal> categories = new LinkedHashMap<>();
        categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .forEach(entry -> categories.put(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)));
        List<MerchantFrequency> frequentMerchants = merchantCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(entry -> new MerchantFrequency(entry.getKey(), entry.getValue()))
                .toList();
        largeTransactions.sort(Comparator.comparing(LargeTransaction::amount).reversed());

        return new PeriodSummary(
                from, to, income.setScale(2, RoundingMode.HALF_UP), expense.setScale(2, RoundingMode.HALF_UP),
                income.subtract(expense).setScale(2, RoundingMode.HALF_UP), categories,
                List.copyOf(largeTransactions), frequentMerchants);
    }

    private SubscriptionSummary subscriptions(AgentToolExecution execution) {
        int activeCount = 0;
        BigDecimal recurringTotal = BigDecimal.ZERO;
        List<SubscriptionItem> items = new ArrayList<>();
        JsonNode data = execution.evidence().relevantStructuredResult();
        if (data.isArray()) {
            for (JsonNode subscription : data) {
                if (!"ACTIVE".equals(subscription.path("status").asText())) {
                    continue;
                }
                activeCount++;
                BigDecimal amount = money(subscription.path("amount").asText("0"));
                recurringTotal = recurringTotal.add(amount);
                items.add(new SubscriptionItem(
                        subscription.path("serviceName").asText(), subscription.path("merchantName").asText(),
                        amount.setScale(2, RoundingMode.HALF_UP), subscription.path("currency").asText(),
                        subscription.path("billingCycle").asText(), subscription.path("nextChargeDate").asText()));
            }
        }
        return new SubscriptionSummary(
                activeCount, recurringTotal.setScale(2, RoundingMode.HALF_UP), List.copyOf(items));
    }

    private PeriodComparison compare(PeriodSummary current, PeriodSummary previous) {
        BigDecimal difference = current.expense().subtract(previous.expense()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentage = previous.expense().signum() == 0
                ? null
                : difference.multiply(BigDecimal.valueOf(100))
                        .divide(previous.expense(), 2, RoundingMode.HALF_UP);
        return new PeriodComparison(previous.from(), previous.to(), current.from(), current.to(), difference, percentage);
    }

    private static BigDecimal money(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Tool result contains malformed money", exception);
        }
    }

    private static String safeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record BillingAnalysis(
            List<PeriodSummary> periods,
            PeriodComparison comparison,
            SubscriptionSummary subscriptions) {
        public boolean hasData() {
            return !periods.isEmpty() || subscriptions.activeCount() > 0;
        }
    }

    public record PeriodSummary(
            LocalDate from,
            LocalDate to,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            Map<String, BigDecimal> categorySpend,
            List<LargeTransaction> largeTransactions,
            List<MerchantFrequency> frequentMerchants) {}

    public record PeriodComparison(
            LocalDate previousFrom,
            LocalDate previousTo,
            LocalDate currentFrom,
            LocalDate currentTo,
            BigDecimal expenseDifference,
            BigDecimal expenseChangePercent) {}

    public record LargeTransaction(
            String transactionId, String bookingTime, String merchant, String category, BigDecimal amount) {}

    public record MerchantFrequency(String merchant, int count) {}

    public record SubscriptionSummary(int activeCount, BigDecimal recurringAmountTotal, List<SubscriptionItem> items) {
        static SubscriptionSummary empty() {
            return new SubscriptionSummary(0, BigDecimal.ZERO.setScale(2), List.of());
        }
    }

    public record SubscriptionItem(
            String serviceName, String merchantName, BigDecimal amount, String currency,
            String billingCycle, String nextChargeDate) {}

    private record PeriodKey(LocalDate from, LocalDate to) {}
}
