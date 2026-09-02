package com.fintechathon.silvershield.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.security")
public class BankingSecurityProperties {

    private Duration sessionTtl = Duration.ofHours(2);
    private Duration aal2Ttl = Duration.ofMinutes(5);
    private String sandboxAal2Code = "";

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public Duration getAal2Ttl() { return aal2Ttl; }
    public void setAal2Ttl(Duration aal2Ttl) { this.aal2Ttl = aal2Ttl; }
    public String getSandboxAal2Code() { return sandboxAal2Code; }
    public void setSandboxAal2Code(String sandboxAal2Code) { this.sandboxAal2Code = sandboxAal2Code; }
}
