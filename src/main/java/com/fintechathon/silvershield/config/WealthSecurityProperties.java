package com.fintechathon.silvershield.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.wealth-security")
public class WealthSecurityProperties {
    private Duration suitabilityTtl = Duration.ofMinutes(10);

    public Duration getSuitabilityTtl() {
        return suitabilityTtl;
    }

    public void setSuitabilityTtl(Duration suitabilityTtl) {
        this.suitabilityTtl = suitabilityTtl;
    }
}
