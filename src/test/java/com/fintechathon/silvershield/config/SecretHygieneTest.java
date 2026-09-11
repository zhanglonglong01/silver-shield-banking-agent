package com.fintechathon.silvershield.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SecretHygieneTest {

    private static final Pattern OPENAI_SECRET = Pattern.compile("sk-[A-Za-z0-9_-]{20,}");

    @Test
    void committedConfigurationMustNotContainAnOpenAiApiKey() throws IOException {
        List<Path> files = List.of(
                Path.of("src/main/resources/application.yml"),
                Path.of("src/main/resources/application-dev.yml"),
                Path.of("src/main/resources/application-test.yml"),
                Path.of("src/main/resources/application-prod.yml"),
                Path.of("src/main/resources/application-llm.yml"),
                Path.of("src/main/resources/application-offline-demo.yml"),
                Path.of("compose.yml"),
                Path.of(".env.example"));

        for (Path file : files) {
            String content = Files.readString(file);
            assertThat(OPENAI_SECRET.matcher(content).find())
                    .as("No OpenAI-style secret may be committed in %s", file)
                    .isFalse();
        }
        assertThat(Files.readString(Path.of("src/main/resources/application.yml")))
                .contains("${OPENAI_API_KEY:}");
    }

    @Test
    void localSecretFilesMustBeIgnored() throws IOException {
        String gitignore = Files.readString(Path.of(".gitignore"));
        assertThat(gitignore).contains(".env", "application-secrets.yml", "secrets/", "*.pem", "*.key");
    }
}
