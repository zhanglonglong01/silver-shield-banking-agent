package com.fintechathon.silvershield.operation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.banking.domain.*;
import com.fintechathon.silvershield.banking.repository.*;
import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import com.fintechathon.silvershield.config.TransferSecurityProperties;
import com.fintechathon.silvershield.identity.repository.AppUserRepository;
import com.fintechathon.silvershield.operation.domain.*;
import com.fintechathon.silvershield.operation.repository.*;
import com.fintechathon.silvershield.security.AssuranceLevel;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecureTransferOperationService implements TransferOperationUseCase {
    private final OperationRequestRepository operations; private final ConfirmationChallengeRepository challenges;
    private final RiskDecisionRepository risks; private final IdempotencyRecordRepository idempotencies;
    private final TransferOrderRepository transfers; private final OperationAuditRepository audits;
    private final AccountRepository accounts; private final PayeeRepository payees; private final AccountTransactionRepository transactions;
    private final AppUserRepository users;
    private final TransferRiskEngine riskEngine; private final TransferSecurityProperties properties; private final ObjectMapper mapper; private final Clock clock;
    public SecureTransferOperationService(OperationRequestRepository operations,ConfirmationChallengeRepository challenges,
            RiskDecisionRepository risks,IdempotencyRecordRepository idempotencies,TransferOrderRepository transfers,
            OperationAuditRepository audits,AccountRepository accounts,PayeeRepository payees,
            AccountTransactionRepository transactions,AppUserRepository users,TransferRiskEngine riskEngine,
            TransferSecurityProperties properties,ObjectMapper mapper,Clock clock){
        this.operations=operations;this.challenges=challenges;this.risks=risks;this.idempotencies=idempotencies;
        this.transfers=transfers;this.audits=audits;this.accounts=accounts;this.payees=payees;this.transactions=transactions;
        this.users=users;this.riskEngine=riskEngine;this.properties=properties;this.mapper=mapper;this.clock=clock;
        if(properties.getOperationTtl()==null||properties.getOperationTtl().isZero()||properties.getOperationTtl().isNegative()
                ||properties.getIdempotencyTtl()==null||properties.getIdempotencyTtl().compareTo(properties.getOperationTtl())<0){
            throw new IllegalArgumentException("Invalid transfer operation TTL configuration");
        }
    }

    @Override @Transactional
    public synchronized PreparationResult prepare(PreparationContext context,TransferCommand command){
        validate(command);
        Instant now=Instant.now(clock); String canonical=json(command); String requestHash=hash(canonical); String keyHash=hash(context.idempotencyKey());
        Optional<IdempotencyRecord> existing=idempotencies.findByUserIdAndScopeAndIdempotencyKeyHash(context.userId(),"TRANSFER",keyHash);
        if(existing.isPresent()){
            if(!MessageDigest.isEqual(existing.get().getRequestHash().getBytes(StandardCharsets.US_ASCII),requestHash.getBytes(StandardCharsets.US_ASCII)))
                return new PreparationResult(false,"IDEMPOTENCY_CONFLICT","同一幂等键不能用于不同转账参数",get(context.userId(),existing.get().getOperationId()));
            OperationView prior=get(context.userId(),existing.get().getOperationId());
            boolean reusable=!Set.of("DENIED","FAILED","EXPIRED","CANCELLED").contains(prior.status());
            return new PreparationResult(reusable,"IDEMPOTENT_REPLAY",reusable?"返回此前创建的同一操作":"此前操作已终止",prior);
        }
        Account account=accounts.findByIdAndUserId(command.sourceAccountId(),context.userId()).orElse(null);
        Payee payee=payees.findByIdAndUserId(command.payeeId(),context.userId()).orElse(null);
        UUID operationId=UUID.randomUUID(); Instant expires=now.plus(properties.getOperationTtl());
        if(account==null||payee==null){
            return new PreparationResult(false,"RESOURCE_NOT_AVAILABLE","账户或收款人不存在或不属于当前用户",null);
        }
        TransferRiskEngine.Evaluation evaluation=riskEngine.evaluate(account,payee,command.amount(),command.currency());
        Map<String,Object> summary=summary(account,payee,command);
        OperationRequest operation=new OperationRequest(operationId,context.requestId(),context.conversationId(),context.userId(),
                canonical,requestHash,json(summary),hash(json(summary)),evaluation.requiredAal(),keyHash,now,expires);
        if(!evaluation.allowed()) operation.markDenied(evaluation.reasonCodes().get(0),now);
        operations.save(operation);
        risks.save(risk(operationId,context.userId(),RiskDecision.Phase.PRE_CONFIRM,evaluation,requestHash,now));
        audits.save(new OperationAudit(operationId,context.userId(),"PREPARE",evaluation.allowed()?"ALLOW":"DENY",
                evaluation.reasonCodes().get(0),evaluation.explanation(),now));
        idempotencies.save(new IdempotencyRecord(context.userId(),keyHash,requestHash,operationId,now,now.plus(properties.getIdempotencyTtl())));
        if(!evaluation.allowed()){
            idempotencies.findByOperationId(operationId).ifPresent(row->row.denied(now));
            return new PreparationResult(false,evaluation.reasonCodes().get(0),evaluation.explanation(),view(operation,null));
        }
        ConfirmationChallenge challenge=challenges.save(new ConfirmationChallenge(UUID.randomUUID(),operationId,context.userId(),now,expires));
        return new PreparationResult(true,"CONFIRMATION_REQUIRED",evaluation.explanation(),view(operation,challenge));
    }

    @Override @Transactional(readOnly=true,noRollbackFor=ResourceNotFoundException.class)
    public OperationView get(UUID userId,UUID operationId){
        OperationRequest operation=operations.findByIdAndUserId(operationId,userId).orElseThrow(ResourceNotFoundException::new);
        return view(operation,challenges.findByOperationId(operationId).orElse(null));
    }

    @Override @Transactional
    public ConfirmationResult confirm(UUID userId,AssuranceLevel assurance,UUID operationId,UUID challengeId,boolean confirmed){
        Instant now=Instant.now(clock);
        OperationRequest operation=operations.lockOwned(operationId,userId).orElseThrow(ResourceNotFoundException::new);
        if(operation.getStatus()==OperationRequest.Status.SUCCEEDED){
            TransferOrder receipt=transfers.findByOperationId(operationId).orElseThrow();
            return success(operation,receipt,"IDEMPOTENT_REPLAY","该操作已成功，返回原始回执");
        }
        if(operation.terminal()) return new ConfirmationResult("DENIED",operation.getFailureCode(),"操作已终止，不能再次执行",view(operation,null),null,List.of(operation.getFailureCode()));
        ConfirmationChallenge challenge=challenges.lockBound(challengeId,operationId,userId).orElseThrow(ResourceNotFoundException::new);
        if(!operation.getExpiresAt().isAfter(now)||!challenge.getExpiresAt().isAfter(now)){
            operation.markExpired(now);challenge.expire();audit(operation,"CONFIRM","DENY","OPERATION_EXPIRED","确认窗口已过期",now);
            return denied(operation,"OPERATION_EXPIRED","确认窗口已过期",List.of("OPERATION_EXPIRED"));
        }
        if(challenge.getStatus()!=ConfirmationChallenge.Status.PENDING)
            return denied(operation,"CHALLENGE_ALREADY_USED","确认挑战已使用或失效",List.of("CHALLENGE_ALREADY_USED"));
        if(!confirmed){operation.cancel(now);challenge.cancel();audit(operation,"CONFIRM","DENY","USER_CANCELLED","用户取消了操作",now);return denied(operation,"USER_CANCELLED","用户取消了操作",List.of("USER_CANCELLED"));}
        if(!assurance.atLeast(operation.getRequiredAal())){
            audit(operation,"CONFIRM","DENY","AAL2_REQUIRED","当前会话认证强度不足",now);
            return denied(operation,"AAL2_REQUIRED","请先完成 AAL2 验证后再次确认",List.of("AAL2_REQUIRED"));
        }
        TransferCommand command=read(operation.getCanonicalArgsJson());
        if(!hash(json(command)).equals(operation.getArgsHash())
                || !hash(operation.getDisplaySummaryJson()).equals(operation.getSummaryHash())){
            operation.markDenied("OPERATION_INTEGRITY_FAILED",now);audit(operation,"FINAL_GUARD","DENY","OPERATION_INTEGRITY_FAILED","操作参数完整性校验失败",now);
            return denied(operation,"OPERATION_INTEGRITY_FAILED","操作完整性校验失败",List.of("OPERATION_INTEGRITY_FAILED"));
        }
        users.lockById(userId).orElseThrow(ResourceNotFoundException::new);
        List<UUID> lockIds=new ArrayList<>();lockIds.add(command.sourceAccountId());
        Payee payee=payees.lockOwned(command.payeeId(),userId).orElseThrow(ResourceNotFoundException::new);
        if(payee.getInternalAccountId()!=null&&!payee.getInternalAccountId().equals(command.sourceAccountId())) lockIds.add(payee.getInternalAccountId());
        lockIds.sort(Comparator.comparing(UUID::toString)); Map<UUID,Account> locked=new HashMap<>();
        for(UUID id:lockIds) locked.put(id,accounts.lockById(id).orElseThrow(ResourceNotFoundException::new));
        Account source=locked.get(command.sourceAccountId());
        if(!source.getUserId().equals(userId)) throw new ResourceNotFoundException();
        TransferRiskEngine.Evaluation evaluation=riskEngine.evaluate(source,payee,command.amount(),command.currency());
        risks.save(risk(operationId,userId,RiskDecision.Phase.FINAL_GUARD,evaluation,operation.getArgsHash(),now));
        if(!evaluation.allowed()){
            operation.markDenied(evaluation.reasonCodes().get(0),now);challenge.consume(now);idempotencies.findByOperationId(operationId).ifPresent(row->row.denied(now));
            audit(operation,"FINAL_GUARD","DENY",evaluation.reasonCodes().get(0),evaluation.explanation(),now);
            return denied(operation,evaluation.reasonCodes().get(0),evaluation.explanation(),evaluation.reasonCodes());
        }
        operation.markExecuting(now); BigDecimal before=source.getAvailableBalance(); source.debit(command.amount());
        if(payee.getInternalAccountId()!=null){Account target=locked.get(payee.getInternalAccountId());target.credit(command.amount());
            transactions.save(new AccountTransaction(UUID.randomUUID(),target.getId(),now,AccountTransaction.Direction.IN,command.amount(),command.currency(),
                    "转账入账",command.purpose(),source.getAccountMasked(),target.getLedgerBalance()));}
        transactions.save(new AccountTransaction(UUID.randomUUID(),source.getId(),now,AccountTransaction.Direction.OUT,command.amount(),command.currency(),
                payee.getName(),command.purpose(),payee.getAccountMasked(),source.getLedgerBalance()));
        payee.markUsed(now); UUID receiptId=UUID.randomUUID();
        TransferOrder receipt=transfers.save(new TransferOrder(receiptId,operationId,userId,source.getId(),payee.getId(),command.amount(),
                command.currency(),command.purpose(),before,source.getAvailableBalance(),now));
        challenge.consume(now);operation.markSucceeded(receiptId,now);idempotencies.findByOperationId(operationId).ifPresent(row->row.succeeded(now));
        audit(operation,"FINAL_GUARD","ALLOW","TRANSFER_EXECUTED","最终权限、风险、确认与完整性检查均通过",now);
        return success(operation,receipt,"TRANSFER_EXECUTED","转账已原子入账并生成回执");
    }

    private OperationView view(OperationRequest operation,ConfirmationChallenge challenge){
        Object summary;try{summary=mapper.readTree(operation.getDisplaySummaryJson());}catch(JsonProcessingException e){summary=Map.of("unavailable",true);}
        UUID cid=challenge==null?null:challenge.getId();
        String effectiveStatus=!operation.terminal()&&!operation.getExpiresAt().isAfter(Instant.now(clock))
                ?OperationRequest.Status.EXPIRED.name():operation.getStatus().name();
        return new OperationView(operation.getId(),effectiveStatus,operation.getRequiredAal(),operation.getExpiresAt(),cid,
                "/api/v1/operations/"+operation.getId()+"/confirm",summary,operation.getResultRefId(),operation.getFailureCode());
    }
    private ConfirmationResult denied(OperationRequest op,String code,String explanation,List<String> reasons){return new ConfirmationResult("DENIED",code,explanation,view(op,challenges.findByOperationId(op.getId()).orElse(null)),null,reasons);}
    private ConfirmationResult success(OperationRequest op,TransferOrder receipt,String code,String explanation){return new ConfirmationResult("SUCCEEDED",code,explanation,view(op,challenges.findByOperationId(op.getId()).orElse(null)),
            new TransferReceipt(receipt.getId(),op.getId(),"SUCCEEDED",receipt.getAmount(),receipt.getCurrency(),receipt.getSourceBalanceBefore(),receipt.getSourceBalanceAfter(),receipt.getCompletedAt()),List.of("ALL_FINAL_GUARDS_PASSED"));}
    private RiskDecision risk(UUID op,UUID user,RiskDecision.Phase phase,TransferRiskEngine.Evaluation e,String input,Instant now){
        RiskDecision.Outcome outcome=!e.allowed()?RiskDecision.Outcome.DENY
                :phase==RiskDecision.Phase.FINAL_GUARD?RiskDecision.Outcome.ALLOW
                :e.requiredAal()==AssuranceLevel.AAL2?RiskDecision.Outcome.REQUIRE_STEP_UP:RiskDecision.Outcome.REQUIRE_CONFIRMATION;
        return new RiskDecision(op,user,phase,outcome,input,json(e.reasonCodes()),e.explanation(),now);
    }
    private void audit(OperationRequest op,String stage,String outcome,String code,String text,Instant now){audits.save(new OperationAudit(op.getId(),op.getUserId(),stage,outcome,code,text,now));}
    private static Map<String,Object> summary(Account a,Payee p,TransferCommand c){return new LinkedHashMap<>(Map.of("sourceAccountId",a.getId(),"sourceAccount",a.getAccountMasked(),"payeeId",p.getId(),"payee",p.getName()+" "+p.getAccountMasked(),"amount",c.amount().setScale(2).toPlainString(),"currency",c.currency(),"purpose",c.purpose()==null?"":c.purpose()));}
    private static void validate(TransferCommand c){if(c==null||c.sourceAccountId()==null||c.payeeId()==null||c.amount()==null||c.currency()==null)throw new IllegalArgumentException("Missing transfer fields");}
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("Unable to serialize operation",e);}}
    private TransferCommand read(String json){try{return mapper.readValue(json,TransferCommand.class);}catch(JsonProcessingException e){throw new IllegalStateException("Unable to read operation",e);}}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
