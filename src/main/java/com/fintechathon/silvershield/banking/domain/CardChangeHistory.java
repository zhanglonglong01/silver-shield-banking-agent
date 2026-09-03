package com.fintechathon.silvershield.banking.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_change_history")
public class CardChangeHistory {
    public enum ChangeType { STATUS, CONTROLS }

    @Id private UUID id;
    @Column(name = "operation_id", nullable = false) private UUID operationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "card_id", nullable = false) private UUID cardId;
    @Enumerated(EnumType.STRING) @Column(name = "change_type", nullable = false) private ChangeType changeType;
    @Column(name = "before_state_json", nullable = false, columnDefinition = "longtext") private String beforeStateJson;
    @Column(name = "after_state_json", nullable = false, columnDefinition = "longtext") private String afterStateJson;
    @Column(name = "reason_code", nullable = false) private String reasonCode;
    @Column(name = "completed_at", nullable = false) private Instant completedAt;

    protected CardChangeHistory() {}

    public CardChangeHistory(UUID id, UUID operationId, UUID userId, UUID cardId, ChangeType changeType,
            String beforeStateJson, String afterStateJson, String reasonCode, Instant completedAt) {
        this.id = id; this.operationId = operationId; this.userId = userId; this.cardId = cardId;
        this.changeType = changeType; this.beforeStateJson = beforeStateJson; this.afterStateJson = afterStateJson;
        this.reasonCode = reasonCode; this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getOperationId() { return operationId; }
    public UUID getCardId() { return cardId; }
    public String getAfterStateJson() { return afterStateJson; }
    public Instant getCompletedAt() { return completedAt; }
}
