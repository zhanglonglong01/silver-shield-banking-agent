package com.fintechathon.silvershield.agent.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintechathon.silvershield.sandbox.SandboxDatasetGenerator;
import com.fintechathon.silvershield.sandbox.SandboxDatasetInitializer;
import com.fintechathon.silvershield.sandbox.SandboxDatasetStore;
import com.fintechathon.silvershield.sandbox.SandboxDigestService;
import com.fintechathon.silvershield.sandbox.SandboxResetController;
import com.fintechathon.silvershield.sandbox.SandboxResetService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class OfflineDemoProfileTest {

    @Test
    void offlineDemoSelectsExactlyTheDeterministicProvider() {
        assertThat(profileValues(OfflineDemoBankingModelClient.class)).containsExactly("offline-demo");
        assertThat(profileValues(SpringAiBankingModelClient.class)).containsExactly("!offline-demo");
    }

    @Test
    void allResetDependenciesAreAvailableOfflineButNeverInProd() {
        List<Class<?>> sandboxTypes = List.of(
                SandboxDatasetGenerator.class, SandboxDatasetInitializer.class, SandboxDatasetStore.class,
                SandboxDigestService.class, SandboxResetService.class, SandboxResetController.class);

        for (Class<?> type : sandboxTypes) {
            assertThat(profileValues(type)).as(type.getSimpleName())
                    .containsExactly("!prod & (dev | test | offline-demo)");
        }
    }

    @Test
    void composeDefaultsToOfflineDemo() throws IOException {
        assertThat(Files.readString(Path.of("compose.yml")))
                .contains("SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-offline-demo}");
    }

    private static String[] profileValues(Class<?> type) {
        return type.getAnnotation(Profile.class).value();
    }
}
