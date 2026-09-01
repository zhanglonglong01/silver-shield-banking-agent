package com.fintechathon.silvershield.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssuranceLevelTest {

    @Test
    void aal2MustSatisfyBothLevels() {
        assertThat(AssuranceLevel.AAL2.atLeast(AssuranceLevel.AAL1)).isTrue();
        assertThat(AssuranceLevel.AAL2.atLeast(AssuranceLevel.AAL2)).isTrue();
    }

    @Test
    void aal1MustNotSatisfyAal2() {
        assertThat(AssuranceLevel.AAL1.atLeast(AssuranceLevel.AAL2)).isFalse();
    }
}
