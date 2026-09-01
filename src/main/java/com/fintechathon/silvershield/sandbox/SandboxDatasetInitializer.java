package com.fintechathon.silvershield.sandbox;

import com.fintechathon.silvershield.config.SandboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
@Order(0)
public class SandboxDatasetInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SandboxDatasetInitializer.class);

    private final SandboxProperties properties;
    private final SandboxResetService resetService;

    public SandboxDatasetInitializer(SandboxProperties properties, SandboxResetService resetService) {
        this.properties = properties;
        this.resetService = resetService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!resetService.isCurrentAndIntact()) {
            SandboxDatasetMetadata metadata = resetService.reset();
            log.info("Initialized mock banking dataset version={} digest={}",
                    metadata.datasetVersion(), metadata.datasetDigest());
        }
    }
}
