package com.fintechathon.silvershield.operation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="confirmation_challenge")
public class ConfirmationChallenge {
    public enum Status { PENDING, CONSUMED, EXPIRED, CANCELLED }
    @Id private UUID id;
    @Column(name="operation_id",nullable=false) private UUID operationId;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="consumed_at") private Instant consumedAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Version private long version;
    protected ConfirmationChallenge() {}
    public ConfirmationChallenge(UUID id, UUID operationId, UUID userId, Instant now, Instant expiresAt){
        this.id=id;this.operationId=operationId;this.userId=userId;this.status=Status.PENDING;this.createdAt=now;this.expiresAt=expiresAt;
    }
    public UUID getId(){return id;} public UUID getOperationId(){return operationId;} public UUID getUserId(){return userId;}
    public Status getStatus(){return status;} public Instant getExpiresAt(){return expiresAt;}
    public void consume(Instant now){status=Status.CONSUMED;consumedAt=now;}
    public void expire(){status=Status.EXPIRED;} public void cancel(){status=Status.CANCELLED;}
}
