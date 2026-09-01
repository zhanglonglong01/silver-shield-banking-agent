package com.fintechathon.silvershield.tool.model;

public record ToolPayload(Object data, int recordCount) {

    public ToolPayload {
        if (recordCount < 0) {
            throw new IllegalArgumentException("recordCount must not be negative");
        }
    }
}
