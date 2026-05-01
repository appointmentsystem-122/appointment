package com.appointmentscheduler.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyResultTest {

    @Test
    void allowDenyAndMessage() {
        Policy.PolicyResult ok = Policy.PolicyResult.allow();
        assertThat(ok.isPassed()).isTrue();
        assertThat(ok.getMessage()).isEmpty();
        Policy.PolicyResult no = Policy.PolicyResult.deny("reason");
        assertThat(no.isPassed()).isFalse();
        assertThat(no.getMessage()).isEqualTo("reason");
    }
}
