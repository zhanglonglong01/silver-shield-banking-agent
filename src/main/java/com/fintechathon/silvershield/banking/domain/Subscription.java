package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class Subscription {

    public enum BillingCycle { WEEKLY, MONTHLY, QUARTERLY, YEARLY }
    public enum Status { ACTIVE, PAUSED, CANCELLED, EXPIRED }

    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "charge_account_id", nullable = false) private UUID chargeAccountId;
    @Column(name = "merchant_name", nullable = false) private String merchantName;
    @Column(name = "service_name", nullable = false) private String serviceName;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "billing_cycle", nullable = false) private BillingCycle billingCycle;
    @Column(name = "next_charge_date") private LocalDate nextChargeDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "detected_from", nullable = false) private String detectedFrom;
    @Column(name = "cancellation_effect", nullable = false) private String cancellationEffect;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private long version;

    protected Subscription() {}

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getChargeAccountId() { return chargeAccountId; }
    public String getMerchantName() { return merchantName; }
    public String getServiceName() { return serviceName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public LocalDate getNextChargeDate() { return nextChargeDate; }
    public Status getStatus() { return status; }
    public String getCancellationEffect() { return cancellationEffect; }
    public long getVersion() { return version; }

    public void cancel() {
        status = Status.CANCELLED;
        nextChargeDate = null;
    }
}
