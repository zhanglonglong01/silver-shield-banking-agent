package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_change_history")
public class SubscriptionChangeHistory {
    @Id private UUID id;
    @Column(name = "operation_id", nullable = false) private UUID operationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "subscription_id", nullable = false) private UUID subscriptionId;
    @Column(nullable = false) private String action;
    @Enumerated(EnumType.STRING) @Column(name = "before_status", nullable = false) private Subscription.Status beforeStatus;
    @Enumerated(EnumType.STRING) @Column(name = "after_status", nullable = false) private Subscription.Status afterStatus;
    @Column(name = "reason_code", nullable = false) private String reasonCode;
    @Column(name = "effective_at", nullable = false) private Instant effectiveAt;
    @Column(name = "completed_at", nullable = false) private Instant completedAt;

    protected SubscriptionChangeHistory() {}

    public SubscriptionChangeHistory(UUID id, UUID operationId, UUID userId, UUID subscriptionId,
            Subscription.Status beforeStatus, String reasonCode, Instant effectiveAt, Instant completedAt) {
        this.id = id; this.operationId = operationId; this.userId = userId; this.subscriptionId = subscriptionId;
        this.action = "CANCEL"; this.beforeStatus = beforeStatus; this.afterStatus = Subscription.Status.CANCELLED;
        this.reasonCode = reasonCode; this.effectiveAt = effectiveAt; this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getOperationId() { return operationId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public Instant getEffectiveAt() { return effectiveAt; }
}
