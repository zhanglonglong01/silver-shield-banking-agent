package com.fintechathon.silvershield.tool.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fintechathon.silvershield.tool.model.ToolAccessMode;
import com.fintechathon.silvershield.tool.model.ToolArguments;
import com.fintechathon.silvershield.tool.model.ToolCapability;
import com.fintechathon.silvershield.tool.model.ToolDefinition;
import com.fintechathon.silvershield.tool.model.ToolExecutionContext;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.model.ToolPayload;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExplicitToolRegistryTest {

    @Test
    void registryMustResolveOnlyExactExplicitNameAndVersionWithoutFallback() {
        ToolHandler<EmptyArguments> handler = handler("get_accounts", "1.0");
        ExplicitToolRegistry registry = new ExplicitToolRegistry(List.of(handler));

        assertThat(registry.resolve(new ToolId("get_accounts", "1.0"))).contains(handler);
        assertThat(registry.resolve(new ToolId("get_accounts", "2.0"))).isEmpty();
        assertThat(registry.resolve(new ToolId("get_account", "1.0"))).isEmpty();
    }

    @Test
    void duplicateToolIdMustFailApplicationStartupRatherThanOverride() {
        assertThatThrownBy(() -> new ExplicitToolRegistry(List.of(
                handler("get_accounts", "1.0"), handler("get_accounts", "1.0"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate tool registration");
    }

    private static ToolHandler<EmptyArguments> handler(String name, String version) {
        return new ToolHandler<>() {
            private final ToolDefinition definition = new ToolDefinition(
                    new ToolId(name, version), "test read tool", ToolCapability.ACCOUNT_READ,
                    ToolAccessMode.READ_ONLY, "test-v1", Set.of(), false);
            @Override public ToolDefinition definition() { return definition; }
            @Override public Class<EmptyArguments> argumentsType() { return EmptyArguments.class; }
            @Override public ToolPayload execute(ToolExecutionContext context, EmptyArguments arguments) {
                return new ToolPayload(List.of(), 0);
            }
        };
    }

    private record EmptyArguments() implements ToolArguments {}
}
