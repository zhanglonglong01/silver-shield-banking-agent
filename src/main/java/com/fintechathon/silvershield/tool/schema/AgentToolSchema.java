package com.fintechathon.silvershield.tool.schema;

import com.fintechathon.silvershield.tool.model.ToolId;
import java.util.Objects;

public record AgentToolSchema(
        ToolId id,
        String description,
        String inputSchemaVersion,
        String inputJsonSchema,
        String descriptorDigest) {

    public AgentToolSchema {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(inputSchemaVersion, "inputSchemaVersion");
        Objects.requireNonNull(inputJsonSchema, "inputJsonSchema");
        Objects.requireNonNull(descriptorDigest, "descriptorDigest");
    }
}
