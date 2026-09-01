package com.fintechathon.silvershield.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAssuranceContext implements AssuranceContext {

    @Override
    public AssuranceLevel currentLevel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BankingPrincipal principal) {
            return principal.assuranceLevel();
        }
        throw new IllegalStateException("No authenticated assurance context");
    }

    @Override
    public boolean satisfies(AssuranceLevel required) {
        return currentLevel().atLeast(required);
    }
}
