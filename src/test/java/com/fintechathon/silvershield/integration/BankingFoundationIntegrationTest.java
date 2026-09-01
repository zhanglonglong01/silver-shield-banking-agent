package com.fintechathon.silvershield.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BankingFoundationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("silver_shield")
            .withUsername("silver")
            .withPassword("silver_test_password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired TestRestTemplate rest;
    @Autowired Flyway flyway;
    @LocalServerPort int port;

    @Test
    void flywayMustApplyAllM1Migrations() {
        long applied = Arrays.stream(flyway.info().applied()).count();
        assertThat(applied).isEqualTo(2);
    }

    @Test
    void normalLoginMustReturnAal1BearerSession() {
        ResponseEntity<JsonNode> response = loginResponse("demo.alice", "Demo@2026!");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(response.getBody().path("assuranceLevel").asText()).isEqualTo("AAL1");
        assertThat(response.getBody().path("accessToken").asText()).isNotBlank();
    }

    @Test
    void invalidCredentialsAndUnauthenticatedAccessMustBeRejected() {
        assertThat(loginResponse("demo.alice", "wrong-password").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity(url("/api/v1/accounts"), JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedUserMustOnlyListOwnAccounts() {
        JsonNode aliceAccounts = get("/api/v1/accounts", token("demo.alice", "Demo@2026!"));
        JsonNode bobAccounts = get("/api/v1/accounts", token("demo.bob", "Demo@2026!"));

        assertThat(aliceAccounts).hasSize(2);
        assertThat(bobAccounts).hasSize(1);
        assertThat(aliceAccounts.toString()).contains("**** 8899", "**** 6628").doesNotContain("**** 3106");
        assertThat(bobAccounts.toString()).contains("**** 3106").doesNotContain("**** 8899");
    }

    @Test
    void crossUserAccountAccessMustReturnNonEnumeratingNotFound() {
        String aliceToken = token("demo.alice", "Demo@2026!");
        JsonNode bobAccounts = get("/api/v1/accounts", token("demo.bob", "Demo@2026!"));
        String bobAccountId = bobAccounts.get(0).path("id").asText();

        ResponseEntity<JsonNode> response = exchangeGet("/api/v1/accounts/" + bobAccountId, aliceToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("code").asText()).isEqualTo("RESOURCE_NOT_AVAILABLE");
    }

    @Test
    void accountTransactionsAndOtherFoundationResourcesMustBeQueryable() {
        String aliceToken = token("demo.alice", "Demo@2026!");
        JsonNode accounts = get("/api/v1/accounts", aliceToken);
        String accountId = accounts.get(0).path("id").asText();

        JsonNode transactions = get(
                "/api/v1/accounts/" + accountId + "/transactions?from=2026-03-01&to=2026-08-31&limit=100",
                aliceToken);

        assertThat(transactions.size()).isGreaterThan(0);
        assertThat(get("/api/v1/cards", aliceToken)).hasSize(2);
        assertThat(get("/api/v1/payees", aliceToken)).hasSize(5);
        assertThat(get("/api/v1/subscriptions", aliceToken)).hasSize(6);
        assertThat(get("/api/v1/wealth-products", aliceToken)).hasSize(8);
    }

    @Test
    void customerMustNotResetSandbox() {
        String aliceToken = token("demo.alice", "Demo@2026!");
        HttpHeaders headers = bearer(aliceToken);

        ResponseEntity<JsonNode> response = rest.exchange(
                url("/api/v1/sandbox/reset"), HttpMethod.POST, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminResetMustProduceExactlyTheSameDigestCountsAndAccountSnapshot() {
        String aliceToken = token("demo.alice", "Demo@2026!");
        String adminToken = token("demo.admin", "Admin@2026!");
        JsonNode beforeMetadata = get("/api/v1/sandbox/metadata", adminToken);
        JsonNode beforeAccounts = get("/api/v1/accounts", aliceToken);

        JsonNode firstReset = reset(adminToken);
        JsonNode secondReset = reset(adminToken);
        JsonNode afterAccounts = get("/api/v1/accounts", aliceToken);

        assertThat(firstReset.path("datasetVersion").asText()).isEqualTo("fintechathon-demo-v1");
        assertThat(firstReset.path("randomSeed").asLong()).isEqualTo(2026L);
        assertThat(firstReset.path("baselineDate").asText()).isEqualTo("2026-08-31");
        assertThat(firstReset.path("datasetDigest").asText()).isEqualTo(beforeMetadata.path("datasetDigest").asText());
        assertThat(secondReset.path("datasetDigest").asText()).isEqualTo(firstReset.path("datasetDigest").asText());
        assertThat(secondReset.path("rowCounts")).isEqualTo(firstReset.path("rowCounts"));
        assertThat(afterAccounts).isEqualTo(beforeAccounts);
    }

    private JsonNode reset(String token) {
        ResponseEntity<JsonNode> response = rest.exchange(
                url("/api/v1/sandbox/reset"), HttpMethod.POST, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private JsonNode get(String path, String token) {
        ResponseEntity<JsonNode> response = exchangeGet(path, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<JsonNode> exchangeGet(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
    }

    private String token(String username, String password) {
        ResponseEntity<JsonNode> response = loginResponse(username, password);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("accessToken").asText();
    }

    private ResponseEntity<JsonNode> loginResponse(String username, String password) {
        return rest.postForEntity(
                url("/api/v1/auth/login"), new LoginRequest(username, password), JsonNode.class);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private record LoginRequest(String username, String password) {}
}
