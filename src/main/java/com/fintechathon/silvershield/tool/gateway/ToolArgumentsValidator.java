package com.fintechathon.silvershield.tool.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintechathon.silvershield.config.ToolSecurityProperties;
import com.fintechathon.silvershield.tool.model.ToolArguments;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ToolArgumentsValidator {

    private final ObjectMapper strictMapper;
    private final Validator validator;
    private final ToolSecurityProperties properties;

    public ToolArgumentsValidator(ObjectMapper objectMapper, Validator validator, ToolSecurityProperties properties) {
        this.strictMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.validator = validator;
        this.properties = properties;
    }

    public ValidationResult validate(JsonNode arguments, Class<? extends ToolArguments> targetType) {
        if (arguments == null || !arguments.isObject()) {
            return ValidationResult.invalid();
        }
        try {
            byte[] serialized = strictMapper.writeValueAsBytes(arguments);
            if (serialized.length > properties.getMaxArgumentsBytes()) {
                return ValidationResult.invalid();
            }
            ToolArguments parsed = strictMapper.treeToValue(arguments, targetType);
            Set<ConstraintViolation<ToolArguments>> violations = validator.validate(parsed);
            if (!violations.isEmpty()) {
                return ValidationResult.invalid();
            }
            parsed.validateSemantics();
            return ValidationResult.valid(parsed);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return ValidationResult.invalid();
        }
    }

    public String fingerprint(JsonNode arguments) {
        try {
            JsonNode canonical = canonicalize(arguments == null ? JsonNodeFactory.instance.nullNode() : arguments);
            byte[] bytes = strictMapper.writeValueAsBytes(canonical);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to fingerprint tool arguments", exception);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            node.properties().stream().sorted(Comparator.comparing(java.util.Map.Entry::getKey))
                    .forEach(entry -> sorted.set(entry.getKey(), canonicalize(entry.getValue())));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(child -> array.add(canonicalize(child)));
            return array;
        }
        return node.deepCopy();
    }

    public record ValidationResult(boolean valid, ToolArguments arguments) {
        static ValidationResult valid(ToolArguments arguments) { return new ValidationResult(true, arguments); }
        static ValidationResult invalid() { return new ValidationResult(false, null); }
    }
}
