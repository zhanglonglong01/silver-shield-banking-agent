package com.fintechathon.silvershield.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

class ToolSafetyConfigurationTest {

    @Test
    void shouldKeepFrameworkToolFallbackAndAutomaticCallingDisabled() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.ai.tools.resolution.fallback.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.ai.chat.client.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.ai.chat.client.tool-calling.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.ai.model.audio.speech")).isEqualTo("none");
        assertThat(properties.getProperty("spring.ai.model.audio.transcription")).isEqualTo("none");
        assertThat(properties.getProperty("spring.ai.model.embedding")).isEqualTo("none");
        assertThat(properties.getProperty("spring.ai.model.image")).isEqualTo("none");
        assertThat(properties.getProperty("spring.ai.model.moderation")).isEqualTo("none");
        assertThat(properties.getProperty("silvershield.tool-security.policy-version"))
                .isEqualTo("m4-transfer-v1");
        assertThat(properties.getProperty("silvershield.tool-security.snapshot-ttl")).isEqualTo("PT60S");
    }

    @Test
    void m3MustContainSpringAiButKeepProviderDisabledByDefault() {
        assertThat(ClassUtils.isPresent("org.springframework.ai.chat.client.ChatClient", getClass().getClassLoader()))
                .isTrue();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.ai.model.chat")).isEqualTo("${SPRING_AI_CHAT_MODEL:none}");
        assertThat(properties.getProperty("spring.ai.openai.api-key")).isEqualTo("${OPENAI_API_KEY:}");
    }
}
