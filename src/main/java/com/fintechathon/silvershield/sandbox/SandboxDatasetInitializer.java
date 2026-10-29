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
@Profile("!prod & (dev | test | offline-demo)")
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
        var existing = resetService.metadata();
        if (existing.isEmpty()) {
            SandboxDatasetMetadata metadata = resetService.reset();
            log.info("Initialized mock banking dataset version={} digest={}",
                    metadata.datasetVersion(), metadata.datasetDigest());
        } else {
            var metadata = existing.orElseThrow();
            if (!metadata.datasetVersion().equals(properties.getDatasetVersion())
                    || metadata.randomSeed() != properties.getRandomSeed()
                    || !metadata.baselineDate().equals(properties.getBaselineDate())) {
                throw new IllegalStateException("Sandbox dataset configuration changed; use explicit reset to replace existing data");
            }
            // Transfers and cancellations legitimately change the baseline digest.
            // Only the explicit reset endpoint may discard those business changes.
            log.info("Preserved existing sandbox dataset version={}", metadata.datasetVersion());
        }
    }
}
