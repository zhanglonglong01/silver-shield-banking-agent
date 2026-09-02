package com.fintechathon.silvershield.agent.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintechathon.silvershield.agent.evidence.ExecutionEvidence;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelToolCall;
import com.fintechathon.silvershield.tool.model.ToolDecision;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingAnalysisServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BillingAnalysisService service = new BillingAnalysisService();

    @Test
    void mustCalculateIncomeExpenseNetCategoriesLargeTransactionsAndFrequentMerchantsDeterministically() {
        AgentToolExecution current = transactionExecution("2026-08-01", "2026-08-31", transactions(
                tx("IN", "3000.00", "SALARY", "Mock Employer"),
                tx("OUT", "120.00", "FOOD", "Demo Cafe"),
                tx("OUT", "80.00", "FOOD", "Demo Cafe"),
                tx("OUT", "1500.00", "HOUSING", "Mock Property")));

        var analysis = service.analyze(List.of(current));
        var period = analysis.periods().get(0);

        assertThat(period.income()).isEqualByComparingTo("3000.00");
        assertThat(period.expense()).isEqualByComparingTo("1700.00");
        assertThat(period.net()).isEqualByComparingTo("1300.00");
        assertThat(period.categorySpend()).containsEntry("FOOD", new BigDecimal("200.00"));
        assertThat(period.largeTransactions()).singleElement()
                .satisfies(item -> assertThat(item.amount()).isEqualByComparingTo("1500.00"));
        assertThat(period.frequentMerchants().get(0).merchant()).isEqualTo("Demo Cafe");
        assertThat(period.frequentMerchants().get(0).count()).isEqualTo(2);
    }

    @Test
    void mustCompareLatestPeriodWithPreviousPeriodUsingJavaDecimalArithmetic() {
        AgentToolExecution previous = transactionExecution("2026-07-01", "2026-07-31", transactions(
                tx("OUT", "1000.00", "FOOD", "Demo Cafe")));
        AgentToolExecution current = transactionExecution("2026-08-01", "2026-08-31", transactions(
                tx("OUT", "1250.00", "FOOD", "Demo Cafe")));

        var comparison = service.analyze(List.of(current, previous)).comparison();

        assertThat(comparison.expenseDifference()).isEqualByComparingTo("250.00");
        assertThat(comparison.expenseChangePercent()).isEqualByComparingTo("25.00");
        assertThat(comparison.currentFrom()).hasToString("2026-08-01");
    }

    @Test
    void mustAggregateMultipleAccountsBeforeComputingOnePeriodSummary() {
        AgentToolExecution firstAccount = transactionExecution("2026-08-01", "2026-08-31", transactions(
                tx("IN", "1000.00", "SALARY", "Mock Employer A"),
                tx("OUT", "200.00", "FOOD", "Demo Cafe")));
        AgentToolExecution secondAccount = transactionExecution("2026-08-01", "2026-08-31", transactions(
                tx("IN", "2000.00", "SALARY", "Mock Employer B"),
                tx("OUT", "300.00", "FOOD", "Demo Cafe"),
                tx("OUT", "1500.00", "HOUSING", "Mock Property")));

        var analysis = service.analyze(List.of(firstAccount, secondAccount));

        assertThat(analysis.periods()).hasSize(1);
        var period = analysis.periods().get(0);
        assertThat(period.income()).isEqualByComparingTo("3000.00");
        assertThat(period.expense()).isEqualByComparingTo("2000.00");
        assertThat(period.net()).isEqualByComparingTo("1000.00");
        assertThat(period.categorySpend()).containsEntry("FOOD", new BigDecimal("500.00"));
        assertThat(period.largeTransactions()).hasSize(1);
        assertThat(period.frequentMerchants()).first()
                .satisfies(merchant -> {
                    assertThat(merchant.merchant()).isEqualTo("Demo Cafe");
                    assertThat(merchant.count()).isEqualTo(2);
                });
    }

    @Test
    void mustSummarizeOnlyActiveSubscriptions() {
        ArrayNode data = mapper.createArrayNode()
                .add(subscription("Video Demo", "88.00", "ACTIVE"))
                .add(subscription("Cancelled Demo", "999.00", "CANCELLED"));

        var analysis = service.analyze(List.of(execution(
                "list_subscriptions", mapper.createObjectNode(), data, 2)));

        assertThat(analysis.subscriptions().activeCount()).isEqualTo(1);
        assertThat(analysis.subscriptions().recurringAmountTotal()).isEqualByComparingTo("88.00");
        assertThat(analysis.subscriptions().items()).singleElement()
                .satisfies(item -> assertThat(item.serviceName()).isEqualTo("Video Demo"));
    }

    private AgentToolExecution transactionExecution(String from, String to, ArrayNode data) {
        ObjectNode arguments = mapper.createObjectNode()
                .put("accountId", UUID.randomUUID().toString())
                .put("from", from)
                .put("to", to)
                .put("limit", 100);
        return execution("list_transactions", arguments, data, data.size());
    }

    private AgentToolExecution execution(String name, JsonNode arguments, JsonNode data, int count) {
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        UUID invocationId = UUID.randomUUID();
        ToolGatewayResult result = new ToolGatewayResult(
                invocationId, UUID.randomUUID(), UUID.randomUUID(), name, "1.0", ToolDecision.ALLOW,
                "READ_ONLY_EXECUTED", "allowed", data, count, now);
        ExecutionEvidence evidence = new ExecutionEvidence(
                UUID.randomUUID(), UUID.randomUUID(), invocationId, name, "1.0", ToolDecision.ALLOW,
                "READ_ONLY_EXECUTED", "digest", data, now);
        return new AgentToolExecution(new ModelToolCall(UUID.randomUUID().toString(), name, "1.0", arguments),
                result, evidence);
    }

    private ArrayNode transactions(ObjectNode... transactions) {
        ArrayNode array = mapper.createArrayNode();
        for (ObjectNode transaction : transactions) {
            array.add(transaction);
        }
        return array;
    }

    private ObjectNode tx(String direction, String amount, String category, String merchant) {
        return mapper.createObjectNode()
                .put("id", UUID.randomUUID().toString())
                .put("bookingTime", "2026-08-15T08:00:00Z")
                .put("direction", direction)
                .put("amount", amount)
                .put("currency", "CNY")
                .put("categoryCode", category)
                .put("merchantName", merchant)
                .put("status", "POSTED");
    }

    private ObjectNode subscription(String service, String amount, String status) {
        return mapper.createObjectNode()
                .put("serviceName", service)
                .put("merchantName", "Mock Merchant")
                .put("amount", amount)
                .put("currency", "CNY")
                .put("billingCycle", "MONTHLY")
                .put("nextChargeDate", "2026-09-15")
                .put("status", status);
    }
}
