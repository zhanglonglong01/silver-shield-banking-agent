package com.fintechathon.silvershield.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.mock.web.MockServletContext;

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

    @Test
    void productionProfileMustNotRegisterSandboxResetHttpRoute() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().setActiveProfiles("prod");
            context.register(ProdWebConfiguration.class, SandboxResetController.class);
            context.refresh();

            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(post("/api/v1/sandbox/reset")).andExpect(status().isNotFound());
            assertThat(context.getBeansOfType(SandboxResetController.class)).isEmpty();
        }
    }

    @Test
    void productionCombinedWithOfflineDemoMustNotRegisterAnySandboxBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod", "offline-demo");
            context.register(
                    SandboxDatasetGenerator.class,
                    SandboxDatasetInitializer.class,
                    SandboxDatasetStore.class,
                    SandboxDigestService.class,
                    SandboxResetService.class,
                    SandboxResetController.class);
            context.refresh();

            assertThat(context.getBeansOfType(SandboxDatasetGenerator.class)).isEmpty();
            assertThat(context.getBeansOfType(SandboxDatasetInitializer.class)).isEmpty();
            assertThat(context.getBeansOfType(SandboxDatasetStore.class)).isEmpty();
            assertThat(context.getBeansOfType(SandboxDigestService.class)).isEmpty();
            assertThat(context.getBeansOfType(SandboxResetService.class)).isEmpty();
            assertThat(context.getBeansOfType(SandboxResetController.class)).isEmpty();
        }
    }

    @Test
    void productionCombinedWithOfflineDemoMustNotRegisterSandboxResetHttpRoute() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().setActiveProfiles("prod", "offline-demo");
            context.register(ProdWebConfiguration.class, SandboxResetController.class);
            context.refresh();

            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(post("/api/v1/sandbox/reset")).andExpect(status().isNotFound());
            assertThat(context.getBeansOfType(SandboxResetController.class)).isEmpty();
        }
    }

    @Configuration
    @EnableWebMvc
    static class ProdWebConfiguration {}
}
