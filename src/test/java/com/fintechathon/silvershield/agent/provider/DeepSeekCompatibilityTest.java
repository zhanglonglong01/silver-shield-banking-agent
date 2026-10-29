package com.fintechathon.silvershield.agent.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelRequest;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ToolExchange;
import com.fintechathon.silvershield.tool.model.ToolId;
import com.fintechathon.silvershield.tool.schema.AgentToolSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DeepSeekCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void profileMustBindThinkingDisabledAndRetainSafetySwitches() {
        var environment = environment();
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("openai");
        assertThat(environment.getProperty("spring.ai.openai.base-url")).isEqualTo("https://api.deepseek.com");
        assertThat(environment.getProperty("spring.ai.openai.chat.completions-path")).isEqualTo("/chat/completions");
        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("test-deepseek-key");
        assertThat(environment.getProperty("spring.ai.retry.max-attempts")).isEqualTo("1");
        assertThat(environment.getProperty("spring.ai.tools.resolution.fallback.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.ai.chat.client.tool-calling.enabled")).isEqualTo("false");
        var options = options(environment);
        assertThat(options.getModel()).isEqualTo("deepseek-flash");
        assertThat(options.getMaxTokens()).isEqualTo(1536);
        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(options.getToolCallbacks()).isEmpty();
    }

    @Test
    void actualHttpPayloadMustUseNonThinkingToolsAndReturnResultsOnlyInUserControlledLoop() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = client(builder);
        var schema = new AgentToolSchema(new ToolId("get_accounts", "1.0"), "Read owned accounts", "test",
                "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}", "digest");
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-deepseek-key"))
                .andExpect(jsonPath("$.model").value("deepseek-flash"))
                .andExpect(jsonPath("$.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.tools.length()").value(1))
                .andExpect(jsonPath("$.tools[0].function.name").value("get_accounts"))
                .andRespond(withSuccess("""
                    {"id":"first","object":"chat.completion","created":1,"model":"deepseek-flash",
                     "choices":[{"index":0,"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                     "tool_calls":[{"id":"call-1","type":"function","function":{"name":"get_accounts","arguments":"{}"}}]}}]}
                    """, MediaType.APPLICATION_JSON));
        // Register both HTTP expectations up front; a framework-initiated call would fail the test.
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(jsonPath("$.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[3].role").value("tool"))
                .andExpect(jsonPath("$.messages[3].tool_call_id").value("call-1"))
                .andRespond(withSuccess("""
                    {"id":"second","object":"chat.completion","created":1,"model":"deepseek-flash",
                     "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant","content":"余额999999，已取消会员"}}]}
                    """, MediaType.APPLICATION_JSON));

        var response = client.complete(new ModelRequest("safe system", List.of(), "查询账户", List.of(), "", List.of(schema)));
        assertThat(response.toolCalls()).singleElement().satisfies(call -> assertThat(call.name()).isEqualTo("get_accounts"));
        var finalResponse = client.complete(new ModelRequest("safe system", List.of(), "查询账户",
                List.of(new ToolExchange(1, response.toolCalls().get(0), "{\"accounts\":[]}")), "", List.of(schema)));
        assertThat(finalResponse.answer()).isEqualTo("查询完成").doesNotContain("999999", "已取消");
        assertThat(finalResponse.toolCalls()).isEmpty();
        server.verify();
    }

    @Test
    void authenticationFailureMustNotFallBackToAnOfflineSuccess() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Invalid authentication\",\"type\":\"authentication_error\"}}"));
        assertThatThrownBy(() -> client(builder).complete(new ModelRequest("system", List.of(), "hello", List.of(), "", List.of())))
                .isInstanceOf(ModelProviderException.class);
        server.verify();
    }

    private SpringAiBankingModelClient client(RestClient.Builder builder) {
        var environment = environment();
        var api = OpenAiApi.builder().baseUrl(environment.getRequiredProperty("spring.ai.openai.base-url"))
                .apiKey(environment.getRequiredProperty("spring.ai.openai.api-key"))
                .completionsPath(environment.getRequiredProperty("spring.ai.openai.chat.completions-path"))
                .restClientBuilder(builder).build();
        ChatModel model = OpenAiChatModel.builder().openAiApi(api).defaultOptions(options(environment))
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
        var factory = new StaticListableBeanFactory();
        factory.addBean("chatModel", model);
        return new SpringAiBankingModelClient(factory.getBeanProvider(ChatModel.class), mapper);
    }

    private OpenAiChatOptions options(StandardEnvironment environment) {
        return Binder.get(environment).bind("spring.ai.openai.chat.options", Bindable.of(OpenAiChatOptions.class))
                .orElseThrow(() -> new IllegalStateException("Missing DeepSeek options"));
    }

    private StandardEnvironment environment() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"), new ClassPathResource("application-deepseek.yml"));
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new PropertiesPropertySource("profile", yaml.getObject()));
        environment.getPropertySources().addFirst(new MapPropertySource("test-secrets", Map.of("DEEPSEEK_API_KEY", "test-deepseek-key")));
        return environment;
    }
}
