package com.fintechathon.silvershield.tool.gateway;

public final class ToolDeniedException extends RuntimeException {
    private final String reasonCode;
    private final String safeExplanation;

    public ToolDeniedException(String reasonCode, String safeExplanation) {
        super(safeExplanation);
        this.reasonCode = reasonCode;
        this.safeExplanation = safeExplanation;
    }

    public String reasonCode() { return reasonCode; }
    public String safeExplanation() { return safeExplanation; }
}
