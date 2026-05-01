package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;
import com.appointmentscheduler.presentation.notification.NotificationCenter;
import com.appointmentscheduler.presentation.notification.NotificationPriority;
import com.appointmentscheduler.presentation.notification.NotificationType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Broadcasts closed/reopened day announcements to in-app notifications and to all
 * non-admin users via the observer {@link NotificationService} (email/calendar hooks).
 */
public final class ClosedDayBroadcast {

    private static final DateTimeFormatter PRETTY_EN = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH);

    private ClosedDayBroadcast() {
    }

    /**
     * Publishes a high-priority system notification and notifies each patient user.
     */
    public static void broadcastDayClosed(LocalDate d) {
        if (d == null) return;
        String iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String pretty = d.format(PRETTY_EN);
        String title = "يوم مغلق · Closed day";
        String message = "تم إغلاق يوم " + pretty + " (" + iso + "). لا توجد مواعيد حجز في هذا اليوم. · "
            + "This date is closed for bookings. No appointment slots are available.";
        NotificationCenter.getInstance().notify(
            NotificationType.SYSTEM,
            NotificationPriority.HIGH,
            title,
            message,
            "ClosedDay",
            iso
        );
        pushToAllPatients(
            "تم إغلاق يوم " + iso + " — لا حجوزات في هذا اليوم. / Closed day " + iso + " — no bookings."
        );
    }

    /**
     * Announces that a date is open again for booking.
     */
    public static void broadcastDayReopened(LocalDate d) {
        if (d == null) return;
        String iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String pretty = d.format(PRETTY_EN);
        String title = "تم فتح اليوم · Day reopened";
        String message = "أصبح يوم " + pretty + " (" + iso + ") متاحاً للحجز مجدداً. · "
            + "This date is open for booking again.";
        NotificationCenter.getInstance().notify(
            NotificationType.SYSTEM,
            NotificationPriority.NORMAL,
            title,
            message,
            "ClosedDay",
            iso
        );
        pushToAllPatients(
            "تم فتح يوم " + iso + " للحجز. / Day " + iso + " is open for booking again."
        );
    }

    private static void pushToAllPatients(String observerMessage) {
        NotificationService ns = ApplicationContext.getNotificationService();
        AuthService auth = ApplicationContext.getAuthService();
        if (ns == null || auth == null) return;
        UserRepository ur = auth.getUserRepository();
        if (ur == null) return;
        for (User u : ur.getAllUsers()) {
            if (u != null && !u.isAdmin()) {
                ns.notifyAllObservers(u, observerMessage);
            }
        }
    }
}
