package com.fintechathon.silvershield.agent.application;

import com.fintechathon.silvershield.agent.evidence.ExecutionEvidence;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ModelToolCall;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;

public record AgentToolExecution(
        ModelToolCall call,
        ToolGatewayResult result,
        ExecutionEvidence evidence) {}
