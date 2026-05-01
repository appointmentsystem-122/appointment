package com.appointmentscheduler.application;

/**
 * One bookable choice: a configured service type plus delivery mode (online / on-site),
 * with duration and capacity from {@link AppointmentTypeConfig}.
 */
public final class BookingOption {

    private final String serviceName;
    private final boolean online;
    private final int durationMinutes;
    private final int maxParticipants;
    private final String id;

    private BookingOption(String serviceName, boolean online, int durationMinutes, int maxParticipants) {
        this.serviceName = serviceName != null ? serviceName.trim() : "General";
        this.online = online;
        this.durationMinutes = Math.max(15, durationMinutes);
        this.maxParticipants = Math.max(1, maxParticipants);
        this.id = this.serviceName.toLowerCase() + "|" + (online ? "1" : "0") + "|" + this.durationMinutes + "|" + this.maxParticipants;
    }

    public static BookingOption of(AppointmentTypeConfig.Type t, boolean online) {
        return new BookingOption(t.getName(), online, t.getDurationMinutes(), t.getMaxParticipants());
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isOnline() {
        return online;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public String getId() {
        return id;
    }

    /**
     * Bilingual summary for combo boxes (neutral wording — any business, not clinic-only).
     */
    public String getDisplayLabel() {
        String mode = online
            ? "Remote / عن بُعد"
            : "In person / حضوري";
        return serviceName + " · " + mode + " · " + durationMinutes + " min";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingOption that = (BookingOption) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
