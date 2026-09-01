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
    }

    public IssuedSession issue(BankingPrincipal principal) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant issuedAt = Instant.now(clock);
        AuthSession session = new AuthSession(principal.userId(), principal.getUsername(), principal.displayName(),
                principal.role().name(), AssuranceLevel.AAL1, issuedAt, issuedAt.plus(properties.getSessionTtl()));
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

    private static String key(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedSession(String accessToken, AuthSession session) {}
}
