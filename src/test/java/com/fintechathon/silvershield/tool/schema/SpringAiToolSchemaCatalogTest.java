package com.fintechathon.silvershield.tool.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.banking.application.BankingReadUseCase;
import com.fintechathon.silvershield.config.ToolSecurityProperties;
import com.fintechathon.silvershield.identity.domain.AppUser;
import com.fintechathon.silvershield.security.AssuranceLevel;
import com.fintechathon.silvershield.tool.capability.DefaultCapabilityPolicy;
import com.fintechathon.silvershield.tool.handler.ReadOnlyBankingTools;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.registry.ExplicitToolRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpringAiToolSchemaCatalogTest {

    @Test
    void mustGenerateSevenSchemasOnlyFromTheCurrentAllowedSnapshot() throws Exception {
        BankingReadUseCase banking = mock(BankingReadUseCase.class);
        ExplicitToolRegistry registry = registry(banking);
        ToolSecurityProperties properties = new ToolSecurityProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC);
        DefaultCapabilityPolicy policy = new DefaultCapabilityPolicy(registry, properties, clock);
        ToolExecutionContext context = new ToolExecutionContext(
                UUID.randomUUID(), UUID.randomUUID(), AppUser.Role.CUSTOMER, AssuranceLevel.AAL1, "AGENT_CHAT");
        Set<ToolId> all = registry.definitions().stream().map(definition -> definition.id())
                .collect(java.util.stream.Collectors.toSet());

        List<AgentToolSchema> schemas = new SpringAiToolSchemaCatalog(registry)
                .schemasFor(policy.issue(context, all));

        assertThat(schemas).hasSize(7);
        assertThat(schemas).extracting(schema -> schema.id().name()).containsExactlyInAnyOrder(
                "get_accounts", "get_account_balance", "list_transactions", "list_cards",
                "list_payees", "list_subscriptions", "list_wealth_products");
        JsonNode transactionSchema = new ObjectMapper().readTree(schemas.stream()
                .filter(schema -> schema.id().name().equals("list_transactions"))
                .findFirst().orElseThrow().inputJsonSchema());
        assertThat(transactionSchema.path("properties").has("accountId")).isTrue();
        assertThat(transactionSchema.path("properties").has("from")).isTrue();
        assertThat(transactionSchema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(transactionSchema.toString()).doesNotContain(
                "userId", "role", "capability", "assuranceLevel", "allowedTools");
    }

    @Test
    void mustNotLeakRegistryToolsThatAreAbsentFromSnapshot() {
        BankingReadUseCase banking = mock(BankingReadUseCase.class);
        ExplicitToolRegistry registry = registry(banking);
        ToolSecurityProperties properties = new ToolSecurityProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC);
        DefaultCapabilityPolicy policy = new DefaultCapabilityPolicy(registry, properties, clock);
        ToolExecutionContext context = new ToolExecutionContext(
                UUID.randomUUID(), UUID.randomUUID(), AppUser.Role.CUSTOMER, AssuranceLevel.AAL1, "AGENT_CHAT");

        List<AgentToolSchema> schemas = new SpringAiToolSchemaCatalog(registry).schemasFor(
                policy.issue(context, Set.of(new ToolId("get_accounts", "1.0"))));

        assertThat(schemas).singleElement()
                .extracting(schema -> schema.id().toString()).isEqualTo("get_accounts@1.0");
    }

    private ExplicitToolRegistry registry(BankingReadUseCase banking) {
        return new ExplicitToolRegistry(List.of(
                new ReadOnlyBankingTools.GetAccountsTool(banking),
                new ReadOnlyBankingTools.GetAccountBalanceTool(banking),
                new ReadOnlyBankingTools.ListTransactionsTool(banking),
                new ReadOnlyBankingTools.ListCardsTool(banking),
                new ReadOnlyBankingTools.ListPayeesTool(banking),
                new ReadOnlyBankingTools.ListSubscriptionsTool(banking),
                new ReadOnlyBankingTools.ListWealthProductsTool(banking)));
    }
}
