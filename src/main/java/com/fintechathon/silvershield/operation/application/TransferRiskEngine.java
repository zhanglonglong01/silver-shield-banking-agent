package com.fintechathon.silvershield.operation.application;

import com.fintechathon.silvershield.banking.domain.Account;
import com.fintechathon.silvershield.banking.domain.Payee;
import com.fintechathon.silvershield.config.TransferSecurityProperties;
import com.fintechathon.silvershield.operation.repository.TransferOrderRepository;
import com.fintechathon.silvershield.security.AssuranceLevel;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TransferRiskEngine {
    private final TransferOrderRepository transfers;
    private final TransferSecurityProperties properties;
    private final Clock clock;
    public TransferRiskEngine(TransferOrderRepository transfers, TransferSecurityProperties properties, Clock clock){
        this.transfers=transfers;this.properties=properties;this.clock=clock;
        if(properties.getAal2Threshold()==null||properties.getAal2Threshold().signum()<=0
                ||properties.getVelocityWindow()==null||properties.getVelocityWindow().isZero()||properties.getVelocityWindow().isNegative()
                ||properties.getVelocityMaxCount()<1||properties.getVelocityMaxAmount()==null||properties.getVelocityMaxAmount().signum()<=0){
            throw new IllegalArgumentException("Invalid transfer risk configuration");
        }
    }

    public Evaluation evaluate(Account account, Payee payee, BigDecimal amount, String currency) {
        List<String> reasons=new ArrayList<>();
        if(account.getStatus()!=Account.Status.ACTIVE) reasons.add("SOURCE_ACCOUNT_NOT_ACTIVE");
        if(payee.getStatus()!=Payee.Status.ACTIVE) reasons.add("PAYEE_NOT_ACTIVE");
        if(amount==null||amount.signum()<=0||amount.scale()>2||amount.compareTo(new BigDecimal("1000000"))>0) reasons.add("AMOUNT_INVALID");
        if(currency==null||!currency.matches("[A-Z]{3}")||!currency.equals(account.getCurrency())||!currency.equals(payee.getCurrency())) reasons.add("CURRENCY_MISMATCH");
        if(amount!=null&&amount.signum()>0&&account.getAvailableBalance().compareTo(amount)<0) reasons.add("INSUFFICIENT_FUNDS");
        Instant now=Instant.now(clock);
        Instant dayStart=LocalDate.ofInstant(now,ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal usedToday=transfers.sumSince(account.getId(),dayStart);
        if(amount!=null&&amount.signum()>0&&usedToday.add(amount).compareTo(account.getDailyTransferLimit())>0) reasons.add("DAILY_LIMIT_EXCEEDED");
        Instant velocityFrom=now.minus(properties.getVelocityWindow());
        long recentCount=transfers.countRecent(account.getUserId(),velocityFrom);
        BigDecimal recentAmount=transfers.sumRecent(account.getUserId(),velocityFrom);
        if(recentCount>=properties.getVelocityMaxCount()||amount!=null&&amount.signum()>0
                &&recentAmount.add(amount).compareTo(properties.getVelocityMaxAmount())>0) reasons.add("VELOCITY_LIMIT_EXCEEDED");
        if(!reasons.isEmpty()) return new Evaluation(false,AssuranceLevel.AAL1,List.copyOf(reasons),"转账未通过确定性风险规则");
        boolean aal2=amount.compareTo(properties.getAal2Threshold())>=0||!payee.isTrusted()||payee.getLastUsedAt()==null;
        return new Evaluation(true,aal2?AssuranceLevel.AAL2:AssuranceLevel.AAL1,
                List.of(aal2?"STEP_UP_REQUIRED":"STANDARD_CONFIRMATION_REQUIRED"),
                aal2?"高金额或非可信收款人要求 AAL2 后确认":"风险检查通过，等待用户确认");
    }
    public record Evaluation(boolean allowed, AssuranceLevel requiredAal, List<String> reasonCodes, String explanation) {}
}
