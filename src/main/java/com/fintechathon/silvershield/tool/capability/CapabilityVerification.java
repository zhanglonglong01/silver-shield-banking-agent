package com.fintechathon.silvershield.tool.capability;

public record CapabilityVerification(boolean allowed, CapabilityDenyReason denyReason) {

    public static CapabilityVerification allow() {
        return new CapabilityVerification(true, null);
    }

    public static CapabilityVerification deny(CapabilityDenyReason reason) {
        return new CapabilityVerification(false, reason);
    }
}
