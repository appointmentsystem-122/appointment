package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.presentation.notification.AppNotification;
import com.appointmentscheduler.presentation.notification.NotificationPriority;
import com.appointmentscheduler.presentation.notification.NotificationType;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Dense branch coverage for smaller presentation types to lift overall package branch %.
 */
@ResourceLock("ApplicationContextServices")
class PresentationBranchLiftBatchTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void restoreClosedDayService() {
        ApplicationContext.setClosedDayService(null);
    }

    @Test
    void appNotification_constructors_coalesceNulls_andShortOverload() {
        AppNotification a1 = new AppNotification(null, "t", "m");
        assertThat(a1.getType()).isEqualTo(NotificationType.INFO);
        assertThat(a1.getPriority()).isEqualTo(NotificationPriority.NORMAL);

        AppNotification a2 = new AppNotification(NotificationType.WARNING, null, null, null, null, null);
        assertThat(a2.getTitle()).isEmpty();
        assertThat(a2.getMessage()).isEmpty();
        assertThat(a2.getEntityType()).isNull();
        assertThat(a2.getEntityId()).isNull();

        AppNotification a3 = new AppNotification(NotificationType.SYSTEM, NotificationPriority.HIGH, "x", "y");
        assertThat(a3.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(a3.getTimeFormatted()).matches("\\d{2}:\\d{2}");
        assertThat(a3.getDateTimeFormatted()).matches("\\d{2}/\\d{2} \\d{2}:\\d{2}");
    }

    @Test
    void appNotification_equals_and_hashCode_useId() {
        AppNotification a = new AppNotification(NotificationType.INFO, "t", "m");
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(new AppNotification(NotificationType.INFO, "t", "m"));
        assertThat(a).isNotEqualTo("not-a-notification");
        assertThat(a).isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(Objects.hash(a.getId()));
    }

    @Test
    void bookingDateMessages_nullDate_orNullClosedDayService_usesNoSlots() {
        ApplicationContext.setClosedDayService(null);
        assertThat(BookingDateMessages.unavailable(null)).isEqualTo(I18n.get("booking.day_no_slots"));

        LocalDate d = LocalDate.of(2031, 1, 5);
        assertThat(BookingDateMessages.unavailable(d)).isEqualTo(I18n.get("booking.day_no_slots"));
    }

    @Test
    void bookingExtrasUi_configureEarlyReturn_whenItemsAlreadyPopulated() {
        ComboBox<String> reminder = new ComboBox<>();
        reminder.getItems().add(BookingRequestFields.REMINDER_SMS);
        BookingExtrasUi.configureReminderCombo(reminder);
        assertThat(reminder.getItems()).hasSize(1);

        ComboBox<String> lang = new ComboBox<>();
        lang.getItems().add(BookingRequestFields.LANG_EN);
        BookingExtrasUi.configureLanguageCombo(lang);
        assertThat(lang.getItems()).hasSize(1);
    }

    @Test
    void bookingExtrasUi_stringConverters_roundTripLabels() {
        ComboBox<String> reminder = new ComboBox<>();
        BookingExtrasUi.configureReminderCombo(reminder);
        StringConverter<String> rc = (StringConverter<String>) reminder.getConverter();
        assertThat(rc.toString(BookingRequestFields.REMINDER_SMS)).contains("SMS");
        assertThat(rc.fromString("ignored")).isEqualTo(BookingRequestFields.REMINDER_APP);

        ComboBox<String> lang = new ComboBox<>();
        BookingExtrasUi.configureLanguageCombo(lang);
        StringConverter<String> lc = (StringConverter<String>) lang.getConverter();
        assertThat(lc.toString(BookingRequestFields.LANG_AR)).contains("Arabic");
        assertThat(lc.fromString("ignored")).isEqualTo(BookingRequestFields.LANG_ANY);
    }

    @Test
    void bookingExtrasUi_updatePartySpinner_maxOne_setsTooltipAndNotEditable() {
        javafx.scene.control.Spinner<Integer> sp = new javafx.scene.control.Spinner<>(1, 5, 3);
        BookingExtrasUi.updatePartySpinner(sp, 1);
        assertThat(sp.isEditable()).isFalse();
        assertThat(sp.getTooltip()).isNotNull();
        assertThat(((SpinnerValueFactory.IntegerSpinnerValueFactory) sp.getValueFactory()).getMax()).isEqualTo(1);
    }

    @Test
    void appNotificationStore_getRecent_whenSizeEqualsMax_usesFullCopyBranch() {
        AppNotificationStore s = new AppNotificationStore();
        s.add("1", "a");
        s.add("2", "b");
        s.add("3", "c");
        assertThat(s.getRecent(3)).hasSize(3);
        assertThat(s.getRecent(3).get(0).getTitle()).isEqualTo("1");
    }

    @Test
    void loadingSpinnerOverlay_doubleAttach_doesNotDuplicate() {
        LoadingSpinnerOverlay o = new LoadingSpinnerOverlay();
        StackPane root = new StackPane();
        o.attachTo(root);
        o.attachTo(root);
        assertThat(root.getChildren()).hasSize(1);
        o.show();
        o.hide();
    }

    @Test
    void calendarViewComponent_applyAppointmentEventStyle_allStatusBranches() {
        User u = new User("u-cal", "Cal", "cal@e.com", "pw");
        LocalDateTime t = LocalDateTime.of(2032, 4, 5, 10, 0);
        TimeSlot slot = new TimeSlot(t, t.plusHours(1));

        VBox b1 = new VBox();
        InPersonAppointment cancelled = new InPersonAppointment("c1", u, slot, "L");
        cancelled.setStatus("CANCELLED");
        CalendarViewComponent.applyAppointmentEventStyle(b1, cancelled);
        assertThat(b1.getStyle()).contains("#94a3b8");

        VBox b2 = new VBox();
        InPersonAppointment expired = new InPersonAppointment("e1", u, slot, "L");
        expired.setStatus("EXPIRED");
        CalendarViewComponent.applyAppointmentEventStyle(b2, expired);
        assertThat(b2.getStyle()).contains("#94a3b8");

        VBox b3 = new VBox();
        UrgentAppointment urg = new UrgentAppointment("u1", u, slot);
        urg.setStatus("CONFIRMED");
        CalendarViewComponent.applyAppointmentEventStyle(b3, urg);
        assertThat(b3.getStyle()).contains("#dc2626");

        VBox b4 = new VBox();
        AssessmentAppointment asmt = new AssessmentAppointment("a1", u, slot);
        asmt.setStatus("CONFIRMED");
        CalendarViewComponent.applyAppointmentEventStyle(b4, asmt);
        assertThat(b4.getStyle()).contains("#d97706");

        VBox b5 = new VBox();
        InPersonAppointment conf = new InPersonAppointment("ok", u, slot, "L");
        conf.setStatus("CONFIRMED");
        CalendarViewComponent.applyAppointmentEventStyle(b5, conf);
        assertThat(b5.getStyle()).contains("#059669");

        VBox b6 = new VBox();
        InPersonAppointment pend = new InPersonAppointment("p1", u, slot, "L");
        pend.setStatus("PENDING");
        CalendarViewComponent.applyAppointmentEventStyle(b6, pend);
        assertThat(b6.getStyle() == null || b6.getStyle().isEmpty()).isTrue();

        CalendarViewComponent.applyAppointmentEventStyle(null, conf);
        CalendarViewComponent.applyAppointmentEventStyle(new VBox(), null);
    }

    @Test
    void screenConstants_titles_nonBlank() {
        try (MockedStatic<com.appointmentscheduler.application.AppConfig> cfg =
                     mockStatic(com.appointmentscheduler.application.AppConfig.class)) {
            cfg.when(com.appointmentscheduler.application.AppConfig::getAppName).thenReturn("LiftTestApp");
            assertThat(ScreenConstants.titleLogin()).contains("LiftTestApp");
            assertThat(ScreenConstants.titleAdminDashboard()).contains("LiftTestApp");
            assertThat(ScreenConstants.titlePatientDashboard()).contains("LiftTestApp");
            assertThat(ScreenConstants.titleBookAppointment()).contains("LiftTestApp");
            assertThat(ScreenConstants.titleModifyAppointment()).contains("LiftTestApp");
        }
    }
}
