package com.fintechathon.silvershield.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.tool-security")
public class ToolSecurityProperties {

    private String policyVersion = "m2-read-only-v1";
    private Duration snapshotTtl = Duration.ofSeconds(60);
    private int maxArgumentsBytes = 8192;

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public Duration getSnapshotTtl() {
        return snapshotTtl;
    }

    public void setSnapshotTtl(Duration snapshotTtl) {
        this.snapshotTtl = snapshotTtl;
    }

    public int getMaxArgumentsBytes() {
        return maxArgumentsBytes;
    }

    public void setMaxArgumentsBytes(int maxArgumentsBytes) {
        this.maxArgumentsBytes = maxArgumentsBytes;
    }
}
