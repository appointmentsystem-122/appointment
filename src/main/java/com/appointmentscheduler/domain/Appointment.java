package com.appointmentscheduler.domain;

import com.appointmentscheduler.domain.notifiers.Observer;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the base abstract class for all Appointments in the system.
 * Supports soft delete and audit trail.
 */
public abstract class Appointment {
    private final String id;
    private final User patient;
    private TimeSlot timeSlot;
    private String status; // "PENDING", "CONFIRMED", "CANCELLED", "EXPIRED", "COMPLETED"
    private int participantCount;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private String doctorId;
    private String roomId;
    private String clinicId;
    private boolean urgent;

    /** Optional: customer-facing notes / special requests (persisted). */
    private String customerNotes;
    /** Day-of contact phone (may differ from profile). */
    private String contactPhone;
    /** Reminder channel: APP, EMAIL, SMS, NONE. */
    private String reminderChannel;
    /** Accessibility or assistance needs (short text). */
    private String accessibilityNeeds;
    /** Preferred service language: AR, EN, ANY. */
    private String preferredLanguage;

    /**
     * Constructs a base appointment (new entity).
     */
    /**
     * Creates a new appointment for the supplied patient and time slot.
     *
     * @param patient patient who owns the appointment
     * @param timeSlot scheduled slot for the appointment
     */
    public Appointment(User patient, TimeSlot timeSlot) {
        if (patient == null) throw new IllegalArgumentException("Patient cannot be null");
        if (timeSlot == null) throw new IllegalArgumentException("TimeSlot cannot be null");
        this.id = UUID.randomUUID().toString();
        this.patient = patient;
        this.timeSlot = timeSlot;
        this.status = "PENDING";
        this.participantCount = 1;
    }

    /**
     * Reconstitution constructor for persistence (load from DB).
     */
    protected Appointment(String id, User patient, TimeSlot timeSlot) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID cannot be null or empty");
        if (patient == null) throw new IllegalArgumentException("Patient cannot be null");
        if (timeSlot == null) throw new IllegalArgumentException("TimeSlot cannot be null");
        this.id = id;
        this.patient = patient;
        this.timeSlot = timeSlot;
        this.status = "PENDING";
        this.participantCount = 1;
    }

    /** For persistence: restore soft-delete state when loading from DB. */
    /**
     * Restores the soft-delete state when rehydrating an appointment from persistence.
     *
     * @param deleted whether the appointment has been soft deleted
     * @param deletedAt timestamp at which the delete occurred
     * @param deletedBy identifier of the user who performed the deletion
     */
    public void setDeletedState(boolean deleted, LocalDateTime deletedAt, String deletedBy) {
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    /**
     * Get unique ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the patient/user.
     */
    public User getPatient() {
        return patient;
    }

    /**
     * Get time slot.
     */
    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    /**
     * Set the time slot (modification).
     */
    public void setTimeSlot(TimeSlot timeSlot) {
        if (timeSlot == null) throw new IllegalArgumentException("TimeSlot cannot be null");
        this.timeSlot = timeSlot;
    }

    /**
     * Get the current status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Update the status.
     * @param status the new status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get participant count.
     */
    public int getParticipantCount() {
        return participantCount;
    }

    /**
     * Set participant count.
     */
    public void setParticipantCount(int participantCount) {
        if (participantCount < 1) throw new IllegalArgumentException("Participant count must be at least 1");
        this.participantCount = participantCount;
    }

    /** Soft delete flag. */
    public boolean isDeleted() {
        return deleted;
    }

    /** Mark as soft-deleted. */
    /**
     * Soft-deletes the appointment while recording who performed the action and when it happened.
     *
     * @param deletedByUserId identifier of the actor requesting the delete
     */
    public void markDeleted(String deletedByUserId) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUserId;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    /** Optional: ID of the assigned doctor. */
    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    /** Optional: ID of the assigned room. */
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    /** Optional: ID of the clinic/branch. */
    public String getClinicId() { return clinicId; }
    public void setClinicId(String clinicId) { this.clinicId = clinicId; }

    /** Whether this appointment is marked urgent. */
    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getReminderChannel() {
        return reminderChannel;
    }

    public void setReminderChannel(String reminderChannel) {
        this.reminderChannel = reminderChannel;
    }

    public String getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    /**
     * Sends an appointment reminder via the given observer only when the appointment start is in the future.
     */
    /**
     * Sends a reminder through the provided observer only when the appointment starts in the future.
     *
     * @param observer observer implementation responsible for dispatching the reminder
     */
    public void sendReminder(Observer observer) {
        Objects.requireNonNull(observer, "observer");
        LocalDateTime start = timeSlot.getStartTime();
        if (!start.isAfter(LocalDateTime.now())) {
            return;
        }
        String message = "Reminder: You have an appointment at " + start;
        observer.notify(patient, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
