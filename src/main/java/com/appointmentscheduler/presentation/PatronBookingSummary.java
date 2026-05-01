package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregates booking activity for a single user (patient) for admin patron views.
 */
public final class PatronBookingSummary {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final User user;
    private final int totalBookings;
    private final int activeBookings;
    private final LocalDateTime lastBookingAt;
    private final LocalDateTime nextUpcomingAt;

    private PatronBookingSummary(User user, int totalBookings, int activeBookings,
                                 LocalDateTime lastBookingAt, LocalDateTime nextUpcomingAt) {
        this.user = Objects.requireNonNull(user);
        this.totalBookings = totalBookings;
        this.activeBookings = activeBookings;
        this.lastBookingAt = lastBookingAt;
        this.nextUpcomingAt = nextUpcomingAt;
    }

    public User getUser() {
        return user;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public int getActiveBookings() {
        return activeBookings;
    }

    public LocalDateTime getLastBookingAt() {
        return lastBookingAt;
    }

    public LocalDateTime getNextUpcomingAt() {
        return nextUpcomingAt;
    }

    public String arabicStatsLine() {
        String last = lastBookingAt != null ? lastBookingAt.format(DATE_FMT) : "—";
        String next = nextUpcomingAt != null ? nextUpcomingAt.format(DATE_FMT) : "لا يوجد";
        return String.format("حجوزات: %d · نشطة: %d · آخر موعد: %s · القادم: %s",
            totalBookings, activeBookings, last, next);
    }

    public String englishStatsLine() {
        String last = lastBookingAt != null ? lastBookingAt.format(DATE_FMT) : "—";
        String next = nextUpcomingAt != null ? nextUpcomingAt.format(DATE_FMT) : "None";
        return String.format("Bookings: %d · Active: %d · Last: %s · Next: %s",
            totalBookings, activeBookings, last, next);
    }

    private static boolean isInactiveStatus(String status) {
        return "CANCELLED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status);
    }

    /**
     * When the admin header has a branch selected, include appointments for that branch.
     * Appointments with no branch set (legacy / before defaulting) still appear in every branch view.
     */
    private static boolean matchesClinic(Appointment a, String clinicId) {
        if (clinicId == null || clinicId.isEmpty()) return true;
        String aid = a.getClinicId();
        if (aid == null || aid.isEmpty()) return true;
        return clinicId.equals(aid);
    }

    /**
     * Users who have at least one (non–soft-deleted) appointment, optionally scoped to a clinic.
     * Sorted by most recent appointment first.
     */
    public static List<PatronBookingSummary> build(List<Appointment> appointments,
                                                   Map<String, User> usersById,
                                                   String clinicId,
                                                   LocalDateTime now) {
        List<Appointment> scoped = appointments.stream()
            .filter(Objects::nonNull)
            .filter(a -> !a.isDeleted())
            .filter(a -> matchesClinic(a, clinicId))
            .filter(a -> a.getPatient() != null && a.getTimeSlot() != null && a.getTimeSlot().getStartTime() != null)
            .collect(Collectors.toList());

        Map<String, List<Appointment>> byPatient = scoped.stream()
            .collect(Collectors.groupingBy(a -> a.getPatient().getId()));

        List<PatronBookingSummary> rows = new ArrayList<>();
        for (Map.Entry<String, List<Appointment>> e : byPatient.entrySet()) {
            List<Appointment> appts = e.getValue();
            if (appts.isEmpty()) continue;

            User patientRef = appts.get(0).getPatient();
            User user = usersById != null ? usersById.getOrDefault(e.getKey(), patientRef) : patientRef;

            int total = appts.size();
            int active = (int) appts.stream().filter(a -> !isInactiveStatus(a.getStatus())).count();

            LocalDateTime lastAt = appts.stream()
                .map(a -> a.getTimeSlot().getStartTime())
                .max(Comparator.naturalOrder())
                .orElse(null);

            LocalDateTime nextAt = appts.stream()
                .filter(a -> !isInactiveStatus(a.getStatus()))
                .map(a -> a.getTimeSlot().getStartTime())
                .filter(t -> !t.isBefore(now))
                .min(Comparator.naturalOrder())
                .orElse(null);

            rows.add(new PatronBookingSummary(user, total, active, lastAt, nextAt));
        }

        rows.sort(Comparator
            .comparing((PatronBookingSummary p) -> p.lastBookingAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(p -> p.user.getName(), String.CASE_INSENSITIVE_ORDER));

        return rows;
    }
}
