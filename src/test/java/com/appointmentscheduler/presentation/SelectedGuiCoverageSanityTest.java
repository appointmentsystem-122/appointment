package com.appointmentscheduler.presentation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.application.ClosedDayService;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.presentation.notification.AppNotification;
import com.appointmentscheduler.presentation.notification.NotificationPriority;
import com.appointmentscheduler.presentation.notification.NotificationType;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;

/**
 * Small, stable coverage probes for the GUI files that SonarCloud reported as uncovered.
 */
class SelectedGuiCoverageSanityTest {

    @BeforeAll
    static void startFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void appNotification_defaults_getters_formatters_andReadState() {
        AppNotification n = new AppNotification(null, null, null, null, "appointment", "42");

        assertThat(n.getId()).isNotBlank();
        assertThat(n.getType()).isEqualTo(NotificationType.INFO);
        assertThat(n.getPriority()).isEqualTo(NotificationPriority.NORMAL);
        assertThat(n.getTitle()).isEmpty();
        assertThat(n.getMessage()).isEmpty();
        assertThat(n.getEntityType()).isEqualTo("appointment");
        assertThat(n.getEntityId()).isEqualTo("42");
        assertThat(n.getAt()).isNotNull();
        assertThat(n.getTimeFormatted()).contains(":");
        assertThat(n.getDateTimeFormatted()).contains("/");

        assertThat(n.isRead()).isFalse();

        n.setRead(true);

        assertThat(n.isRead()).isTrue();
        assertThat(n.equals(n)).isTrue();

        Object other = new Object();
        assertThat(n.equals(other)).isFalse();

        int initialHashCode = n.hashCode();
        assertThat(initialHashCode).isEqualTo(n.hashCode());
    }

    @Test
    void bookingDateMessages_coversNullOpenAndClosedDayBranches() {
        ApplicationContext.setClosedDayService(null);

        assertThat(BookingDateMessages.unavailable(null)).isNotBlank();
        assertThat(BookingDateMessages.unavailable(LocalDate.of(2026, 1, 5))).isNotBlank();

        ClosedDayService closedDayService = new ClosedDayService();
        LocalDate closed = LocalDate.of(2026, 1, 6);
        closedDayService.addClosedDay(closed);
        ApplicationContext.setClosedDayService(closedDayService);

        assertThat(BookingDateMessages.unavailable(closed)).isNotBlank();
    }

    @Test
    void bookingExtrasUi_coversComboLabelsAndSpinnerBounds() {
        ComboBox<String> reminder = new ComboBox<>();
        BookingExtrasUi.configureReminderCombo(reminder);

        assertThat(reminder.getItems()).contains(
                BookingRequestFields.REMINDER_APP,
                BookingRequestFields.REMINDER_SMS,
                BookingRequestFields.REMINDER_NONE
        );
        assertThat(reminder.getConverter().toString(BookingRequestFields.REMINDER_SMS)).contains("SMS");
        assertThat(reminder.getConverter().fromString("ignored")).isEqualTo(BookingRequestFields.REMINDER_APP);

        ComboBox<String> language = new ComboBox<>();
        BookingExtrasUi.configureLanguageCombo(language);

        assertThat(language.getItems()).contains(
                BookingRequestFields.LANG_ANY,
                BookingRequestFields.LANG_AR,
                BookingRequestFields.LANG_EN
        );
        assertThat(language.getConverter().toString(BookingRequestFields.LANG_EN)).contains("English");
        assertThat(language.getConverter().fromString("ignored")).isEqualTo(BookingRequestFields.LANG_ANY);

        Spinner<Integer> spinner = new Spinner<>();
        BookingExtrasUi.updatePartySpinner(spinner, 1);

        assertThat(spinner.getValue()).isEqualTo(1);
        assertThat(spinner.getTooltip()).isNotNull();
        assertThat(spinner.isEditable()).isFalse();

        BookingExtrasUi.updatePartySpinner(spinner, 4);

        assertThat(spinner.getValue()).isBetween(1, 4);
        assertThat(spinner.getTooltip()).isNull();
        assertThat(spinner.isEditable()).isTrue();
    }

    @Test
    void bookAppointmentPrivateUiStateMethods_coverMessageAndLoadingBranches() throws Exception {
        BookAppointmentController c = new BookAppointmentController();

        Label message = new Label();
        ComboBox<TimeSlot> slots = new ComboBox<>();
        Button confirm = new Button();
        DatePicker datePicker = new DatePicker();
        ComboBox<Object> typeCombo = new ComboBox<>();

        setField(c, "messageLabel", message);
        setField(c, "timeSlotCombo", slots);
        setField(c, "btnConfirmBooking", confirm);
        setField(c, "datePicker", datePicker);
        setField(c, "typeCombo", typeCombo);

        invoke(c, "showMessage", new Class<?>[]{String.class, boolean.class}, "Bad input", true);

        assertThat(message.getText()).isEqualTo("Bad input");
        assertThat(message.getStyleClass()).contains("error-label");

        invoke(c, "showMessage", new Class<?>[]{String.class, boolean.class}, "Saved", false);

        assertThat(message.getStyleClass()).contains("success-label");

        invoke(c, "showLoadingState", new Class<?>[]{boolean.class}, true);

        assertThat(slots.isDisabled()).isTrue();
        assertThat(confirm.isDisabled()).isTrue();
        assertThat(message.getStyleClass()).contains("info-label");

        invoke(c, "showLoadingState", new Class<?>[]{boolean.class}, false);

        assertThat(slots.isDisabled()).isFalse();
    }

    @Test
    void adminDashboardPrivateHelpers_coverKpiTrendAndRequestSummaryBranches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();

        Label rate = new Label();
        ProgressBar progress = new ProgressBar();
        Label status = new Label();

        setField(c, "cancellationRateLabel", rate);
        setField(c, "cancellationRateProgressBar", progress);
        setField(c, "cancellationRateStatusLabel", status);

        for (double value : new double[]{Double.NaN, 3.0, 7.0, 12.0, 17.0, 25.0, 120.0}) {
            invoke(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, value);

            assertThat(rate.getText()).endsWith("%");
            assertThat(progress.getProgress()).isBetween(0.0, 1.0);
            assertThat(status.getText()).isNotBlank();
        }

        assertThat((String) invokeStatic(
                "formatTrend",
                new Class<?>[]{long.class, long.class, String.class},
                0L,
                0L,
                "yesterday"
        )).isEmpty();

        assertThat((String) invokeStatic(
                "formatTrend",
                new Class<?>[]{long.class, long.class, String.class},
                0L,
                5L,
                "yesterday"
        )).startsWith("↑");

        assertThat((String) invokeStatic(
                "formatTrend",
                new Class<?>[]{long.class, long.class, String.class},
                10L,
                10L,
                "last week"
        )).startsWith("→");

        assertThat((String) invokeStatic(
                "formatTrend",
                new Class<?>[]{long.class, long.class, String.class},
                10L,
                5L,
                "last week"
        )).startsWith("↓");

        assertThat((String) invokeStatic(
                "summarizeBookingRequests",
                new Class<?>[]{com.appointmentscheduler.domain.Appointment.class},
                new Object[]{null}
        )).isEmpty();

        InPersonAppointment appt = new InPersonAppointment(
                new User("p1", "Patient One", "p1@example.com", "pw"),
                new TimeSlot(
                        LocalDateTime.of(2026, 2, 1, 9, 0),
                        LocalDateTime.of(2026, 2, 1, 10, 0)
                ),
                "Room 1"
        );

        appt.setCustomerNotes(" Needs quiet room ");
        appt.setContactPhone(" 0599000000 ");
        appt.setReminderChannel(BookingRequestFields.REMINDER_SMS);
        appt.setPreferredLanguage(BookingRequestFields.LANG_AR);
        appt.setAccessibilityNeeds(" Wheelchair access ");
        appt.setParticipantCount(3);

        String summary = (String) invokeStatic(
                "summarizeBookingRequests",
                new Class<?>[]{com.appointmentscheduler.domain.Appointment.class},
                appt
        );

        assertThat(summary).contains(
                "Needs quiet room",
                "0599000000",
                "SMS",
                "العربية",
                "Wheelchair",
                "Party: 3"
        );
    }

    @Test
    void nullInputsRemainNoOpsForSelectedGuiHelpers() {
        assertThatCode(() -> BookingExtrasUi.configureReminderCombo(null)).doesNotThrowAnyException();
        assertThatCode(() -> BookingExtrasUi.configureLanguageCombo(null)).doesNotThrowAnyException();
        assertThatCode(() -> BookingExtrasUi.updatePartySpinner(null, 10)).doesNotThrowAnyException();

        assertThat(BookingExtrasUi.reminderChannelLabel(null)).isEmpty();
        assertThat(BookingExtrasUi.preferredLanguageLabel(null)).isEmpty();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Object invokeStatic(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }
}