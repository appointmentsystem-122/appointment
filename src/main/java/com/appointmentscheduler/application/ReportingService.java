package com.appointmentscheduler.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.persistence.AppointmentRepository;

/**
 * Backend reporting engine.
 * Generates dynamic reports from system data.
 */
public class ReportingService {

    private final AppointmentRepository appointmentRepository;

    public ReportingService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /** Align with PatronBookingSummary: bookings without a branch still match any header filter. */
    private static boolean matchesBranchFilter(Appointment a, String clinicId) {
        if (clinicId == null || clinicId.isEmpty()) return true;
        String aid = a.getClinicId();
        if (aid == null || aid.isEmpty()) return true;
        return clinicId.equals(aid);
    }

    /**
     * Appointments count per type.
     */
    public Map<String, Long> getAppointmentsPerType() {
        return appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .collect(Collectors.groupingBy(
                        a -> a.getClass().getSimpleName(),
                        Collectors.counting()
                ));
    }

    /**
     * Cancellation rate as percentage (0-100). Optional clinic filter.
     */
    public double getCancellationRate() {
        return getCancellationRate(null);
    }

    public double getCancellationRate(String clinicId) {
        List<Appointment> all = appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .filter(a -> matchesBranchFilter(a, clinicId))
                .collect(Collectors.toList());
        if (all.isEmpty()) return 0.0;
        long cancelled = all.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count();
        return 100.0 * cancelled / all.size();
    }

    /**
     * Peak booking hour (0-23) based on appointment start times.
     */
    public int getPeakBookingHour() {
        Map<Integer, Long> byHour = appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .map(a -> a.getTimeSlot().getStartTime().getHour())
                .collect(Collectors.groupingBy(h -> h, Collectors.counting()));
        return byHour.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(9);
    }

    /**
     * Appointments in date range.
     */
    public List<Appointment> getAppointmentsInRange(LocalDate from, LocalDate to) {
        return getAppointmentsInRange(from, to, null);
    }

    /**
     * Total appointments count (excluding soft-deleted). Optional clinic filter.
     */
    public long getTotalAppointmentsCount() {
        return getTotalAppointmentsCount(null);
    }

    public long getTotalAppointmentsCount(String clinicId) {
        return appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .filter(a -> matchesBranchFilter(a, clinicId))
                .count();
    }

    /**
     * Today's appointments count. Optional clinic filter.
     */
    public long getTodayAppointmentsCount() {
        return getTodayAppointmentsCount(null);
    }

    public long getTodayAppointmentsCount(String clinicId) {
        LocalDate today = LocalDate.now();
        return getAppointmentsInRange(today, today, clinicId).size();
    }

    /**
     * This week's appointments count (Mon–Sun). Optional clinic filter.
     */
    public long getThisWeekAppointmentsCount() {
        return getThisWeekAppointmentsCount(null);
    }

    public long getThisWeekAppointmentsCount(String clinicId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1L);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return getAppointmentsInRange(startOfWeek, endOfWeek, clinicId).size();
    }

    /**
     * Last week's appointments count (Mon–Sun). Optional clinic filter. For trend comparison.
     */
    public long getLastWeekAppointmentsCount(String clinicId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfThisWeek = now.minusDays(now.getDayOfWeek().getValue() - 1L);
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
        return getAppointmentsInRange(startOfLastWeek, endOfLastWeek, clinicId).size();
    }

    /**
     * Yesterday's appointments count. Optional clinic filter. For today vs yesterday trend.
     */
    public long getYesterdayAppointmentsCount(String clinicId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return getAppointmentsInRange(yesterday, yesterday, clinicId).size();
    }

    /**
     * Appointments in date range with optional clinic filter.
     */
    public List<Appointment> getAppointmentsInRange(LocalDate from, LocalDate to, String clinicId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .filter(a -> matchesBranchFilter(a, clinicId))
                .filter(a -> !a.getTimeSlot().getStartTime().isBefore(start)
                        && a.getTimeSlot().getStartTime().isBefore(end))
                .collect(Collectors.toList());
    }

    /**
     * Appointments count per doctor ID (excluding deleted/cancelled).
     */
    public Map<String, Long> getAppointmentsByDoctor() {
        return appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .filter(a -> !"CANCELLED".equals(a.getStatus()) && !"EXPIRED".equals(a.getStatus()))
                .filter(a -> a.getDoctorId() != null && !a.getDoctorId().isEmpty())
                .collect(Collectors.groupingBy(Appointment::getDoctorId, Collectors.counting()));
    }

    /**
     * Stats for a given doctor: total bookings, today's count.
     */
    public DoctorStats getDoctorStats(String doctorId) {
        List<Appointment> all = appointmentRepository.findAll().stream()
                .filter(Objects::nonNull).filter(a -> !a.isDeleted())
                .filter(a -> doctorId.equals(a.getDoctorId()))
                .filter(a -> !"CANCELLED".equals(a.getStatus()) && !"EXPIRED".equals(a.getStatus()))
                .collect(Collectors.toList());
        LocalDate today = LocalDate.now();
        long todayCount = all.stream()
                .filter(a -> a.getTimeSlot().getStartTime().toLocalDate().equals(today))
                .count();
        return new DoctorStats(doctorId, all.size(), (int) todayCount);
    }

    /**
     * Simple DTO for doctor statistics.
     */
    public static final class DoctorStats {
        public final String doctorId;
        public final int totalBookings;
        public final int todayBookings;

        public DoctorStats(String doctorId, int totalBookings, int todayBookings) {
            this.doctorId = doctorId;
            this.totalBookings = totalBookings;
            this.todayBookings = todayBookings;
        }
    }
}