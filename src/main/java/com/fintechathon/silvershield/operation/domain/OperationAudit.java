package com.fintechathon.silvershield.operation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="operation_audit")
public class OperationAudit {
    @Id private UUID id;
    @Column(name="operation_id",nullable=false) private UUID operationId;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(nullable=false) private String stage;
    @Column(nullable=false) private String outcome;
    @Column(name="reason_code",nullable=false) private String reasonCode;
    @Column(name="safe_explanation",nullable=false) private String safeExplanation;
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    protected OperationAudit() {}
    public OperationAudit(UUID operationId,UUID userId,String stage,String outcome,String reasonCode,String explanation,Instant now){
        this.id=UUID.randomUUID();this.operationId=operationId;this.userId=userId;this.stage=stage;this.outcome=outcome;
        this.reasonCode=reasonCode;this.safeExplanation=explanation;this.occurredAt=now;
    }
}
