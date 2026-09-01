package com.fintechathon.silvershield.tool.audit;

public interface ToolSecurityAuditSink {

    void record(ToolSecurityAuditRecord record);
}
