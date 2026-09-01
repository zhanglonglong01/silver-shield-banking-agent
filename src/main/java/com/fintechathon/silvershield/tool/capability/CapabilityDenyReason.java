package com.fintechathon.silvershield.tool.capability;

public enum CapabilityDenyReason {
    TOOL_NOT_REGISTERED,
    ROLE_NOT_ALLOWED,
    CAPABILITY_NOT_GRANTED,
    SNAPSHOT_INVALID,
    SNAPSHOT_EXPIRED,
    SNAPSHOT_CONTEXT_MISMATCH,
    TOOL_NOT_IN_ALLOWED_SET
}
