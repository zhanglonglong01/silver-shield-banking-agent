package com.fintechathon.silvershield.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintechathon.silvershield.agent.provider.BankingModelClient;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelRequest;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("real-llm")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "llm"})
@Testcontainers
class RealLlmIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("silver_shield")
            .withUsername("silver")
            .withPassword("silver_test_password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired BankingModelClient modelClient;

    @Test
    void configuredOpenAiCompatibleProviderMustRespondWithoutAutomaticToolExecution() {
        var response = modelClient.complete(new ModelRequest(
                "Return only the word READY. Do not call tools.", List.of(), "health check",
                List.of(), "", List.of()));

        assertThat(response.toolCalls()).isEmpty();
        assertThat(response.answer()).isNotBlank();
    }
}
