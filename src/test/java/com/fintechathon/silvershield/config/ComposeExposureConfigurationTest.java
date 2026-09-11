package com.fintechathon.silvershield.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ComposeExposureConfigurationTest {

    @Test
    void allPublishedServicesMustUseLoopbackSafeDefaults() throws IOException {
        String compose = Files.readString(Path.of("compose.yml"));

        assertThat(compose)
                .contains("- \"127.0.0.1:${MYSQL_PORT:-3307}:3306\"")
                .contains("- \"127.0.0.1:${REDIS_PORT:-6379}:6379\"")
                .contains("- \"${API_BIND_ADDRESS:-127.0.0.1}:${API_PORT:-8080}:8080\"")
                .contains("- \"${WEB_BIND_ADDRESS:-127.0.0.1}:${WEB_PORT:-3000}:80\"")
                .doesNotContain("- \"${MYSQL_PORT:-3307}:3306\"")
                .doesNotContain("- \"${REDIS_PORT:-6379}:6379\"")
                .doesNotContain("- \"${API_PORT:-8080}:8080\"")
                .doesNotContain("- \"${WEB_PORT:-3000}:80\"")
                .doesNotContain("0.0.0.0:${MYSQL_PORT:-3307}:3306")
                .doesNotContain("0.0.0.0:${REDIS_PORT:-6379}:6379");
    }

    @Test
    void exampleEnvironmentMustKeepPublicServicesOnLoopbackByDefault() throws IOException {
        String exampleEnvironment = Files.readString(Path.of(".env.example"));

        assertThat(exampleEnvironment)
                .contains("API_BIND_ADDRESS=127.0.0.1")
                .contains("WEB_BIND_ADDRESS=127.0.0.1");
    }
}
