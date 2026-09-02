package com.fintechathon.silvershield.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintechathon.silvershield.config.BankingSecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private static final String KEY_PREFIX = "silvershield:auth:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BankingSecurityProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthSessionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            BankingSecurityProperties properties,
            Clock clock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
        if (properties.getAal2Ttl() == null || properties.getAal2Ttl().isZero()
                || properties.getAal2Ttl().isNegative()) {
            throw new IllegalArgumentException("AAL2 TTL must be positive");
        }
    }

    public IssuedSession issue(BankingPrincipal principal) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant issuedAt = Instant.now(clock);
        AuthSession session = new AuthSession(principal.userId(), principal.getUsername(), principal.displayName(),
                principal.role().name(), AssuranceLevel.AAL1, issuedAt, issuedAt.plus(properties.getSessionTtl()), null);
        try {
            redisTemplate.opsForValue().set(key(token), objectMapper.writeValueAsString(session), properties.getSessionTtl());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize authentication session", exception);
        }
        return new IssuedSession(token, session);
    }

    public Optional<AuthSession> find(String token) {
        if (token == null || token.length() < 32 || token.length() > 128) {
            return Optional.empty();
        }
        String json = redisTemplate.opsForValue().get(key(token));
        if (json == null) {
            return Optional.empty();
        }
        try {
            AuthSession session = objectMapper.readValue(json, AuthSession.class);
            if (!session.expiresAt().isAfter(Instant.now(clock))) {
                redisTemplate.delete(key(token));
                return Optional.empty();
            }
            if (session.assuranceLevel() == AssuranceLevel.AAL2
                    && (session.aal2ExpiresAt() == null || !session.aal2ExpiresAt().isAfter(Instant.now(clock)))) {
                AuthSession downgraded = new AuthSession(
                        session.userId(), session.username(), session.displayName(), session.role(),
                        AssuranceLevel.AAL1, session.issuedAt(), session.expiresAt(), null);
                store(token, downgraded);
                return Optional.of(downgraded);
            }
            return Optional.of(session);
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(key(token));
            return Optional.empty();
        }
    }

    public void revoke(String token) {
        if (token != null) {
            redisTemplate.delete(key(token));
        }
    }

    public boolean stepUpAvailable() {
        return properties.getSandboxAal2Code() != null && !properties.getSandboxAal2Code().isBlank();
    }

    public AuthSession stepUp(String token, String code) {
        AuthSession current = find(token).orElseThrow(() -> new StepUpDeniedException("AUTH_SESSION_INVALID"));
        String expected = properties.getSandboxAal2Code();
        if (!stepUpAvailable()) {
            throw new StepUpDeniedException("AAL2_NOT_CONFIGURED");
        }
        byte[] supplied = code == null ? new byte[0] : code.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied)) {
            throw new StepUpDeniedException("AAL2_CODE_INVALID");
        }
        Instant now = Instant.now(clock);
        Instant aal2ExpiresAt = now.plus(properties.getAal2Ttl());
        if (aal2ExpiresAt.isAfter(current.expiresAt())) {
            aal2ExpiresAt = current.expiresAt();
        }
        AuthSession upgraded = new AuthSession(
                current.userId(), current.username(), current.displayName(), current.role(), AssuranceLevel.AAL2,
                current.issuedAt(), current.expiresAt(), aal2ExpiresAt);
        store(token, upgraded);
        return upgraded;
    }

    private void store(String token, AuthSession session) {
        try {
            Duration remaining = Duration.between(Instant.now(clock), session.expiresAt());
            if (remaining.isNegative() || remaining.isZero()) {
                redisTemplate.delete(key(token));
                return;
            }
            redisTemplate.opsForValue().set(key(token), objectMapper.writeValueAsString(session), remaining);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize authentication session", exception);
        }
    }

    private static String key(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedSession(String accessToken, AuthSession session) {}

    public static final class StepUpDeniedException extends RuntimeException {
        private final String reasonCode;
        public StepUpDeniedException(String reasonCode) {
            super("AAL2 step-up denied");
            this.reasonCode = reasonCode;
        }
        public String reasonCode() { return reasonCode; }
    }
}
