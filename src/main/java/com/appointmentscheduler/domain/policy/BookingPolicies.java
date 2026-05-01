package com.appointmentscheduler.domain.policy;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;

import java.util.List;
import java.util.Objects;

/**
 * Policy contexts and implementations for booking operations.
 */
public final class BookingPolicies {

    /** Context for booking an appointment. */
    public static final class BookingContext {
        public final Appointment appointment;
        public final User requester;

        public BookingContext(Appointment appointment, User requester) {
            this.appointment = appointment;
            this.requester = requester;
        }
    }

    /** Context for modify/cancel operations. */
    public static final class ModifyCancelContext {
        public final String appointmentId;
        public final Appointment appointment;
        public final User requester;

        public ModifyCancelContext(String appointmentId, Appointment appointment, User requester) {
            this.appointmentId = appointmentId;
            this.appointment = appointment;
            this.requester = requester;
        }
    }

    /** Prevents modifying or cancelling CANCELLED or EXPIRED appointments. */
    public static class NoModifyCancelledExpiredPolicy implements Policy<ModifyCancelContext> {
        @Override
        public PolicyResult evaluate(ModifyCancelContext context) {
            if (context == null || context.appointment == null) return PolicyResult.deny("Appointment not found");
            String s = context.appointment.getStatus();
            if ("CANCELLED".equals(s) || "EXPIRED".equals(s)) {
                return PolicyResult.deny("Cannot modify or cancel a " + s.toLowerCase() + " appointment");
            }
            return PolicyResult.allow();
        }
    }

    /** Ensures requester is patient or admin. */
    public static class RequesterAuthorizationPolicy implements Policy<ModifyCancelContext> {
        private final boolean allowAdminModifyAny;

        public RequesterAuthorizationPolicy(boolean allowAdminModifyAny) {
            this.allowAdminModifyAny = allowAdminModifyAny;
        }

        @Override
        public PolicyResult evaluate(ModifyCancelContext context) {
            if (context == null || context.requester == null) return PolicyResult.deny("Unauthorized");
            boolean isOwner = context.appointment != null && context.appointment.getPatient().equals(context.requester);
            boolean isAdmin = context.requester instanceof Administrator;
            if (isOwner || (allowAdminModifyAny && isAdmin)) return PolicyResult.allow();
            return PolicyResult.deny("You do not have permission to perform this action");
        }
    }

    /** Prevents double booking for the same user (same time slot). */
    public static class NoDoubleBookingPolicy implements Policy<BookingContext> {
        private final java.util.function.Supplier<java.util.List<Appointment>> allAppointmentsSupplier;

        public NoDoubleBookingPolicy(java.util.function.Supplier<java.util.List<Appointment>> allAppointmentsSupplier) {
            this.allAppointmentsSupplier = allAppointmentsSupplier;
        }

        @Override
        public PolicyResult evaluate(BookingContext context) {
            if (context == null || context.appointment == null) return PolicyResult.deny("Invalid context");
            List<Appointment> existing = allAppointmentsSupplier.get();
            boolean hasOverlap = existing.stream()
                    .filter(Objects::nonNull)
                    .filter(a -> !a.isDeleted())
                    .filter(a -> !"CANCELLED".equals(a.getStatus()))
                    .filter(a -> a.getPatient().equals(context.appointment.getPatient()))
                    .anyMatch(a -> a.getTimeSlot().overlapsWith(context.appointment.getTimeSlot()));
            if (hasOverlap) return PolicyResult.deny("You already have an appointment in this time slot");
            return PolicyResult.allow();
        }
    }

    /** Validates appointment status transitions. */
    public static class StateTransitionPolicy implements Policy<ModifyCancelContext> {
        @Override
        public PolicyResult evaluate(ModifyCancelContext context) {
            if (context == null || context.appointment == null) return PolicyResult.allow();
            String status = context.appointment.getStatus();
            if ("COMPLETED".equals(status) || "EXPIRED".equals(status)) {
                return PolicyResult.deny("Cannot change a " + status.toLowerCase() + " appointment");
            }
            return PolicyResult.allow();
        }
    }
}
