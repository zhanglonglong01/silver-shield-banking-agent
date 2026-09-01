package com.fintechathon.silvershield.config;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.sandbox")
public class SandboxProperties {

    private boolean enabled;
    private String datasetVersion = "fintechathon-demo-v1";
    private long randomSeed = 2026L;
    private LocalDate baselineDate = LocalDate.of(2026, 8, 31);
    private String customerPasswordHash;
    private String adminPasswordHash;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(String datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public LocalDate getBaselineDate() {
        return baselineDate;
    }

    public void setBaselineDate(LocalDate baselineDate) {
        this.baselineDate = baselineDate;
    }

    public String getCustomerPasswordHash() {
        return customerPasswordHash;
    }

    public void setCustomerPasswordHash(String customerPasswordHash) {
        this.customerPasswordHash = customerPasswordHash;
    }

    public String getAdminPasswordHash() {
        return adminPasswordHash;
    }

    public void setAdminPasswordHash(String adminPasswordHash) {
        this.adminPasswordHash = adminPasswordHash;
    }
}
