package com.fintechathon.silvershield.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.security")
public class BankingSecurityProperties {

    private Duration sessionTtl = Duration.ofHours(2);

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }
}
