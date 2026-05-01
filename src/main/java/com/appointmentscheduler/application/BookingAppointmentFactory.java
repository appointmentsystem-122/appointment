package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;

/**
 * Creates domain {@link Appointment} instances from a unified {@link BookingOption}.
 */
public final class BookingAppointmentFactory {

    private BookingAppointmentFactory() {}

    public static Appointment create(BookingOption opt, User patient, TimeSlot slot) {
        if (opt == null || patient == null || slot == null) {
            throw new IllegalArgumentException("BookingOption, patient and slot are required.");
        }
        if (opt.getMaxParticipants() > 1) {
            GroupAppointment g = new GroupAppointment(patient, slot, opt.getMaxParticipants());
            g.setParticipantCount(1);
            return g;
        }
        if (opt.isOnline()) {
            String link = AppConfig.getOnlineLocationLabel();
            return new VirtualAppointment(patient, slot, link);
        }
        String name = opt.getServiceName();
        if (matchesConsultation(name)) {
            return new AssessmentAppointment(patient, slot);
        }
        if (matchesUrgent(name)) {
            return new UrgentAppointment(patient, slot);
        }
        if (matchesFollowUp(name)) {
            return new FollowUpAppointment(patient, slot);
        }
        return new InPersonAppointment(patient, slot, AppConfig.getOnsiteLocationLabel());
    }

    private static boolean matchesConsultation(String serviceName) {
        if (serviceName == null) return false;
        String name = serviceName.trim();
        String[] types = AppConfig.getBookingAppointmentTypes();
        int ci = AppConfig.getBookingConsultationTypeIndex();
        return types != null && ci >= 0 && ci < types.length && types[ci].trim().equalsIgnoreCase(name);
    }

    private static boolean matchesFollowUp(String serviceName) {
        if (serviceName == null) return false;
        String name = serviceName.trim();
        String[] types = AppConfig.getBookingAppointmentTypes();
        int fi = AppConfig.getBookingFollowUpTypeIndex();
        if (types != null && fi >= 0 && fi < types.length && types[fi].trim().equalsIgnoreCase(name)) {
            return true;
        }
        String lower = name.toLowerCase();
        if (fi < 0) {
            return lower.contains("follow") || lower.contains("return");
        }
        return false;
    }

    private static boolean matchesUrgent(String serviceName) {
        if (serviceName == null) return false;
        String lower = serviceName.toLowerCase();
        return lower.contains("urgent") || lower.contains("express") || lower.contains("priority");
    }
}
