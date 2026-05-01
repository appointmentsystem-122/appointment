package com.appointmentscheduler.domain;

/**
 * Represents a virtual appointment.
 */
public class VirtualAppointment extends Appointment {
    private String meetingLink;

    public VirtualAppointment(User patient, TimeSlot timeSlot, String meetingLink) {
        super(patient, timeSlot);
        this.meetingLink = meetingLink;
    }

    public VirtualAppointment(String id, User patient, TimeSlot timeSlot, String meetingLink) {
        super(id, patient, timeSlot);
        this.meetingLink = meetingLink;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }
}
