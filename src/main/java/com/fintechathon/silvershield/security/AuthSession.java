package com.fintechathon.silvershield.security;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        UUID userId,
        String username,
        String displayName,
        String role,
        AssuranceLevel assuranceLevel,
        Instant issuedAt,
        Instant expiresAt,
        Instant aal2ExpiresAt) {}
