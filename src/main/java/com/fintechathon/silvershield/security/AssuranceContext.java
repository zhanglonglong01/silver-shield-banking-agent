package com.fintechathon.silvershield.security;

public interface AssuranceContext {
    AssuranceLevel currentLevel();
    boolean satisfies(AssuranceLevel required);
}
