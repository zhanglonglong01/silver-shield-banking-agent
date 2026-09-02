package com.fintechathon.silvershield.operation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="idempotency_record")
public class IdempotencyRecord {
    public enum Status { PENDING, SUCCEEDED, DENIED, FAILED }
    @Id private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(nullable=false) private String scope;
    @Column(name="idempotency_key_hash",nullable=false) private String idempotencyKeyHash;
    @Column(name="request_hash",nullable=false) private String requestHash;
    @Column(name="operation_id",nullable=false) private UUID operationId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    protected IdempotencyRecord() {}
    public IdempotencyRecord(UUID userId,String keyHash,String requestHash,UUID operationId,Instant now,Instant expiresAt){
        this.id=UUID.randomUUID();this.userId=userId;this.scope="TRANSFER";this.idempotencyKeyHash=keyHash;
        this.requestHash=requestHash;this.operationId=operationId;this.status=Status.PENDING;this.createdAt=now;this.updatedAt=now;this.expiresAt=expiresAt;
    }
    public String getRequestHash(){return requestHash;} public UUID getOperationId(){return operationId;}
    public void succeeded(Instant now){status=Status.SUCCEEDED;updatedAt=now;} public void denied(Instant now){status=Status.DENIED;updatedAt=now;}
}
