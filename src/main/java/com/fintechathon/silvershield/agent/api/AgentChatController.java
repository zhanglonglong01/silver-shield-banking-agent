package com.fintechathon.silvershield.agent.api;

import com.fintechathon.silvershield.agent.application.ReadOnlyBankingAgent;
import com.fintechathon.silvershield.agent.model.AgentChatResult;
import com.fintechathon.silvershield.security.BankingPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentChatController {

    private final ReadOnlyBankingAgent agent;

    public AgentChatController(ReadOnlyBankingAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public AgentChatResult chat(
            @AuthenticationPrincipal BankingPrincipal principal,
            @RequestHeader(name="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody ChatRequest request) {
        return agent.chat(principal, request.message(), request.conversationId(), idempotencyKey);
    }

    public record ChatRequest(
            @NotBlank @Size(max = 4000) String message,
            UUID conversationId) {}
}
