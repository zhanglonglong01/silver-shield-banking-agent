package com.fintechathon.silvershield.operation.api;

import com.fintechathon.silvershield.operation.application.TransferOperationUseCase;
import com.fintechathon.silvershield.operation.application.TransferOperationUseCase.*;
import com.fintechathon.silvershield.security.BankingPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operations")
public class TransferOperationController {
    private final TransferOperationUseCase operations;
    public TransferOperationController(TransferOperationUseCase operations){this.operations=operations;}
    @GetMapping("/{operationId}")
    public OperationView get(@AuthenticationPrincipal BankingPrincipal principal,@PathVariable UUID operationId){
        return operations.get(principal.userId(),operationId);
    }
    @PostMapping("/{operationId}/confirm")
    public ConfirmationResult confirm(@AuthenticationPrincipal BankingPrincipal principal,@PathVariable UUID operationId,
            @RequestBody JsonNode request){
        if(request==null||!request.isObject()||!fieldNames(request).equals(Set.of("challengeId","confirmed"))
                ||!request.path("confirmed").isBoolean()) throw new IllegalArgumentException("Confirmation body is not exact");
        UUID challengeId;
        try{challengeId=UUID.fromString(request.path("challengeId").asText());}catch(RuntimeException e){throw new IllegalArgumentException("Malformed challenge",e);}
        return operations.confirm(principal.userId(),principal.assuranceLevel(),operationId,challengeId,request.path("confirmed").asBoolean());
    }
    private static Set<String> fieldNames(JsonNode node){Set<String> names=new java.util.HashSet<>();node.fieldNames().forEachRemaining(names::add);return names;}
}
