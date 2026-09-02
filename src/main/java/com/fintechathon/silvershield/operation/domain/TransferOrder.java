package com.fintechathon.silvershield.operation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="transfer_order")
public class TransferOrder {
    public enum Status { SUCCEEDED, FAILED }
    @Id private UUID id;
    @Column(name="operation_id",nullable=false) private UUID operationId;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="source_account_id",nullable=false) private UUID sourceAccountId;
    @Column(name="payee_id",nullable=false) private UUID payeeId;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal amount;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal fee;
    @Column(nullable=false,length=3,columnDefinition="char(3)") private String currency;
    private String purpose;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="source_balance_before",nullable=false,precision=19,scale=4) private BigDecimal sourceBalanceBefore;
    @Column(name="source_balance_after",nullable=false,precision=19,scale=4) private BigDecimal sourceBalanceAfter;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="completed_at") private Instant completedAt;
    @Column(name="failure_code") private String failureCode;
    @Version private long version;
    protected TransferOrder() {}
    public TransferOrder(UUID id,UUID operationId,UUID userId,UUID sourceAccountId,UUID payeeId,BigDecimal amount,
            String currency,String purpose,BigDecimal before,BigDecimal after,Instant now){
        this.id=id;this.operationId=operationId;this.userId=userId;this.sourceAccountId=sourceAccountId;this.payeeId=payeeId;
        this.amount=amount;this.fee=BigDecimal.ZERO.setScale(4);this.currency=currency;this.purpose=purpose;this.status=Status.SUCCEEDED;
        this.sourceBalanceBefore=before;this.sourceBalanceAfter=after;this.createdAt=now;this.completedAt=now;
    }
    public UUID getId(){return id;} public UUID getOperationId(){return operationId;} public BigDecimal getAmount(){return amount;}
    public String getCurrency(){return currency;} public BigDecimal getSourceBalanceBefore(){return sourceBalanceBefore;}
    public BigDecimal getSourceBalanceAfter(){return sourceBalanceAfter;} public Instant getCompletedAt(){return completedAt;}
}
