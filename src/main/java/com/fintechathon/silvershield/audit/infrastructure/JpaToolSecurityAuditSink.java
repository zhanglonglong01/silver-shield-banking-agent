package com.fintechathon.silvershield.audit.infrastructure;

import com.fintechathon.silvershield.tool.audit.ToolSecurityAuditRecord;
import com.fintechathon.silvershield.tool.audit.ToolSecurityAuditSink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaToolSecurityAuditSink implements ToolSecurityAuditSink {

    private final ToolSecurityAuditRepository repository;

    public JpaToolSecurityAuditSink(ToolSecurityAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ToolSecurityAuditRecord record) {
        repository.saveAndFlush(new ToolSecurityAuditEntity(record));
    }
}
