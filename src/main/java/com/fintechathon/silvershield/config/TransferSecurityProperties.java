package com.fintechathon.silvershield.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.transfer-security")
public class TransferSecurityProperties {
    private Duration operationTtl = Duration.ofMinutes(5);
    private Duration idempotencyTtl = Duration.ofHours(24);
    private BigDecimal aal2Threshold = new BigDecimal("10000.00");
    private Duration velocityWindow = Duration.ofMinutes(10);
    private int velocityMaxCount = 3;
    private BigDecimal velocityMaxAmount = new BigDecimal("20000.00");

    public Duration getOperationTtl() { return operationTtl; }
    public void setOperationTtl(Duration operationTtl) { this.operationTtl = operationTtl; }
    public Duration getIdempotencyTtl() { return idempotencyTtl; }
    public void setIdempotencyTtl(Duration idempotencyTtl) { this.idempotencyTtl = idempotencyTtl; }
    public BigDecimal getAal2Threshold() { return aal2Threshold; }
    public void setAal2Threshold(BigDecimal aal2Threshold) { this.aal2Threshold = aal2Threshold; }
    public Duration getVelocityWindow() { return velocityWindow; }
    public void setVelocityWindow(Duration velocityWindow) { this.velocityWindow = velocityWindow; }
    public int getVelocityMaxCount() { return velocityMaxCount; }
    public void setVelocityMaxCount(int velocityMaxCount) { this.velocityMaxCount = velocityMaxCount; }
    public BigDecimal getVelocityMaxAmount() { return velocityMaxAmount; }
    public void setVelocityMaxAmount(BigDecimal velocityMaxAmount) { this.velocityMaxAmount = velocityMaxAmount; }
}
