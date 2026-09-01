package com.fintechathon.silvershield.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class SandboxProductionProfileTest {

    @Test
    void productionProfileMustNotRegisterResetControllerOrInitializer() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(SandboxResetController.class, SandboxDatasetInitializer.class);
            context.refresh();

            assertThat(context.getBeansOfType(SandboxResetController.class)).isEmpty();
            assertThat(context.getBeansOfType(SandboxDatasetInitializer.class)).isEmpty();
        }
    }
}
