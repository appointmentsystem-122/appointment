package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.application.ClosedDayService;
import com.appointmentscheduler.presentation.notification.AppNotification;
import com.appointmentscheduler.presentation.notification.NotificationPriority;
import com.appointmentscheduler.presentation.notification.NotificationType;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Additional tests for the smaller Sonar-selected files shown in the screenshot. */
class SelectedSmallFilesCoverageTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void resetClosedDayService() {
        ApplicationContext.setClosedDayService(null);
    }

    @Test
    void appNotification_allConstructorsAndGetters_areCovered() {
        AppNotification basic = new AppNotification(NotificationType.WARNING, "Title", "Message");
        assertThatCode(() -> UUID.fromString(basic.getId())).doesNotThrowAnyException();
        assertThat(basic.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(basic.getPriority()).isEqualTo(NotificationPriority.NORMAL);
        assertThat(basic.getTitle()).isEqualTo("Title");
        assertThat(basic.getMessage()).isEqualTo("Message");
        assertThat(basic.getAt()).isNotNull();
        assertThat(basic.getEntityType()).isNull();
        assertThat(basic.getEntityId()).isNull();

        AppNotification full = new AppNotification(null, null, null, null, "appointment", "a-1");
        assertThat(full.getType()).isEqualTo(NotificationType.INFO);
        assertThat(full.getPriority()).isEqualTo(NotificationPriority.NORMAL);
        assertThat(full.getTitle()).isEmpty();
        assertThat(full.getMessage()).isEmpty();
        assertThat(full.getEntityType()).isEqualTo("appointment");
        assertThat(full.getEntityId()).isEqualTo("a-1");
        assertThat(full.getTimeFormatted()).matches("\\d{2}:\\d{2}");
        assertThat(full.getDateTimeFormatted()).matches("\\d{2}/\\d{2} \\d{2}:\\d{2}");

        full.setRead(true);
        assertThat(full.isRead()).isTrue();
        assertThat(full).isEqualTo(full);
        assertThat(full).isNotEqualTo(basic);
        assertThat(full).isNotEqualTo("not-a-notification");
        assertThat(full.hashCode()).isEqualTo(full.hashCode());
    }

    @Test
    void bookingDateMessages_returnsClosedMessageOnlyForClosedDay() {
        LocalDate closed = LocalDate.now().plusDays(5);
        ClosedDayService closedDayService = new ClosedDayService();
        closedDayService.addClosedDay(closed);
        ApplicationContext.setClosedDayService(closedDayService);

        String closedMessage = BookingDateMessages.unavailable(closed);
        String openMessage = BookingDateMessages.unavailable(closed.plusDays(1));
        String nullMessage = BookingDateMessages.unavailable(null);

        assertThat(closedMessage).isNotBlank();
        assertThat(openMessage).isNotBlank();
        assertThat(nullMessage).isNotBlank();
        assertThat(closedMessage).isNotEqualTo(openMessage);
    }

    @Test
    void bookingExtrasUi_reconfigurationSkipsExistingItemsAndLabelsUnknowns() {
        ComboBox<String> reminder = new ComboBox<>();
        reminder.getItems().add("existing");
        BookingExtrasUi.configureReminderCombo(reminder);
        assertThat(reminder.getItems()).containsExactly("existing");

        ComboBox<String> language = new ComboBox<>();
        BookingExtrasUi.configureLanguageCombo(language);
        assertThat(language.getItems()).containsExactly(
                BookingRequestFields.LANG_ANY,
                BookingRequestFields.LANG_AR,
                BookingRequestFields.LANG_EN);
        assertThat(language.getConverter().toString(null)).isEmpty();
        assertThat(language.getConverter().toString("unexpected")).contains("No preference");
        assertThat(language.getConverter().fromString("Arabic")).isEqualTo(BookingRequestFields.LANG_ANY);

        assertThat(BookingExtrasUi.reminderChannelLabel(null)).isEmpty();
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_EMAIL)).contains("Email");
        assertThat(BookingExtrasUi.reminderChannelLabel("unexpected")).contains("App");
    }

    @Test
    void updatePartySpinner_boundsExistingValueAndSwitchesTooltip() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 9));

        BookingExtrasUi.updatePartySpinner(spinner, 4);
        assertThat(spinner.getValue()).isEqualTo(4);
        assertThat(spinner.isEditable()).isTrue();
        assertThat(spinner.getTooltip()).isNull();

        BookingExtrasUi.updatePartySpinner(spinner, 0);
        assertThat(spinner.getValue()).isEqualTo(1);
        assertThat(spinner.isEditable()).isFalse();
        assertThat(spinner.getTooltip()).isNotNull();
    }
}
