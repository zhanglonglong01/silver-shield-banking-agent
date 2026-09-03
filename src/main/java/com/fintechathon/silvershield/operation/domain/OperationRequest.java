package com.fintechathon.silvershield.operation.domain;

import com.fintechathon.silvershield.security.AssuranceLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_request")
public class OperationRequest {
    public enum Status { AWAITING_STEP_UP, AWAITING_CONFIRMATION, EXECUTING, SUCCEEDED, DENIED, FAILED, EXPIRED, CANCELLED }

    @Id private UUID id;
    @Column(name="request_id", nullable=false) private UUID requestId;
    @Column(name="conversation_id") private UUID conversationId;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="tool_name", nullable=false) private String toolName;
    @Column(name="tool_version", nullable=false) private String toolVersion;
    @Column(name="canonical_args_json", nullable=false, columnDefinition="longtext") private String canonicalArgsJson;
    @Column(name="args_hash", nullable=false) private String argsHash;
    @Column(name="display_summary_json", nullable=false, columnDefinition="longtext") private String displaySummaryJson;
    @Column(name="summary_hash", nullable=false) private String summaryHash;
    @Enumerated(EnumType.STRING) @Column(name="required_aal", nullable=false) private AssuranceLevel requiredAal;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="idempotency_key_hash", nullable=false) private String idempotencyKeyHash;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="result_ref_type") private String resultRefType;
    @Column(name="result_ref_id") private UUID resultRefId;
    @Column(name="failure_code") private String failureCode;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Version private long version;

    protected OperationRequest() {}

    public OperationRequest(UUID id, UUID requestId, UUID conversationId, UUID userId,
            String canonicalArgsJson, String argsHash, String displaySummaryJson, String summaryHash,
            AssuranceLevel requiredAal, String idempotencyKeyHash, Instant now, Instant expiresAt) {
        this(id, requestId, conversationId, userId, "prepare_transfer", "1.0", canonicalArgsJson, argsHash,
                displaySummaryJson, summaryHash, requiredAal, idempotencyKeyHash, now, expiresAt);
    }

    public OperationRequest(UUID id, UUID requestId, UUID conversationId, UUID userId,
            String toolName, String toolVersion, String canonicalArgsJson, String argsHash,
            String displaySummaryJson, String summaryHash, AssuranceLevel requiredAal,
            String idempotencyKeyHash, Instant now, Instant expiresAt) {
        this.id=id; this.requestId=requestId; this.conversationId=conversationId; this.userId=userId;
        this.toolName=toolName; this.toolVersion=toolVersion; this.canonicalArgsJson=canonicalArgsJson;
        this.argsHash=argsHash; this.displaySummaryJson=displaySummaryJson; this.summaryHash=summaryHash;
        this.requiredAal=requiredAal; this.idempotencyKeyHash=idempotencyKeyHash; this.createdAt=now;
        this.updatedAt=now; this.expiresAt=expiresAt;
        this.status=requiredAal == AssuranceLevel.AAL2 ? Status.AWAITING_STEP_UP : Status.AWAITING_CONFIRMATION;
    }

    public UUID getId(){return id;} public UUID getUserId(){return userId;} public UUID getConversationId(){return conversationId;}
    public String getToolName(){return toolName;} public String getToolVersion(){return toolVersion;}
    public String getCanonicalArgsJson(){return canonicalArgsJson;} public String getArgsHash(){return argsHash;}
    public String getDisplaySummaryJson(){return displaySummaryJson;} public String getSummaryHash(){return summaryHash;}
    public AssuranceLevel getRequiredAal(){return requiredAal;} public Status getStatus(){return status;}
    public Instant getExpiresAt(){return expiresAt;} public UUID getResultRefId(){return resultRefId;}
    public String getFailureCode(){return failureCode;} public Instant getCreatedAt(){return createdAt;}
    public boolean terminal(){return status==Status.SUCCEEDED||status==Status.DENIED||status==Status.FAILED||status==Status.EXPIRED||status==Status.CANCELLED;}
    public void markExecuting(Instant now){status=Status.EXECUTING;updatedAt=now;}
    public void markSucceeded(UUID receiptId, Instant now){status=Status.SUCCEEDED;resultRefType="TRANSFER_ORDER";resultRefId=receiptId;updatedAt=now;}
    public void markSucceeded(String resultType, UUID receiptId, Instant now){status=Status.SUCCEEDED;resultRefType=resultType;resultRefId=receiptId;updatedAt=now;}
    public void markDenied(String code, Instant now){status=Status.DENIED;failureCode=code;updatedAt=now;}
    public void markExpired(Instant now){status=Status.EXPIRED;failureCode="OPERATION_EXPIRED";updatedAt=now;}
    public void cancel(Instant now){status=Status.CANCELLED;failureCode="USER_CANCELLED";updatedAt=now;}
}
