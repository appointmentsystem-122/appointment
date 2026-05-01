package com.appointmentscheduler.domain.policy;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Policy objects: authorization, state guards, and double-booking prevention.
 */
@DisplayName("BookingPolicies")
class BookingPoliciesTest {

    private final User patient = new User("p1", "P", "p@t.com", "x");
    private final User otherPatient = new User("p2", "Q", "q@t.com", "x");
    private final TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));

    @Nested
    @DisplayName("NoModifyCancelledExpiredPolicy")
    class NoModifyCancelledExpired {

        @Test
        @DisplayName("Denies cancelled appointments with a clear message")
        void deniesCancelled() {
            var pol = new BookingPolicies.NoModifyCancelledExpiredPolicy();
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            a.setStatus("CANCELLED");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, patient);
            Policy.PolicyResult r = pol.evaluate(ctx);
            assertThat(r.isPassed()).isFalse();
            assertThat(r.getMessage()).containsIgnoringCase("cancelled");
        }

        @Test
        @DisplayName("Denies expired appointments")
        void deniesExpired() {
            var pol = new BookingPolicies.NoModifyCancelledExpiredPolicy();
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            a.setStatus("EXPIRED");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }

        @Test
        @DisplayName("Allows normal confirmed flow")
        void allowsConfirmed() {
            var pol = new BookingPolicies.NoModifyCancelledExpiredPolicy();
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            a.setStatus("CONFIRMED");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Null appointment in context is treated as not found")
        void nullAppointment() {
            var pol = new BookingPolicies.NoModifyCancelledExpiredPolicy();
            var ctx = new BookingPolicies.ModifyCancelContext("id", null, patient);
            Policy.PolicyResult r = pol.evaluate(ctx);
            assertThat(r.isPassed()).isFalse();
            assertThat(r.getMessage()).containsIgnoringCase("not found");
        }
    }

    @Nested
    @DisplayName("StateTransitionPolicy")
    class StateTransition {

        @Test
        @DisplayName("Blocks modify/cancel when status is COMPLETED")
        void deniesCompleted() {
            var pol = new BookingPolicies.StateTransitionPolicy();
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            a.setStatus("COMPLETED");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, new Administrator("a", "A", "a@x.com", "p"));
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }

        @Test
        @DisplayName("Blocks modify/cancel when status is EXPIRED")
        void deniesExpired() {
            var pol = new BookingPolicies.StateTransitionPolicy();
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            a.setStatus("EXPIRED");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }

        @Test
        @DisplayName("Null context is a no-op (allow) for this policy")
        void nullContextAllows() {
            assertThat(new BookingPolicies.StateTransitionPolicy().evaluate(null).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Null appointment in context is allow (policy defers to other policies)")
        void nullAppointmentAllows() {
            assertThat(new BookingPolicies.StateTransitionPolicy()
                    .evaluate(new BookingPolicies.ModifyCancelContext("id", null, patient)).isPassed()).isTrue();
        }
    }

    @Nested
    @DisplayName("RequesterAuthorizationPolicy")
    class RequesterAuthorization {

        @Test
        @DisplayName("Owner may act on own appointment")
        void ownerAllowed() {
            var pol = new BookingPolicies.RequesterAuthorizationPolicy(false);
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Non-owner denied when admin flag is false")
        void nonOwnerDenied() {
            var pol = new BookingPolicies.RequesterAuthorizationPolicy(false);
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, otherPatient);
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }

        @Test
        @DisplayName("Administrator allowed when flag permits admin override")
        void adminAllowedWhenFlagTrue() {
            var pol = new BookingPolicies.RequesterAuthorizationPolicy(true);
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, new Administrator("a", "A", "a@x.com", "p"));
            assertThat(pol.evaluate(ctx).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Null requester is always denied")
        void nullRequesterDenied() {
            var pol = new BookingPolicies.RequesterAuthorizationPolicy(true);
            InPersonAppointment a = new InPersonAppointment(patient, slot, "L");
            var ctx = new BookingPolicies.ModifyCancelContext(a.getId(), a, null);
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }
    }

    @Nested
    @DisplayName("NoDoubleBookingPolicy")
    class NoDoubleBooking {

        @Test
        @DisplayName("Same patient overlapping an active appointment is denied")
        void samePatientOverlapDenied() {
            InPersonAppointment existing = new InPersonAppointment(patient, slot, "A");
            existing.setStatus("CONFIRMED");
            InPersonAppointment candidate = new InPersonAppointment(patient, slot, "B");
            var pol = new BookingPolicies.NoDoubleBookingPolicy(() -> List.of(existing));
            var ctx = new BookingPolicies.BookingContext(candidate, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }

        @Test
        @DisplayName("Different patient may use the same slot (policy only guards same patient)")
        void differentPatientSameSlotAllowed() {
            InPersonAppointment existing = new InPersonAppointment(patient, slot, "A");
            existing.setStatus("CONFIRMED");
            InPersonAppointment candidate = new InPersonAppointment(otherPatient, slot, "B");
            var pol = new BookingPolicies.NoDoubleBookingPolicy(() -> List.of(existing));
            var ctx = new BookingPolicies.BookingContext(candidate, otherPatient);
            assertThat(pol.evaluate(ctx).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Cancelled existing appointment does not block a new booking for same patient/slot")
        void cancelledOverlapIgnored() {
            InPersonAppointment existing = new InPersonAppointment(patient, slot, "A");
            existing.setStatus("CANCELLED");
            InPersonAppointment candidate = new InPersonAppointment(patient, slot, "B");
            var pol = new BookingPolicies.NoDoubleBookingPolicy(() -> List.of(existing));
            var ctx = new BookingPolicies.BookingContext(candidate, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Soft-deleted appointments are excluded from overlap checks")
        void deletedExistingIgnored() {
            InPersonAppointment existing = new InPersonAppointment(patient, slot, "A");
            existing.setStatus("CONFIRMED");
            existing.markDeleted("admin");
            InPersonAppointment candidate = new InPersonAppointment(patient, slot, "B");
            var pol = new BookingPolicies.NoDoubleBookingPolicy(() -> List.of(existing));
            var ctx = new BookingPolicies.BookingContext(candidate, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isTrue();
        }

        @Test
        @DisplayName("Null booking context is invalid")
        void nullContextDenied() {
            var pol = new BookingPolicies.NoDoubleBookingPolicy(Collections::emptyList);
            assertThat(pol.evaluate(null).isPassed()).isFalse();
        }

        @Test
        @DisplayName("Null appointment inside context is invalid")
        void nullAppointmentDenied() {
            var pol = new BookingPolicies.NoDoubleBookingPolicy(Collections::emptyList);
            var ctx = new BookingPolicies.BookingContext(null, patient);
            assertThat(pol.evaluate(ctx).isPassed()).isFalse();
        }
    }
}
