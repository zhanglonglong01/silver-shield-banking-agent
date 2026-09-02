package com.fintechathon.silvershield.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintechathon.silvershield.config.BankingSecurityProperties;
import com.fintechathon.silvershield.identity.domain.AppUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthSessionServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final AtomicReference<String> storedJson = new AtomicReference<>();
    private AuthSessionService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(invocation -> {
                    storedJson.set(invocation.getArgument(1));
                    return null;
                })
                .when(values).set(anyString(), anyString(), any(Duration.class));
        when(values.get(anyString())).thenAnswer(invocation -> storedJson.get());
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        BankingSecurityProperties properties = new BankingSecurityProperties();
        properties.setSessionTtl(Duration.ofHours(2));
        properties.setAal2Ttl(Duration.ofMinutes(5));
        properties.setSandboxAal2Code("246810");
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T02:00:00Z"), ZoneOffset.UTC);
        service = new AuthSessionService(redis, objectMapper, properties, clock);
    }

    @Test
    void issuedSessionMustAlwaysStartAtAal1AndBeReadable() {
        BankingPrincipal principal = new BankingPrincipal(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "demo.alice", "hash",
                "林晓岚（虚构）", AppUser.Role.CUSTOMER, AppUser.Status.ACTIVE, AssuranceLevel.AAL1);

        AuthSessionService.IssuedSession issued = service.issue(principal);

        assertThat(issued.accessToken()).hasSizeGreaterThanOrEqualTo(40);
        assertThat(issued.session().assuranceLevel()).isEqualTo(AssuranceLevel.AAL1);
        assertThat(service.find(issued.accessToken())).contains(issued.session());
    }

    @Test
    void redisKeyMustHashRatherThanExposeBearerToken() {
        BankingPrincipal principal = new BankingPrincipal(
                UUID.randomUUID(), "demo.alice", "hash", "林晓岚（虚构）",
                AppUser.Role.CUSTOMER, AppUser.Status.ACTIVE, AssuranceLevel.AAL1);
        AuthSessionService.IssuedSession issued = service.issue(principal);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);

        verify(values).set(key.capture(), anyString(), any(Duration.class));

        assertThat(key.getValue()).startsWith("silvershield:auth:session:");
        assertThat(key.getValue()).doesNotContain(issued.accessToken());
    }

    @Test
    void sandboxStepUpMustUpgradeOnlyWithConfiguredCode() {
        BankingPrincipal principal = new BankingPrincipal(
                UUID.randomUUID(), "demo.alice", "hash", "林晓岚（虚构）",
                AppUser.Role.CUSTOMER, AppUser.Status.ACTIVE, AssuranceLevel.AAL1);
        AuthSessionService.IssuedSession issued = service.issue(principal);

        AuthSession upgraded = service.stepUp(issued.accessToken(), "246810");

        assertThat(upgraded.assuranceLevel()).isEqualTo(AssuranceLevel.AAL2);
        assertThat(upgraded.aal2ExpiresAt()).isEqualTo(Instant.parse("2026-09-01T02:05:00Z"));
        assertThat(service.find(issued.accessToken())).contains(upgraded);
    }

    @Test
    void sandboxStepUpMustFailClosedForWrongCode() {
        BankingPrincipal principal = new BankingPrincipal(
                UUID.randomUUID(), "demo.alice", "hash", "林晓岚（虚构）",
                AppUser.Role.CUSTOMER, AppUser.Status.ACTIVE, AssuranceLevel.AAL1);
        AuthSessionService.IssuedSession issued = service.issue(principal);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.stepUp(issued.accessToken(), "000000"))
                .isInstanceOf(AuthSessionService.StepUpDeniedException.class);
        assertThat(service.find(issued.accessToken()).orElseThrow().assuranceLevel()).isEqualTo(AssuranceLevel.AAL1);
    }
}
