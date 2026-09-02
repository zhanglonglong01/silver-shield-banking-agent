package com.fintechathon.silvershield.tool.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintechathon.silvershield.security.BankingPrincipal;
import com.fintechathon.silvershield.tool.capability.AllowedToolSetSnapshot;
import com.fintechathon.silvershield.tool.gateway.ToolRuntimeService;
import com.fintechathon.silvershield.tool.gateway.ToolRuntimeService.ToolReference;
import com.fintechathon.silvershield.tool.model.ToolGatewayResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolRuntimeController {

    private final ToolRuntimeService runtime;

    public ToolRuntimeController(ToolRuntimeService runtime) {
        this.runtime = runtime;
    }

    @PostMapping("/allowed")
    public AllowedToolSetSnapshot allowed(
            @AuthenticationPrincipal BankingPrincipal principal,
            @Valid @RequestBody AllowedToolsRequest request) {
        return runtime.allowedTools(principal, request.candidates().stream()
                .map(candidate -> new ToolReference(candidate.name(), candidate.version())).toList());
    }

    @PostMapping("/execute")
    public ToolGatewayResult execute(
            @AuthenticationPrincipal BankingPrincipal principal,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExecuteToolRequest request) {
        return runtime.execute(principal, request.toolName(), request.toolVersion(), request.arguments(), idempotencyKey);
    }

    public record AllowedToolsRequest(
            @NotEmpty @Size(max = 20) List<@NotNull @Valid ToolCandidate> candidates) {}

    public record ToolCandidate(
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Size(max = 16) String version) {}

    public record ExecuteToolRequest(
            @NotBlank @Size(max = 128) String toolName,
            @NotBlank @Size(max = 32) String toolVersion,
            @NotNull JsonNode arguments) {}
}
