package com.fintechathon.silvershield.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final AuthSessionService sessionService;

    public AuthController(AuthenticationManager authenticationManager, AuthSessionService sessionService) {
        this.authenticationManager = authenticationManager;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        BankingPrincipal principal = (BankingPrincipal) authentication.getPrincipal();
        AuthSessionService.IssuedSession issued = sessionService.issue(principal);
        return new LoginResponse(
                issued.accessToken(), "Bearer", issued.session().expiresAt(), issued.session().assuranceLevel(),
                new CurrentUser(principal.userId(), principal.getUsername(), principal.displayName(), principal.role().name()));
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal BankingPrincipal principal) {
        return new MeResponse(
                new CurrentUser(principal.userId(), principal.getUsername(), principal.displayName(), principal.role().name()),
                principal.assuranceLevel(), false);
    }

    @GetMapping("/assurance")
    public AssuranceResponse assurance(@AuthenticationPrincipal BankingPrincipal principal) {
        return new AssuranceResponse(principal.assuranceLevel(), AssuranceLevel.AAL2, false,
                "AAL2 接口已预留，将在高风险操作阶段实现");
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization.startsWith(BEARER_PREFIX)) {
            sessionService.revoke(authorization.substring(BEARER_PREFIX.length()).trim());
        }
    }

    public record LoginRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 128) String password) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            AssuranceLevel assuranceLevel,
            CurrentUser user) {}

    public record CurrentUser(UUID id, String username, String displayName, String role) {}

    public record MeResponse(CurrentUser user, AssuranceLevel assuranceLevel, boolean aal2Available) {}

    public record AssuranceResponse(
            AssuranceLevel currentLevel,
            AssuranceLevel nextLevel,
            boolean upgradeAvailable,
            String message) {}
}
