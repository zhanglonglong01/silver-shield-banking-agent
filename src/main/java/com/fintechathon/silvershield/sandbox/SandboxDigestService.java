package com.fintechathon.silvershield.sandbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod & (dev | test | offline-demo)")
public class SandboxDigestService {

    private static final List<String> TABLES = List.of(
            "app_user", "customer_risk_profile", "account", "bank_card", "account_transaction",
            "payee", "subscription", "wealth_product");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper canonicalMapper;

    public SandboxDigestService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.canonicalMapper = objectMapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public String computePersistedDigest() {
        Map<String, Object> canonical = new LinkedHashMap<>();
        for (String table : TABLES) {
            canonical.put(table, jdbcTemplate.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        try {
            byte[] json = canonicalMapper.writeValueAsBytes(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to compute sandbox dataset digest", exception);
        }
    }

    public Map<String, Long> rowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : TABLES) {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            counts.put(table, count == null ? 0L : count);
        }
        return Map.copyOf(counts);
    }
}
