package com.fintechathon.silvershield.tool.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record ToolDefinition(
        ToolId id,
        String description,
        ToolCapability requiredCapability,
        ToolAccessMode accessMode,
        String inputSchemaVersion,
        Set<String> acceptedArguments,
        boolean confirmationRequired) {

    public ToolDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(requiredCapability, "requiredCapability");
        Objects.requireNonNull(accessMode, "accessMode");
        Objects.requireNonNull(inputSchemaVersion, "inputSchemaVersion");
        acceptedArguments = Set.copyOf(acceptedArguments);
        if (description.isBlank() || inputSchemaVersion.isBlank()) {
            throw new IllegalArgumentException("Tool definition text must not be blank");
        }
        if (accessMode != ToolAccessMode.READ_ONLY || confirmationRequired) {
            throw new IllegalArgumentException("M2 registry only accepts read-only tools without confirmation");
        }
    }

    public String descriptorDigest() {
        String canonical = id + "|" + description + "|" + requiredCapability + "|" + accessMode + "|"
                + inputSchemaVersion + "|" + String.join(",", new TreeSet<>(acceptedArguments)) + "|"
                + confirmationRequired;
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
