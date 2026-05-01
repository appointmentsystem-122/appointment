package com.appointmentscheduler.domain.policy;

/**
 * Extensible policy interface for business operation validation.
 * Policies can be added without modifying existing services.
 *
 * @param <T> the context type (e.g. Appointment, BookingRequest)
 */
public interface Policy<T> {

    /**
     * Evaluates the policy against the context.
     *
     * @param context the operation context
     * @return result containing whether the policy passes and optional message
     */
    PolicyResult evaluate(T context);

    /**
     * Result of policy evaluation.
     */
    final class PolicyResult {
        private final boolean passed;
        private final String message;

        public PolicyResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message != null ? message : "";
        }

        public static PolicyResult allow() {
            return new PolicyResult(true, "");
        }

        public static PolicyResult deny(String message) {
            return new PolicyResult(false, message);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }
    }
}
