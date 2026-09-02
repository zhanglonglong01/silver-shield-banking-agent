package com.fintechathon.silvershield.agent.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelRequest;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.schema.AgentToolSchema;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class SpringAiBankingModelClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mustSendOnlyRequestSchemasAndDisableSpringInternalToolExecution() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        ChatModel fakeSpringModel = prompt -> {
            captured.set(prompt);
            AssistantMessage output = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(new AssistantMessage.ToolCall(
                            "call-1", "function", "get_accounts", "{}")))
                    .build();
            return new ChatResponse(List.of(new Generation(output)));
        };
        SpringAiBankingModelClient client = client(fakeSpringModel);
        AgentToolSchema schema = new AgentToolSchema(
                new ToolId("get_accounts", "1.0"), "List owned accounts", "m3-v1",
                "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}", "digest");

        var response = client.complete(new ModelRequest(
                "safe system", List.of(), "accounts", List.of(), "", List.of(schema)));

        assertThat(response.toolCalls()).singleElement().satisfies(call -> {
            assertThat(call.name()).isEqualTo("get_accounts");
            assertThat(call.version()).isEqualTo("1.0");
        });
        ToolCallingChatOptions options = (ToolCallingChatOptions) captured.get().getOptions();
        assertThat(options.getInternalToolExecutionEnabled()).isFalse();
        assertThat(options.getToolCallbacks()).singleElement().satisfies(callback -> {
            assertThat(callback.getToolDefinition().name()).isEqualTo("get_accounts");
            assertThatThrownBy(() -> callback.call("{}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SecureToolGateway");
        });
    }

    @Test
    void unavailableProviderMustFailBeforeAnyToolCanRun() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        SpringAiBankingModelClient client = new SpringAiBankingModelClient(
                factory.getBeanProvider(ChatModel.class), mapper);

        assertThatThrownBy(() -> client.complete(new ModelRequest(
                "system", List.of(), "hello", List.of(), "", List.of())))
                .isInstanceOf(ModelProviderException.class)
                .hasMessageContaining("No chat model");
    }

    @Test
    void malformedProviderToolArgumentsMustFailClosed() {
        ChatModel fakeSpringModel = prompt -> new ChatResponse(List.of(new Generation(
                AssistantMessage.builder().content("").toolCalls(List.of(
                        new AssistantMessage.ToolCall("call-1", "function", "get_accounts", "not-json")))
                        .build())));
        SpringAiBankingModelClient client = client(fakeSpringModel);

        assertThatThrownBy(() -> client.complete(new ModelRequest(
                "system", List.of(), "accounts", List.of(), "", List.of())))
                .isInstanceOf(ModelProviderException.class)
                .hasMessageContaining("Malformed model tool arguments");
    }

    private SpringAiBankingModelClient client(ChatModel chatModel) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("chatModel", chatModel);
        return new SpringAiBankingModelClient(factory.getBeanProvider(ChatModel.class), mapper);
    }
}
