package com.fintechathon.silvershield.agent.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintechathon.silvershield.tool.schema.AgentToolSchema;
import java.util.List;
import java.util.Objects;

public interface BankingModelClient {

    ModelResponse complete(ModelRequest request);

    record ModelRequest(
            String systemPrompt,
            List<ConversationMessage> conversationHistory,
            String userMessage,
            List<ToolExchange> toolExchanges,
            String deterministicContext,
            List<AgentToolSchema> allowedTools) {
        public ModelRequest {
            Objects.requireNonNull(systemPrompt, "systemPrompt");
            conversationHistory = List.copyOf(conversationHistory);
            Objects.requireNonNull(userMessage, "userMessage");
            toolExchanges = List.copyOf(toolExchanges);
            deterministicContext = deterministicContext == null ? "" : deterministicContext;
            allowedTools = List.copyOf(allowedTools);
        }
    }

    record ModelResponse(String answer, List<ModelToolCall> toolCalls) {
        public ModelResponse {
            answer = answer == null ? "" : answer;
            toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        }

        public static ModelResponse answer(String answer) {
            return new ModelResponse(answer, List.of());
        }

        public static ModelResponse tools(ModelToolCall... calls) {
            return new ModelResponse("", List.of(calls));
        }
    }

    record ModelToolCall(String id, String name, String version, JsonNode arguments) {
        public ModelToolCall {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(arguments, "arguments");
        }
    }

    record ToolExchange(int round, ModelToolCall call, String responseData) {
        public ToolExchange {
            if (round < 1) {
                throw new IllegalArgumentException("round must be positive");
            }
            Objects.requireNonNull(call, "call");
            Objects.requireNonNull(responseData, "responseData");
        }
    }

    record ConversationMessage(ConversationRole role, String text) {
        public ConversationMessage {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(text, "text");
        }
    }

    enum ConversationRole { USER, ASSISTANT }
}
