package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.policy.Policy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ordered policy evaluation: first failure wins; only {@link ClassCastException} is swallowed when a
 * policy does not apply to the context type.
 */
@DisplayName("PolicyEngine")
class PolicyEngineTest {

    @Nested
    @DisplayName("evaluate")
    class Evaluate {

        @Test
        @DisplayName("First denying policy short-circuits later policies")
        void firstDenyWins() {
            PolicyEngine engine = new PolicyEngine();
            engine.registerPolicy((Policy<Object>) ctx -> Policy.PolicyResult.deny("no"));
            engine.registerPolicy((Policy<Object>) ctx -> Policy.PolicyResult.allow());

            Policy.PolicyResult r = engine.evaluate(new Object());
            assertThat(r.isPassed()).isFalse();
            assertThat(r.getMessage()).isEqualTo("no");
        }

        @Test
        @DisplayName("All-allow policies yield success")
        void allAllow() {
            PolicyEngine engine = new PolicyEngine();
            engine.registerPolicy((Policy<Object>) ctx -> Policy.PolicyResult.allow());
            engine.registerPolicy((Policy<Object>) ctx -> Policy.PolicyResult.allow());

            assertThat(engine.evaluate(new Object()).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Policies that cannot accept the context type are skipped (ClassCastException)")
        void wrongContextTypeSkipsPolicy() {
            PolicyEngine engine = new PolicyEngine();
            engine.registerPolicy((Policy<String>) s -> Policy.PolicyResult.deny("str"));
            assertThat(engine.evaluate(Integer.valueOf(42)).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Runtime exceptions other than ClassCastException propagate (fail-fast)")
        void nonClassCastExceptionPropagates() {
            PolicyEngine engine = new PolicyEngine();
            engine.registerPolicy((Policy<Object>) ctx -> {
                throw new IllegalStateException("boom");
            });
            assertThrows(IllegalStateException.class, () -> engine.evaluate(new Object()));
        }
    }

    @Nested
    @DisplayName("registration")
    class Registration {

        @Test
        @DisplayName("Null policy registration is ignored")
        void nullPolicyIgnored() {
            PolicyEngine engine = new PolicyEngine();
            engine.registerPolicy(null);
            assertThat(engine.evaluate("any").isPassed()).isTrue();
        }
    }
}
