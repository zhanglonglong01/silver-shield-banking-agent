package com.fintechathon.silvershield.security;

public enum AssuranceLevel {
    AAL1,
    AAL2;

    public boolean atLeast(AssuranceLevel required) {
        return ordinal() >= required.ordinal();
    }
}
