package com.fintechathon.silvershield.agent.model;
import com.fintechathon.silvershield.security.AssuranceLevel;
import java.time.Instant; import java.util.UUID;
public record PendingAction(UUID operationId,String status,AssuranceLevel requiredAal,Instant expiresAt,
        UUID challengeId,String confirmEndpoint,Object summary) {}
